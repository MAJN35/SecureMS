package com.example.data

import android.content.Context
import android.util.Base64
import com.example.crypto.CryptoEngine
import com.example.crypto.KeystoreHelper
import com.example.util.ContactHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.security.KeyFactory
import java.security.KeyPair
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.concurrent.ConcurrentHashMap

/**
 * High-level repository that transparently encrypts and decrypts sensitive message
 * content at rest using the Android Keystore master key, manages long-term identity
 * keys, and supports Direct Chats, Group Chats, and Broadcast Channels.
 */
class EncryptedStorageRepository(
    private val dao: SecureSmsDao,
    private val keystoreHelper: KeystoreHelper
) {

    @Volatile
    private var cachedKeyPair: KeyPair? = null

    private val decryptedCache = ConcurrentHashMap<Long, Pair<Int, String>>()

    suspend fun getOrCreateIdentityKeyPair(): KeyPair {
        cachedKeyPair?.let { return it }

        val existingEntity = dao.getIdentityKey()
        if (existingEntity != null) {
            try {
                val pubBytes = Base64.decode(existingEntity.publicKeyBase64, Base64.NO_WRAP)
                val pubKey = CryptoEngine.parsePublicKey(pubBytes)

                val decryptedPrivBytes = keystoreHelper.decryptWithMasterKey(existingEntity.encryptedPrivateKey)
                val privSpec = PKCS8EncodedKeySpec(decryptedPrivBytes)
                val kf = KeyFactory.getInstance("EC")
                val privKey = kf.generatePrivate(privSpec)

                val kp = KeyPair(pubKey, privKey)
                cachedKeyPair = kp
                return kp
            } catch (e: Exception) {
                // Regenerate if corrupted or Keystore invalidated
            }
        }

        val newKp = CryptoEngine.generateIdentityKeyPair()
        val pubBase64 = CryptoEngine.encodePublicKeyToBase64(newKp.public)
        val encryptedPriv = keystoreHelper.encryptWithMasterKey(newKp.private.encoded)
        val fingerprint = CryptoEngine.computeFingerprint(newKp.public)

        dao.insertOrUpdateIdentityKey(
            IdentityKeyEntity(
                id = 1,
                publicKeyBase64 = pubBase64,
                encryptedPrivateKey = encryptedPriv,
                fingerprint = fingerprint
            )
        )

        cachedKeyPair = newKp
        return newKp
    }

    suspend fun getMyPublicKey(): PublicKey {
        return getOrCreateIdentityKeyPair().public
    }

    suspend fun getMyPrivateKey(): PrivateKey {
        return getOrCreateIdentityKeyPair().private
    }

    suspend fun getMyFingerprint(): String {
        return CryptoEngine.computeFingerprint(getMyPublicKey())
    }

    suspend fun regenerateIdentityKeyPair(): KeyPair {
        val newKp = CryptoEngine.generateIdentityKeyPair()
        val pubBase64 = CryptoEngine.encodePublicKeyToBase64(newKp.public)
        val encryptedPriv = keystoreHelper.encryptWithMasterKey(newKp.private.encoded)
        val fingerprint = CryptoEngine.computeFingerprint(newKp.public)

        dao.insertOrUpdateIdentityKey(
            IdentityKeyEntity(
                id = 1,
                publicKeyBase64 = pubBase64,
                encryptedPrivateKey = encryptedPriv,
                fingerprint = fingerprint
            )
        )

        cachedKeyPair = newKp

        try {
            val all = dao.getAllConversationsList()
            for (conv in all) {
                if (!conv.theirPublicKeyBase64.isNullOrBlank()) {
                    val theirPub = CryptoEngine.parsePublicKeyFromBase64(conv.theirPublicKeyBase64)
                    val newSafetyNumber = CryptoEngine.computeSafetyNumber(newKp.public, theirPub)
                    dao.updateConversation(conv.copy(safetyNumber = newSafetyNumber))
                }
            }
        } catch (_: Exception) {}

        return newKp
    }

    fun getAllConversationsFlow(): Flow<List<ConversationEntity>> {
        return dao.getAllConversationsFlow()
    }

    fun getConversationFlow(id: Long): Flow<ConversationEntity?> {
        return dao.getConversationFlow(id)
    }

    suspend fun getConversationByPhone(phone: String): ConversationEntity? {
        if (phone.startsWith("GROUP:") || phone.startsWith("CHANNEL:")) {
            return dao.getConversationByPhone(phone)
        }
        val normalized = normalizePhoneNumber(phone)
        val directMatch = dao.getConversationByPhone(normalized)
        if (directMatch != null) return directMatch

        val all = dao.getAllConversationsList()
        for (conv in all) {
            if (conv.isDirect && ContactHelper.isSamePhoneNumber(conv.phoneNumber, phone)) {
                return conv
            }
        }
        return null
    }

    suspend fun getConversationById(id: Long): ConversationEntity? {
        return dao.getConversationById(id)
    }

    suspend fun createOrUpdateConversation(
        phoneNumber: String,
        contactName: String,
        theirPublicKey: PublicKey? = null,
        preferredEncoding: String = "AUTOMATIC",
        avatarUri: String? = null
    ): ConversationEntity {
        val normalized = if (phoneNumber.startsWith("GROUP:") || phoneNumber.startsWith("CHANNEL:")) {
            phoneNumber
        } else {
            normalizePhoneNumber(phoneNumber)
        }
        var existing = dao.getConversationByPhone(normalized)
        if (existing == null && !normalized.startsWith("GROUP:") && !normalized.startsWith("CHANNEL:")) {
            val all = dao.getAllConversationsList()
            for (conv in all) {
                if (conv.isDirect && ContactHelper.isSamePhoneNumber(conv.phoneNumber, phoneNumber)) {
                    existing = conv
                    break
                }
            }
        }

        val myPub = getMyPublicKey()
        var safetyNumber = ""
        var status = VerificationStatus.UNVERIFIED
        var pubBase64: String? = null

        if (theirPublicKey != null) {
            pubBase64 = CryptoEngine.encodePublicKeyToBase64(theirPublicKey)
            safetyNumber = CryptoEngine.computeSafetyNumber(myPub, theirPublicKey)

            if (existing?.theirPublicKeyBase64 != null && existing.theirPublicKeyBase64 != pubBase64) {
                status = VerificationStatus.IDENTITY_CHANGED
            } else if (existing != null) {
                status = existing.verificationStatus
            }
        } else if (existing != null) {
            pubBase64 = existing.theirPublicKeyBase64
            safetyNumber = existing.safetyNumber
            status = existing.verificationStatus
        }

        val finalName = when {
            contactName.isNotBlank() && contactName != phoneNumber && contactName != normalized -> contactName
            existing != null && existing.contactName.isNotBlank() && existing.contactName != existing.phoneNumber -> existing.contactName
            else -> normalized
        }

        val finalAvatarUri = when {
            !avatarUri.isNullOrBlank() -> avatarUri
            existing != null && existing.avatarUri.isNotBlank() -> existing.avatarUri
            else -> ""
        }

        val entity = ConversationEntity(
            id = existing?.id ?: 0,
            phoneNumber = existing?.phoneNumber ?: normalized,
            contactName = finalName,
            theirPublicKeyBase64 = pubBase64,
            safetyNumber = safetyNumber,
            verificationStatus = status,
            lastMessageText = existing?.lastMessageText ?: "",
            lastMessageTimestamp = existing?.lastMessageTimestamp ?: System.currentTimeMillis(),
            unreadCount = existing?.unreadCount ?: 0,
            preferredEncoding = preferredEncoding,
            nextOutboundSeq = existing?.nextOutboundSeq ?: 1,
            highestInboundSeq = existing?.highestInboundSeq ?: 0,
            sendReadReceipts = existing?.sendReadReceipts ?: false,
            lastSeenAckPayloadId = existing?.lastSeenAckPayloadId ?: 0,
            conversationType = existing?.conversationType ?: ConversationType.DIRECT.name,
            memberPhones = existing?.memberPhones ?: "",
            memberNames = existing?.memberNames ?: "",
            description = existing?.description ?: "",
            isOwner = existing?.isOwner ?: true,
            avatarUri = finalAvatarUri
        )

        val id = dao.insertConversation(entity)
        return dao.getConversationById(id) ?: entity.copy(id = id)
    }

    /**
     * Creates a new Group Chat or Broadcast Channel with a dedicated cryptographic identity keypair
     * and member list so messages can be encrypted and fanned out over SMS.
     */
    suspend fun createGroupOrChannelConversation(
        title: String,
        description: String,
        type: ConversationType,
        members: List<Pair<String, String>>, // List of Pair(displayName, phoneNumber)
        preferredEncoding: String = "PERSIAN_LETTERS",
        customCode: String? = null,
        isOwner: Boolean = true,
        avatarUri: String = ""
    ): ConversationEntity {
        val code = customCode ?: ((System.currentTimeMillis() % 900000L) + 100000L).toString()
        val identifier = "${type.name}:$code"
        val existing = dao.getConversationByPhone(identifier)

        val myPub = getMyPublicKey()
        // Generate or preserve a deterministic/dedicated room public key so group/channel encryption works seamlessly
        val roomKeyPair = CryptoEngine.generateIdentityKeyPair()
        val pubBase64 = existing?.theirPublicKeyBase64 ?: CryptoEngine.encodePublicKeyToBase64(roomKeyPair.public)
        val safetyNumber = existing?.safetyNumber?.takeIf { it.isNotBlank() }
            ?: CryptoEngine.computeSafetyNumber(myPub, roomKeyPair.public)

        val normalizedMembers = members
            .map { Pair(it.first.trim().ifBlank { it.second.trim() }, normalizePhoneNumber(it.second)) }
            .filter { it.second.isNotBlank() }
            .distinctBy { it.second }

        val phonesCsv = normalizedMembers.joinToString(",") { it.second }
        val namesCsv = normalizedMembers.joinToString(",") { it.first.replace(",", " ") }

        val welcomeMsg = if (type == ConversationType.CHANNEL) {
            "📢 کانال «$title» ایجاد شد (${normalizedMembers.size} مشترک)"
        } else {
            "👥 گروه «$title» ایجاد شد (${normalizedMembers.size} عضو)"
        }

        val entity = ConversationEntity(
            id = existing?.id ?: 0,
            phoneNumber = identifier,
            contactName = title.trim(),
            theirPublicKeyBase64 = pubBase64,
            safetyNumber = safetyNumber,
            verificationStatus = VerificationStatus.VERIFIED,
            lastMessageText = existing?.lastMessageText?.takeIf { it.isNotBlank() } ?: welcomeMsg,
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = existing?.unreadCount ?: 0,
            preferredEncoding = preferredEncoding,
            nextOutboundSeq = existing?.nextOutboundSeq ?: 1,
            highestInboundSeq = existing?.highestInboundSeq ?: 0,
            sendReadReceipts = existing?.sendReadReceipts ?: false,
            lastSeenAckPayloadId = existing?.lastSeenAckPayloadId ?: 0,
            conversationType = type.name,
            memberPhones = phonesCsv,
            memberNames = namesCsv,
            description = description.trim(),
            isOwner = isOwner,
            avatarUri = avatarUri.ifBlank { existing?.avatarUri ?: "" }
        )

        val id = dao.insertConversation(entity)
        if (existing == null) {
            saveSystemNotice(id, welcomeMsg)
        }
        return dao.getConversationById(id) ?: entity.copy(id = id)
    }

    suspend fun addMemberToGroupOrChannel(
        conversationId: Long,
        memberName: String,
        memberPhone: String
    ) {
        val conv = dao.getConversationById(conversationId) ?: return
        val cleanPhone = normalizePhoneNumber(memberPhone)
        if (cleanPhone.isBlank()) return
        val cleanName = memberName.trim().ifBlank { cleanPhone }.replace(",", " ")
        val current = conv.parsedMembers().toMutableList()
        if (current.none { ContactHelper.isSamePhoneNumber(it.second, cleanPhone) }) {
            current.add(Pair(cleanName, cleanPhone))
            val phonesCsv = current.joinToString(",") { it.second }
            val namesCsv = current.joinToString(",") { it.first }
            dao.updateConversationMembers(conversationId, phonesCsv, namesCsv)
        }
    }

    suspend fun removeMemberFromGroupOrChannel(
        conversationId: Long,
        memberPhone: String
    ) {
        val conv = dao.getConversationById(conversationId) ?: return
        val current = conv.parsedMembers().filterNot {
            ContactHelper.isSamePhoneNumber(it.second, memberPhone) || it.second == memberPhone
        }
        val phonesCsv = current.joinToString(",") { it.second }
        val namesCsv = current.joinToString(",") { it.first }
        dao.updateConversationMembers(conversationId, phonesCsv, namesCsv)
    }

    suspend fun updateChannelRole(conversationId: Long, isOwner: Boolean) {
        dao.updateChannelRole(conversationId, isOwner)
    }

    suspend fun updateConversationAvatar(conversationId: Long, avatarUri: String) {
        dao.updateConversationAvatar(conversationId, avatarUri)
    }

    suspend fun syncContactPhotoForConversation(context: Context, conversationId: Long): Boolean {
        val conv = dao.getConversationById(conversationId) ?: return false
        val contactPhotoUri = ContactHelper.getContactPhotoUriByPhone(context, conv.phoneNumber) ?: return false
        val localCopy = ContactHelper.copyUriToInternalAvatarStorage(
            context = context,
            sourceUri = android.net.Uri.parse(contactPhotoUri),
            identifier = "conv_$conversationId"
        ) ?: contactPhotoUri
        dao.updateConversationAvatar(conversationId, localCopy)
        return true
    }

    suspend fun updateVerificationStatus(conversationId: Long, newStatus: VerificationStatus) {
        val conv = dao.getConversationById(conversationId) ?: return
        dao.updateConversation(conv.copy(verificationStatus = newStatus))
    }

    suspend fun updatePreferredEncoding(conversationId: Long, encoding: String) {
        val conv = dao.getConversationById(conversationId) ?: return
        dao.updateConversation(conv.copy(preferredEncoding = encoding))
    }

    suspend fun updateSendReadReceipts(conversationId: Long, enabled: Boolean) {
        dao.updateSendReadReceipts(conversationId, enabled)
    }

    suspend fun updateAllConversationsReadReceipts(enabled: Boolean) {
        dao.updateAllConversationsReadReceipts(enabled)
    }

    suspend fun updateLastSeenAckPayloadId(conversationId: Long, payloadId: Int) {
        dao.updateLastSeenAckPayloadId(conversationId, payloadId)
    }

    suspend fun markConversationRead(conversationId: Long) {
        dao.markConversationRead(conversationId)
    }

    suspend fun markOutgoingMessagesAsSeen(conversationId: Long, targetPayloadId: Int = 0) {
        if (targetPayloadId != 0) {
            dao.updateStatusByPayloadId(targetPayloadId, DeliveryStatus.SEEN)
        }
        dao.markAllOutgoingMessagesSeenInConversation(conversationId)
    }

    suspend fun toggleMessageReaction(messageId: Long, emoji: String): String {
        val msg = dao.getMessageById(messageId) ?: return ""
        val currentList = msg.reactions
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()
        if (currentList.contains(emoji)) {
            currentList.remove(emoji)
        } else {
            currentList.add(emoji)
        }
        val updated = currentList.joinToString(",")
        dao.updateMessageReactions(messageId, updated)
        return updated
    }

    suspend fun applyIncomingReaction(conversationId: Long, targetPayloadId: Int, emoji: String) {
        val targetMsg = (if (targetPayloadId != 0) dao.getMessageByPayloadId(targetPayloadId) else null)
            ?: dao.getMessagesForConversationList(conversationId).lastOrNull()
            ?: return
        val currentList = targetMsg.reactions
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()
        if (currentList.contains(emoji)) {
            currentList.remove(emoji)
        } else {
            currentList.add(emoji)
        }
        dao.updateMessageReactions(targetMsg.id, currentList.joinToString(","))
    }

    suspend fun deleteConversation(id: Long) {
        dao.deleteMessagesForConversation(id)
        dao.deleteConversation(id)
    }

    fun getDecryptedMessagesFlow(conversationId: Long): Flow<List<DecryptedMessage>> {
        return dao.getMessagesForConversationFlow(conversationId).map { messages ->
            messages.map { msg ->
                val cipherHash = msg.encryptedPlaintext.hashCode()
                val cached = decryptedCache[msg.id]
                val decryptedText = if (cached != null && cached.first == cipherHash) {
                    cached.second
                } else {
                    val text = try {
                        if (msg.encryptedPlaintext.isNotEmpty()) {
                            String(keystoreHelper.decryptWithMasterKey(msg.encryptedPlaintext), Charsets.UTF_8)
                        } else {
                            ""
                        }
                    } catch (e: Exception) {
                        "[خطا در رمزگشایی حافظه محلی]"
                    }
                    if (msg.id != 0L) {
                        decryptedCache[msg.id] = Pair(cipherHash, text)
                    }
                    text
                }
                DecryptedMessage(
                    id = msg.id,
                    conversationId = msg.conversationId,
                    senderPhone = msg.senderPhone,
                    recipientPhone = msg.recipientPhone,
                    isOutgoing = msg.isOutgoing,
                    plaintext = decryptedText,
                    rawCiphertext = msg.rawCiphertext,
                    deliveryStatus = msg.deliveryStatus,
                    timestamp = msg.timestamp,
                    sequenceNumber = msg.sequenceNumber,
                    isEncrypted = msg.isEncrypted,
                    fragmentCount = msg.fragmentCount,
                    encodingUsed = msg.encodingUsed,
                    messagePayloadId = msg.messagePayloadId,
                    replyToMessageId = msg.replyToMessageId,
                    replyToPreview = msg.replyToPreview,
                    reactions = msg.reactions,
                    senderDisplayName = msg.senderDisplayName
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    suspend fun saveOutgoingMessage(
        conversationId: Long,
        senderPhone: String,
        recipientPhone: String,
        plaintext: String,
        rawCiphertext: String,
        sequenceNumber: Int,
        fragmentCount: Int,
        encodingUsed: String,
        messagePayloadId: Int,
        replyToMessageId: Long = 0L,
        replyToPreview: String = "",
        senderDisplayName: String = ""
    ): Long {
        val encryptedAtRest = keystoreHelper.encryptWithMasterKey(plaintext.toByteArray(Charsets.UTF_8))
        val message = MessageEntity(
            conversationId = conversationId,
            senderPhone = senderPhone,
            recipientPhone = recipientPhone,
            isOutgoing = true,
            encryptedPlaintext = encryptedAtRest,
            rawCiphertext = rawCiphertext,
            deliveryStatus = DeliveryStatus.QUEUED,
            timestamp = System.currentTimeMillis(),
            sequenceNumber = sequenceNumber,
            isEncrypted = true,
            fragmentCount = fragmentCount,
            encodingUsed = encodingUsed,
            messagePayloadId = messagePayloadId,
            replyToMessageId = replyToMessageId,
            replyToPreview = replyToPreview,
            senderDisplayName = senderDisplayName
        )
        val msgId = dao.insertMessage(message)

        val conv = dao.getConversationById(conversationId)
        if (conv != null) {
            dao.updateConversation(
                conv.copy(
                    lastMessageText = plaintext,
                    lastMessageTimestamp = System.currentTimeMillis(),
                    nextOutboundSeq = conv.nextOutboundSeq + 1
                )
            )
        }
        return msgId
    }

    suspend fun saveOrUpdateIncompleteIncomingMessage(
        conversationId: Long,
        senderPhone: String,
        messagePayloadId: Int,
        receivedCount: Int,
        totalCount: Int,
        encodingUsed: String
    ): Long {
        val existing = dao.getMessageByPayloadId(messagePayloadId)
        val placeholder = "در حال تجمیع قطعات: $receivedCount از $totalCount قطعه دریافت شد"
        val encryptedAtRest = keystoreHelper.encryptWithMasterKey(placeholder.toByteArray(Charsets.UTF_8))
        if (existing != null) {
            dao.updateMessage(
                existing.copy(
                    encryptedPlaintext = encryptedAtRest,
                    rawCiphertext = "قطعات دریافت شده: $receivedCount/$totalCount",
                    deliveryStatus = DeliveryStatus.INCOMPLETE,
                    fragmentCount = totalCount
                )
            )
            return existing.id
        } else {
            val message = MessageEntity(
                conversationId = conversationId,
                senderPhone = senderPhone,
                recipientPhone = "Me",
                isOutgoing = false,
                encryptedPlaintext = encryptedAtRest,
                rawCiphertext = "قطعات دریافت شده: $receivedCount/$totalCount",
                deliveryStatus = DeliveryStatus.INCOMPLETE,
                timestamp = System.currentTimeMillis(),
                sequenceNumber = 0,
                isEncrypted = true,
                fragmentCount = totalCount,
                encodingUsed = encodingUsed,
                messagePayloadId = messagePayloadId
            )
            return dao.insertMessage(message)
        }
    }

    suspend fun updateOutgoingChunkProgress(messagePayloadId: Int, sentChunks: Int, totalChunks: Int) {
        val existing = dao.getMessageByPayloadId(messagePayloadId) ?: return
        val statusText = if (sentChunks < totalChunks) {
            "در حال ارسال قطعه $sentChunks از $totalChunks (${(sentChunks * 100) / totalChunks}٪)"
        } else {
            "تحویل به اپراتور ($totalChunks قطعه)"
        }
        val status = if (sentChunks < totalChunks) DeliveryStatus.SENDING else DeliveryStatus.SENT_TO_CARRIER
        dao.updateMessage(
            existing.copy(
                deliveryStatus = status,
                rawCiphertext = statusText
            )
        )
    }

    suspend fun saveIncomingMessage(
        conversationId: Long,
        senderPhone: String,
        recipientPhone: String,
        plaintext: String,
        rawCiphertext: String,
        sequenceNumber: Int,
        fragmentCount: Int,
        encodingUsed: String,
        messagePayloadId: Int,
        replyToMessageId: Long = 0L,
        replyToPreview: String = "",
        senderDisplayName: String = ""
    ): Long {
        val existing = dao.getMessageByPayloadId(messagePayloadId)
        val encryptedAtRest = keystoreHelper.encryptWithMasterKey(plaintext.toByteArray(Charsets.UTF_8))
        val msgId = if (existing != null) {
            dao.updateMessage(
                existing.copy(
                    encryptedPlaintext = encryptedAtRest,
                    rawCiphertext = rawCiphertext,
                    deliveryStatus = DeliveryStatus.DECRYPTED,
                    timestamp = System.currentTimeMillis(),
                    sequenceNumber = sequenceNumber,
                    fragmentCount = fragmentCount,
                    encodingUsed = encodingUsed,
                    replyToMessageId = replyToMessageId,
                    replyToPreview = replyToPreview,
                    senderDisplayName = senderDisplayName
                )
            )
            existing.id
        } else {
            val message = MessageEntity(
                conversationId = conversationId,
                senderPhone = senderPhone,
                recipientPhone = recipientPhone,
                isOutgoing = false,
                encryptedPlaintext = encryptedAtRest,
                rawCiphertext = rawCiphertext,
                deliveryStatus = DeliveryStatus.DECRYPTED,
                timestamp = System.currentTimeMillis(),
                sequenceNumber = sequenceNumber,
                isEncrypted = true,
                fragmentCount = fragmentCount,
                encodingUsed = encodingUsed,
                messagePayloadId = messagePayloadId,
                replyToMessageId = replyToMessageId,
                replyToPreview = replyToPreview,
                senderDisplayName = senderDisplayName
            )
            dao.insertMessage(message)
        }

        val conv = dao.getConversationById(conversationId)
        if (conv != null) {
            val previewLine = if (senderDisplayName.isNotBlank() && conv.isGroup) {
                "$senderDisplayName: $plaintext"
            } else {
                plaintext
            }
            dao.updateConversation(
                conv.copy(
                    lastMessageText = previewLine,
                    lastMessageTimestamp = System.currentTimeMillis(),
                    unreadCount = conv.unreadCount + 1,
                    highestInboundSeq = Math.max(conv.highestInboundSeq, sequenceNumber)
                )
            )
        }
        return msgId
    }

    suspend fun saveIncomingStandardSms(
        conversationId: Long,
        senderPhone: String,
        plaintext: String,
        replyToMessageId: Long = 0L,
        replyToPreview: String = "",
        senderDisplayName: String = ""
    ): Long {
        val encryptedAtRest = keystoreHelper.encryptWithMasterKey(plaintext.toByteArray(Charsets.UTF_8))
        val message = MessageEntity(
            conversationId = conversationId,
            senderPhone = senderPhone,
            recipientPhone = "Me",
            isOutgoing = false,
            encryptedPlaintext = encryptedAtRest,
            rawCiphertext = "[پیامک عادی]",
            deliveryStatus = DeliveryStatus.DECRYPTED,
            timestamp = System.currentTimeMillis(),
            sequenceNumber = 0,
            isEncrypted = false,
            fragmentCount = 1,
            encodingUsed = "PLAINTEXT",
            messagePayloadId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt(),
            replyToMessageId = replyToMessageId,
            replyToPreview = replyToPreview,
            senderDisplayName = senderDisplayName
        )
        val msgId = dao.insertMessage(message)

        val conv = dao.getConversationById(conversationId)
        if (conv != null) {
            val previewLine = if (senderDisplayName.isNotBlank() && conv.isGroup) {
                "$senderDisplayName: $plaintext"
            } else {
                plaintext
            }
            dao.updateConversation(
                conv.copy(
                    lastMessageText = previewLine,
                    lastMessageTimestamp = System.currentTimeMillis(),
                    unreadCount = conv.unreadCount + 1
                )
            )
        }
        return msgId
    }

    suspend fun saveOutgoingStandardSms(
        conversationId: Long,
        senderPhone: String,
        recipientPhone: String,
        plaintext: String,
        fragmentCount: Int = 1,
        replyToMessageId: Long = 0L,
        replyToPreview: String = "",
        senderDisplayName: String = ""
    ): Long {
        val encryptedAtRest = keystoreHelper.encryptWithMasterKey(plaintext.toByteArray(Charsets.UTF_8))
        val message = MessageEntity(
            conversationId = conversationId,
            senderPhone = senderPhone,
            recipientPhone = recipientPhone,
            isOutgoing = true,
            encryptedPlaintext = encryptedAtRest,
            rawCiphertext = "[پیامک عادی]",
            deliveryStatus = DeliveryStatus.SENT_TO_CARRIER,
            timestamp = System.currentTimeMillis(),
            sequenceNumber = 0,
            isEncrypted = false,
            fragmentCount = fragmentCount,
            encodingUsed = "PLAINTEXT",
            messagePayloadId = (System.currentTimeMillis() and 0x7FFFFFFF).toInt(),
            replyToMessageId = replyToMessageId,
            replyToPreview = replyToPreview,
            senderDisplayName = senderDisplayName
        )
        val msgId = dao.insertMessage(message)

        val conv = dao.getConversationById(conversationId)
        if (conv != null) {
            dao.updateConversation(
                conv.copy(
                    lastMessageText = plaintext,
                    lastMessageTimestamp = System.currentTimeMillis()
                )
            )
        }
        return msgId
    }

    suspend fun saveSystemNotice(conversationId: Long, notice: String) {
        val encryptedAtRest = keystoreHelper.encryptWithMasterKey(notice.toByteArray(Charsets.UTF_8))
        val message = MessageEntity(
            conversationId = conversationId,
            senderPhone = "سیستم امنیتی",
            recipientPhone = "Me",
            isOutgoing = false,
            encryptedPlaintext = encryptedAtRest,
            rawCiphertext = notice,
            deliveryStatus = DeliveryStatus.DECRYPTED,
            timestamp = System.currentTimeMillis(),
            sequenceNumber = 0,
            isEncrypted = true,
            fragmentCount = 1,
            encodingUsed = "SYSTEM",
            messagePayloadId = 0,
            senderDisplayName = "سیستم"
        )
        dao.insertMessage(message)
    }

    suspend fun syncContactNames(context: Context) {
        try {
            val conversations = dao.getAllConversationsList()
            for (conv in conversations) {
                if (conv.isDirect) {
                    val resolvedName = ContactHelper.getContactNameByPhone(context, conv.phoneNumber)
                    val resolvedPhoto = if (conv.avatarUri.isBlank()) {
                        ContactHelper.getContactPhotoUriByPhone(context, conv.phoneNumber)
                    } else {
                        null
                    }
                    val newName = if (!resolvedName.isNullOrBlank() && resolvedName != conv.phoneNumber) {
                        resolvedName
                    } else {
                        conv.contactName
                    }
                    val newAvatar = if (!resolvedPhoto.isNullOrBlank()) {
                        ContactHelper.copyUriToInternalAvatarStorage(
                            context = context,
                            sourceUri = android.net.Uri.parse(resolvedPhoto),
                            identifier = "conv_${conv.id}"
                        ) ?: resolvedPhoto
                    } else {
                        conv.avatarUri
                    }
                    if (newName != conv.contactName || newAvatar != conv.avatarUri) {
                        dao.updateConversation(
                            conv.copy(
                                contactName = newName,
                                avatarUri = newAvatar
                            )
                        )
                    }
                }
            }
        } catch (ignored: Exception) {}
    }

    suspend fun updateMessageStatus(messageId: Long, status: DeliveryStatus) {
        dao.updateMessageStatus(messageId, status)
    }

    suspend fun updateStatusByPayloadId(payloadId: Int, status: DeliveryStatus) {
        dao.updateStatusByPayloadId(payloadId, status)
    }

    suspend fun getMessageIdByPayloadId(payloadId: Int): Long {
        if (payloadId == 0) return 0L
        return dao.getMessageByPayloadId(payloadId)?.id ?: 0L
    }

    suspend fun deleteMessage(id: Long) {
        decryptedCache.remove(id)
        dao.deleteMessage(id)
    }

    suspend fun wipeAllData() {
        decryptedCache.clear()
        dao.deleteAllMessages()
        dao.deleteAllConversations()
        dao.deleteIdentityKey()
        cachedKeyPair = null
    }

    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace(Regex("[^0-9+]"), "").trim()
    }

    companion object {
        fun packWireGroupHeader(
            text: String,
            conversationType: String,
            roomIdentifier: String,
            roomTitle: String
        ): String {
            if (conversationType == ConversationType.DIRECT.name) return text
            val cleanTitle = roomTitle.replace("\n", " ").replace("⟧", "").take(32).trim()
            return "⟦G:$conversationType:$roomIdentifier:$cleanTitle⟧\n$text"
        }

        data class UnpackedGroupWire(
            val conversationType: String,
            val roomIdentifier: String,
            val roomTitle: String,
            val body: String
        )

        fun unpackWireGroupHeader(wireText: String): UnpackedGroupWire {
            if (wireText.startsWith("⟦G:") && wireText.contains("⟧\n")) {
                val endIdx = wireText.indexOf("⟧\n")
                val header = wireText.substring(3, endIdx)
                val body = wireText.substring(endIdx + 2)
                val parts = header.split(":", limit = 4)
                if (parts.size >= 4) {
                    val type = parts[0]
                    val roomId = "${parts[1]}:${parts[2]}"
                    val title = parts[3]
                    return UnpackedGroupWire(type, roomId, title, body)
                }
            }
            return UnpackedGroupWire(ConversationType.DIRECT.name, "", "", wireText)
        }

        fun packWireReply(plaintext: String, replyToPayloadId: Int, replyToPreview: String): String {
            if (replyToPreview.isBlank()) return plaintext
            val cleanPreview = replyToPreview
                .replace("\n", " ")
                .replace("⟧", "")
                .take(48)
                .trim()
            return "⟦R:$replyToPayloadId:$cleanPreview⟧\n$plaintext"
        }

        fun unpackWireReply(wireText: String): Triple<Int, String, String> {
            if (wireText.startsWith("⟦R:") && wireText.contains("⟧\n")) {
                val headerEnd = wireText.indexOf("⟧\n")
                val header = wireText.substring(3, headerEnd)
                val body = wireText.substring(headerEnd + 2)
                val colonIndex = header.indexOf(':')
                if (colonIndex > 0) {
                    val payloadId = header.substring(0, colonIndex).toIntOrNull() ?: 0
                    val preview = header.substring(colonIndex + 1)
                    return Triple(payloadId, preview, body)
                }
            }
            return Triple(0, "", wireText)
        }
    }

    data class DecryptedMessage(
        val id: Long,
        val conversationId: Long,
        val senderPhone: String,
        val recipientPhone: String,
        val isOutgoing: Boolean,
        val plaintext: String,
        val rawCiphertext: String,
        val deliveryStatus: DeliveryStatus,
        val timestamp: Long,
        val sequenceNumber: Int,
        val isEncrypted: Boolean,
        val fragmentCount: Int,
        val encodingUsed: String,
        val messagePayloadId: Int,
        val replyToMessageId: Long = 0L,
        val replyToPreview: String = "",
        val reactions: String = "",
        val senderDisplayName: String = ""
    )
}
