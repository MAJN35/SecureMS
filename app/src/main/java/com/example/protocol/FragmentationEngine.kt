package com.example.protocol

import com.example.crypto.PersianCiphertextCodec
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

/**
 * Handles fragmentation of outgoing encrypted payloads into SMS-compliant chunks
 * and reassembly of incoming out-of-order, duplicated, or fragmented packets.
 *
 * Supports large multi-kilobyte payloads (up to 250 fragments, well over 16KB)
 * by streaming and reassembling chunk-by-chunk with memory safety guarantees.
 */
class FragmentationEngine {

    // In-memory buffer for pending fragments: MessageId -> Map<FragmentIndex, SmsPacket>
    private val pendingReassembly = ConcurrentHashMap<Int, ConcurrentHashMap<Int, SmsPacket>>()
    private val reassemblyTimestamps = ConcurrentHashMap<Int, Long>()

    // Event bus for notifying UI of incoming and outgoing chunk stack progress
    private val _fragmentEvents = MutableSharedFlow<FragmentProgressEvent>(extraBufferCapacity = 64)
    val fragmentEvents: SharedFlow<FragmentProgressEvent> = _fragmentEvents

    /**
     * Splits an encrypted payload into fragmented SmsPackets.
     *
     * @param fullPayload Combined IV + Ciphertext + Tag
     * @param messageType Packet type
     * @param messageId Unique 32-bit ID
     * @param senderKeyId 8-byte sender ID
     * @param recipientKeyId 8-byte recipient ID
     * @param sequenceNumber Monotonic message sequence
     * @param maxChunkSize Max payload bytes per SMS fragment
     */
    fun fragment(
        fullPayload: ByteArray,
        messageType: SmsPacket.MessageType,
        messageId: Int,
        senderKeyId: ByteArray,
        recipientKeyId: ByteArray,
        sequenceNumber: Int,
        maxChunkSize: Int = DEFAULT_CHUNK_SIZE
    ): List<SmsPacket> {
        val totalBytes = fullPayload.size
        val chunkCount = Math.ceil(totalBytes.toDouble() / maxChunkSize.toDouble()).toInt()
        val totalFragments = Math.max(1, chunkCount)

        require(totalFragments <= SmsPacket.MAX_FRAGMENTS) {
            "Payload requires $totalFragments fragments, exceeding protocol maximum (${SmsPacket.MAX_FRAGMENTS})"
        }

        val fragments = ArrayList<SmsPacket>(totalFragments)

        for (index in 0 until totalFragments) {
            val start = index * maxChunkSize
            val end = Math.min(start + maxChunkSize, totalBytes)
            val chunk = fullPayload.copyOfRange(start, end)

            fragments.add(
                SmsPacket(
                    version = SmsPacket.PROTOCOL_VERSION,
                    messageType = messageType,
                    messageId = messageId,
                    fragmentIndex = index,
                    fragmentCount = totalFragments,
                    senderKeyId = senderKeyId,
                    recipientKeyId = recipientKeyId,
                    sequenceNumber = sequenceNumber,
                    payload = chunk
                )
            )
        }

        return fragments
    }

    /**
     * Ingest an incoming SmsPacket.
     *
     * @return ReassembledResult: if all fragments for this messageId are now present, returns
     *         COMPLETE with the combined payload and representative packet; otherwise returns
     *         PENDING with current count vs total.
     */
    @Synchronized
    fun ingestFragment(packet: SmsPacket): ReassemblyResult {
        cleanExpiredFragments()

        // Fast path: single fragment message
        if (packet.fragmentCount == 1) {
            return ReassemblyResult.Complete(
                messageId = packet.messageId,
                fullPayload = packet.payload,
                representativePacket = packet
            )
        }

        val messageId = packet.messageId
        val fragmentMap = pendingReassembly.computeIfAbsent(messageId) {
            ConcurrentHashMap()
        }
        reassemblyTimestamps.putIfAbsent(messageId, System.currentTimeMillis())

        // Duplicate fragment detection
        fragmentMap[packet.fragmentIndex] = packet

        val receivedCount = fragmentMap.size
        val totalCount = packet.fragmentCount

        // Emit real-time progress for UI chunk stack tracking
        _fragmentEvents.tryEmit(
            FragmentProgressEvent(
                messageId = messageId,
                receivedCount = receivedCount,
                totalCount = totalCount,
                receivedIndices = fragmentMap.keys.toSet(),
                isComplete = receivedCount == totalCount
            )
        )

        if (receivedCount == totalCount) {
            // All fragments received! Reassemble in strict index order
            val baos = ByteArrayOutputStream()
            for (i in 0 until totalCount) {
                val frag = fragmentMap[i]
                    ?: return ReassemblyResult.Pending(messageId, receivedCount, totalCount)
                baos.write(frag.payload)
            }

            pendingReassembly.remove(messageId)
            reassemblyTimestamps.remove(messageId)

            return ReassemblyResult.Complete(
                messageId = messageId,
                fullPayload = baos.toByteArray(),
                representativePacket = packet
            )
        } else {
            return ReassemblyResult.Pending(messageId, receivedCount, totalCount)
        }
    }

    fun getReceivedIndices(messageId: Int): Set<Int> {
        return pendingReassembly[messageId]?.keys?.toSet() ?: emptySet()
    }

    fun getPendingInfo(messageId: Int): Pair<Int, Int>? {
        val map = pendingReassembly[messageId] ?: return null
        val total = map.values.firstOrNull()?.fragmentCount ?: return null
        return Pair(map.size, total)
    }

    /**
     * Purges fragments older than the timeout (e.g. 24 hours) to prevent memory leak/DoS.
     */
    fun cleanExpiredFragments(timeoutMillis: Long = TIMEOUT_MILLIS) {
        val now = System.currentTimeMillis()
        val it = reassemblyTimestamps.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            if (now - entry.value > timeoutMillis) {
                pendingReassembly.remove(entry.key)
                it.remove()
            }
        }
    }

    /**
     * Returns count of currently incomplete reassembly buffers.
     */
    fun getPendingMessageCount(): Int = pendingReassembly.size

    data class FragmentProgressEvent(
        val messageId: Int,
        val receivedCount: Int,
        val totalCount: Int,
        val receivedIndices: Set<Int>,
        val isComplete: Boolean
    )

    sealed class ReassemblyResult {
        data class Complete(
            val messageId: Int,
            val fullPayload: ByteArray,
            val representativePacket: SmsPacket
        ) : ReassemblyResult()

        data class Pending(
            val messageId: Int,
            val receivedCount: Int,
            val totalCount: Int
        ) : ReassemblyResult()
    }

    companion object {
        const val DEFAULT_CHUNK_SIZE = 65 // Optimized for multi-part SMS and Persian Base32
        const val TIMEOUT_MILLIS = 24 * 60 * 60 * 1000L // 24 hours
    }
}
