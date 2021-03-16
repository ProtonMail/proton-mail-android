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

import android.content.Intent
import androidx.core.app.JobIntentService
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.api.segments.contact.ContactEmailsManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.NetworkResults
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.FetchLabelsEvent
import ch.protonmail.android.events.MailboxLoadedEvent
import ch.protonmail.android.events.MailboxNoMessagesEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.Logger
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

// region constants
private const val TAG_MESSAGES_SERVICE = "MessagesService"

private const val ACTION_FETCH_MESSAGE_LABELS = "ACTION_FETCH_MESSAGE_LABELS"
private const val ACTION_FETCH_CONTACT_GROUPS_LABELS = "ACTION_FETCH_CONTACT_GROUPS_LABELS"
private const val ACTION_FETCH_MESSAGES_BY_PAGE = "ACTION_FETCH_MESSAGES_BY_PAGE"
private const val ACTION_FETCH_MESSAGES_BY_TIME = "ACTION_FETCH_MESSAGES_BY_TIME_MESSAGE_ID"

private const val EXTRA_LABEL_ID = "label"
private const val EXTRA_MESSAGE_LOCATION = "location"
private const val EXTRA_REFRESH_DETAILS = "refreshDetails"
private const val EXTRA_TIME = "time"
private const val EXTRA_UUID = "uuid"
private const val EXTRA_REFRESH_MESSAGES = "refreshMessages"

