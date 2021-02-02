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
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.Crypto
import ch.protonmail.android.jobs.messages.PostMessageJob
import ch.protonmail.android.utils.ServerTime
import com.birbit.android.jobqueue.JobManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.ArrayList
import javax.inject.Inject

class PostMessageServiceFactory @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository,
    private val userManager: UserManager,
    private val jobManager: JobManager
) {

    private val bgDispatcher: CoroutineDispatcher = Dispatchers.IO

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
                messageDetailsRepository.saveMessageLocally(message)
            } catch (e: Exception) {
                Timber.e(e, "handleMessage in PostMessageTask failed")
            }
        }
        return message
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
        messageDetailsRepository.saveMessageLocally(message)
        insertPendingSend(context, message.messageId, message.dbId)
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
}
