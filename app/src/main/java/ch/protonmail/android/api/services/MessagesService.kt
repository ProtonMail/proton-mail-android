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
package ch.protonmail.android.api.services

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.contacts.receive.LabelsMapper
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.segments.contact.ContactEmailsManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.NetworkResults
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.data.local.PendingActionDatabase
import ch.protonmail.android.events.FetchLabelsEvent
import ch.protonmail.android.events.MailboxLoadedEvent
import ch.protonmail.android.events.MailboxNoMessagesEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

private const val ACTION_FETCH_MESSAGE_LABELS = "ACTION_FETCH_MESSAGE_LABELS"
private const val ACTION_FETCH_CONTACT_GROUPS_LABELS = "ACTION_FETCH_CONTACT_GROUPS_LABELS"
private const val ACTION_FETCH_MESSAGES_BY_PAGE = "ACTION_FETCH_MESSAGES_BY_PAGE"

private const val EXTRA_USER_ID = "extra.user.id"
private const val EXTRA_LABEL_ID = "label"
private const val EXTRA_MESSAGE_LOCATION = "location"
private const val EXTRA_REFRESH_DETAILS = "refreshDetails"
private const val EXTRA_UUID = "uuid"
private const val EXTRA_REFRESH_MESSAGES = "refreshMessages"


@AndroidEntryPoint
class MessagesService : JobIntentService() {

    @Inject
    internal lateinit var mApi: ProtonMailApiManager

    @Inject
    internal lateinit var mJobManager: JobManager

    @Inject
    internal lateinit var userManager: UserManager

    @Inject
    internal lateinit var mNetworkResults: NetworkResults

    @Inject
    lateinit var contactEmailsManager: ContactEmailsManager

    @Inject
    lateinit var messageDetailsRepositoryFactory: MessageDetailsRepository.AssistedFactory
    lateinit var messageDetailsRepository: MessageDetailsRepository

    override fun onHandleWork(intent: Intent) {

        Timber.v("onHandleWork $intent")
        val userId = UserId(requireNotNull(intent.getStringExtra(EXTRA_USER_ID)))
        messageDetailsRepository = messageDetailsRepositoryFactory.create(userId)

        messageDetailsRepository.reloadDependenciesForUser(userId)
        when (intent.action) {
            ACTION_FETCH_MESSAGES_BY_PAGE -> {
                val location = intent.getIntExtra(EXTRA_MESSAGE_LOCATION, 0)
                val refreshDetails = intent.getBooleanExtra(EXTRA_REFRESH_DETAILS, false)
                val refreshMessages = intent.getBooleanExtra(EXTRA_REFRESH_MESSAGES, false)
                if (Constants.MessageLocationType.fromInt(location)
                    in listOf(Constants.MessageLocationType.LABEL, Constants.MessageLocationType.LABEL_FOLDER)
                ) {
                    val labelId = intent.getStringExtra(EXTRA_LABEL_ID)!!
                    handleFetchFirstLabelPage(
                        Constants.MessageLocationType.LABEL,
                        labelId,
                        userId,
                        refreshMessages
                    )
                } else {
                    handleFetchFirstPage(
                        Constants.MessageLocationType.fromInt(location),
                        refreshDetails,
                        intent.getStringExtra(EXTRA_UUID),
                        userId,
                        refreshMessages
                    )
                }
            }
            ACTION_FETCH_MESSAGE_LABELS -> handleFetchLabels()
            ACTION_FETCH_CONTACT_GROUPS_LABELS -> handleFetchContactGroups()
        }
    }

    private fun handleFetchFirstPage(
        location: Constants.MessageLocationType,
        refreshDetails: Boolean,
        uuid: String?,
        currentUserId: UserId,
        refreshMessages: Boolean
    ) {
        try {
            val messages = runBlocking {
                mApi.getMessages(GetAllMessagesParameters(currentUserId, labelId = location.asLabelId()))
            }
            if (messages.code == Constants.RESPONSE_CODE_OK)
                handleResult(messages, location, refreshDetails, uuid, currentUserId, refreshMessages)
            else {
                val errorMessage = messages.error ?: ""
                val event = MailboxLoadedEvent(Status.FAILED, uuid, errorMessage)
                AppUtil.postEventOnUi(event)
                mNetworkResults.setMailboxLoaded(event)
                Timber.v(Exception(errorMessage), "error while fetching messages")
            }
        } catch (error: Exception) {
            val event = MailboxLoadedEvent(Status.FAILED, uuid)
            AppUtil.postEventOnUi(event)
            mNetworkResults.setMailboxLoaded(event)
            Timber.e(error, "Error while fetching messages")
        }
    }