private const val PREF_LAST_MESSAGE_TIME_INBOX = "lastMessageTimeInbox"
private const val PREF_LAST_MESSAGE_TIME_SENT = "lastMessageTimeSent"
private const val PREF_LAST_MESSAGE_TIME_DRAFTS = "lastMessageTimeDrafts"
private const val PREF_LAST_MESSAGE_TIME_STARRED = "lastMessageTimeStarred"
private const val PREF_LAST_MESSAGE_TIME_ARCHIVE = "lastMessageTimeArchive"
private const val PREF_LAST_MESSAGE_TIME_SPAM = "lastMessageTimeSpam"
private const val PREF_LAST_MESSAGE_TIME_TRASH = "lastMessageTimeTrash"
private const val PREF_LAST_MESSAGE_TIME_ALL = "lastMessageTimeAll"
// endregion

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
    lateinit var messageDetailsRepository: MessageDetailsRepository

    override fun onHandleWork(intent: Intent) {

        val currentUser = userManager.username
        messageDetailsRepository.reloadDependenciesForUserId(userManager.currentUserId)
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
                        currentUser,
                        refreshMessages
                    )
                } else {
                    handleFetchFirstPage(
                        Constants.MessageLocationType.fromInt(location), refreshDetails,
                        intent.getStringExtra(EXTRA_UUID), currentUser, refreshMessages
                    )
                }
            }
            ACTION_FETCH_MESSAGES_BY_TIME -> {
                val location = intent.getIntExtra(EXTRA_MESSAGE_LOCATION, 0)
                val extraTime = intent.getLongExtra(EXTRA_TIME, 0)
                val savedTime = getLastMessageTime(Constants.MessageLocationType.fromInt(location), "")
                val time = minOf(savedTime, extraTime)
                if (Constants.MessageLocationType.fromInt(location) in listOf(
                        Constants.MessageLocationType.LABEL,
                        Constants.MessageLocationType.LABEL_FOLDER
                    )
                ) {
                    val labelId = intent.getStringExtra(EXTRA_LABEL_ID)!!
                    val labelTime = getLastMessageTime(Constants.MessageLocationType.fromInt(location), labelId)
                    handleFetchMessagesByLabel(
                        Constants.MessageLocationType.fromInt(location),
                        labelTime,
                        labelId,
                        currentUser
                    )
                } else {
                    handleFetchMessages(Constants.MessageLocationType.fromInt(location), time, currentUser)
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
        currentUser: String,
        refreshMessages: Boolean
    ) {
        try {
            val messages = mApi.messages(location.messageLocationTypeValue, RetrofitTag(currentUser))
            if (messages?.code == Constants.RESPONSE_CODE_OK)
                handleResult(messages, location, refreshDetails, uuid, currentUser, refreshMessages)
            else {
                val errorMessage = messages?.error ?: ""
                val event = MailboxLoadedEvent(Status.FAILED, uuid, errorMessage)
                AppUtil.postEventOnUi(event)
                mNetworkResults.setMailboxLoaded(event)
                Timber.v( "error while fetching messages", Exception(errorMessage))
            }

        } catch (error: Exception) {
            val event = MailboxLoadedEvent(Status.FAILED, uuid)
            AppUtil.postEventOnUi(event)
            mNetworkResults.setMailboxLoaded(event)
            Logger.doLogException(TAG_MESSAGES_SERVICE, "error while fetching messages", error)
        }
    }

    private fun handleFetchMessages(location: Constants.MessageLocationType, time: Long, currentUser: String) {
        try {
            val messages = mApi.fetchMessages(location.messageLocationTypeValue, time)
            handleResult(messages, location, false, null, currentUser)
        } catch (error: Exception) {
            AppUtil.postEventOnUi(MailboxLoadedEvent(Status.FAILED, null))
            Logger.doLogException(TAG_MESSAGES_SERVICE, "error while fetching messages", error)
        }
    }

    private fun handleFetchFirstLabelPage(
        location: Constants.MessageLocationType,
        labelId: String,
        currentUser: String,
        refreshMessages: Boolean
    ) {
        try {
            val messagesResponse = mApi.searchByLabelAndPage(labelId, 0)
            handleResult(messagesResponse, location, labelId, currentUser, refreshMessages)
        } catch (error: Exception) {
            AppUtil.postEventOnUi(MailboxLoadedEvent(Status.FAILED, null))
            Logger.doLogException(TAG_MESSAGES_SERVICE, "error while fetching messages", error)
        }
    }

    private fun handleFetchMessagesByLabel(
        location: Constants.MessageLocationType,
        unixTime: Long,
        labelId: String,
        currentUser: String
    ) {
        try {
            val messages = mApi.searchByLabelAndTime(labelId, unixTime)
            handleResult(messages, location, labelId, currentUser)
        } catch (error: Exception) {
            AppUtil.postEventOnUi(MailboxLoadedEvent(Status.FAILED, null))
            Logger.doLogException(TAG_MESSAGES_SERVICE, "error while fetching messages", error)
        }
    }

    private fun handleFetchContactGroups() {
        try {
            contactEmailsManager.refreshBlocking()
        } catch (e: Exception) {
            Timber.i(e, "handleFetchContactGroups has failed")
        }
    }

    private fun handleFetchLabels() {
        try {
            val currentUser = userManager.username
            val db = MessagesDatabaseFactory.getInstance(applicationContext, currentUser).getDatabase()
            val labelList = mApi.fetchLabels(RetrofitTag(currentUser)).labels
            db.saveAllLabels(labelList)
            AppUtil.postEventOnUi(FetchLabelsEvent(Status.SUCCESS))
        } catch (error: Exception) {
            AppUtil.postEventOnUi(FetchLabelsEvent(Status.FAILED))
        }
    }

    // TODO extract common logic from handleResult methods
    private fun handleResult(
        messages: MessagesResponse?,
        location: Constants.MessageLocationType,
        refreshDetails: Boolean,
        uuid: String?,
        currentUser: String,
        refreshMessages: Boolean = false
    ) {
        val messageList = messages?.messages
        if (messageList == null || messageList.isEmpty()) {
            Logger.doLog(TAG_MESSAGES_SERVICE, "no more messages")
            AppUtil.postEventOnUi(MailboxNoMessagesEvent())
            return
        }
        try {
            var unixTime = 0L
            val actionsDbFactory = PendingActionsDatabaseFactory.getInstance(applicationContext, currentUser)
            val messagesDbFactory = MessagesDatabaseFactory.getInstance(applicationContext, currentUser)
            val messagesDb = messagesDbFactory.getDatabase()
            val actionsDb = actionsDbFactory.getDatabase()
            messageDetailsRepository.reloadDependenciesForUserId(userManager.currentUserId)
            messagesDbFactory.runInTransaction {
                actionsDbFactory.runInTransaction {
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
                                msg.setIsEncrypted(savedMessage.getIsEncrypted())
                            }
                            msg.isInline = savedMessage.isInline
                            savedMessage.location = location.messageLocationTypeValue
                            savedMessage.setFolderLocation(messagesDb)
                            val attachments = savedMessage.Attachments
                            if (attachments.isNotEmpty()) {
                                msg.setAttachmentList(attachments)
                            }
                        }
                        msg
                    }.filterNotNull().toList().let(messageDetailsRepository::saveAllMessages)
                }
            }
            saveLastMessageTime(unixTime, location, "")
            val event = MailboxLoadedEvent(Status.SUCCESS, uuid)
            AppUtil.postEventOnUi(event)
            mNetworkResults.setMailboxLoaded(event)
            Logger.doLog(TAG_MESSAGES_SERVICE, "fetched messages successfully")
        } catch (e: Exception) {
            val event = MailboxLoadedEvent(Status.FAILED, uuid)
            AppUtil.postEventOnUi(event)
            Logger.doLogException(TAG_MESSAGES_SERVICE, "fetched messages error", e)
        }
    }

    private fun handleResult(
        messagesResponse: MessagesResponse,
        location: Constants.MessageLocationType,
        labelId: String,
        currentUser: String,
        refreshMessages: Boolean = false
    ) {
        val messageList = messagesResponse.messages
        if (messageList.isEmpty()) {
            Logger.doLog(TAG_MESSAGES_SERVICE, "no more messages")
            AppUtil.postEventOnUi(MailboxNoMessagesEvent())
            return
        }
        try {
            var unixTime = 0L
            val actionsDbFactory = PendingActionsDatabaseFactory.getInstance(applicationContext, currentUser)
            val messagesDbFactory = MessagesDatabaseFactory.getInstance(applicationContext, currentUser)
            val messagesDb = messagesDbFactory.getDatabase()
            val actionsDb = actionsDbFactory.getDatabase()
            messageDetailsRepository.reloadDependenciesForUserId(userManager.currentUserId)
            messagesDbFactory.runInTransaction {
                actionsDbFactory.runInTransaction {
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
                            msg.setIsEncrypted(savedMessage.getIsEncrypted())
                            msg.isInline = savedMessage.isInline
                            msg.mimeType = savedMessage.mimeType
                            savedMessage.location = location.messageLocationTypeValue
                            savedMessage.setFolderLocation(messagesDb)
                            val attachments = savedMessage.Attachments
                            if (attachments.isNotEmpty()) {
                                msg.setAttachmentList(attachments)
                            }
                        }
                        msg
                    }.filterNotNull().toList().let(messageDetailsRepository::saveAllMessages)
                }
            }
            saveLastMessageTime(unixTime, location, labelId)
            AppUtil.postEventOnUi(MailboxLoadedEvent(Status.SUCCESS, null))
        } catch (e: Exception) {
            Logger.doLogException(TAG_MESSAGES_SERVICE, "fetched messages error", e)
        }
    }

    companion object {

        fun startFetchLabels() {
            val context = ProtonMailApplication.getApplication()
            val intent = Intent(context, MessagesService::class.java)
            intent.action = ACTION_FETCH_MESSAGE_LABELS
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        fun startFetchContactGroups() {
            val context = ProtonMailApplication.getApplication()
            val intent = Intent(context, MessagesService::class.java)
            intent.action = ACTION_FETCH_CONTACT_GROUPS_LABELS
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        /**
         * Load initial page and detail of every message it fetch
         */
        fun startFetchFirstPage(location: Constants.MessageLocationType) {
            val context = ProtonMailApplication.getApplication()
            val intent = Intent(context, MessagesService::class.java)
            intent.action = ACTION_FETCH_MESSAGES_BY_PAGE
            intent.putExtra(EXTRA_MESSAGE_LOCATION, location.messageLocationTypeValue)
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        /**
         * Load initial page and detail of every message it fetch
         */
        fun startFetchFirstPage(
            location: Constants.MessageLocationType,
            refreshDetails: Boolean,
            uuid: String?,
            refreshMessages: Boolean
        ) {
            val context = ProtonMailApplication.getApplication()
            val intent = Intent(context, MessagesService::class.java)
            intent.action = ACTION_FETCH_MESSAGES_BY_PAGE
            intent.putExtra(EXTRA_MESSAGE_LOCATION, location.messageLocationTypeValue)
            intent.putExtra(EXTRA_REFRESH_DETAILS, refreshDetails)
            intent.putExtra(EXTRA_UUID, uuid)
            intent.putExtra(EXTRA_REFRESH_MESSAGES, refreshMessages)
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        fun startFetchMessages(location: Constants.MessageLocationType, time: Long) {
            val context = ProtonMailApplication.getApplication()
            val intent = Intent(context, MessagesService::class.java)
            intent.action = ACTION_FETCH_MESSAGES_BY_TIME
            intent.putExtra(EXTRA_MESSAGE_LOCATION, location.messageLocationTypeValue)
            intent.putExtra(EXTRA_TIME, time)
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        /**
         * Load initial page and detail of every message it fetch dino
         */
        fun startFetchFirstPageByLabel(
            location: Constants.MessageLocationType,
            labelId: String?,
            refreshMessages: Boolean
        ) {
            val context = ProtonMailApplication.getApplication()
            val intent = Intent(context, MessagesService::class.java)
            intent.action = ACTION_FETCH_MESSAGES_BY_PAGE
            intent.putExtra(EXTRA_MESSAGE_LOCATION, location.messageLocationTypeValue)
            intent.putExtra(EXTRA_LABEL_ID, labelId)
            intent.putExtra(EXTRA_REFRESH_MESSAGES, refreshMessages)
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        fun startFetchMessagesByLabel(location: Constants.MessageLocationType, time: Long, labelId: String) {
            val context = ProtonMailApplication.getApplication()
            val intent = Intent(context, MessagesService::class.java)
            intent.action = ACTION_FETCH_MESSAGES_BY_TIME
            intent.putExtra(EXTRA_MESSAGE_LOCATION, location.messageLocationTypeValue)
            intent.putExtra(EXTRA_TIME, time)
            intent.putExtra(EXTRA_LABEL_ID, labelId)
            enqueueWork(context, MessagesService::class.java, Constants.JOB_INTENT_SERVICE_ID_MESSAGES, intent)
        }

        fun getLastMessageTime(location: Constants.MessageLocationType, labelId: String?): Long {
            val pref = ProtonMailApplication.getApplication().defaultSharedPreferences
            return getPrefsNameByLocation(location, labelId)?.let { pref.getLong(it, 0L) } ?: 0L
        }

        fun saveLastMessageTime(unixTime: Long, location: Constants.MessageLocationType, labelId: String) {
            val pref = ProtonMailApplication.getApplication().defaultSharedPreferences
            getPrefsNameByLocation(location, labelId)?.let {
                pref.edit().putLong(it, unixTime).apply()
            }
        }

        private fun getPrefsNameByLocation(location: Constants.MessageLocationType, labelId: String?): String? {
            return when (location) {
                Constants.MessageLocationType.INBOX -> PREF_LAST_MESSAGE_TIME_INBOX
                Constants.MessageLocationType.SENT -> PREF_LAST_MESSAGE_TIME_SENT
                Constants.MessageLocationType.DRAFT -> PREF_LAST_MESSAGE_TIME_DRAFTS
                Constants.MessageLocationType.STARRED -> PREF_LAST_MESSAGE_TIME_STARRED
                Constants.MessageLocationType.ARCHIVE -> PREF_LAST_MESSAGE_TIME_ARCHIVE
                Constants.MessageLocationType.SPAM -> PREF_LAST_MESSAGE_TIME_SPAM
                Constants.MessageLocationType.TRASH -> PREF_LAST_MESSAGE_TIME_TRASH
                Constants.MessageLocationType.ALL_MAIL -> PREF_LAST_MESSAGE_TIME_ALL
                Constants.MessageLocationType.LABEL, Constants.MessageLocationType.LABEL_FOLDER -> labelId
                else -> null
            }
        }
    }
}
