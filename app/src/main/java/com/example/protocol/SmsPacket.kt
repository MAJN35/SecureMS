package com.example.protocol

import java.nio.ByteBuffer

/**
 * Binary wire envelope for SecureSMS packets transmitted over SMS.
 *
 * Header Format (29 bytes total):
 * - Magic (2 bytes): 0x53, 0x53 ('S', 'S')
 * - Version (1 byte): 0x01
 * - Message Type (1 byte): DATA=1, KEY_OFFER=2, KEY_ACCEPT=3, ACK=4
 * - Message ID (4 bytes): Unique identifier correlating fragments of the same message
 * - Fragment Index (1 byte): 0..Count-1
 * - Fragment Count (1 byte): 1..20
 * - Sender Key ID (8 bytes): Truncated SHA-256 of sender public key
 * - Recipient Key ID (8 bytes): Truncated SHA-256 of recipient public key
 * - Sequence Number (4 bytes): Monotonic message sequence counter
 *
 * Body:
 * - Chunk payload bytes (either whole ciphertext or fragment slice)
 */
data class SmsPacket(
    val version: Byte = PROTOCOL_VERSION,
    val messageType: MessageType,
    val messageId: Int,
    val fragmentIndex: Int,
    val fragmentCount: Int,
    val senderKeyId: ByteArray,
    val recipientKeyId: ByteArray,
    val sequenceNumber: Int,
    val payload: ByteArray
) {
    enum class MessageType(val code: Byte) {
        DATA(1),
        KEY_OFFER(2),
        KEY_ACCEPT(3),
        DELIVERY_ACK(4),
        SEEN_ACK(5),
        REACTION(6);

        companion object {
            fun fromCode(code: Byte): MessageType? {
                return entries.find { it.code == code }
            }
        }
    }

    init {
        require(senderKeyId.size == 8) { "Sender Key ID must be 8 bytes" }
        require(recipientKeyId.size == 8) { "Recipient Key ID must be 8 bytes" }
        require(fragmentCount in 1..MAX_FRAGMENTS) { "Fragment count out of bounds: $fragmentCount" }
        require(fragmentIndex in 0 until fragmentCount) { "Fragment index invalid: $fragmentIndex" }
    }

    /**
     * Compute the Additional Authenticated Data (AAD) for this packet.
     * Binds protocol metadata directly into the AES-256-GCM tag.
     */
    fun computeAad(): ByteArray {
        val bb = ByteBuffer.allocate(26)
        bb.put(version)
        bb.put(messageType.code)
        bb.putInt(messageId)
        bb.put(senderKeyId)
        bb.put(recipientKeyId)
        bb.putInt(sequenceNumber)
        return bb.array()
    }

    /**
     * Serialize to raw binary envelope.
     */
    fun toByteArray(): ByteArray {
        val totalSize = HEADER_SIZE + payload.size
        val bb = ByteBuffer.allocate(totalSize)
        bb.put(MAGIC_0)
        bb.put(MAGIC_1)
        bb.put(version)
        bb.put(messageType.code)
        bb.putInt(messageId)
        bb.put(fragmentIndex.toByte())
        bb.put(fragmentCount.toByte())
        bb.put(senderKeyId)
        bb.put(recipientKeyId)
        bb.putInt(sequenceNumber)
        bb.put(payload)
        return bb.array()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SmsPacket
        return messageId == other.messageId &&
                fragmentIndex == other.fragmentIndex &&
                fragmentCount == other.fragmentCount &&
                sequenceNumber == other.sequenceNumber &&
                messageType == other.messageType &&
                senderKeyId.contentEquals(other.senderKeyId) &&
                recipientKeyId.contentEquals(other.recipientKeyId) &&
                payload.contentEquals(other.payload)
    }

    override fun hashCode(): Int {
        var result = messageId
        result = 31 * result + fragmentIndex
        result = 31 * result + fragmentCount
        result = 31 * result + sequenceNumber
        result = 31 * result + messageType.hashCode()
        result = 31 * result + senderKeyId.contentHashCode()
        result = 31 * result + recipientKeyId.contentHashCode()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    companion object {
        const val PROTOCOL_VERSION: Byte = 1
        const val MAGIC_0: Byte = 0x53 // 'S'
        const val MAGIC_1: Byte = 0x53 // 'S'
        const val HEADER_SIZE = 30
        const val MAX_FRAGMENTS = 250

        /**
         * Parse a binary wire packet.
         */
        fun fromByteArray(data: ByteArray): SmsPacket {
            require(data.size >= HEADER_SIZE) { "Data too short for SecureSMS envelope: ${data.size} bytes" }

            val bb = ByteBuffer.wrap(data)
            val m0 = bb.get()
            val m1 = bb.get()
            require(m0 == MAGIC_0 && m1 == MAGIC_1) { "Invalid Magic bytes: not a SecureSMS packet" }

            val version = bb.get()
            require(version == PROTOCOL_VERSION) { "Unsupported protocol version: $version" }

            val typeCode = bb.get()
            val messageType = MessageType.fromCode(typeCode)
                ?: throw IllegalArgumentException("Unknown message type code: $typeCode")

            val messageId = bb.getInt()
            val fragmentIndex = bb.get().toInt() and 0xFF
            val fragmentCount = bb.get().toInt() and 0xFF

            val senderKeyId = ByteArray(8)
            bb.get(senderKeyId)

            val recipientKeyId = ByteArray(8)
            bb.get(recipientKeyId)

            val sequenceNumber = bb.getInt()

            val payloadLength = bb.remaining()
            val payload = ByteArray(payloadLength)
            bb.get(payload)

            return SmsPacket(
                version = version,
                messageType = messageType,
                messageId = messageId,
                fragmentIndex = fragmentIndex,
                fragmentCount = fragmentCount,
                senderKeyId = senderKeyId,
                recipientKeyId = recipientKeyId,
                sequenceNumber = sequenceNumber,
                payload = payload
            )
        }
    }
}
