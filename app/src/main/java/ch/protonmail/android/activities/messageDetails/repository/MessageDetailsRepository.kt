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
import androidx.lifecycle.Transformations
import androidx.room.Transaction
import ch.protonmail.android.activities.messageDetails.IntentExtrasData
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.BigContentHolder
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.PendingActionDao
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.LocalAttachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.PendingSend
import ch.protonmail.android.data.local.model.PendingUpload
import ch.protonmail.android.di.SearchMessageDaoQualifier
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.jobs.ApplyLabelJob
import ch.protonmail.android.jobs.FetchMessageDetailJob
import ch.protonmail.android.jobs.PostReadJob
import ch.protonmail.android.jobs.PostUnreadJob
import ch.protonmail.android.jobs.RemoveLabelJob
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.extensions.asyncMap
import com.birbit.android.jobqueue.Job
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
import me.proton.core.util.kotlin.equalsNoCase
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Arrays
import java.util.HashSet
import javax.inject.Inject

private const val MAX_BODY_SIZE_IN_DB = 900 * 1024 // 900 KB
private const val DEPRECATION_MESSAGE = "We should strive towards moving methods out of this repository and stopping using it."

class MessageDetailsRepository @Inject constructor(
    private val applicationContext: Context,
    private val jobManager: JobManager,
    private val api: ProtonMailApiManager,
    private var messagesDao: MessageDao,
    @SearchMessageDaoQualifier var searchDatabaseDao: MessageDao,
    private var pendingActionDao: PendingActionDao,
    private val databaseProvider: DatabaseProvider,
    private val attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer
) {

    @AssistedInject
    constructor(
        context: Context,
        jobManager: JobManager,
        api: ProtonMailApiManager,
        databaseProvider: DatabaseProvider,
        attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer,
        @Assisted userId: Id
    ) : this(
        applicationContext = context,
        jobManager = jobManager,
        api = api,
        messagesDao = databaseProvider.provideMessageDao(userId),
        searchDatabaseDao = databaseProvider.provideMessageSearchDao(userId),
        pendingActionDao = databaseProvider.providePendingActionDao(userId),
        databaseProvider = databaseProvider,
        attachmentsWorker = attachmentsWorker
    )

    fun reloadDependenciesForUser(userId: Id) {
        pendingActionDao = databaseProvider.providePendingActionDao(userId)
        messagesDao = databaseProvider.provideMessageDao(userId)
    }

    fun findMessageByIdAsync(messageId: String): LiveData<Message> =
        messagesDao.findMessageByIdAsync(messageId).asyncMap(readMessageBodyFromFileIfNeeded)

    fun findSearchMessageByIdAsync(messageId: String): LiveData<Message> =
        searchDatabaseDao.findMessageByIdAsync(messageId).asyncMap(readMessageBodyFromFileIfNeeded)

    fun findMessageByIdBlocking(messageId: String): Message? =
        messagesDao.findMessageByIdBlocking(messageId)?.apply { readMessageBodyFromFileIfNeeded(this) }

    fun findMessageById(messageId: String): Flow<Message?> =
        messagesDao.findMessageById(messageId).map { readMessageBodyFromFileIfNeeded(it) }

    fun findSearchMessageById(messageId: String): Flow<Message?> =
        searchDatabaseDao.findMessageById(messageId).map { readMessageBodyFromFileIfNeeded(it) }

    fun findMessageByIdSingle(messageId: String): Single<Message> =
        messagesDao.findMessageByIdSingle(messageId).map(readMessageBodyFromFileIfNeeded)

    fun findMessageByIdObservable(messageId: String): Flowable<Message> =
        messagesDao.findMessageByIdObservable(messageId).map(readMessageBodyFromFileIfNeeded)

    fun findMessageByMessageDbId(messageDbId: Long): Flow<Message?> =
        messagesDao.findMessageByMessageDbId(messageDbId).map { readMessageBodyFromFileIfNeeded(it) }

    @Deprecated("Use Flow", ReplaceWith("findMessageByMessageDbId(messageDbId).first()"))
    fun findMessageByMessageDbIdBlocking(messageDbId: Long): Message? =
        runBlocking {
            findMessageByMessageDbId(messageDbId).first()
        }

    fun findMessageByDbId(messageDbId: Long): Flow<Message?> =
        messagesDao.findMessageByDbId(messageDbId)

    fun findAllMessageByLastMessageAccessTime(laterThan: Long = 0): Flow<List<Message>> =
        messagesDao.findAllMessageByLastMessageAccessTime(laterThan)
            .map { list ->
                list.mapNotNull { readMessageBodyFromFileIfNeeded(it) }
            }

    fun findAllMessageByLastMessageAccessTimeBlocking(laterThan: Long = 0): List<Message> =
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

    fun getStarredMessagesAsync(): LiveData<List<Message>> = messagesDao.getStarredMessagesAsync()

    fun getMessagesByLabelIdAsync(label: String): LiveData<List<Message>> = messagesDao.getMessagesByLabelIdAsync(label)

    fun getMessagesByLocationAsync(location: Int): LiveData<List<Message>> =
        messagesDao.getMessagesByLocationAsync(location)

    fun getAllMessages(): LiveData<List<Message>> = messagesDao.getAllMessages()

    fun getAllSearchMessages(): LiveData<List<Message>> = searchDatabaseDao.getAllMessages()

    fun searchMessages(subject: String, senderName: String, senderEmail: String): List<Message> =
        messagesDao.searchMessages(subject, senderName, senderEmail).mapNotNull { readMessageBodyFromFileIfNeeded(it) }

    fun setFolderLocation(message: Message) = message.setFolderLocation(messagesDao)

    fun findAttachments(messageLiveData: LiveData<Message>): LiveData<List<Attachment>> {
        return Transformations.switchMap(messageLiveData) { message ->
            message?.let {
                if (it.numAttachments == 0)
                    null
                else {
                    it.getAttachmentsAsync(messagesDao)
                }
            }
        }
    }

    fun findAttachmentsSearchMessage(messageLiveData: LiveData<Message>): LiveData<List<Attachment>> {
        return Transformations.switchMap(messageLiveData) { message ->
            message?.let {
                if (it.numAttachments == 0)
                    null
                else {
                    it.getAttachmentsAsync(searchDatabaseDao)
                }
            }
        }
    }

    suspend fun findAttachmentsByMessageId(messageId: String): List<Attachment> =
        messagesDao.findAttachmentsByMessageId(messageId).first()

    suspend fun findSearchAttachmentsByMessageId(messageId: String): List<Attachment> =
        searchDatabaseDao.findAttachmentsByMessageId(messageId).first()

    suspend fun saveAttachment(attachment: Attachment) = messagesDao.saveAttachment(attachment)

    @Deprecated("Use suspend function", ReplaceWith("saveAttachment(attachment)"))
    fun saveAttachmentBlocking(attachment: Attachment) = messagesDao.saveAttachmentBlocking(attachment)

    fun findPendingSendByOfflineMessageIdAsync(messageId: String) =
        pendingActionDao.findPendingSendByOfflineMessageIdAsync(messageId)

    fun findPendingSendByMessageId(messageId: String) = pendingActionDao.findPendingSendByMessageId(messageId)

    @Deprecated(
        "Use 'saveMessage'",
        ReplaceWith("saveMessage(message, isSearchMessage = isTransient)")
    )
    suspend fun saveMessageInDB(message: Message, isTransient: Boolean) {
        saveMessage(message, isSearchMessage = isTransient)
    }

    suspend fun saveMessage(message: Message, isSearchMessage: Boolean = false): Long {
        return if (isSearchMessage) {
            saveSearchMessageInDB(message)
        } else {
            saveFile(message)
            messagesDao.saveMessage(message)
        }
    }

    @Deprecated("Use suspend function", ReplaceWith("saveMessage(message)"))
    fun saveMessageBlocking(message: Message): Long = runBlocking {
        saveMessage(message)
    }

    @Deprecated("Use 'saveMessage'", ReplaceWith("saveMessage(message)"))
    suspend fun saveMessageInDB(message: Message): Long =
        saveMessage(message)

    @Transaction
    suspend fun saveAllMessages(messages: List<Message>) {
        messages.forEach { saveMessage(it) }
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
        messagesDao.saveMessages(*messages.toTypedArray())
    }

    suspend fun saveSearchMessage(message: Message): Long {
        saveFile(message)
        return searchDatabaseDao.saveMessage(message)
    }

    @Deprecated("Use 'saveSearchMessage'", ReplaceWith("saveSearchMessage(message)"))
    suspend fun saveSearchMessageInDB(message: Message): Long =
        saveSearchMessage(message)

    private fun saveFile(message: Message) {
        val localFilePath = saveBodyToFileIfNeeded(message)
        localFilePath?.let {
            message.messageBody = it
        }
    }

    suspend fun saveAllSearchMessages(messages: List<Message>) {
        messages.forEach { saveSearchMessage(it) }
    }

    @Deprecated("Use suspend function", ReplaceWith("saveAllSearchMessages(messages)"))
    fun saveAllSearchMessagesBlocking(messages: List<Message>) {
        runBlocking { saveAllSearchMessages(messages) }
    }

    @Deprecated("Use 'saveAllSearchMessages'", ReplaceWith("saveAllSearchMessages(messages)"))
    suspend fun saveAllSearchMessagesInDB(messages: List<Message>) {
        saveAllSearchMessages(messages)
    }

    suspend fun saveSearchMessagesInOneTransaction(messages: List<Message>) {
        if (messages.isEmpty()) {
            return
        }
        messages.forEach { message ->
            saveFile(message)
        }
        searchDatabaseDao.saveMessages(*messages.toTypedArray())
    }

    fun clearSearchMessagesCache() = searchDatabaseDao.clearMessagesCache()

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
            val messageBodyDirectory = File(applicationContext.filesDir.toString() + Constants.DIR_MESSAGE_BODY_DOWNLOADS)
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

    fun updateStarred(messageId: String, starred: Boolean) = messagesDao.updateStarred(messageId, starred)

    fun findAttachmentById(attachmentId: String) = messagesDao.findAttachmentById(attachmentId)

    fun getAllLabelsLiveData() = messagesDao.getAllLabelsLiveData()

    suspend fun getAllLabels(): List<Label> = messagesDao.getAllLabels().first()

    fun findAllLabelsWithIds(labelIds: List<String>): List<Label> = messagesDao.findLabelsByIdBlocking(labelIds)

    suspend fun findAllLabelsWithIds(
        message: Message,
        checkedLabelIds: List<String>,
        labels: List<Label>,
        isTransient: Boolean
    ) {
        val labelsToRemove = ArrayList<String>()
        val jobList = ArrayList<Job>()
        val messageId = message.messageId
        val mutableLabelIds = checkedLabelIds.toMutableList()
        for (label in labels) {
            val labelId = label.id
            val exclusive = label.exclusive
            if (!mutableLabelIds.contains(labelId) && !exclusive) {
                // this label should be removed
                labelsToRemove.add(labelId)
                jobList.add(RemoveLabelJob(listOf(messageId), labelId))
            } else if (mutableLabelIds.contains(labelId)) {
                // the label remains
                mutableLabelIds.remove(labelId)
            }
        }
        // what remains are the new labels
        val applyLabelsJobs = mutableLabelIds.map { ApplyLabelJob(listOf(messageId), it) }
        jobList.addAll(applyLabelsJobs)
        // update the message with the new labels
        message.addLabels(mutableLabelIds)
        message.removeLabels(labelsToRemove)

        jobList.forEach(jobManager::addJobInBackground)
        return saveMessageInDB(message, isTransient)
    }

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
        dispatcher: CoroutineDispatcher,
        isTransient: Boolean
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
            ArrayList(
                LocalAttachment.createLocalAttachmentList(
                    if (!isTransient) {
                        message.attachments(messagesDao)
                    } else {
                        message.attachments(searchDatabaseDao)
                    }
                )
            )
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
            .messageIsEncrypted()
            .messageId()
            .addressID()
            .addressEmailAlias()
            .attachments(attachments, embeddedImagesAttachments)
            .build()
    }

    suspend fun checkIfAttHeadersArePresent(message: Message, dispatcher: CoroutineDispatcher): Boolean =
        withContext(dispatcher) {
            message.checkIfAttHeadersArePresent(messagesDao)
        }

    @Deprecated(
        message = DEPRECATION_MESSAGE,
        replaceWith = ReplaceWith(
            expression = "messageRepository.getMessage(messageId, username, true)",
            imports = arrayOf("ch.protonmail.android.repositories.MessageRepository")
        )
    )
    fun fetchMessageDetails(messageId: String): MessageResponse {
        return try {
            api.fetchMessageDetailsBlocking(messageId) // try to fetch the message details from the API
        } catch (ioException: IOException) {
            // if there is an IO exception, meaning the connection could not be established
            // then schedule a background job for run in future
            jobManager.addJobInBackground(FetchMessageDetailJob(messageId))
            throw ioException
        }
    }

    fun fetchSearchMessageDetails(messageId: String): MessageResponse =
        api.fetchMessageDetailsBlocking(messageId) // try to fetch the message details from the API

    fun startDownloadEmbeddedImages(messageId: String, userId: Id) {
        attachmentsWorker.enqueue(messageId, userId, "")
    }

    @Deprecated("Use a method from [MessageRepository]")
    fun markRead(messageIds: List<String>) {
        jobManager.addJobInBackground(PostReadJob(messageIds))
    }

    @Deprecated("Use a method from [MessageRepository]")
    fun markUnRead(messageIds: List<String>) {
        jobManager.addJobInBackground(PostUnreadJob(messageIds))
    }

    fun findAllPendingSendsAsync(): LiveData<List<PendingSend>> =
        pendingActionDao.findAllPendingSendsAsync()

    fun findAllPendingUploadsAsync(): LiveData<List<PendingUpload>> =
        pendingActionDao.findAllPendingUploadsAsync()

    @AssistedInject.Factory
    interface AssistedFactory {

        fun create(userId: Id): MessageDetailsRepository
    }
}
