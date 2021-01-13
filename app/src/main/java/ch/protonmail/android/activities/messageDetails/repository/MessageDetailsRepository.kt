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
import ch.protonmail.android.activities.messageDetails.IntentExtrasData
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.api.models.room.messages.LocalAttachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.BigContentHolder
import ch.protonmail.android.core.Constants
import ch.protonmail.android.jobs.ApplyLabelJob
import ch.protonmail.android.jobs.FetchMessageDetailJob
import ch.protonmail.android.jobs.PostReadJob
import ch.protonmail.android.jobs.RemoveLabelJob
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.extensions.asyncMap
import com.birbit.android.jobqueue.Job
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.equalsNoCase
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Arrays
import java.util.HashSet
import javax.inject.Inject
import javax.inject.Named

private const val MAX_BODY_SIZE_IN_DB = 900 * 1024 // 900 KB


class MessageDetailsRepository @Inject constructor(
    private val jobManager: JobManager,
    private val api: ProtonMailApiManager,
    @Named("messages_search") var searchDatabaseDao: MessagesDao,
    private var pendingActionsDatabase: PendingActionsDao,
    private val applicationContext: Context,
    val databaseProvider: DatabaseProvider,
    private val dispatchers: DispatcherProvider
) {

    private var messagesDao: MessagesDao = databaseProvider.provideMessagesDao()

    /**
     * Reloads all statically required dependencies when currently active user changes.
     */
    fun reloadDependenciesForUser(username: String?) {
        pendingActionsDatabase = databaseProvider.providePendingActionsDao(username)
        messagesDao = databaseProvider.provideMessagesDao(username)
    }

    fun findMessageByIdAsync(messageId: String): LiveData<Message> =
        messagesDao.findMessageByIdAsync(messageId).asyncMap(readMessageBodyFromFileIfNeeded)

    fun findSearchMessageByIdAsync(messageId: String): LiveData<Message> =
        searchDatabaseDao.findMessageByIdAsync(messageId).asyncMap(readMessageBodyFromFileIfNeeded)

    suspend fun findMessageById(messageId: String) =
        withContext(dispatchers.Io) {
            findMessageByIdBlocking(messageId)
        }

    suspend fun findSearchMessageById(messageId: String, dispatcher: CoroutineDispatcher) =
        withContext(dispatcher) {
            searchDatabaseDao.findMessageById(messageId)?.apply { readMessageBodyFromFileIfNeeded(this) }
        }


    suspend fun findMessageByMessageDbId(dbId: Long, dispatcher: CoroutineDispatcher): Message? =
        withContext(dispatcher) {
            findMessageByMessageDbId(dbId)
        }

    fun findMessageByIdBlocking(messageId: String): Message? =
        messagesDao.findMessageById(messageId)?.apply { readMessageBodyFromFileIfNeeded(this) }

    fun findSearchMessageById(messageId: String): Message? =
        searchDatabaseDao.findMessageById(messageId)?.apply { readMessageBodyFromFileIfNeeded(this) }

    fun findMessageByIdSingle(messageId: String): Single<Message> =
        messagesDao.findMessageByIdSingle(messageId).map(readMessageBodyFromFileIfNeeded)

    fun findMessageByIdObservable(messageId: String): Flowable<Message> =
        messagesDao.findMessageByIdObservable(messageId).map(readMessageBodyFromFileIfNeeded)

    fun findMessageByMessageDbId(messageDbId: Long): Message? =
        messagesDao.findMessageByMessageDbId(messageDbId)?.apply { readMessageBodyFromFileIfNeeded(this) }

    fun findAllMessageByLastMessageAccessTime(laterThan: Long = 0): List<Message> =
        messagesDao.findAllMessageByLastMessageAccessTime(laterThan).mapNotNull { readMessageBodyFromFileIfNeeded(it) }

    /**
     * Helper function mapping Message with body saved in file to body in memory.
     */
    private val readMessageBodyFromFileIfNeeded: (Message?) -> Message? = { message ->
        message?.apply {
            if (
                Constants.FeatureFlags.SAVE_MESSAGE_BODY_TO_FILE &&
                true == message.messageBody?.startsWith("file://")
            ) {
                val messageBodyFile = File(
                    applicationContext.filesDir.toString() + Constants.DIR_MESSAGE_BODY_DOWNLOADS,
                    message.messageId?.replace(" ", "_")?.replace("/", ":")
                )
                message.messageBody = try {
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

    fun findAllLabels(message: LiveData<Message>): LiveData<List<Label>> {
        return Transformations.switchMap(message) {
            it?.labelIDsNotIncludingLocations?.let(messagesDao::findAllLabelsWithIdsAsync)
        }
    }

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

    fun findAttachmentsByMessageId(messageId: String) = messagesDao.findAttachmentsByMessageId(messageId)

    fun findSearchAttachmentsByMessageId(messageId: String) = searchDatabaseDao.findAttachmentsByMessageId(messageId)

    fun saveAttachment(attachment: Attachment) = messagesDao.saveAttachment(attachment)

    fun findPendingSendByOfflineMessageIdAsync(messageId: String) =
        pendingActionsDatabase.findPendingSendByOfflineMessageIdAsync(messageId)

    fun findPendingSendByMessageId(messageId: String) = pendingActionsDatabase.findPendingSendByMessageId(messageId)


    @Suppress("RedundantSuspendModifier")
    // suspend not needed as now, but may all db operations will be wrapped into a Coroutine
    suspend fun saveMessageInDB(message: Message, isTransient: Boolean) {
        return if (isTransient)
            saveSearchMessageInDB(message)
        else {
            saveMessageInDB(message)
            Unit // saveMessageInDB is returning Long
        }
    }

    fun saveMessageInDB(message: Message): Long {
        checkSaveFlagAndSaveFile(message)
        return messagesDao.saveMessage(message)
    }

    suspend fun saveMessageLocally(message: Message): Long =
        withContext(dispatchers.Io) {
            saveMessageInDB(message)
        }

    fun saveAllMessages(messages: List<Message>) {
        messages.map(this::saveMessageInDB)
    }

    fun saveMessagesInOneTransaction(messages: List<Message>) {
        if (messages.isEmpty()) {
            return
        }
        messages.forEach { message ->
            checkSaveFlagAndSaveFile(message)
        }
        messagesDao.saveMessages(*messages.toTypedArray())
    }

    fun saveSearchMessageInDB(message: Message) {
        checkSaveFlagAndSaveFile(message)
        searchDatabaseDao.saveMessage(message)
    }

    private fun checkSaveFlagAndSaveFile(message: Message) {
        if (Constants.FeatureFlags.SAVE_MESSAGE_BODY_TO_FILE) {
            val localFilePath = saveBodyToFileIfNeeded(message)
            localFilePath?.let {
                message.messageBody = it
            }
        }
    }

    fun saveAllSearchMessagesInDB(messages: List<Message>) {
        messages.map(this::saveSearchMessageInDB)
    }

    fun saveSearchMessagesInOneTransaction(messages: List<Message>) {
        if (messages.isEmpty()) {
            return
        }
        messages.forEach { message ->
            checkSaveFlagAndSaveFile(message)
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
                e.printStackTrace()
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

    fun getAllLabels() = messagesDao.getAllLabels()

    fun findAllLabelsWithIds(labelIds: List<String>): List<Label> = messagesDao.findAllLabelsWithIds(labelIds)

    suspend fun findAllLabelsWithIds(
        message: Message,
        checkedLabelIds: MutableList<String>,
        labels: List<Label>,
        isTransient: Boolean
    ) {
        val labelsToRemove = ArrayList<String>()
        val jobList = ArrayList<Job>()
        val messageId = message.messageId
        for (label in labels) {
            val labelId = label.id
            val exclusive = label.exclusive
            if (!checkedLabelIds.contains(labelId) && !exclusive) {
                // this label should be removed
                labelsToRemove.add(labelId)
                jobList.add(RemoveLabelJob(listOf(messageId), labelId))
            } else if (checkedLabelIds.contains(labelId)) {
                // the label remains
                checkedLabelIds.remove(labelId)
            }
        }
        // what remains are the new labels
        val applyLabelsJobs = checkedLabelIds.map { ApplyLabelJob(listOf(messageId), it) }
        jobList.addAll(applyLabelsJobs)
        // update the message with the new labels
        message.addLabels(checkedLabelIds)
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
                    Arrays.asList(*message.toListString.split(Constants.EMAIL_DELIMITER.toRegex())
                        .dropLastWhile { it.isEmpty() }.toTypedArray())
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
                //NO OP
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

    fun fetchMessageDetails(messageId: String): MessageResponse {
        return try {
            api.messageDetail(messageId) // try to fetch the message details from the API
        } catch (ioException: IOException) {
            // if there is an IO exception, meaning the connection could not be established
            // then schedule a background job for run in future
            jobManager.addJobInBackground(FetchMessageDetailJob(messageId))
            throw ioException
        }
    }

    fun fetchSearchMessageDetails(messageId: String): MessageResponse =
        api.messageDetail(messageId) // try to fetch the message details from the API

    fun startDownloadEmbeddedImages(messageId: String, username: String) {
        DownloadEmbeddedAttachmentsWorker.enqueue(messageId, username, "")
    }

    fun markRead(messageId: String) {
        jobManager.addJobInBackground(PostReadJob(listOf(messageId)))
    }

    fun findAllPendingSendsAsync(): LiveData<List<PendingSend>> {
        return pendingActionsDatabase.findAllPendingSendsAsync()
    }

    fun findAllPendingUploadsAsync(): LiveData<List<PendingUpload>> {
        return pendingActionsDatabase.findAllPendingUploadsAsync()
    }
}
