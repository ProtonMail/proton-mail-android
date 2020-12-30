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
import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessageSender
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.NONE
import ch.protonmail.android.core.Constants.MessageActionType.REPLY
import ch.protonmail.android.core.Constants.MessageActionType.REPLY_ALL
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.base64.Base64Encoder
import ch.protonmail.android.utils.extensions.deserialize
import ch.protonmail.android.utils.extensions.serialize
import kotlinx.coroutines.flow.Flow
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val KEY_INPUT_SAVE_DRAFT_MSG_DB_ID = "keySaveDraftMessageDbId"
internal const val KEY_INPUT_SAVE_DRAFT_MSG_LOCAL_ID = "keySaveDraftMessageLocalId"
internal const val KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID = "keySaveDraftMessageParentId"
internal const val KEY_INPUT_SAVE_DRAFT_ACTION_TYPE_JSON = "keySaveDraftMessageActionTypeSerialized"
internal const val KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID = "keySaveDraftPreviousSenderAddressId"

internal const val KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM = "keySaveDraftErrorResult"
internal const val KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID = "keySaveDraftSuccessResultDbId"

private const val INPUT_MESSAGE_DB_ID_NOT_FOUND = -1L
private const val SAVE_DRAFT_MAX_RETRIES = 10

class CreateDraftWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val messageFactory: MessageFactory,
    private val userManager: UserManager,
    private val addressCryptoFactory: AddressCrypto.Factory,
    private val base64: Base64Encoder,
    val apiManager: ProtonMailApiManager
) : CoroutineWorker(context, params) {

    internal var retries: Int = 0

    override suspend fun doWork(): Result {
        val message = messageDetailsRepository.findMessageByMessageDbId(getInputMessageDbId())
            ?: return failureWithError(CreateDraftWorkerErrors.MessageNotFound)
        val senderAddressId = requireNotNull(message.addressID)
        val senderAddress = requireNotNull(getSenderAddress(senderAddressId))
        val parentId = getInputParentId()
        val createDraftRequest = messageFactory.createDraftApiRequest(message)

        parentId?.let {
            createDraftRequest.parentID = parentId
            createDraftRequest.action = getInputActionType().messageActionTypeValue
            val parentMessage = messageDetailsRepository.findMessageByIdBlocking(parentId)
            val attachments = parentMessage?.attachments(messageDetailsRepository.databaseProvider.provideMessagesDao())

            buildDraftRequestParentAttachments(attachments, senderAddress).forEach {
                createDraftRequest.addAttachmentKeyPacket(it.key, it.value)
            }
        }

        val encryptedMessage = requireNotNull(message.messageBody)
        createDraftRequest.setMessageBody(encryptedMessage)
        createDraftRequest.setSender(buildMessageSender(message, senderAddress))

        return runCatching {
            if (MessageUtils.isLocalMessageId(message.messageId)) {
                apiManager.createDraft(createDraftRequest)
            } else {
                apiManager.updateDraft(
                    requireNotNull(message.messageId),
                    createDraftRequest,
                    RetrofitTag(userManager.username)
                )
            }
        }.fold(
            onSuccess = { response ->
                if (response.code != Constants.RESPONSE_CODE_OK) {
                    return handleFailure(response.error)
                }

                val responseDraft = response.message.copy()
                updateStoredLocalDraft(responseDraft, message)
                Timber.i("Create Draft Worker API call succeeded")
                Result.success(
                    workDataOf(KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to response.messageId)
                )
            },
            onFailure = {
                handleFailure(it.message)
            }
        )
    }

    private suspend fun updateStoredLocalDraft(apiDraft: Message, localDraft: Message) {
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
            localId = localDraft.messageId
        }

        messageDetailsRepository.saveMessageLocally(apiDraft)
    }

    private fun handleFailure(error: String?): Result {
        if (retries <= SAVE_DRAFT_MAX_RETRIES) {
            retries++
            Timber.w("Create Draft Worker API call FAILED with error = $error. Retrying...")
            return Result.retry()
        }
        Timber.e("Create Draft Worker API call failed all the retries. error = $error. FAILING")
        return failureWithError(CreateDraftWorkerErrors.ServerError)
    }

    private fun buildDraftRequestParentAttachments(
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
        val addressCrypto = addressCryptoFactory.create(Id(previousSenderAddressId), Name(userManager.username))
        val primaryKey = senderAddress.keys
        val publicKey = addressCrypto.buildArmoredPublicKey(primaryKey.primaryKey!!.privateKey)

        return attachment.keyPackets?.let {
            val keyPackage = base64.decode(it)
            val sessionKey = addressCrypto.decryptKeyPacket(keyPackage)
            val newKeyPackage = addressCrypto.encryptKeyPacket(sessionKey, publicKey)
            return base64.encode(newKeyPackage)
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

    private fun getSenderAddress(senderAddressId: String): Address? {
        val user = userManager.getUser(userManager.username).loadNew(userManager.username)
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

    private fun getInputActionType(): Constants.MessageActionType =
        inputData
            .getString(KEY_INPUT_SAVE_DRAFT_ACTION_TYPE_JSON)?.deserialize()
            ?: NONE

    private fun getInputPreviousSenderAddressId() =
        inputData.getString(KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID)

    private fun getInputParentId() =
        inputData.getString(KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID)

    private fun getInputMessageDbId() =
        inputData.getLong(KEY_INPUT_SAVE_DRAFT_MSG_DB_ID, INPUT_MESSAGE_DB_ID_NOT_FOUND)

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
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
                        KEY_INPUT_SAVE_DRAFT_MSG_DB_ID to message.dbId,
                        KEY_INPUT_SAVE_DRAFT_MSG_LOCAL_ID to message.messageId,
                        KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID to parentId,
                        KEY_INPUT_SAVE_DRAFT_ACTION_TYPE_JSON to actionType.serialize(),
                        KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID to previousSenderAddressId
                    )
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2 * TEN_SECONDS, TimeUnit.SECONDS)
                .build()

            workManager.enqueueUniqueWork(
                requireNotNull(message.messageId),
                ExistingWorkPolicy.REPLACE,
                createDraftRequest
            )
            return workManager.getWorkInfoByIdLiveData(createDraftRequest.id).asFlow()
        }
    }

}