    private fun handleFetchFirstLabelPage(
        location: Constants.MessageLocationType,
        labelId: String,
        currentUserId: UserId,
        refreshMessages: Boolean
    ) {
        try {
            val messagesResponse = runBlocking {
                mApi.getMessages(GetAllMessagesParameters(currentUserId, labelId = labelId))
            }
            handleResult(messagesResponse, location, labelId, currentUserId, refreshMessages)
        } catch (error: Exception) {
            AppUtil.postEventOnUi(MailboxLoadedEvent(Status.FAILED, null))
            Timber.e(error, "Error while fetching messages")
        }
    }

    private fun handleFetchContactGroups() {
        val userId = userManager.currentUserId

        if (userId == null) {
            Timber.i("No logged in user")
            return
        }

        try {
            runBlocking {
                contactEmailsManager.refresh()
            }
        } catch (e: Exception) {
            Timber.w(e, "handleFetchContactGroups has failed")
        }
    }

    private fun handleFetchLabels() {
        try {
            val currentUserId = userManager.requireCurrentUserId()
            val db = MessageDatabase.getInstance(applicationContext, currentUserId).getDao()

            runBlocking {
                val serverLabels = mApi.fetchLabels(currentUserId).valueOrThrow.labels
                val serverFolders = mApi.fetchFolders(currentUserId).valueOrThrow.labels
                val labelMapper = LabelsMapper()
                val labelList = serverLabels.map { labelMapper.mapLabelToLabelEntity(it, currentUserId) }
                val foldersList = serverFolders.map { labelMapper.mapLabelToLabelEntity(it, currentUserId) }
                db.saveAllLabels(labelList + foldersList)
            }
            AppUtil.postEventOnUi(FetchLabelsEvent(Status.SUCCESS))
        } catch (error: Exception) {
            Timber.w(error, "handleFetchLabels has failed")
            AppUtil.postEventOnUi(FetchLabelsEvent(Status.FAILED))
        }
    }

    // TODO extract common logic from handleResult methods
    private fun handleResult(
        messages: MessagesResponse?,
        location: Constants.MessageLocationType,
        refreshDetails: Boolean,
        uuid: String?,
        currentUserId: UserId,
        refreshMessages: Boolean = false
    ) {
        val messageList = messages?.messages
        if (messageList == null || messageList.isEmpty()) {
            Timber.i("No more messages")
            AppUtil.postEventOnUi(MailboxNoMessagesEvent())
            return
        }
        try {
            var unixTime = 0L
            val actionsDbFactory = PendingActionDatabase.getInstance(applicationContext, currentUserId)
            val messagesDbFactory = MessageDatabase.getInstance(applicationContext, currentUserId)
            val messagesDb = messagesDbFactory.getDao()
            val actionsDb = actionsDbFactory.getDao()
            messageDetailsRepository.reloadDependenciesForUser(currentUserId)
            if (refreshMessages) messageDetailsRepository.deleteMessagesByLocation(location)
            messageList.asSequence().map { msg ->
                unixTime = msg.time
                val savedMessage = messageDetailsRepository.findMessageByIdBlocking(msg.messageId!!)
                msg.setLabelIDs(msg.getEventLabelIDs())
                msg.location = location.messageLocationTypeValue
                msg.setFolderLocation(messagesDb)
                if (savedMessage != null) {
                    if (actionsDb.findPendingSendByDbId(savedMessage.dbId!!) != null) {
                        return@map null
                    }
                    msg.location = savedMessage.location
                    msg.mimeType = savedMessage.mimeType
                    msg.toList = savedMessage.toList
                    msg.ccList = savedMessage.ccList
                    msg.bccList = savedMessage.bccList
                    msg.replyTos = savedMessage.replyTos
                    msg.sender = savedMessage.sender
                    msg.header = savedMessage.header
                    msg.parsedHeaders = savedMessage.parsedHeaders
                    if (!refreshDetails) {
                        msg.isDownloaded = savedMessage.isDownloaded
                        if (savedMessage.isDownloaded) {
                            msg.messageBody = savedMessage.messageBody
                        }
                        msg.messageEncryption = savedMessage.messageEncryption
                    }
                    msg.isInline = savedMessage.isInline
                    savedMessage.location = location.messageLocationTypeValue
                    savedMessage.setFolderLocation(messagesDb)
                    val attachments = savedMessage.attachments
                    if (attachments.isNotEmpty()) {
                        msg.setAttachmentList(attachments)
                    }
                }
                msg
            }.filterNotNull()
                .toList()
                .let { list ->
                    runBlocking {
                        messageDetailsRepository.saveMessagesInOneTransaction(list)
                    }
                }
            val event = MailboxLoadedEvent(Status.SUCCESS, uuid)
            AppUtil.postEventOnUi(event)
            mNetworkResults.setMailboxLoaded(event)
            Timber.v("Fetched messages successfully")
        } catch (e: Exception) {
            val event = MailboxLoadedEvent(Status.FAILED, uuid)
            AppUtil.postEventOnUi(event)
            Timber.e(e, "Fetch messages error")
        }
    }

