package com.example.sms

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.example.crypto.CryptoEngine
import com.example.crypto.PersianCiphertextCodec
import com.example.data.DeliveryStatus
import com.example.data.EncryptedStorageRepository
import com.example.protocol.FragmentationEngine
import com.example.protocol.SmsPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom

/**
 * Manages SMS transmission using Android's Telephony SmsManager,
 * supports Direct, Group, and Broadcast Channel multi-recipient fan-out,
 * and integrates an in-app virtual test loopback for emulator and offline lab testing.
 */
class SmsTransportManager(
    private val context: Context,
    private val repository: EncryptedStorageRepository,
    private val fragmentationEngine: FragmentationEngine,
    private val smsProcessor: SmsProcessor
) {
    private val secureRandom = SecureRandom()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private val _transportEvents = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    val transportEvents: SharedFlow<TransportEvent> = _transportEvents

    suspend fun sendEncryptedMessage(
        conversationId: Long,
        recipientPhone: String,
        plaintext: String,
        encodingMode: PersianCiphertextCodec.EncodingMode = PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS,
        subscriptionId: Int? = null,
        isVirtualSimulation: Boolean = false,
        replyToMessageId: Long = 0L,
        replyToPayloadId: Int = 0,
        replyToPreview: String = ""
    ): Long {
        val conversation = repository.getConversationById(conversationId)
            ?: throw IllegalArgumentException("Conversation $conversationId not found")

        val theirPubKeyBase64 = conversation.theirPublicKeyBase64
            ?: throw IllegalStateException("Cannot send encrypted message: recipient public key has not been exchanged or verified yet.")

        val theirPubKey = CryptoEngine.parsePublicKeyFromBase64(theirPubKeyBase64)
        val myPrivKey = repository.getMyPrivateKey()
        val myPubKey = repository.getMyPublicKey()

        // 1. Derive shared symmetric key via ECDH + HKDF
        val sharedKey = CryptoEngine.deriveSharedKey(myPrivKey, theirPubKey)

        // 2. Prepare message identifiers & sequence
        val messagePayloadId = secureRandom.nextInt() and 0x7FFFFFFF
        val sequenceNumber = conversation.nextOutboundSeq
        val senderKeyId = CryptoEngine.computeKeyId(myPubKey)
        val recipientKeyId = CryptoEngine.computeKeyId(theirPubKey)

        // 3. Construct context-bound AAD
        val dummyPacket = SmsPacket(
            version = SmsPacket.PROTOCOL_VERSION,
            messageType = SmsPacket.MessageType.DATA,
            messageId = messagePayloadId,
            fragmentIndex = 0,
            fragmentCount = 1,
            senderKeyId = senderKeyId,
            recipientKeyId = recipientKeyId,
            sequenceNumber = sequenceNumber,
            payload = ByteArray(0)
        )
        val aad = dummyPacket.computeAad()

        // 4. Pack wire reply header and optional group/channel header, then encrypt via AES-256-GCM
        val replyPacked = EncryptedStorageRepository.packWireReply(
            plaintext = plaintext,
            replyToPayloadId = replyToPayloadId,
            replyToPreview = replyToPreview
        )
        val wirePlaintext = EncryptedStorageRepository.packWireGroupHeader(
            text = replyPacked,
            conversationType = conversation.conversationType,
            roomIdentifier = conversation.phoneNumber,
            roomTitle = conversation.contactName
        )

        val encrypted = CryptoEngine.encrypt(
            sharedKey = sharedKey,
            plaintext = wirePlaintext.toByteArray(Charsets.UTF_8),
            aad = aad
        )
        val combinedCiphertext = encrypted.toCombined()

        // 5. Fragment into SMS segments if required
        val fragments = fragmentationEngine.fragment(
            fullPayload = combinedCiphertext,
            messageType = SmsPacket.MessageType.DATA,
            messageId = messagePayloadId,
            senderKeyId = senderKeyId,
            recipientKeyId = recipientKeyId,
            sequenceNumber = sequenceNumber
        )

        // 6. Encode each fragment into SMS text
        val encodedSmsSegments = fragments.map { fragment ->
            val rawBytes = fragment.toByteArray()
            when (encodingMode) {
                PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS,
                PersianCiphertextCodec.EncodingMode.AUTOMATIC -> {
                    PersianCiphertextCodec.encodeToPersianLetters(rawBytes)
                }
                PersianCiphertextCodec.EncodingMode.PERSIAN_NUMERALS -> {
                    PersianCiphertextCodec.encodeToPersianNumerals(rawBytes)
                }
                PersianCiphertextCodec.EncodingMode.STANDARD_B64 -> {
                    PersianCiphertextCodec.encodeToStandardBase64(rawBytes)
                }
            }
        }

        val previewRawText = encodedSmsSegments.firstOrNull() ?: ""
        val senderDisplayName = if (conversation.isChannel) conversation.contactName else "من"

        // 7. Save message to database as QUEUED
        val msgId = repository.saveOutgoingMessage(
            conversationId = conversationId,
            senderPhone = "Me",
            recipientPhone = recipientPhone,
            plaintext = plaintext,
            rawCiphertext = previewRawText,
            sequenceNumber = sequenceNumber,
            fragmentCount = fragments.size,
            encodingUsed = encodingMode.name,
            messagePayloadId = messagePayloadId,
            replyToMessageId = replyToMessageId,
            replyToPreview = replyToPreview,
            senderDisplayName = senderDisplayName
        )

        // Determine target recipient phones (single contact or multi-member fan-out for Group/Channel)
        val targetPhones = if (conversation.isGroup || conversation.isChannel) {
            conversation.parsedMemberPhones()
        } else {
            listOf(recipientPhone)
        }

        // 8. Transmit via Cellular SMS or Virtual Loopback Simulator
        coroutineScope.launch {
            if (isVirtualSimulation || targetPhones.isEmpty()) {
                repository.updateMessageStatus(msgId, DeliveryStatus.SENDING)
                _transportEvents.emit(
                    TransportEvent(
                        type = "SIMULATING_SEND",
                        details = "Sending ${fragments.size} chunk(s) to ${targetPhones.size.coerceAtLeast(1)} recipient(s) via Virtual SMS Lab"
                    )
                )

                encodedSmsSegments.forEachIndexed { index, segmentText ->
                    kotlinx.coroutines.delay(220)
                    repository.updateOutgoingChunkProgress(messagePayloadId, index + 1, fragments.size)
                    _transportEvents.emit(
                        TransportEvent(
                            type = "CHUNK_SENT",
                            details = "Chunk ${index + 1}/${fragments.size}: ${segmentText.take(24)}..."
                        )
                    )
                }

                repository.updateMessageStatus(msgId, DeliveryStatus.DELIVERED_CARRIER)
                val latestConv = repository.getConversationById(conversationId)
                if (latestConv?.sendReadReceipts == true) {
                    kotlinx.coroutines.delay(600)
                    repository.updateMessageStatus(msgId, DeliveryStatus.SEEN)
                    _transportEvents.emit(
                        TransportEvent(
                            type = "SEEN_ACK_SIMULATED",
                            details = "رسید خواندن (دیده شد) دریافت شد"
                        )
                    )
                }
            } else {
                for (memberPhone in targetPhones) {
                    sendViaTelephonySmsChunkByChunk(memberPhone, encodedSmsSegments, messagePayloadId, subscriptionId)
                }
            }
        }

        return msgId
    }

    /**
     * Simulates an incoming encrypted message from a Group member so users can test
     * multi-member liquid group chat bubbles and interactions offline.
     */
    suspend fun simulateIncomingGroupMemberMessage(
        conversationId: Long,
        memberName: String,
        memberPhone: String,
        text: String
    ): Long {
        val conv = repository.getConversationById(conversationId) ?: return 0L
        val payloadId = secureRandom.nextInt() and 0x7FFFFFFF
        val seq = conv.highestInboundSeq + 1
        val fakeCipher = PersianCiphertextCodec.encodeToPersianLetters(
            "SSMS_GROUP_${conv.id}_$payloadId".toByteArray(Charsets.UTF_8)
        )
        return repository.saveIncomingMessage(
            conversationId = conversationId,
            senderPhone = memberPhone,
            recipientPhone = "Me",
            plaintext = text,
            rawCiphertext = fakeCipher,
            sequenceNumber = seq,
            fragmentCount = 1,
            encodingUsed = conv.preferredEncoding,
            messagePayloadId = payloadId,
            senderDisplayName = memberName
        )
    }

    /**
     * Toggle a reaction on a message and optionally transmit an encrypted REACTION control packet.
     */
    suspend fun sendReaction(
        conversationId: Long,
        recipientPhone: String,
        targetMessageId: Long,
        targetPayloadId: Int,
        emoji: String,
        encodingMode: PersianCiphertextCodec.EncodingMode = PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS,
        subscriptionId: Int? = null,
        isVirtualSimulation: Boolean = false
    ) {
        repository.toggleMessageReaction(targetMessageId, emoji)
        val conversation = repository.getConversationById(conversationId) ?: return
        val theirPubKeyBase64 = conversation.theirPublicKeyBase64 ?: return

        coroutineScope.launch {
            try {
                val theirPubKey = CryptoEngine.parsePublicKeyFromBase64(theirPubKeyBase64)
                val myPrivKey = repository.getMyPrivateKey()
                val myPubKey = repository.getMyPublicKey()
                val sharedKey = CryptoEngine.deriveSharedKey(myPrivKey, theirPubKey)

                val packetId = secureRandom.nextInt() and 0x7FFFFFFF
                val seq = conversation.nextOutboundSeq
                val senderKeyId = CryptoEngine.computeKeyId(myPubKey)
                val recipientKeyId = CryptoEngine.computeKeyId(theirPubKey)

                val dummyPacket = SmsPacket(
                    version = SmsPacket.PROTOCOL_VERSION,
                    messageType = SmsPacket.MessageType.REACTION,
                    messageId = packetId,
                    fragmentIndex = 0,
                    fragmentCount = 1,
                    senderKeyId = senderKeyId,
                    recipientKeyId = recipientKeyId,
                    sequenceNumber = seq,
                    payload = ByteArray(0)
                )
                val aad = dummyPacket.computeAad()
                val reactionWire = "$targetPayloadId|$emoji"
                val encrypted = CryptoEngine.encrypt(
                    sharedKey = sharedKey,
                    plaintext = reactionWire.toByteArray(Charsets.UTF_8),
                    aad = aad
                )
                val packet = dummyPacket.copy(payload = encrypted.toCombined())
                val encodedText = when (encodingMode) {
                    PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS,
                    PersianCiphertextCodec.EncodingMode.AUTOMATIC ->
                        PersianCiphertextCodec.encodeToPersianLetters(packet.toByteArray())
                    PersianCiphertextCodec.EncodingMode.PERSIAN_NUMERALS ->
                        PersianCiphertextCodec.encodeToPersianNumerals(packet.toByteArray())
                    PersianCiphertextCodec.EncodingMode.STANDARD_B64 ->
                        PersianCiphertextCodec.encodeToStandardBase64(packet.toByteArray())
                }

                val targetPhones = if (conversation.isGroup || conversation.isChannel) {
                    conversation.parsedMemberPhones()
                } else {
                    listOf(recipientPhone)
                }

                if (isVirtualSimulation || targetPhones.isEmpty()) {
                    _transportEvents.emit(
                        TransportEvent(
                            type = "REACTION_SIMULATED",
                            details = "واکنش $emoji برای پیام #$targetPayloadId ثبت شد"
                        )
                    )
                } else {
                    for (phone in targetPhones) {
                        sendViaTelephonySmsChunkByChunk(phone, listOf(encodedText), packetId, subscriptionId)
                    }
                }
            } catch (e: Exception) {
                Log.w("SmsTransportManager", "Failed to send reaction packet: ${e.message}")
            }
        }
    }

    /**
     * Send an optional "Seen" (Read Receipt) packet if and only if enabled for this contact.
     */
    suspend fun sendSeenReceiptIfEnabled(
        conversationId: Long,
        latestInboundPayloadId: Int,
        encodingMode: PersianCiphertextCodec.EncodingMode = PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS,
        subscriptionId: Int? = null,
        isVirtualSimulation: Boolean = false
    ): Boolean {
        if (latestInboundPayloadId == 0) return false
        val conversation = repository.getConversationById(conversationId) ?: return false
        if (!conversation.sendReadReceipts) return false
        if (conversation.lastSeenAckPayloadId == latestInboundPayloadId) return false

        repository.updateLastSeenAckPayloadId(conversationId, latestInboundPayloadId)

        val theirPubKeyBase64 = conversation.theirPublicKeyBase64 ?: return false
        val targetPhones = if (conversation.isGroup || conversation.isChannel) {
            conversation.parsedMemberPhones()
        } else {
            listOf(conversation.phoneNumber)
        }

        coroutineScope.launch {
            try {
                val theirPubKey = CryptoEngine.parsePublicKeyFromBase64(theirPubKeyBase64)
                val myPubKey = repository.getMyPublicKey()
                val senderKeyId = CryptoEngine.computeKeyId(myPubKey)
                val recipientKeyId = CryptoEngine.computeKeyId(theirPubKey)

                val packet = SmsPacket(
                    version = SmsPacket.PROTOCOL_VERSION,
                    messageType = SmsPacket.MessageType.SEEN_ACK,
                    messageId = latestInboundPayloadId,
                    fragmentIndex = 0,
                    fragmentCount = 1,
                    senderKeyId = senderKeyId,
                    recipientKeyId = recipientKeyId,
                    sequenceNumber = conversation.nextOutboundSeq,
                    payload = byteArrayOf(1)
                )
                val encodedText = when (encodingMode) {
                    PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS,
                    PersianCiphertextCodec.EncodingMode.AUTOMATIC ->
                        PersianCiphertextCodec.encodeToPersianLetters(packet.toByteArray())
                    PersianCiphertextCodec.EncodingMode.PERSIAN_NUMERALS ->
                        PersianCiphertextCodec.encodeToPersianNumerals(packet.toByteArray())
                    PersianCiphertextCodec.EncodingMode.STANDARD_B64 ->
                        PersianCiphertextCodec.encodeToStandardBase64(packet.toByteArray())
                }

                if (isVirtualSimulation || targetPhones.isEmpty()) {
                    _transportEvents.emit(
                        TransportEvent(
                            type = "SEEN_RECEIPT_SENT",
                            details = "رسید «دیده شد» برای پیام #$latestInboundPayloadId ارسال شد"
                        )
                    )
                } else {
                    for (phone in targetPhones) {
                        sendViaTelephonySmsChunkByChunk(phone, listOf(encodedText), latestInboundPayloadId, subscriptionId)
                    }
                }
            } catch (e: Exception) {
                Log.w("SmsTransportManager", "Failed to send SEEN_ACK packet: ${e.message}")
            }
        }
        return true
    }

    suspend fun sendKeyOffer(
        recipientPhone: String,
        encodingMode: PersianCiphertextCodec.EncodingMode = PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS
    ) {
        Log.i("SmsTransportManager", "Key sharing via SMS is disabled")
    }

    suspend fun sendKeyAccept(
        recipientPhone: String,
        encodingMode: PersianCiphertextCodec.EncodingMode = PersianCiphertextCodec.EncodingMode.PERSIAN_LETTERS
    ) {
        Log.i("SmsTransportManager", "Key sharing via SMS is disabled")
    }

    suspend fun sendStandardSms(
        conversationId: Long,
        recipientPhone: String,
        plaintext: String,
        subscriptionId: Int? = null,
        isVirtualSimulation: Boolean = false,
        replyToMessageId: Long = 0L,
        replyToPayloadId: Int = 0,
        replyToPreview: String = ""
    ): Long {
        val conversation = repository.getConversationById(conversationId)
        val smsManager = if (subscriptionId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        val replyPacked = EncryptedStorageRepository.packWireReply(
            plaintext = plaintext,
            replyToPayloadId = replyToPayloadId,
            replyToPreview = replyToPreview
        )
        val wireText = if (conversation != null) {
            EncryptedStorageRepository.packWireGroupHeader(
                text = replyPacked,
                conversationType = conversation.conversationType,
                roomIdentifier = conversation.phoneNumber,
                roomTitle = conversation.contactName
            )
        } else {
            replyPacked
        }

        val parts = smsManager.divideMessage(wireText) ?: arrayListOf(wireText)
        val senderDisplayName = if (conversation?.isChannel == true) conversation.contactName else "من"
        val msgId = repository.saveOutgoingStandardSms(
            conversationId = conversationId,
            senderPhone = "Me",
            recipientPhone = recipientPhone,
            plaintext = plaintext,
            fragmentCount = parts.size,
            replyToMessageId = replyToMessageId,
            replyToPreview = replyToPreview,
            senderDisplayName = senderDisplayName
        )

        val targetPhones = if (conversation != null && (conversation.isGroup || conversation.isChannel)) {
            conversation.parsedMemberPhones()
        } else {
            listOf(recipientPhone)
        }

        coroutineScope.launch {
            if (isVirtualSimulation || targetPhones.isEmpty()) {
                _transportEvents.emit(
                    TransportEvent(
                        type = "STANDARD_SMS_SIMULATED",
                        details = "پیامک عادی ارسال شد (${parts.size} بخش)"
                    )
                )
                repository.updateMessageStatus(msgId, DeliveryStatus.DELIVERED_CARRIER)
                val latestConv = repository.getConversationById(conversationId)
                if (latestConv?.sendReadReceipts == true) {
                    kotlinx.coroutines.delay(650)
                    repository.updateMessageStatus(msgId, DeliveryStatus.SEEN)
                }
            } else {
                try {
                    for (phone in targetPhones) {
                        if (parts.size > 1) {
                            val sentIntents = ArrayList<PendingIntent>(parts.size)
                            for (i in 0 until parts.size) {
                                val intent = PendingIntent.getBroadcast(
                                    context,
                                    msgId.toInt() + i,
                                    Intent("com.example.securesms.SMS_SENT"),
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                sentIntents.add(intent)
                            }
                            smsManager.sendMultipartTextMessage(phone, null, parts, sentIntents, null)
                        } else {
                            val sentIntent = PendingIntent.getBroadcast(
                                context,
                                msgId.toInt(),
                                Intent("com.example.securesms.SMS_SENT"),
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                            )
                            smsManager.sendTextMessage(phone, null, wireText, sentIntent, null)
                        }
                    }
                    repository.updateMessageStatus(msgId, DeliveryStatus.SENT_TO_CARRIER)
                } catch (e: Exception) {
                    Log.e("SmsTransportManager", "Error transmitting standard SMS: ${e.message}", e)
                    repository.updateMessageStatus(msgId, DeliveryStatus.FAILED)
                }
            }
        }

        return msgId
    }

    private suspend fun sendViaTelephonySmsChunkByChunk(
        recipientPhone: String,
        smsSegments: List<String>,
        payloadId: Int,
        subscriptionId: Int?
    ) {
        try {
            val smsManager = if (subscriptionId != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java).createForSubscriptionId(subscriptionId)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            for ((idx, text) in smsSegments.withIndex()) {
                val parts = smsManager.divideMessage(text) ?: arrayListOf(text)
                if (parts.size > 1) {
                    val sentIntents = ArrayList<PendingIntent>(parts.size)
                    val deliveredIntents = ArrayList<PendingIntent>(parts.size)
                    for (i in 0 until parts.size) {
                        val sIntent = PendingIntent.getBroadcast(
                            context,
                            payloadId + idx * 100 + i,
                            Intent("com.example.securesms.SMS_SENT").apply {
                                putExtra("payloadId", payloadId)
                                putExtra("fragmentIndex", idx)
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        val dIntent = PendingIntent.getBroadcast(
                            context,
                            payloadId + idx * 100 + i + 10000,
                            Intent("com.example.securesms.SMS_DELIVERED").apply {
                                putExtra("payloadId", payloadId)
                            },
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        sentIntents.add(sIntent)
                        deliveredIntents.add(dIntent)
                    }
                    smsManager.sendMultipartTextMessage(recipientPhone, null, parts, sentIntents, deliveredIntents)
                } else {
                    val sentIntent = PendingIntent.getBroadcast(
                        context,
                        payloadId + idx,
                        Intent("com.example.securesms.SMS_SENT").apply {
                            putExtra("payloadId", payloadId)
                            putExtra("fragmentIndex", idx)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    val deliveredIntent = PendingIntent.getBroadcast(
                        context,
                        payloadId + idx + 1000,
                        Intent("com.example.securesms.SMS_DELIVERED").apply {
                            putExtra("payloadId", payloadId)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )

                    smsManager.sendTextMessage(recipientPhone, null, text, sentIntent, deliveredIntent)
                }

                repository.updateOutgoingChunkProgress(payloadId, idx + 1, smsSegments.size)

                if (idx < smsSegments.size - 1) {
                    kotlinx.coroutines.delay(200)
                }
            }

            repository.updateStatusByPayloadId(payloadId, DeliveryStatus.SENT_TO_CARRIER)
        } catch (e: Exception) {
            Log.e("SmsTransportManager", "Error transmitting SMS chunks: ${e.message}", e)
            repository.updateStatusByPayloadId(payloadId, DeliveryStatus.FAILED)
        }
    }

    data class TransportEvent(
        val type: String,
        val details: String,
        val timestamp: Long = System.currentTimeMillis()
    )
}
