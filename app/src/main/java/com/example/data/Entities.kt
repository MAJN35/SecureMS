package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ConversationType(val labelFa: String) {
    DIRECT("گفتگوی شخصی"),
    GROUP("گروه"),
    CHANNEL("کانال")
}

@Entity(
    tableName = "conversations",
    indices = [Index(value = ["phoneNumber"], unique = true)]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val phoneNumber: String,
    val contactName: String,
    val theirPublicKeyBase64: String? = null,
    val safetyNumber: String = "",
    val verificationStatus: VerificationStatus = VerificationStatus.UNVERIFIED,
    val lastMessageText: String = "",
    val lastMessageTimestamp: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0,
    val preferredEncoding: String = "AUTOMATIC",
    val nextOutboundSeq: Int = 1,
    val highestInboundSeq: Int = 0,
    val sendReadReceipts: Boolean = false,
    val lastSeenAckPayloadId: Int = 0,
    val conversationType: String = "DIRECT",
    val memberPhones: String = "",
    val memberNames: String = "",
    val description: String = "",
    val isOwner: Boolean = true,
    val avatarUri: String = ""
) {
    val isGroup: Boolean get() = conversationType == ConversationType.GROUP.name
    val isChannel: Boolean get() = conversationType == ConversationType.CHANNEL.name
    val isDirect: Boolean get() = !isGroup && !isChannel

    fun parsedMemberPhones(): List<String> {
        return memberPhones.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    fun parsedMembers(): List<Pair<String, String>> {
        val phones = parsedMemberPhones()
        val names = memberNames.split(",").map { it.trim() }
        return phones.mapIndexed { index, phone ->
            val name = names.getOrNull(index)?.takeIf { it.isNotBlank() } ?: phone
            Pair(name, phone)
        }
    }
}

enum class VerificationStatus(val label: String, val badgeColorHex: String) {
    UNVERIFIED("تأیید نشده (Unverified)", "#FFB703"),
    VERIFIED("تأیید شده (Verified)", "#06D6A0"),
    IDENTITY_CHANGED("تغییر کلید! (Identity Changed)", "#EF476F"),
    VERIFICATION_REQUIRED("نیاز به احراز هویت (Verification Required)", "#E63946")
}

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["messagePayloadId"])
    ]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val conversationId: Long,
    val senderPhone: String,
    val recipientPhone: String,
    val isOutgoing: Boolean,
    val encryptedPlaintext: String, // Encrypted at rest via Android Keystore
    val rawCiphertext: String,      // The actual SMS envelope text (Persian or Base64)
    val deliveryStatus: DeliveryStatus,
    val timestamp: Long = System.currentTimeMillis(),
    val sequenceNumber: Int = 0,
    val isEncrypted: Boolean = true,
    val fragmentCount: Int = 1,
    val encodingUsed: String = "PERSIAN_LETTERS",
    val messagePayloadId: Int = 0,
    val replyToMessageId: Long = 0L,
    val replyToPreview: String = "",
    val reactions: String = "",
    val senderDisplayName: String = ""
)

enum class DeliveryStatus(val label: String) {
    QUEUED("در صف ارسال"),
    SENDING("در حال ارسال"),
    SENT_TO_CARRIER("تحویل به اپراتور"),
    DELIVERED_CARRIER("تحویل داده شد"),
    SEEN("دیده شد"),
    RECEIVED("دریافت شد"),
    DECRYPTED("رمزگشایی شد"),
    FAILED("خطا در ارسال"),
    INCOMPLETE("در انتظار تکمیل بخش‌ها")
}

@Entity(tableName = "identity_key")
data class IdentityKeyEntity(
    @PrimaryKey
    val id: Int = 1,
    val publicKeyBase64: String,
    val encryptedPrivateKey: String,
    val fingerprint: String,
    val createdAt: Long = System.currentTimeMillis()
)
