package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SecureSmsDao {

    // Identity Key
    @Query("SELECT * FROM identity_key WHERE id = 1 LIMIT 1")
    suspend fun getIdentityKey(): IdentityKeyEntity?

    @Query("SELECT * FROM identity_key WHERE id = 1 LIMIT 1")
    fun getIdentityKeyFlow(): Flow<IdentityKeyEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateIdentityKey(key: IdentityKeyEntity)

    // Conversations
    @Query("SELECT * FROM conversations ORDER BY lastMessageTimestamp DESC")
    fun getAllConversationsFlow(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    fun getConversationFlow(id: Long): Flow<ConversationEntity?>

    @Query("SELECT * FROM conversations WHERE phoneNumber = :phone LIMIT 1")
    suspend fun getConversationByPhone(phone: String): ConversationEntity?

    @Query("SELECT * FROM conversations")
    suspend fun getAllConversationsList(): List<ConversationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("UPDATE conversations SET unreadCount = 0 WHERE id = :conversationId")
    suspend fun markConversationRead(conversationId: Long)

    @Query("UPDATE conversations SET sendReadReceipts = :enabled WHERE id = :conversationId")
    suspend fun updateSendReadReceipts(conversationId: Long, enabled: Boolean)

    @Query("UPDATE conversations SET sendReadReceipts = :enabled")
    suspend fun updateAllConversationsReadReceipts(enabled: Boolean)

    @Query("UPDATE conversations SET lastSeenAckPayloadId = :payloadId WHERE id = :conversationId")
    suspend fun updateLastSeenAckPayloadId(conversationId: Long, payloadId: Int)

    @Query("UPDATE conversations SET memberPhones = :memberPhones, memberNames = :memberNames WHERE id = :conversationId")
    suspend fun updateConversationMembers(conversationId: Long, memberPhones: String, memberNames: String)

    @Query("UPDATE conversations SET isOwner = :isOwner WHERE id = :conversationId")
    suspend fun updateChannelRole(conversationId: Long, isOwner: Boolean)

    @Query("UPDATE conversations SET avatarUri = :avatarUri WHERE id = :conversationId")
    suspend fun updateConversationAvatar(conversationId: Long, avatarUri: String)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversation(id: Long)

    // Messages
    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversationFlow(conversationId: Long): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    suspend fun getMessagesForConversationList(conversationId: Long): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id LIMIT 1")
    suspend fun getMessageById(id: Long): MessageEntity?

    @Query("SELECT * FROM messages WHERE messagePayloadId = :payloadId LIMIT 1")
    suspend fun getMessageByPayloadId(payloadId: Int): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: DeliveryStatus)

    @Query("UPDATE messages SET deliveryStatus = :status WHERE messagePayloadId = :payloadId")
    suspend fun updateStatusByPayloadId(payloadId: Int, status: DeliveryStatus)

    @Query("UPDATE messages SET deliveryStatus = 'SEEN' WHERE conversationId = :conversationId AND isOutgoing = 1 AND deliveryStatus != 'FAILED'")
    suspend fun markAllOutgoingMessagesSeenInConversation(conversationId: Long)

    @Query("UPDATE messages SET reactions = :reactions WHERE id = :id")
    suspend fun updateMessageReactions(id: Long, reactions: String)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteMessage(id: Long)

    @Query("DELETE FROM messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)

    @Query("SELECT COUNT(*) FROM messages WHERE deliveryStatus = 'INCOMPLETE'")
    fun getIncompleteMessageCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM messages WHERE deliveryStatus = 'FAILED'")
    fun getFailedMessageCountFlow(): Flow<Int>

    // Total wipe for reset
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("DELETE FROM conversations")
    suspend fun deleteAllConversations()

    @Query("DELETE FROM identity_key")
    suspend fun deleteIdentityKey()
}
