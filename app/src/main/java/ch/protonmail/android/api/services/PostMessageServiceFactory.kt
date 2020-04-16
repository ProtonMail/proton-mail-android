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
import android.os.Build
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingDraft
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.LogTags.SENDING_FAILED_DEVICE_TAG
import ch.protonmail.android.core.Constants.LogTags.SENDING_FAILED_TAG
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.DraftCreatedEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.CreateAndPostDraftJob
import ch.protonmail.android.jobs.UpdateAndPostDraftJob
import ch.protonmail.android.jobs.messages.PostMessageJob
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.ServerTime
import ch.protonmail.android.utils.crypto.Crypto
import com.birbit.android.jobqueue.JobManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class PostMessageServiceFactory {

    @Inject
    internal lateinit var messageDetailsRepository: MessageDetailsRepository
    @Inject
    internal lateinit var userManager: UserManager
    @Inject
    internal lateinit var jobManager: JobManager
    @Inject
    internal lateinit var networkUtil: QueueNetworkUtil

    private val bgDispatcher: CoroutineDispatcher = Dispatchers.IO

    init {
        ProtonMailApplication.getApplication().appComponent.inject(this)
    }

    suspend fun startCreateDraftService(messageId: Long, localMessageId: String, parentId: String?, actionType: Constants.MessageActionType, content: String, uploadAttachments: Boolean, newAttachments: List<String>, oldSenderId: String, isTransient: Boolean, username: String = userManager.username) {
        val message = handleMessage(messageId, content, username) ?: return
        insertPendingDraft(ProtonMailApplication.getApplication(), messageId)
        handleCreateDraft(message, localMessageId, uploadAttachments, newAttachments, ProtonMailApplication.getApplication())
        jobManager.addJobInBackground(CreateAndPostDraftJob(messageId, localMessageId, parentId, actionType, uploadAttachments, newAttachments, oldSenderId, isTransient, username))
    }

    fun startUpdateDraftService(messageId: Long, content: String, newAttachments: List<String>, uploadAttachments: Boolean, oldSenderId: String, username: String = userManager.username) {
        // this is temp fix
        GlobalScope.launch {
            val message = handleMessage(messageId, content, username) ?: return@launch
            insertPendingDraft(ProtonMailApplication.getApplication(), messageId)
            handleUpdateDraft(message, uploadAttachments, newAttachments, ProtonMailApplication.getApplication())
            jobManager.addJobInBackground(UpdateAndPostDraftJob(messageId, newAttachments, uploadAttachments, oldSenderId, username))
        }
    }

    fun startSendingMessage(messageDbId: Long, content: String, outsidersPassword: String?, outsidersHint: String?, expiresIn: Long, parentId: String?, actionType: Constants.MessageActionType, newAttachments: List<String>,
                            sendPreferences: ArrayList<SendPreference>, oldSenderId: String, username: String = userManager.username) {
        // this is temp fix
        GlobalScope.launch {
            val message = handleMessage(messageDbId, content, username) ?: return@launch
            handleSendMessage(ProtonMailApplication.getApplication(), message)
            jobManager.addJobInBackground(PostMessageJob(messageDbId, outsidersPassword, outsidersHint,
                    expiresIn, parentId, actionType, newAttachments, sendPreferences, oldSenderId, username))
        }
    }

    private suspend fun handleMessage(messageDbId: Long, content: String, username: String): Message? {
        val message: Message? = messageDetailsRepository.findMessageByMessageDbId(messageDbId, bgDispatcher)

        if (message != null) {
            val crypto = Crypto.forAddress(userManager, username, message.addressID!!)
            try {
                val tct = crypto.encrypt(content, true)
                message.messageBody = tct.armored
                messageDetailsRepository.saveMessageInDB(message, bgDispatcher)
            } catch (e: Exception) {
                Timber.e(e, "handleMessage in PostMessageTask failed")
            }
        }
        return message
    }

    private suspend fun handleCreateDraft(message: Message, localMessageId: String, uploadAttachments: Boolean, newAttachments: List<String>, context: Context) {
        if (!networkUtil.isConnected(ProtonMailApplication.getApplication())) {
            AppUtil.postEventOnUi(DraftCreatedEvent(message.messageId, localMessageId, null, Status.NO_NETWORK))
            return
        }
        val hasAttachment = message.numAttachments >= 1
        message.setLabelIDs(listOf(Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue.toString(), Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue.toString(), Constants.MessageLocationType.DRAFT.messageLocationTypeValue.toString()))
        messageDetailsRepository.saveMessageInDB(message, bgDispatcher)

        if (hasAttachment && uploadAttachments && newAttachments.isNotEmpty()) {
            insertPendingUpload(context, message.messageId!!)
        }
    }

    private suspend fun handleUpdateDraft(message: Message, uploadAttachments: Boolean, newAttachments: List<String>, context: Context) {
        if (!networkUtil.isConnected(ProtonMailApplication.getApplication())) {
            return
        }
        message.setLabelIDs(listOf(Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue.toString(), Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue.toString(), Constants.MessageLocationType.DRAFT.messageLocationTypeValue.toString()))
        messageDetailsRepository.saveMessageInDB(message, bgDispatcher)
        if (uploadAttachments && newAttachments.isNotEmpty()) {
            insertPendingUpload(context, message.messageId!!)
        }
    }

    private suspend fun handleSendMessage(context: Context, message: Message) {
        message.location = Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue
        message.setLabelIDs(listOf(Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue.toString(), Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue.toString(), Constants.MessageLocationType.DRAFT.messageLocationTypeValue.toString()))
        message.time = ServerTime.currentTimeMillis() / 1000
        message.toList = message.toList
        message.isDownloaded = false
        message.ccList = message.ccList
        message.bccList = message.bccList
        message.replyTos = message.replyTos
        message.sender = message.sender
        message.isInline = message.isInline
        message.parsedHeaders = message.parsedHeaders
        messageDetailsRepository.saveMessageInDB(message, bgDispatcher)
        insertPendingSend(context, message.messageId, message.dbId)
    }

    private suspend fun insertPendingUpload(context: Context, messageId: String) =
        withContext(bgDispatcher) {
            val actionsDatabase = PendingActionsDatabaseFactory.getInstance(context).getDatabase()
            actionsDatabase.insertPendingForUpload(PendingUpload(messageId))
        }

    private suspend fun insertPendingSend(context: Context, messageId: String?, messageDbId: Long?) =
        withContext(bgDispatcher) {
            val pendingActionsDatabase = PendingActionsDatabaseFactory.getInstance(context).getDatabase()
            val pendingForSending = PendingSend()
            pendingForSending.messageId = messageId
            pendingForSending.localDatabaseId = messageDbId ?: 0
            messageId?.let {
                val savedPendingSend = pendingActionsDatabase.findPendingSendByMessageId(it)
                if (savedPendingSend != null) {
                    savedPendingSend.sent = null
                    pendingActionsDatabase.insertPendingForSend(savedPendingSend)
                } else {
                    pendingActionsDatabase.insertPendingForSend(pendingForSending)
                }
            }
            if (messageId == null) {
                pendingActionsDatabase.insertPendingForSend(pendingForSending)
            }
        }

    private suspend fun insertPendingDraft(context: Context, messageDbId: Long) =
        withContext(bgDispatcher) {
            val pendingActionsDatabase = PendingActionsDatabaseFactory.getInstance(context).getDatabase()
            pendingActionsDatabase.insertPendingDraft(PendingDraft(messageDbId))
        }
}
