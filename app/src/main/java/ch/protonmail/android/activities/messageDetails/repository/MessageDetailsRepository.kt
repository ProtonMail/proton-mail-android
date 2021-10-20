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
package ch.protonmail.android.activities.messageDetails.repository

import android.content.Context
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Transaction
import ch.protonmail.android.activities.messageDetails.IntentExtrasData
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.User
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.BigContentHolder
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.PendingActionDao
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.LocalAttachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.PendingSend
import ch.protonmail.android.data.local.model.PendingUpload
import ch.protonmail.android.jobs.PostReadJob
import ch.protonmail.android.jobs.PostUnreadJob
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.utils.MessageUtils
import com.birbit.android.jobqueue.JobManager
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.equalsNoCase
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Arrays
import java.util.HashSet
import javax.inject.Inject

private const val MAX_BODY_SIZE_IN_DB = 900 * 1024 // 900 KB
private const val DEPRECATION_MESSAGE =
    "We should strive towards moving methods out of this repository and stopping using it."

@Deprecated("Scheduled to be removed, do not add new usages", ReplaceWith("MessageRepository"))
class MessageDetailsRepository @Inject constructor(
    private val applicationContext: Context,
    private val jobManager: JobManager,
    private var messagesDao: MessageDao,
    private var pendingActionDao: PendingActionDao,
    private val databaseProvider: DatabaseProvider,
    private val attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer,
    private val labelRepository: LabelRepository
) {

    @AssistedInject
    constructor(
        context: Context,
        jobManager: JobManager,
        databaseProvider: DatabaseProvider,
        attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer,
        labelRepository: LabelRepository,
        @Assisted userId: UserId
    ) : this(
        applicationContext = context,
        jobManager = jobManager,
        messagesDao = databaseProvider.provideMessageDao(userId),
        pendingActionDao = databaseProvider.providePendingActionDao(userId),
        databaseProvider = databaseProvider,
        attachmentsWorker = attachmentsWorker,
        labelRepository = labelRepository
    )

    fun reloadDependenciesForUser(userId: UserId) {
        pendingActionDao = databaseProvider.providePendingActionDao(userId)
        messagesDao = databaseProvider.provideMessageDao(userId)
    }

    fun findMessageByIdBlocking(messageId: String): Message? =
        messagesDao.findMessageByIdBlocking(messageId)?.apply { readMessageBodyFromFileIfNeeded(this) }

    fun findMessageById(messageId: String): Flow<Message?> =
        messagesDao.findMessageById(messageId).map { readMessageBodyFromFileIfNeeded(it) }

    fun findMessageByIdSingle(messageId: String): Single<Message> =
        messagesDao.findMessageByIdSingle(messageId).map(readMessageBodyFromFileIfNeeded)

    fun findMessageByIdObservable(messageId: String): Flowable<Message> =
        messagesDao.findMessageByIdObservable(messageId).map(readMessageBodyFromFileIfNeeded)

    fun findMessageByDatabaseId(messageDbId: Long): Flow<Message?> =
        messagesDao.findMessageByDatabaseId(messageDbId).map { readMessageBodyFromFileIfNeeded(it) }

    @Deprecated("Use Flow", ReplaceWith("findMessageByDatabaseId(messageDbId).first()"))
    fun findMessageByDatabaseIdBlocking(messageDbId: Long): Message? =
        runBlocking {
            findMessageByDatabaseId(messageDbId).first()
        }

    private fun findAllMessageByLastMessageAccessTime(laterThan: Long = 0): Flow<List<Message>> =
        messagesDao.findAllMessageByLastMessageAccessTime(laterThan)
            .map { list ->
                list.mapNotNull { readMessageBodyFromFileIfNeeded(it) }
            }

    fun findAllMessageByLastMessageAccessTimeBlocking(laterThan: Long = 0) =
        runBlocking { findAllMessageByLastMessageAccessTime(laterThan).first() }

    /**
     * Helper function mapping Message with body saved in file to body in memory.
     */
    @Deprecated(
        message = DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith(
            expression = "messageBodyFileManager.readMessageBodyFromFile(message)",
            imports = arrayOf("ch.protonmail.android.utils.MessageBodyFileManager")
        )
    )
    private val readMessageBodyFromFileIfNeeded: (Message?) -> Message? = { message ->
        message?.apply {
            if (true == messageBody?.startsWith("file://")) {
                val messageBodyFile = File(
                    applicationContext.filesDir.toString() + Constants.DIR_MESSAGE_BODY_DOWNLOADS,
                    messageId?.replace(" ", "_")?.replace("/", ":")
                )
                messageBody = try {
                    Timber.d("Reading body from file ${messageBodyFile.name}")
                    FileInputStream(messageBodyFile).bufferedReader().use { it.readText() }
                } catch (e: Exception) {
                    Timber.e(e, "Error reading file body")
                    null
                }
            }
        }
    }

    fun searchMessages(subject: String, senderName: String, senderEmail: String): List<Message> =
        runBlocking {
            messagesDao.searchMessages(subject, senderName, senderEmail).first()
                .mapNotNull { readMessageBodyFromFileIfNeeded(it) }
        }

    suspend fun findAttachmentsByMessageId(messageId: String): List<Attachment> =
        messagesDao.findAttachmentsByMessageId(messageId).first()

    suspend fun saveAttachment(attachment: Attachment) = messagesDao.saveAttachment(attachment)

    @Deprecated("Use suspend function", ReplaceWith("saveAttachment(attachment)"))
    fun saveAttachmentBlocking(attachment: Attachment) = messagesDao.saveAttachmentBlocking(attachment)

    fun findPendingSendByMessageId(messageId: String) = pendingActionDao.findPendingSendByMessageId(messageId)

    suspend fun saveMessage(message: Message): Long {
        saveFile(message)
        return messagesDao.saveMessage(message)
    }

    @Deprecated("Use suspend function", ReplaceWith("saveMessage(message)"))
    fun saveMessageBlocking(message: Message): Long = runBlocking {
        saveMessage(message)
    }

    @Deprecated("Use suspend function", ReplaceWith("saveAllMessages(messages)"))
    @Transaction
    fun saveAllMessagesBlocking(messages: List<Message>) {
        messages.forEach { saveMessageBlocking(it) }
    }

    @Transaction
    suspend fun saveMessagesInOneTransaction(messages: List<Message>) {
        if (messages.isEmpty()) {
            return
        }
        messages.forEach { message ->
            saveFile(message)
        }
        messagesDao.saveMessages(messages)
    }

    private fun saveFile(message: Message) {
        val localFilePath = saveBodyToFileIfNeeded(message)
        localFilePath?.let {
            message.messageBody = it
        }
    }

    /**
     * This is a workaround for too large Message bodies saved into database. We offload them to file
     * in internal memory, if needed.
     *
     * @return absolute path to saved File
     */
    @Deprecated(
        message = DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith(
            expression = "messageBodyFileManager.saveMessageBodyToFile(message)",
            imports = arrayOf("ch.protonmail.android.utils.MessageBodyFileManager")
        )
    )
    @WorkerThread
    private fun saveBodyToFileIfNeeded(message: Message, overwrite: Boolean = true): String? {

        val messageId = message.messageId
        val messageBody = message.messageBody
        if (messageId != null && messageBody != null && messageBody.toByteArray().size > MAX_BODY_SIZE_IN_DB) {
            val messageBodyDirectory =
                File(applicationContext.filesDir.toString() + Constants.DIR_MESSAGE_BODY_DOWNLOADS)
            messageBodyDirectory.mkdirs()

            val messageBodyFile = File(messageBodyDirectory, messageId.replace(" ", "_").replace("/", ":"))
            return try {
                if (overwrite || messageBodyFile.length() == 0L /* file doesn't exist */) {
                    FileOutputStream(messageBodyFile).use {
                        it.write(messageBody.toByteArray())
                    }
                    Log.d("PMTAG", "wrote message body to file")
                }
                Log.d("PMTAG", "returning path to message body ${messageBodyFile.absolutePath}")
                "file://${messageBodyFile.absolutePath}"
            } catch (e: Exception) {
                Timber.i(e, "saveBodyToFileIfNeeded error")
                null
            }
        }

        return null
    }

    fun deleteMessage(message: Message) = messagesDao.deleteMessage(message)

    fun deleteMessagesByLocation(location: Constants.MessageLocationType) =
        messagesDao.deleteMessagesByLocation(location.messageLocationTypeValue)

    fun deleteMessagesByLabel(labelId: String) =
        messagesDao.deleteMessagesByLabel(labelId)

    fun findAttachmentById(attachmentId: String) = messagesDao.findAttachmentById(attachmentId)

    suspend fun findLabelsWithIds(labelIds: List<String>): List<Label> =
        labelRepository.findLabels(labelIds.map { LabelId(it) })

    suspend fun prepareEditMessageIntent(
        messageAction: Constants.MessageActionType,
        message: Message,
        user: User,
        newMessageTitle: String?,
        content: String,
        mBigContentHolder: BigContentHolder,
        mImagesDisplayed: Boolean,
        remoteContentDisplayed: Boolean,
        embeddedImagesAttachments: MutableList<Attachment>?,
        dispatcher: CoroutineDispatcher
    ): IntentExtrasData = withContext(dispatcher) {
        var toRecipientListString = ""
        var includeCCList = false
        val replyToEmailsFiltered = ArrayList<String>()
        when (messageAction) {
            Constants.MessageActionType.REPLY -> {
                val replyToEmails = message.replyToEmails
                for (replyToEmail in replyToEmails) {
                    if (user.addresses!!.none { it.email equalsNoCase replyToEmail }) {
                        replyToEmailsFiltered.add(replyToEmail)
                    }
                }
                if (replyToEmailsFiltered.isEmpty()) {
                    replyToEmailsFiltered.addAll(listOf(message.toListString))
                }

                toRecipientListString = MessageUtils.getListOfStringsAsString(replyToEmailsFiltered)
            }
            Constants.MessageActionType.REPLY_ALL -> {
                val emailSet = HashSet(
                    Arrays.asList(
                        *message.toListString.split(Constants.EMAIL_DELIMITER.toRegex())
                            .dropLastWhile { it.isEmpty() }.toTypedArray()
                    )
                )

                val senderEmailAddress = if (message.replyToEmails.isNotEmpty())
                    message.replyToEmails[0] else message.sender!!.emailAddress
                toRecipientListString = if (emailSet.contains(senderEmailAddress)) {
                    message.toListString
                } else {
                    senderEmailAddress + Constants.EMAIL_DELIMITER + message.toListString
                }
                includeCCList = true
            }
            else -> {
                // NO OP
            }
        }

        val attachments = withContext(dispatcher) {
            ArrayList(LocalAttachment.createLocalAttachmentList(message.attachments(messagesDao)))
        }

        IntentExtrasData.Builder()
            .user(user)
            .userAddresses()
            .message(message)
            .toRecipientListString(toRecipientListString)
            .messageCcList()
            .includeCCList(includeCCList)
            .senderEmailAddress()
            .messageSenderName()
            .newMessageTitle(newMessageTitle)
            .content(content)
            .mBigContentHolder(mBigContentHolder)
            .body()
            .messageAction(messageAction)
            .imagesDisplayed(mImagesDisplayed)
            .remoteContentDisplayed(remoteContentDisplayed)
            .isPGPMime()
            .timeMs()
            .messageId()
            .addressID()
            .addressEmailAlias()
            .attachments(attachments, embeddedImagesAttachments)
            .build()
    }

    fun startDownloadEmbeddedImages(messageId: String, userId: UserId) {
        attachmentsWorker.enqueue(messageId, userId, "")
    }

    @Deprecated("Use a method from [MessageRepository]")
    fun markRead(messageIds: List<String>) {
        Timber.d("markRead $messageIds")
        jobManager.addJobInBackground(PostReadJob(messageIds))
    }

    @Deprecated("Use a method from [MessageRepository]")
    fun markUnRead(messageIds: List<String>) {
        Timber.d("markUnRead $messageIds")
        jobManager.addJobInBackground(PostUnreadJob(messageIds))
    }

    fun findAllPendingSendsAsync(): LiveData<List<PendingSend>> =
        pendingActionDao.findAllPendingSendsAsync()

    fun findAllPendingUploadsAsync(): LiveData<List<PendingUpload>> =
        pendingActionDao.findAllPendingUploadsAsync()

    @AssistedInject.Factory
    interface AssistedFactory {

        fun create(userId: UserId): MessageDetailsRepository
    }
}
