package com.example.sms

import android.content.Context
import android.util.Log
import com.example.crypto.CryptoEngine
import com.example.crypto.PersianCiphertextCodec
import com.example.data.ConversationEntity
import com.example.data.ConversationType
import com.example.data.DeliveryStatus
import com.example.data.EncryptedStorageRepository
import com.example.data.VerificationStatus
import com.example.protocol.FragmentationEngine
import com.example.protocol.SmsPacket
import com.example.util.ContactHelper
import com.example.util.NotificationHelper
import java.util.concurrent.ConcurrentHashMap

/**
 * Core inbound processing pipeline for SecureSMS.
 * Receives raw SMS strings from broadcast receivers or simulators, decodes the wire
 * representations, buffers fragments, checks replay sequence counters, decrypts payloads,
 * and routes Direct, Group, and Channel messages.
 */
class SmsProcessor(
    private val context: Context,
    private val repository: EncryptedStorageRepository,
    private val fragmentationEngine: FragmentationEngine
) {

    var transportManager: SmsTransportManager? = null

    private val recentSmsDedupe = ConcurrentHashMap<String, Long>()

    sealed class ProcessResult {
        data class DecryptedData(
            val messageId: Long,
            val conversationId: Long,
            val senderPhone: String,
            val plaintext: String,
            val isVerified: Boolean
        ) : ProcessResult()

        data class StandardSmsReceived(
            val messageId: Long,
            val conversationId: Long,
            val senderPhone: String,
            val plaintext: String,
            val senderName: String
        ) : ProcessResult()

        data class KeyExchanged(
            val senderPhone: String,
            val safetyNumber: String,
            val isNewKey: Boolean
        ) : ProcessResult()

        data class FragmentBuffered(
            val messagePayloadId: Int,
            val received: Int,
            val total: Int
        ) : ProcessResult()

        data class Ignored(val reason: String) : ProcessResult()
        data class Error(val error: String) : ProcessResult()
    }

    private suspend fun resolveTargetConversation(
        senderPhone: String,
        contactName: String,
        unpackedGroup: EncryptedStorageRepository.Companion.UnpackedGroupWire
    ): ConversationEntity {
        if (unpackedGroup.conversationType == ConversationType.GROUP.name ||
            unpackedGroup.conversationType == ConversationType.CHANNEL.name
        ) {
            val existingRoom = repository.getConversationByPhone(unpackedGroup.roomIdentifier)
            if (existingRoom != null) {
                repository.addMemberToGroupOrChannel(existingRoom.id, contactName, senderPhone)
                return repository.getConversationById(existingRoom.id) ?: existingRoom
            }
            val code = unpackedGroup.roomIdentifier.substringAfter(":", "100000")
            val type = if (unpackedGroup.conversationType == ConversationType.CHANNEL.name) {
                ConversationType.CHANNEL
            } else {
                ConversationType.GROUP
            }
            return repository.createGroupOrChannelConversation(
                title = unpackedGroup.roomTitle.ifBlank { type.labelFa },
                description = "",
                type = type,
                members = listOf(Pair(contactName, senderPhone)),
                customCode = code,
                isOwner = type != ConversationType.CHANNEL
            )
        }

        var conv = repository.getConversationByPhone(senderPhone)
        if (conv == null) {
            conv = repository.createOrUpdateConversation(
                phoneNumber = senderPhone,
                contactName = contactName
            )
        } else if (conv.contactName == conv.phoneNumber && contactName != senderPhone) {
            conv = repository.createOrUpdateConversation(
                phoneNumber = senderPhone,
                contactName = contactName
            )
        }
        return conv
    }

    suspend fun processIncomingSms(senderPhone: String, rawText: String): ProcessResult {
        val trimmedText = rawText.trim()
        if (trimmedText.isBlank()) return ProcessResult.Ignored("Empty message text")

        val dedupeKey = "${senderPhone.trim()}:${trimmedText.hashCode()}"
        val now = System.currentTimeMillis()
        val previousTimestamp = recentSmsDedupe[dedupeKey]
        if (previousTimestamp != null && (now - previousTimestamp) < 10_000L) {
            Log.d("SmsProcessor", "Duplicate SMS ignored from $senderPhone within 10s deduplication window")
            return ProcessResult.Ignored("Duplicate SMS received within deduplication window")
        }
        recentSmsDedupe[dedupeKey] = now
        cleanExpiredDedupeCache(now)

        val contactName = ContactHelper.getContactNameByPhone(context, senderPhone) ?: senderPhone

        if (!PersianCiphertextCodec.isSecureSmsPayload(trimmedText)) {
            val unpackedGroup = EncryptedStorageRepository.unpackWireGroupHeader(trimmedText)
            val conv = resolveTargetConversation(senderPhone, contactName, unpackedGroup)
            val (replyPayloadId, replyPreview, cleanBody) = EncryptedStorageRepository.unpackWireReply(unpackedGroup.body)
            val resolvedReplyId = repository.getMessageIdByPayloadId(replyPayloadId)

            val msgId = repository.saveIncomingStandardSms(
                conversationId = conv.id,
                senderPhone = senderPhone,
                plaintext = cleanBody,
                replyToMessageId = resolvedReplyId,
                replyToPreview = replyPreview,
                senderDisplayName = contactName
            )

            NotificationHelper.showMessageNotification(
                context = context,
                senderName = conv.contactName,
                senderPhone = senderPhone,
                messagePreview = cleanBody.take(60),
                isEncrypted = false
            )

            return ProcessResult.StandardSmsReceived(
                messageId = msgId,
                conversationId = conv.id,
                senderPhone = senderPhone,
                plaintext = cleanBody,
                senderName = conv.contactName
            )
        }

        try {
            val (packetBytes, encodingMode) = PersianCiphertextCodec.decodePayload(trimmedText)
            val packet = SmsPacket.fromByteArray(packetBytes)

            val reassembly = fragmentationEngine.ingestFragment(packet)
            if (reassembly is FragmentationEngine.ReassemblyResult.Pending) {
                var conv = repository.getConversationByPhone(senderPhone)
                if (conv == null) {
                    conv = repository.createOrUpdateConversation(
                        phoneNumber = senderPhone,
                        contactName = contactName
                    )
                }
                repository.saveOrUpdateIncompleteIncomingMessage(
                    conversationId = conv.id,
                    senderPhone = senderPhone,
                    messagePayloadId = reassembly.messageId,
                    receivedCount = reassembly.receivedCount,
                    totalCount = reassembly.totalCount,
                    encodingUsed = encodingMode.name
                )

                return ProcessResult.FragmentBuffered(
                    messagePayloadId = reassembly.messageId,
                    received = reassembly.receivedCount,
                    total = reassembly.totalCount
                )
            }

            val complete = reassembly as FragmentationEngine.ReassemblyResult.Complete
            val fullPayload = complete.fullPayload
            val representative = complete.representativePacket

            return when (representative.messageType) {
                SmsPacket.MessageType.KEY_OFFER -> {
                    processKeyOfferPacket(senderPhone, contactName, fullPayload, representative)
                }
                SmsPacket.MessageType.KEY_ACCEPT -> {
                    processKeyAcceptPacket(senderPhone, contactName, fullPayload, representative)
                }
                SmsPacket.MessageType.DATA -> {
                    processDataPacket(senderPhone, contactName, fullPayload, representative, trimmedText, encodingMode)
                }
                SmsPacket.MessageType.DELIVERY_ACK -> {
                    repository.updateStatusByPayloadId(representative.messageId, DeliveryStatus.DELIVERED_CARRIER)
                    ProcessResult.DecryptedData(
                        messageId = 0,
                        conversationId = 0,
                        senderPhone = senderPhone,
                        plaintext = "تأییدیه تحویل پیامک دریافت شد",
                        isVerified = true
                    )
                }
                SmsPacket.MessageType.SEEN_ACK -> {
                    val conv = repository.getConversationByPhone(senderPhone)
                    if (conv != null) {
                        repository.markOutgoingMessagesAsSeen(conv.id, representative.messageId)
                    } else {
                        repository.updateStatusByPayloadId(representative.messageId, DeliveryStatus.SEEN)
                    }
                    ProcessResult.DecryptedData(
                        messageId = 0,
                        conversationId = conv?.id ?: 0,
                        senderPhone = senderPhone,
                        plaintext = "رسید «دیده شد» دریافت شد",
                        isVerified = true
                    )
                }
                SmsPacket.MessageType.REACTION -> {
                    processReactionPacket(senderPhone, fullPayload, representative)
                }
            }
        } catch (e: Exception) {
            Log.w("SmsProcessor", "Failed to decode as SecureSMS envelope, falling back to standard SMS: ${e.message}")
            val unpackedGroup = EncryptedStorageRepository.unpackWireGroupHeader(trimmedText)
            val conv = resolveTargetConversation(senderPhone, contactName, unpackedGroup)
            val (replyPayloadId, replyPreview, cleanBody) = EncryptedStorageRepository.unpackWireReply(unpackedGroup.body)
            val resolvedReplyId = repository.getMessageIdByPayloadId(replyPayloadId)
            val msgId = repository.saveIncomingStandardSms(
                conversationId = conv.id,
                senderPhone = senderPhone,
                plaintext = cleanBody,
                replyToMessageId = resolvedReplyId,
                replyToPreview = replyPreview,
                senderDisplayName = contactName
            )
            NotificationHelper.showMessageNotification(
                context = context,
                senderName = conv.contactName,
                senderPhone = senderPhone,
                messagePreview = cleanBody.take(60),
                isEncrypted = false
            )
            return ProcessResult.StandardSmsReceived(
                messageId = msgId,
                conversationId = conv.id,
                senderPhone = senderPhone,
                plaintext = cleanBody,
                senderName = conv.contactName
            )
        }
    }

    private suspend fun processKeyOfferPacket(
        senderPhone: String,
        contactName: String,
        payload: ByteArray,
        packet: SmsPacket
    ): ProcessResult {
        val theirPubKey = CryptoEngine.parsePublicKey(payload)
        val conversation = repository.createOrUpdateConversation(
            phoneNumber = senderPhone,
            contactName = contactName,
            theirPublicKey = theirPubKey
        )

        repository.saveSystemNotice(
            conversationId = conversation.id,
            notice = "🛡️ کلید امنیتی مخاطب ذخیره شد."
        )

        NotificationHelper.showMessageNotification(
            context = context,
            senderName = conversation.contactName,
            senderPhone = senderPhone,
            messagePreview = "کلید امنیتی تبادل و گفتگوی محرمانه فعال شد",
            isEncrypted = true
        )

        return ProcessResult.KeyExchanged(
            senderPhone = senderPhone,
            safetyNumber = conversation.safetyNumber,
            isNewKey = conversation.verificationStatus == VerificationStatus.IDENTITY_CHANGED
        )
    }

    private suspend fun processKeyAcceptPacket(
        senderPhone: String,
        contactName: String,
        payload: ByteArray,
        packet: SmsPacket
    ): ProcessResult {
        val theirPubKey = CryptoEngine.parsePublicKey(payload)
        val conversation = repository.createOrUpdateConversation(
            phoneNumber = senderPhone,
            contactName = contactName,
            theirPublicKey = theirPubKey
        )

        repository.saveSystemNotice(
            conversationId = conversation.id,
            notice = "🛡️ مخاطب کلید امنیتی شما را پذیرفت و کلید خود را ارسال کرد. اتصال امن برقرار شد."
        )

        NotificationHelper.showMessageNotification(
            context = context,
            senderName = conversation.contactName,
            senderPhone = senderPhone,
            messagePreview = "ارتباط محرمانه با موفقیت برقرار شد",
            isEncrypted = true
        )

        return ProcessResult.KeyExchanged(
            senderPhone = senderPhone,
            safetyNumber = conversation.safetyNumber,
            isNewKey = conversation.verificationStatus == VerificationStatus.IDENTITY_CHANGED
        )
    }

    private suspend fun processDataPacket(
        senderPhone: String,
        contactName: String,
        payload: ByteArray,
        packet: SmsPacket,
        rawText: String,
        encodingMode: PersianCiphertextCodec.EncodingMode
    ): ProcessResult {
        var conversation = repository.getConversationByPhone(senderPhone)
        if (conversation == null) {
            conversation = repository.createOrUpdateConversation(
                phoneNumber = senderPhone,
                contactName = contactName
            )
        }

        val theirPubKeyBase64 = conversation.theirPublicKeyBase64
        if (theirPubKeyBase64 == null) {
            val placeholder = "🔒 یک پیام رمزشده دریافت شد. لطفاً کلید عمومی مخاطب را از طریق کد QR یا وارد کردن دستی ثبت کنید."
            val msgId = repository.saveIncomingMessage(
                conversationId = conversation.id,
                senderPhone = senderPhone,
                recipientPhone = "Me",
                plaintext = placeholder,
                rawCiphertext = rawText,
                sequenceNumber = packet.sequenceNumber,
                fragmentCount = packet.fragmentCount,
                encodingUsed = encodingMode.name,
                messagePayloadId = packet.messageId,
                senderDisplayName = contactName
            )

            NotificationHelper.showMessageNotification(
                context = context,
                senderName = conversation.contactName,
                senderPhone = senderPhone,
                messagePreview = "پیام رمزشده دریافت شد (تنظیم کلید لازم است)",
                isEncrypted = true
            )

            return ProcessResult.DecryptedData(
                messageId = msgId,
                conversationId = conversation.id,
                senderPhone = senderPhone,
                plaintext = placeholder,
                isVerified = false
            )
        }

        val theirPubKey = CryptoEngine.parsePublicKeyFromBase64(theirPubKeyBase64)
        val expectedSenderKeyId = CryptoEngine.computeKeyId(theirPubKey)

        if (!packet.senderKeyId.contentEquals(expectedSenderKeyId)) {
            val errorText = "هشدار: شناسه کلید فرستنده تطابق ندارد! احتمال تغییر کلید یا تلاش برای جعل هویت."
            repository.saveSystemNotice(conversation.id, errorText)
            return ProcessResult.Error("Sender Key ID mismatch")
        }

        if (packet.sequenceNumber <= conversation.highestInboundSeq) {
            val replayWarning = "پیام تکراری یا بازپخش‌شده نادیده گرفته شد (شماره ترتیب: ${packet.sequenceNumber})"
            Log.w("SmsProcessor", replayWarning)
            return ProcessResult.Ignored(replayWarning)
        }

        val myPrivKey = repository.getMyPrivateKey()
        val sharedKey = CryptoEngine.deriveSharedKey(myPrivKey, theirPubKey)
        val aad = packet.computeAad()
        val encryptedPayload = CryptoEngine.EncryptedPayload.fromCombined(payload)

        val decryptedBytes = CryptoEngine.decrypt(
            sharedKey = sharedKey,
            iv = encryptedPayload.iv,
            ciphertextWithTag = encryptedPayload.ciphertext,
            aad = aad
        )

        val rawDecryptedText = String(decryptedBytes, Charsets.UTF_8)
        val unpackedGroup = EncryptedStorageRepository.unpackWireGroupHeader(rawDecryptedText)
        val targetConv = resolveTargetConversation(senderPhone, contactName, unpackedGroup)
        val (replyPayloadId, replyPreview, plaintext) = EncryptedStorageRepository.unpackWireReply(unpackedGroup.body)
        val resolvedReplyId = repository.getMessageIdByPayloadId(replyPayloadId)

        val msgId = repository.saveIncomingMessage(
            conversationId = targetConv.id,
            senderPhone = senderPhone,
            recipientPhone = "Me",
            plaintext = plaintext,
            rawCiphertext = rawText,
            sequenceNumber = packet.sequenceNumber,
            fragmentCount = packet.fragmentCount,
            encodingUsed = encodingMode.name,
            messagePayloadId = packet.messageId,
            replyToMessageId = resolvedReplyId,
            replyToPreview = replyPreview,
            senderDisplayName = contactName
        )

        val isVerified = targetConv.verificationStatus == VerificationStatus.VERIFIED

        NotificationHelper.showMessageNotification(
            context = context,
            senderName = targetConv.contactName,
            senderPhone = senderPhone,
            messagePreview = plaintext.take(60),
            isEncrypted = true
        )

        return ProcessResult.DecryptedData(
            messageId = msgId,
            conversationId = targetConv.id,
            senderPhone = senderPhone,
            plaintext = plaintext,
            isVerified = isVerified
        )
    }

    private suspend fun processReactionPacket(
        senderPhone: String,
        payload: ByteArray,
        packet: SmsPacket
    ): ProcessResult {
        val conversation = repository.getConversationByPhone(senderPhone)
            ?: return ProcessResult.Ignored("No conversation found for reaction from $senderPhone")
        val theirPubKeyBase64 = conversation.theirPublicKeyBase64
            ?: return ProcessResult.Ignored("Missing public key for reaction from $senderPhone")

        val theirPubKey = CryptoEngine.parsePublicKeyFromBase64(theirPubKeyBase64)
        val myPrivKey = repository.getMyPrivateKey()
        val sharedKey = CryptoEngine.deriveSharedKey(myPrivKey, theirPubKey)
        val aad = packet.computeAad()
        val encryptedPayload = CryptoEngine.EncryptedPayload.fromCombined(payload)
        val decryptedBytes = CryptoEngine.decrypt(
            sharedKey = sharedKey,
            iv = encryptedPayload.iv,
            ciphertextWithTag = encryptedPayload.ciphertext,
            aad = aad
        )
        val wireStr = String(decryptedBytes, Charsets.UTF_8)
        val sepIdx = wireStr.indexOf('|')
        if (sepIdx > 0) {
            val targetPayloadId = wireStr.substring(0, sepIdx).toIntOrNull() ?: 0
            val emoji = wireStr.substring(sepIdx + 1).trim()
            if (emoji.isNotEmpty()) {
                repository.applyIncomingReaction(conversation.id, targetPayloadId, emoji)
            }
        }
        return ProcessResult.DecryptedData(
            messageId = 0,
            conversationId = conversation.id,
            senderPhone = senderPhone,
            plaintext = "واکنش دریافت شد",
            isVerified = conversation.verificationStatus == VerificationStatus.VERIFIED
        )
    }

    private fun cleanExpiredDedupeCache(now: Long) {
        if (recentSmsDedupe.size > 200) {
            val iterator = recentSmsDedupe.entries.iterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                if (now - entry.value > 60_000L) {
                    iterator.remove()
                }
            }
        }
    }
}