    private fun handleResult(
        messagesResponse: MessagesResponse,
        location: Constants.MessageLocationType,
        labelId: String,
        currentUserId: UserId,
        refreshMessages: Boolean = false
    ) {
        val messageList = messagesResponse.messages
        if (messageList.isEmpty()) {
            Timber.i("No more messages")
            AppUtil.postEventOnUi(MailboxNoMessagesEvent())
            return
        }
        try {
            var unixTime = 0L
            val actionsDbFactory = PendingActionDatabase.getInstance(applicationContext, currentUserId)
            val messagesDbFactory = MessageDatabase.getInstance(applicationContext, currentUserId)
            val messagesDb = messagesDbFactory.getDao()
            val actionsDb = actionsDbFactory.getDao()
            messageDetailsRepository.reloadDependenciesForUser(currentUserId)
            if (refreshMessages) messageDetailsRepository.deleteMessagesByLabel(labelId)
            messageList.asSequence().map { msg ->
                unixTime = msg.time
                val savedMessage = messageDetailsRepository.findMessageByIdBlocking(msg.messageId!!)
                msg.setLabelIDs(msg.getEventLabelIDs())
                msg.location = location.messageLocationTypeValue
                msg.setFolderLocation(messagesDb)
                if (savedMessage != null) {
                    if (actionsDb.findPendingSendByDbId(savedMessage.dbId!!) != null) {
                        return@map null
                    }
                    msg.toList = savedMessage.toList
                    msg.ccList = savedMessage.ccList
                    msg.bccList = savedMessage.bccList
                    msg.replyTos = savedMessage.replyTos
                    msg.sender = savedMessage.sender
                    msg.isDownloaded = savedMessage.isDownloaded
                    msg.header = savedMessage.header
                    msg.parsedHeaders = savedMessage.parsedHeaders
                    msg.spamScore = savedMessage.spamScore
                    if (savedMessage.isDownloaded) {
                        msg.messageBody = savedMessage.messageBody
                    }
                    msg.messageEncryption = savedMessage.messageEncryption
                    msg.isInline = savedMessage.isInline
                    msg.mimeType = savedMessage.mimeType
                    savedMessage.location = location.messageLocationTypeValue
                    savedMessage.setFolderLocation(messagesDb)
                    val attachments = savedMessage.attachments
                    if (attachments.isNotEmpty()) {
                        msg.setAttachmentList(attachments)
                    }
                }
                msg
            }.filterNotNull()
                .toList()
                .let { list ->
                    runBlocking {
                        messageDetailsRepository.saveMessagesInOneTransaction(list)
                    }
                }

            AppUtil.postEventOnUi(MailboxLoadedEvent(Status.SUCCESS, null))
        } catch (e: Exception) {
            Timber.e(e, "Fetch messages error")
        }
    }

    companion object {

        fun startFetchLabels(
            context: Context,
            userId: UserId
        ) {
            val intent = Intent(context, MessagesService::class.java)
                .setAction(ACTION_FETCH_MESSAGE_LABELS)
                .putExtra(EXTRA_USER_ID, userId.id)
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        fun startFetchContactGroups(
            context: Context,
            userId: UserId
        ) {
            val intent = Intent(context, MessagesService::class.java)
                .setAction(ACTION_FETCH_CONTACT_GROUPS_LABELS)
                .putExtra(EXTRA_USER_ID, userId.id)
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        /**
         * Load initial page and detail of every message it fetch
         */
        fun startFetchFirstPage(
            context: Context,
            userId: UserId,
            location: Constants.MessageLocationType,
            refreshDetails: Boolean,
            uuid: String?,
            refreshMessages: Boolean
        ) {
            val intent = Intent(context, MessagesService::class.java)
                .setAction(ACTION_FETCH_MESSAGES_BY_PAGE)
                .putExtra(EXTRA_USER_ID, userId.id)
                .putExtra(EXTRA_MESSAGE_LOCATION, location.messageLocationTypeValue)
                .putExtra(EXTRA_REFRESH_DETAILS, refreshDetails)
                .putExtra(EXTRA_UUID, uuid)
                .putExtra(EXTRA_REFRESH_MESSAGES, refreshMessages)
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        /**
         * Load initial page and detail of every message it fetch dino
         */
        fun startFetchFirstPageByLabel(
            context: Context,
            userId: UserId,
            location: Constants.MessageLocationType,
            labelId: String?,
            refreshMessages: Boolean
        ) {
            val intent = Intent(context, MessagesService::class.java)
                .setAction(ACTION_FETCH_MESSAGES_BY_PAGE)
                .putExtra(EXTRA_USER_ID, userId.id)
                .putExtra(EXTRA_MESSAGE_LOCATION, location.messageLocationTypeValue)
                .putExtra(EXTRA_LABEL_ID, labelId)
                .putExtra(EXTRA_REFRESH_MESSAGES, refreshMessages)
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }
    }
}
