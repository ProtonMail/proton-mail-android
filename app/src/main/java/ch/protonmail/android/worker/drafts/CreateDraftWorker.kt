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

package ch.protonmail.android.worker.drafts

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.REPLY
import ch.protonmail.android.core.Constants.MessageActionType.REPLY_ALL
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.model.*
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.base64.Base64Encoder
import ch.protonmail.android.utils.notifier.UserNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

public const val SAVE_DRAFT_UNIQUE_WORK_ID_PREFIX = "saveDraftUniqueWork"

internal const val KEY_INPUT_SAVE_DRAFT_USER_ID = "keySaveDraftMessageUserId"
internal const val KEY_INPUT_SAVE_DRAFT_MSG_DB_ID = "keySaveDraftMessageDbId"
internal const val KEY_INPUT_SAVE_DRAFT_MSG_LOCAL_ID = "keySaveDraftMessageLocalId"
internal const val KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID = "keySaveDraftMessageParentId"
internal const val KEY_INPUT_SAVE_DRAFT_ACTION_TYPE = "keySaveDraftMessageActionType"
internal const val KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID = "keySaveDraftPreviousSenderAddressId"

internal const val KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM = "keySaveDraftErrorResult"
internal const val KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID = "keySaveDraftSuccessResultDbId"

private const val INPUT_MESSAGE_DB_ID_NOT_FOUND = -1L
private const val SAVE_DRAFT_MAX_RETRIES = 3

class CreateDraftWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val messageFactory: MessageFactory,
    private val userManager: UserManager,
    private val addressCryptoFactory: AddressCrypto.Factory,
    private val base64: Base64Encoder,
    private val apiManager: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider,
    private val userNotifier: UserNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = getInputUserId()

        val message = messageDetailsRepository.findMessageByMessageDbId(getInputMessageDbId()).first()
            ?: return failureWithError(CreateDraftWorkerErrors.MessageNotFound)
        val senderAddressId = requireNotNull(message.addressID)
        val senderAddress = requireNotNull(getSenderAddress(senderAddressId))
        val parentId = getInputParentId()
        val createDraftRequest = messageFactory.createDraftApiRequest(message)

        parentId?.let {
            if (isDraftBeingCreated(message)) {
                createDraftRequest.parentID = parentId
                createDraftRequest.action = getInputActionType().messageActionTypeValue
                val parentMessage = messageDetailsRepository.findMessageById(parentId).first()
                val attachments = parentMessage?.attachments(databaseProvider.provideMessageDao(userId))

                buildParentAttachmentsKeyPacketsMap(attachments, senderAddress).forEach {
                    createDraftRequest.addAttachmentKeyPacket(it.key, it.value)
                }
            }
        }

        val messageId = requireNotNull(message.messageId)
        val attachments = messageDetailsRepository.findAttachmentsByMessageId(messageId)
        buildMessageAttachmentsKeyPacketsMap(attachments, senderAddress).forEach {
            createDraftRequest.addAttachmentKeyPacket(it.key, it.value)
        }

        val encryptedMessage = requireNotNull(message.messageBody)
        createDraftRequest.setMessageBody(encryptedMessage)
        createDraftRequest.setSender(buildMessageSender(message, senderAddress))

        return runCatching {
            if (isDraftBeingCreated(message)) {
                apiManager.createDraft(createDraftRequest)
            } else {
                apiManager.updateDraft(
                    messageId,
                    createDraftRequest,
                    UserIdTag(userManager.requireCurrentUserId())
                )
            }
        }.fold(
            onSuccess = { response ->
                if (response.code != Constants.RESPONSE_CODE_OK) {
                    Timber.e("Create Draft Worker Failed with bad response code: $response")
                    userNotifier.showPersistentError(response.error, createDraftRequest.message.subject)
                    return failureWithError(CreateDraftWorkerErrors.BadResponseCodeError)
                }

                updateStoredLocalDraft(response.message, message)

                Timber.i("Create Draft Worker API call succeeded")
                Result.success(
                    workDataOf(KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to response.messageId)
                )
            },
            onFailure = {
                retryOrFail(it.message, createDraftRequest.message.subject)
            }
        )
    }

    private fun buildMessageAttachmentsKeyPacketsMap(
        attachments: List<Attachment>,
        senderAddress: Address
    ): Map<String, String> {
        val draftAttachments = mutableMapOf<String, String>()
        attachments.forEach { attachment ->

            val keyPackets = if (isSenderAddressChanged()) {
                reEncryptAttachment(senderAddress, attachment)
            } else {
                attachment.keyPackets
            }

            keyPackets?.let {
                draftAttachments[attachment.attachmentId!!] = keyPackets
            }
        }
        return draftAttachments
    }

    private fun isDraftBeingCreated(message: Message) =
        MessageUtils.isLocalMessageId(message.messageId)

    private suspend fun updateStoredLocalDraft(apiDraft: Message, localDraft: Message) {
        val localAttachments = localDraft.Attachments.filterNot { it.isUploaded }
        apiDraft.apply {
            dbId = localDraft.dbId
            toList = localDraft.toList
            ccList = localDraft.ccList
            bccList = localDraft.bccList
            replyTos = localDraft.replyTos
            sender = localDraft.sender
            setLabelIDs(localDraft.getEventLabelIDs())
            parsedHeaders = localDraft.parsedHeaders
            isDownloaded = true
            setIsRead(true)
            numAttachments = localDraft.numAttachments
            Attachments = localAttachments.plus(Attachments)
            localId = localDraft.messageId
        }

        messageDetailsRepository.saveMessage(apiDraft)
    }

    private fun retryOrFail(error: String?, messageSubject: String?): Result {
        if (runAttemptCount <= SAVE_DRAFT_MAX_RETRIES) {
            Timber.d("Create Draft Worker API call FAILED with error = $error. Retrying...")
            return Result.retry()
        }
        Timber.e("Create Draft Worker API call failed all the retries. error = $error. FAILING")
        userNotifier.showPersistentError(error.orEmpty(), messageSubject)
        return failureWithError(CreateDraftWorkerErrors.ServerError)
    }

    private fun buildParentAttachmentsKeyPacketsMap(
        attachments: List<Attachment>?,
        senderAddress: Address
    ): Map<String, String> {
        if (shouldAddParentAttachments().not()) {
            return emptyMap()
        }

        val draftAttachments = mutableMapOf<String, String>()
        attachments?.forEach { attachment ->
            if (isReplyActionAndAttachmentNotInline(attachment)) {
                return@forEach
            }
            val keyPackets = if (isSenderAddressChanged()) {
                reEncryptAttachment(senderAddress, attachment)
            } else {
                attachment.keyPackets
            }

            keyPackets?.let {
                draftAttachments[attachment.attachmentId!!] = keyPackets
            }
        }
        return draftAttachments
    }

    private fun reEncryptAttachment(senderAddress: Address, attachment: Attachment): String? {
        val previousSenderAddressId = requireNotNull(getInputPreviousSenderAddressId())
        val addressCrypto = addressCryptoFactory.create(getInputUserId(), Id(previousSenderAddressId))
        val primaryKey = senderAddress.keys
        val publicKey = addressCrypto.buildArmoredPublicKey(primaryKey.primaryKey!!.privateKey)

        return attachment.keyPackets?.let {
            return try {
                val keyPackage = base64.decode(it)
                val sessionKey = addressCrypto.decryptKeyPacket(keyPackage)
                val newKeyPackage = addressCrypto.encryptKeyPacket(sessionKey, publicKey)
                return base64.encode(newKeyPackage)
            } catch (exception: Exception) {
                Timber.d("Re-encrypting message attachments threw exception $exception")
                attachment.keyPackets
            }
        }
    }

    private fun isReplyActionAndAttachmentNotInline(attachment: Attachment): Boolean {
        val actionType = getInputActionType()
        val isReplying = actionType == REPLY || actionType == REPLY_ALL

        return isReplying && attachment.inline.not()
    }

    private fun shouldAddParentAttachments(): Boolean {
        val actionType = getInputActionType()
        return actionType == FORWARD ||
            actionType == REPLY ||
            actionType == REPLY_ALL
    }

    private fun isSenderAddressChanged(): Boolean {
        val previousSenderAddressId = getInputPreviousSenderAddressId()
        return previousSenderAddressId?.isNotEmpty() == true
    }

    private suspend fun getSenderAddress(senderAddressId: String): Address? {
        val user = userManager.getUser(getInputUserId())
        return user.findAddressById(Id(senderAddressId))
    }

    private fun buildMessageSender(message: Message, senderAddress: Address): MessageSender {
        if (message.isSenderEmailAlias()) {
            return MessageSender(message.senderName, message.senderEmail)
        }
        return MessageSender(senderAddress.displayName?.s, senderAddress.email.s)
    }

    private fun failureWithError(error: CreateDraftWorkerErrors): Result {
        val errorData = workDataOf(KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM to error.name)
        return Result.failure(errorData)
    }

    private fun getInputUserId(): Id {
        val idString = requireNotNull(inputData.getString(KEY_INPUT_SAVE_DRAFT_USER_ID)?.takeIfNotBlank()) {
            "User id is required"
        }
        return Id(idString)
    }

    private fun getInputActionType(): Constants.MessageActionType =
        Constants.MessageActionType.fromInt(inputData.getInt(KEY_INPUT_SAVE_DRAFT_ACTION_TYPE, -1))

    private fun getInputPreviousSenderAddressId() =
        inputData.getString(KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID)

    private fun getInputParentId() =
        inputData.getString(KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID)

    private fun getInputMessageDbId() =
        inputData.getLong(KEY_INPUT_SAVE_DRAFT_MSG_DB_ID, INPUT_MESSAGE_DB_ID_NOT_FOUND)

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            userId: Id,
            message: Message,
            parentId: String?,
            actionType: Constants.MessageActionType,
            previousSenderAddressId: String
        ): Flow<WorkInfo?> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val createDraftRequest = OneTimeWorkRequestBuilder<CreateDraftWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_SAVE_DRAFT_USER_ID to userId.s,
                        KEY_INPUT_SAVE_DRAFT_MSG_DB_ID to message.dbId,
                        KEY_INPUT_SAVE_DRAFT_MSG_LOCAL_ID to message.messageId,
                        KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID to parentId,
                        KEY_INPUT_SAVE_DRAFT_ACTION_TYPE to actionType.messageActionTypeValue,
                        KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID to previousSenderAddressId
                    )
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2 * TEN_SECONDS, TimeUnit.SECONDS)
                .build()

            val uniqueWorkId = "$SAVE_DRAFT_UNIQUE_WORK_ID_PREFIX-${message.messageId}"
            workManager.enqueueUniqueWork(
                uniqueWorkId,
                ExistingWorkPolicy.REPLACE,
                createDraftRequest
            )
            return workManager.getWorkInfoByIdLiveData(createDraftRequest.id).asFlow()
        }
    }

}
