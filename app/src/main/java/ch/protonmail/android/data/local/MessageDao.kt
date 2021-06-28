/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.data.local

import android.provider.BaseColumns
import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.COLUMN_ATTACHMENT_ID
import ch.protonmail.android.data.local.model.COLUMN_ATTACHMENT_MESSAGE_ID
import ch.protonmail.android.data.local.model.COLUMN_CONVERSATION_ID
import ch.protonmail.android.data.local.model.COLUMN_LABEL_ID
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_ACCESS_TIME
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_EXPIRATION_TIME
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_ID
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_IS_STARRED
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_LABELS
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_LOCATION
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_PREFIX_SENDER
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_SENDER_EMAIL
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_SENDER_NAME
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_SUBJECT
import ch.protonmail.android.data.local.model.COLUMN_MESSAGE_TIME
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.TABLE_ATTACHMENTS
import ch.protonmail.android.data.local.model.TABLE_LABELS
import ch.protonmail.android.data.local.model.TABLE_MESSAGES
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import timber.log.Timber

@Dao
interface MessageDao {

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_MESSAGE_SUBJECT LIKE '%'||:subject||'%'
          OR ${COLUMN_MESSAGE_PREFIX_SENDER + COLUMN_MESSAGE_SENDER_NAME} LIKE '%'||:senderName||'%'
          OR ${COLUMN_MESSAGE_PREFIX_SENDER + COLUMN_MESSAGE_SENDER_EMAIL} LIKE '%'||:senderEmail||'%'
            ORDER BY $COLUMN_MESSAGE_TIME DESC
    """
    )
    fun searchMessages(subject: String, senderName: String, senderEmail: String): List<Message>

    @Query("SELECT COUNT($COLUMN_MESSAGE_ID) FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_LOCATION = :location ")
    fun getMessagesCountByLocation(location: Int): Int

    @Query(
        """
            SELECT * FROM $TABLE_MESSAGES 
            WHERE $COLUMN_MESSAGE_LOCATION = :location  
            ORDER BY $COLUMN_MESSAGE_TIME DESC
            """
    )
    fun observeMessagesByLocation(location: Int): Flow<List<Message>>

    @Query(
        """
        SELECT * FROM $TABLE_MESSAGES 
        WHERE $COLUMN_MESSAGE_IS_STARRED=1
        ORDER BY $COLUMN_MESSAGE_TIME DESC
        """
    )
    fun observeStarredMessages(): Flow<List<Message>>

    @Query(
        """
        SELECT COUNT($COLUMN_MESSAGE_ID)
        FROM $TABLE_MESSAGES 
        WHERE $COLUMN_MESSAGE_LABELS LIKE '%' || :labelId || '%'  
    """
    )
    fun getMessagesCountByByLabelId(labelId: String): Int

    @Query("SELECT * FROM $TABLE_MESSAGES ORDER BY $COLUMN_MESSAGE_TIME DESC")
    fun getAllMessages(): LiveData<List<Message>>

    @Query("SELECT * FROM $TABLE_MESSAGES ORDER BY $COLUMN_MESSAGE_TIME DESC")
    fun observeAllMessages(): Flow<List<Message>>

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_MESSAGE_LABELS LIKE '%' || :label || '%'  
        ORDER BY $COLUMN_MESSAGE_TIME DESC
    """
    )
    fun getMessagesByLabelId(label: String): List<Message>

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_MESSAGE_LABELS LIKE '%' || :label || '%'  
        ORDER BY $COLUMN_MESSAGE_TIME DESC
    """
    )
    fun observeMessagesByLabelId(label: String): Flow<List<Message>>

    fun findMessageById(messageId: String): Flow<Message?> = findMessageInfoById(messageId)
        .onEach { message ->
            message ?: return@onEach
            message.attachments = message.attachments(this)
        }

    suspend fun findMessageByIdOnce(messageId: String): Message? = findMessageInfoByIdOnce(messageId)?.also { message ->
        message.attachments = findAttachmentByMessageId(messageId)
    }

    @Deprecated("Use Flow variant", ReplaceWith("findMessageById(messageId).first()"))
    fun findMessageByIdBlocking(messageId: String): Message? = findMessageInfoByIdBlocking(messageId)
        ?.also { message ->
            message.attachments = message.attachmentsBlocking(this)
        }

    fun findMessageByIdSingle(messageId: String) = findMessageInfoByIdSingle(messageId)

    fun findMessageByIdObservable(messageId: String) = findMessageInfoByIdObservable(messageId)

    fun findMessageByDatabaseId(messageDbId: Long): Flow<Message?> = findMessageInfoByDbId(messageDbId)
        .onEach { message ->
            message ?: return@onEach
            message.attachments = message.attachmentsBlocking(this)
        }

    fun findMessageByDatabaseIdBlocking(messageDbId: Long): Message? = runBlocking {
        findMessageByDatabaseId(messageDbId).first()
    }

    fun findMessageByDbId(dbId: Long): Flow<Message?> =
        findMessageInfoByDbId(dbId).map { message ->
            return@map message?.let {
                it.attachments = it.attachmentsBlocking(this)
                it
            }
        }

    fun findAllMessageByLastMessageAccessTime(laterThan: Long = 0): Flow<List<Message>> =
        findAllMessageInfoByLastMessageAccessTime(laterThan)
            .map { messages ->
                messages.onEach { message ->
                    message.attachments = message.attachments(this)
                }
            }

    fun findAllMessageFromAConversation(conversationId: String): Flow<List<Message>> =
        findAllMessageInfoFromAConversation(conversationId)
            .map { messages ->
                messages.onEach { message ->
                    message.attachments = message.attachments(this)
                }
            }

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    fun findMessageInfoById(messageId: String): Flow<Message?>

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    suspend fun findMessageInfoByIdOnce(messageId: String): Message?

    @Deprecated("Use Flow variant", ReplaceWith("findMessageInfoById(messageId).first()"))
    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    fun findMessageInfoByIdBlocking(messageId: String): Message?

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    fun findMessageInfoByIdSingle(messageId: String): Single<Message>

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    fun findMessageInfoByIdObservable(messageId: String): Flowable<Message>

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE ${BaseColumns._ID}=:messageDbId")
    fun findMessageInfoByDbId(messageDbId: Long): Flow<Message?>

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_MESSAGE_ACCESS_TIME > :laterThan
        ORDER BY $COLUMN_MESSAGE_ACCESS_TIME
    """
    )
    fun findAllMessageInfoByLastMessageAccessTime(laterThan: Long = 0): Flow<List<Message>>

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_CONVERSATION_ID = :conversationId
        ORDER BY $COLUMN_MESSAGE_TIME DESC
    """
    )
    fun findAllMessageInfoFromAConversation(conversationId: String): Flow<List<Message>>

    suspend fun saveMessage(message: Message): Long {
        Timber.d("saveMessage ${message.messageId}, location: ${message.location}, labels: ${message.allLabelIDs}")
        processMessageAttachments(message)
        return saveMessageInfo(message)
    }

    private suspend fun processMessageAttachments(message: Message) {
        val messageId = message.messageId
        var localAttachments: List<Attachment> = ArrayList()
        if (messageId != null) {
            localAttachments = findAttachmentsByMessageId(messageId).first()
        }

        var preservedAttachments = message.attachments.map { attachment ->
            Attachment(
                fileName = attachment.fileName,
                mimeType = attachment.mimeType,
                fileSize = attachment.fileSize,
                keyPackets = attachment.keyPackets,
                messageId = message.messageId ?: "",
                isUploaded = attachment.isUploaded,
                isUploading = attachment.isUploading,
                isNew = attachment.isNew,
                filePath = attachment.filePath,
                attachmentId = attachment.attachmentId,
                headers = attachment.headers,
                inline = attachment.inline
            )
        }

        val hasAnyAttachment =
            message.embeddedImageIds.isNotEmpty() && preservedAttachments.isEmpty() && localAttachments.isNotEmpty()
        if (hasAnyAttachment) {
            localAttachments.forEach { localAttachment ->
                localAttachment.setMessage(message)
                preservedAttachments = localAttachments
            }
        } else {
            preservedAttachments.forEach { preservedAtt ->
                if (localAttachments.isNotEmpty()) {
                    localAttachments.find { it.attachmentId == preservedAtt.attachmentId }?.let { attachment ->
                        if (attachment.inline) {
                            preservedAtt.inline = attachment.inline
                        } else {
                            if (message.embeddedImageIds.isNotEmpty()) {
                                preservedAtt.setMessage(message)
                            }
                        }
                        preservedAtt.isUploaded = attachment.isUploaded
                        preservedAtt.isUploading = attachment.isUploading
                    }
                } else {
                    if (message.embeddedImageIds.isNotEmpty()) {
                        preservedAtt.setMessage(message)
                    }
                }
            }
        }

        val attachmentsToDelete = message.attachments(this) // .filter { it.messageId != message.messageId }
        if (attachmentsToDelete.isNotEmpty() && preservedAttachments.isEmpty()) {
            preservedAttachments = localAttachments
        }
        if (attachmentsToDelete.isNotEmpty()) {
            deleteAllAttachments(attachmentsToDelete)
        }
        if (preservedAttachments.isNotEmpty()) {
            saveAllAttachments(preservedAttachments)
        }
        message.attachments = preservedAttachments
    }

    suspend fun saveMessages(messages: List<Message>) {
        Timber.d("saveMessages ${messages.map { it.messageId }}")
        messages.forEach {
            processMessageAttachments(it)
        }
        return saveMessagesInfo(messages)
    }

    @Query("DELETE FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_LOCATION = :location")
    fun deleteMessagesByLocation(location: Int)

    @Query("DELETE FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_LABELS LIKE '%'||:labelId||'%'")
    fun deleteMessagesByLabel(labelId: String)

    @Query(
        """
        DELETE 
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_MESSAGE_EXPIRATION_TIME <> 0 
          AND $COLUMN_MESSAGE_EXPIRATION_TIME < :currentTime
    """
    )
    fun deleteExpiredMessages(currentTime: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMessageInfo(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveMessagesInfo(messages: List<Message>)

    @Query("DELETE FROM $TABLE_MESSAGES")
    fun clearMessagesCache()

    @Delete
    fun deleteMessage(message: Message)

    @Query(
        """
        UPDATE $TABLE_MESSAGES 
        SET $COLUMN_MESSAGE_IS_STARRED = :starred
        WHERE $COLUMN_MESSAGE_ID = :messageId
    """
    )
    fun updateStarredBlocking(messageId: String, starred: Boolean)

    @Query(
        """
        UPDATE $TABLE_MESSAGES 
        SET $COLUMN_MESSAGE_IS_STARRED = :starred
        WHERE $COLUMN_MESSAGE_ID = :messageId
    """
    )
    suspend fun updateStarred(messageId: String, starred: Boolean)


    @Query("SELECT * FROM $TABLE_ATTACHMENTS WHERE $COLUMN_ATTACHMENT_MESSAGE_ID = :messageId")
    fun findAttachmentsByMessageIdAsync(messageId: String): LiveData<List<Attachment>>

    @Query("SELECT * FROM $TABLE_ATTACHMENTS WHERE $COLUMN_ATTACHMENT_MESSAGE_ID = :messageId")
    fun findAttachmentsByMessageId(messageId: String): Flow<List<Attachment>>

    @Query("SELECT * FROM $TABLE_ATTACHMENTS WHERE $COLUMN_ATTACHMENT_MESSAGE_ID = :messageId")
    suspend fun findAttachmentByMessageId(messageId: String): List<Attachment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttachment(attachment: Attachment): Long

    @Deprecated("Use suspend function", ReplaceWith("saveAttachment(attachment)"))
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAttachmentBlocking(attachment: Attachment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAllAttachments(attachments: List<Attachment>): List<Long>

    @Delete
    suspend fun deleteAllAttachments(attachments: List<Attachment>)

    @Delete
    fun deleteAttachment(vararg attachment: Attachment)

    @Query("SELECT * FROM $TABLE_ATTACHMENTS WHERE $COLUMN_ATTACHMENT_ID=:correctId ")
    fun findAttachmentByIdCorrectId(correctId: String): Attachment?

    fun findAttachmentById(attachmentId: String): Attachment? {
        if (attachmentId.startsWith("PGPAttachment")) {
            val parts = attachmentId.split("_".toRegex()).dropLastWhile { it.isEmpty() }
            if (parts.size != 4) {
                return null
            }
            findMessageInfoById(parts[1]) ?: return null
        }
        return findAttachmentByIdCorrectId(attachmentId)
    }

    @Query("DELETE FROM $TABLE_ATTACHMENTS")
    fun clearAttachmentsCache()


    @Query("SELECT * FROM $TABLE_LABELS")
    @Deprecated("Use with Flow", ReplaceWith("this.getAllLabels()"))
    fun getAllLabelsLiveData(): LiveData<List<Label>>

    @Query("SELECT * FROM $TABLE_LABELS ORDER BY LabelOrder")
    fun getAllLabels(): Flow<List<Label>>

    // Folders
    @Query("SELECT * FROM $TABLE_LABELS WHERE `Exclusive` = 1 ORDER BY `LabelOrder`")
    fun getAllLabelsExclusivePaged(): DataSource.Factory<Int, Label>

    // Labels
    @Query("SELECT * FROM $TABLE_LABELS WHERE `Exclusive` = 0 ORDER BY `LabelOrder`")
    fun getAllLabelsNotExclusivePaged(): DataSource.Factory<Int, Label>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID IN (:labelIds) ORDER BY LabelOrder")
    fun findLabelsById(labelIds: List<String>): Flow<List<Label>>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID IN (:labelIds)")
    fun findLabelsByIdBlocking(labelIds: List<String>): List<Label>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID=:labelId")
    fun findLabelById(labelId: String): Label?

    @Query("DELETE FROM $TABLE_LABELS")
    fun clearLabelsCache()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveLabel(label: Label): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun saveAllLabels(labels: List<Label>): List<Long>

    @Query("DELETE FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID=:labelId")
    fun deleteLabelById(labelId: String)
}
