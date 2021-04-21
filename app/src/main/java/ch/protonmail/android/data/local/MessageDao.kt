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
import ch.protonmail.android.data.local.model.COLUMN_ATTACHMENT_FILE_NAME
import ch.protonmail.android.data.local.model.COLUMN_ATTACHMENT_FILE_PATH
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

@Dao
abstract class MessageDao {

    //region Messages
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
    abstract fun searchMessages(subject: String, senderName: String, senderEmail: String): List<Message>

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_IS_STARRED = 1")
    abstract fun getStarredMessagesAsync(): LiveData<List<Message>>

    @Query(
        """
        SELECT * 
        FROM $TABLE_MESSAGES 
        WHERE $COLUMN_MESSAGE_LABELS LIKE '%' || :label || '%'
        ORDER BY $COLUMN_MESSAGE_TIME DESC
    """
    )
    abstract fun getMessagesByLabelIdAsync(label: String): LiveData<List<Message>>

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_MESSAGE_LOCATION = :location
        ORDER BY $COLUMN_MESSAGE_TIME DESC
    """
    )
    abstract fun getMessagesByLocationAsync(location: Int): LiveData<List<Message>>

    @Query("SELECT COUNT($COLUMN_MESSAGE_ID) FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_LOCATION = :location ")
    abstract fun getMessagesCountByLocation(location: Int): Int

    @Query(
        """
        SELECT COUNT($COLUMN_MESSAGE_ID)
        FROM $TABLE_MESSAGES 
		WHERE $COLUMN_MESSAGE_LABELS LIKE '%' || :labelId || '%'  
    """
    )
    abstract fun getMessagesCountByByLabelId(labelId: String): Int

    @Query("SELECT * FROM $TABLE_MESSAGES ORDER BY $COLUMN_MESSAGE_TIME DESC")
    abstract fun getAllMessages(): LiveData<List<Message>>

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_MESSAGE_LABELS LIKE '%' || :label || '%'  
        ORDER BY $COLUMN_MESSAGE_TIME DESC
    """
    )
    abstract fun getMessagesByLabelId(label: String): List<Message>

    fun findMessageById(messageId: String): Flow<Message?> = findMessageInfoById(messageId)
        .onEach { message ->
            message ?: return@onEach
            message.Attachments = message.attachments(this)
        }

    suspend fun findMessageByIdOnce(messageId: String): Message = findMessageInfoByIdOnce(messageId).also { message ->
        message.Attachments = message.attachmentsBlocking(this)
    }

    @Deprecated("Use Flow variant", ReplaceWith("findMessageById(messageId).first()"))
    fun findMessageByIdBlocking(messageId: String): Message? = findMessageInfoByIdBlocking(messageId)
        ?.also { message ->
            message.Attachments = message.attachmentsBlocking(this)
        }

    fun findMessageByIdAsync(messageId: String) = findMessageInfoByIdAsync(messageId)

    fun findMessageByIdSingle(messageId: String) = findMessageInfoByIdSingle(messageId)

    fun findMessageByIdObservable(messageId: String) = findMessageInfoByIdObservable(messageId)

    fun findMessageByMessageDbId(messageDbId: Long) = findMessageInfoByDbId(messageDbId)
        .onEach { message ->
            message ?: return@onEach
            message.Attachments = message.attachmentsBlocking(this)
        }

    fun findMessageByMessageDbIdBlocking(messageDbId: Long) = findMessageInfoByDbIdBlocking(messageDbId)
        ?.also { message ->
            message.Attachments = message.attachmentsBlocking(this)
        }

    fun findMessageByDbId(dbId: Long): Flow<Message?> =
        findMessageInfoByDbId(dbId).map { message ->
            return@map message?.let {
                it.Attachments = it.attachmentsBlocking(this)
                it
            }
        }

    @JvmOverloads
    fun findAllMessageByLastMessageAccessTime(laterThan: Long = 0): Flow<List<Message>> =
        findAllMessageInfoByLastMessageAccessTime(laterThan)
            .map { messages ->
                messages.onEach { message ->
                    message.Attachments = message.attachments(this)
                }
            }

    fun findAllMessageFromAConversation(conversationId: String): Flow<List<Message>> =
        findAllMessageInfoFromAConversation(conversationId)
            .map { messages ->
                messages.onEach { message ->
                    message.Attachments = message.attachments(this)
                }
            }

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    protected abstract fun findMessageInfoById(messageId: String): Flow<Message?>

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    protected abstract suspend fun findMessageInfoByIdOnce(messageId: String): Message

    @Deprecated("Use Flow variant", ReplaceWith("findMessageInfoById(messageId).first()"))
    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    protected abstract fun findMessageInfoByIdBlocking(messageId: String): Message?

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    protected abstract fun findMessageInfoByIdSingle(messageId: String): Single<Message>

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    protected abstract fun findMessageInfoByIdObservable(messageId: String): Flowable<Message>

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    protected abstract fun findMessageInfoByIdAsync(messageId: String): LiveData<Message>

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE ${BaseColumns._ID} = :messageDbId")
    protected abstract fun findMessageInfoByDbIdBlocking(messageDbId: Long): Message?

    @Query("SELECT * FROM $TABLE_MESSAGES WHERE ${BaseColumns._ID}=:messageDbId")
    protected abstract fun findMessageInfoByDbId(messageDbId: Long): Flow<Message?>

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_MESSAGE_ACCESS_TIME > :laterThan
        ORDER BY $COLUMN_MESSAGE_ACCESS_TIME
    """
    )
    protected abstract fun findAllMessageInfoByLastMessageAccessTime(laterThan: Long = 0): Flow<List<Message>>

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_CONVERSATION_ID = :conversationId
    """
    )
    abstract fun findAllMessageInfoFromAConversation(conversationId: String): Flow<List<Message>>

    open suspend fun saveMessage(message: Message): Long {
        processMessageAttachments(message)
        return saveMessageInfo(message)
    }

    private suspend fun processMessageAttachments(message: Message) {
        val messageId = message.messageId
        var localAttachments: List<Attachment> = ArrayList()
        if (messageId != null) {
            localAttachments = findAttachmentsByMessageId(messageId).first()
        }

        var preservedAttachments = message.Attachments.map {
            Attachment(
                fileName = it.fileName,
                mimeType = it.mimeType,
                fileSize = it.fileSize,
                keyPackets = it.keyPackets,
                messageId = message.messageId ?: "",
                isUploaded = it.isUploaded,
                isUploading = it.isUploading,
                isNew = it.isNew,
                filePath = it.filePath,
                attachmentId = it.attachmentId,
                headers = it.headers,
                inline = it.inline
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
                    localAttachments.find { it.attachmentId == preservedAtt.attachmentId }?.let {
                        if (it.inline) {
                            preservedAtt.inline = it.inline
                        } else {
                            if (message.embeddedImageIds.isNotEmpty()) {
                                preservedAtt.setMessage(message)
                            }
                        }
                        preservedAtt.isUploaded = it.isUploaded
                        preservedAtt.isUploading = it.isUploading
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
        message.Attachments = preservedAttachments
    }

    open suspend fun saveMessages(vararg messages: Message) {
        messages.forEach {
            processMessageAttachments(it)
        }
        return saveMessagesInfo(*messages)
    }

    @Deprecated("Use MessageDetailsRepository's methods that contain logic for large Message bodies")
    open suspend fun saveAllMessages(messages: List<Message>) {
        messages.map { saveMessage(it) }
    }

    @Query("DELETE FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_LOCATION = :location")
    abstract fun deleteMessagesByLocation(location: Int)

    @Query("DELETE FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_LABELS LIKE '%'||:labelId||'%'")
    abstract fun deleteMessagesByLabel(labelId: String)

    @Query(
        """
        DELETE 
        FROM $TABLE_MESSAGES
        WHERE $COLUMN_MESSAGE_EXPIRATION_TIME <> 0 
          AND $COLUMN_MESSAGE_EXPIRATION_TIME < :currentTime
    """
    )
    abstract fun deleteExpiredMessages(currentTime: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveMessageInfo(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveMessageInfoBlocking(message: Message): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveMessagesInfo(vararg messages: Message)

    @Query("DELETE FROM $TABLE_MESSAGES WHERE ${BaseColumns._ID} = :dbId")
    abstract fun deleteByDbId(dbId: Long)

    @Query("DELETE FROM $TABLE_MESSAGES")
    abstract fun clearMessagesCache()

    @Query("DELETE FROM $TABLE_MESSAGES WHERE $COLUMN_MESSAGE_ID = :messageId")
    abstract fun deleteMessageById(messageId: String)

    @Delete
    abstract fun deleteMessage(message: Message)

    @Delete
    abstract fun deleteMessages(message: List<Message>)

    @Query(
        """
        UPDATE $TABLE_MESSAGES 
        SET $COLUMN_MESSAGE_IS_STARRED = :starred
        WHERE $COLUMN_MESSAGE_ID = :messageId
    """
    )
    abstract fun updateStarred(messageId: String, starred: Boolean)
    //endregion Messages

    //region Attachments

    @Query("SELECT * FROM $TABLE_ATTACHMENTS WHERE $COLUMN_ATTACHMENT_MESSAGE_ID = :messageId")
    abstract fun findAttachmentsByMessageIdAsync(messageId: String): LiveData<List<Attachment>>

    @Query("SELECT * FROM $TABLE_ATTACHMENTS WHERE $COLUMN_ATTACHMENT_MESSAGE_ID = :messageId")
    abstract fun findAttachmentsByMessageId(messageId: String): Flow<List<Attachment>>

    @Query(
        """
        SELECT * 
        FROM $TABLE_ATTACHMENTS
        WHERE $COLUMN_ATTACHMENT_MESSAGE_ID = :messageId
          AND $COLUMN_ATTACHMENT_FILE_NAME = :fileName
          AND $COLUMN_ATTACHMENT_FILE_PATH = :filePath
    """
    )
    abstract fun findAttachmentsByMessageIdFileNameAndPath(
        messageId: String,
        fileName: String,
        filePath: String
    ): Attachment

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveAttachment(attachment: Attachment): Long

    @Deprecated("Use suspend function", ReplaceWith("saveAttachment(attachment)"))
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveAttachmentBlocking(attachment: Attachment): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveAttachment(vararg attachments: Attachment): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveAllAttachments(attachments: List<Attachment>): List<Long>

    @Delete
    abstract suspend fun deleteAllAttachments(attachments: List<Attachment>)

    @Delete
    abstract fun deleteAttachment(vararg attachment: Attachment)

    @Query("SELECT * FROM $TABLE_ATTACHMENTS WHERE $COLUMN_ATTACHMENT_ID=:correctId ")
    abstract fun findAttachmentByIdCorrectId(correctId: String): Attachment?

    @Query("SELECT * FROM $TABLE_ATTACHMENTS WHERE $COLUMN_ATTACHMENT_ID=:correctId ")
    abstract fun findAttachmentByIdCorrectIdSingle(correctId: String): Single<Attachment>

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
    abstract fun clearAttachmentsCache()
    //endregion

    //region Labels
    @Query("SELECT * FROM $TABLE_LABELS")
    abstract fun getAllLabelsLiveData(): LiveData<List<Label>>

    @Query("SELECT * FROM $TABLE_LABELS")
    abstract suspend fun getAllLabels(): List<Label>

    // Folders
    @Query("SELECT * FROM $TABLE_LABELS WHERE `Exclusive` = 1 ORDER BY `LabelOrder`")
    abstract fun getAllLabelsExclusivePaged(): DataSource.Factory<Int, Label>

    // Labels
    @Query("SELECT * FROM $TABLE_LABELS WHERE `Exclusive` = 0 ORDER BY `LabelOrder`")
    abstract fun getAllLabelsNotExclusivePaged(): DataSource.Factory<Int, Label>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID IN (:labelIds)")
    abstract fun findAllLabelsWithIds(labelIds: List<String>): List<Label>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID IN (:labelIds)")
    abstract fun findAllLabelsWithIdsAsync(labelIds: List<String>): LiveData<List<Label>>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID=:labelId")
    abstract fun findLabelById(labelId: String): Label?

    @Query("DELETE FROM $TABLE_LABELS")
    abstract fun clearLabelsCache()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveLabel(label: Label): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun saveAllLabels(labels: List<Label>): List<Long>

    @Query("DELETE FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID=:labelId")
    abstract fun deleteLabelById(labelId: String)
    //endregion
}
