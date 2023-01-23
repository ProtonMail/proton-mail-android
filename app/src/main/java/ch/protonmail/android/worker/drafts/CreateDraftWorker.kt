/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.worker.drafts

import android.content.Context
import androidx.hilt.work.HiltWorker
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
import arrow.core.Either
import arrow.core.Left
import arrow.core.right
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.ServerMessageSender
import ch.protonmail.android.api.segments.RESPONSE_CODE_DRAFT_DOES_NOT_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_INVALID_VALUE
import ch.protonmail.android.api.segments.RESPONSE_CODE_MESSAGE_ALREADY_SENT
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.REPLY
import ch.protonmail.android.core.Constants.MessageActionType.REPLY_ALL
import ch.protonmail.android.core.DetailedException
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.core.apiError
import ch.protonmail.android.core.messageId
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.util.orThrow
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.base64.Base64Encoder
import ch.protonmail.android.utils.notifier.UserNotifier
import com.google.gson.Gson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.takeIfNotBlank
import retrofit2.HttpException
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

const val SAVE_DRAFT_UNIQUE_WORK_ID_PREFIX = "saveDraftUniqueWork"

internal const val KEY_INPUT_SAVE_DRAFT_USER_ID = "keySaveDraftMessageUserId"
internal const val KEY_INPUT_SAVE_DRAFT_MSG_DB_ID = "keySaveDraftMessageDbId"
internal const val KEY_INPUT_SAVE_DRAFT_MSG_LOCAL_ID = "keySaveDraftMessageLocalId"
internal const val KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID = "keySaveDraftMessageParentId"
internal const val KEY_INPUT_SAVE_DRAFT_ACTION_TYPE = "keySaveDraftMessageActionType"
internal const val KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID = "keySaveDraftPreviousSenderAddressId"

internal const val KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM = "keySaveDraftErrorResult"
internal const val KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID = "keySaveDraftSuccessResultDbId"

private const val INPUT_MESSAGE_DB_ID_NOT_FOUND = -1L
private const val SAVE_DRAFT_MAX_RETRIES = 1

@HiltWorker
class CreateDraftWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val messageRepository: MessageRepository,
    private val messageFactory: MessageFactory,
    private val userManager: UserManager,
    private val addressCryptoFactory: AddressCrypto.Factory,
    private val base64: Base64Encoder,
    private val apiManager: ProtonMailApiManager,
    private val userNotifier: UserNotifier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val userId = getInputUserId()
        val messageDatabaseId = getInputMessageDbId()

        val message = messageRepository.getMessage(userId, messageDatabaseId)
            ?: return failureWithError(CreateDraftWorkerErrors.MessageNotFound)

        val senderAddressId = requireNotNull(message.addressID)
        val senderAddress = requireNotNull(getSenderAddress(senderAddressId))
        val parentId = getInputParentId()
        val messageId = message.messageId
            ?: return failureWithError(CreateDraftWorkerErrors.MessageHasNullId)

        val draftBody = buildDraftBody(senderAddress, UserId(messageId), message, parentId)
            .mapLeft { return failureWithError(it) }
            .orThrow()

        return runCatching {
            if (isDraftBeingCreated(message)) {
                apiManager.createDraft(draftBody)
            } else {
                apiManager.updateDraft(
                    messageId,
                    draftBody,
                    UserIdTag(userId)
                )
            }
        }.fold(
            onSuccess = { response ->
                if (response.code != Constants.RESPONSE_CODE_OK) {
                    Timber.e(
                        DetailedException().apiError(response.code, response.error),
                        "Create Draft Worker Failed with bad response"
                    )
                    userNotifier.showPersistentError(response.error, draftBody.message.subject)
                    return failureWithError(CreateDraftWorkerErrors.BadResponseCodeError)
                }

                updateStoredLocalDraft(response.message, message)

                Timber.i("Create Draft Worker API call succeeded")
                Result.success(
                    workDataOf(KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to response.messageId)
                )
            },
            onFailure = { throwable ->
                if (throwable is HttpException) {
                    val responseBody: ResponseBody? = Gson().fromJson(
                        throwable.response()?.errorBody()?.string(), ResponseBody::class.java
                    )
                    if (responseBody?.code == RESPONSE_CODE_MESSAGE_ALREADY_SENT) {
                        return failureWithError(CreateDraftWorkerErrors.MessageAlreadySent)
                    }
                    if (responseBody?.code == RESPONSE_CODE_DRAFT_DOES_NOT_EXIST) {
                        Timber.e("Draft does not exist. error: ${responseBody.error}")
                        return failureWithError(CreateDraftWorkerErrors.DraftDoesNotExist)
                    }
                    if (responseBody?.code == RESPONSE_CODE_INVALID_VALUE) {
                        if (responseBody.error == "Invalid sender") {
                            return failureWithError(CreateDraftWorkerErrors.InvalidSender)
                        }
                        if (responseBody.error.contains("Subject")) {
                            return failureWithError(CreateDraftWorkerErrors.InvalidSubject)
                        }
                    }
                }

                return retryOrFail(throwable.messageId(messageId), draftBody.message.subject)
            }
        )
    }

    private suspend fun buildDraftBody(
        senderAddress: Address,
        messageId: UserId,
        message: Message,
        parentId: String?
    ): Either<CreateDraftWorkerErrors, DraftBody> {
        val initialDraftBody = messageFactory.createDraftApiRequest(message)
        val isNew = isDraftBeingCreated(message)

        val withParentDraftBody = if (parentId != null && isNew) {
            val parentMessage = messageDetailsRepository.findMessageById(parentId).firstOrNull()
                ?: fetchParentMessage(parentId)
            val parentAttachmentsKeyPackets =
                buildParentAttachmentsKeyPacketsMap(parentMessage.attachments, senderAddress)

            initialDraftBody.copy(
                parentId = parentId,
                action = getInputActionType().messageActionTypeValue,
                attachmentKeyPackets = parentAttachmentsKeyPackets
            )
        } else {
            initialDraftBody
        }

        val attachments = messageDetailsRepository.findAttachmentsByMessageId(messageId.id).filter { it.isUploaded }
        val messageAttachmentsKeyPackets = buildMessageAttachmentsKeyPacketsMap(attachments, senderAddress)

        val messageBody = message.messageBody
            ?: return Left(CreateDraftWorkerErrors.MessageHasNullBody)
        if (messageBody.isBlank()) {
            return Left(CreateDraftWorkerErrors.MessageHasBlankBody)
        }

        val messageSender = buildMessageSender(message, senderAddress)
        val messageSenderAddress = messageSender.emailAddress
            ?: return Left(CreateDraftWorkerErrors.MessageHasNullSenderAddress)

        return withParentDraftBody.copy(
            message = withParentDraftBody.message.copy(
                body = messageBody,
                sender = ServerMessageSender(messageSender.name, messageSenderAddress)
            ),
            attachmentKeyPackets = messageAttachmentsKeyPackets + withParentDraftBody.attachmentKeyPackets
        ).right()
    }

    private fun fetchParentMessage(parentId: String) =
        apiManager.fetchMessageDetailsBlocking(messageId = parentId).message

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
        val localAttachments = localDraft.attachments.filterNot { it.isUploaded || it.isUploading }
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
            attachments = localAttachments.plus(attachments)
            localId = localDraft.messageId
            // TODO: Improve on this approach; MAILAND-2366
            expirationTime = localDraft.expirationTime
            messageBody = localDraft.messageBody
            subject = localDraft.subject
        }

        messageDetailsRepository.saveMessage(apiDraft)
    }

    private fun retryOrFail(throwable: Throwable, messageSubject: String?): Result {
        if (runAttemptCount < SAVE_DRAFT_MAX_RETRIES) {
            Timber.d(throwable, "Create Draft Worker API call FAILED with error. Retrying...")
            return Result.retry()
        }
        Timber.e(throwable, "Create Draft Worker API call failed all the retries. error. FAILING")
        userNotifier.showPersistentError(null, messageSubject)
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
        val addressCrypto = addressCryptoFactory.create(getInputUserId(), AddressId(previousSenderAddressId))
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
        return user.findAddressById(AddressId(senderAddressId))
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

    private fun getInputUserId(): UserId {
        val idString = requireNotNull(inputData.getString(KEY_INPUT_SAVE_DRAFT_USER_ID)?.takeIfNotBlank()) {
            "User id is required"
        }
        return UserId(idString)
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
            userId: UserId,
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
                        KEY_INPUT_SAVE_DRAFT_USER_ID to userId.id,
                        KEY_INPUT_SAVE_DRAFT_MSG_DB_ID to message.dbId,
                        KEY_INPUT_SAVE_DRAFT_MSG_LOCAL_ID to message.messageId,
                        KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID to parentId,
                        KEY_INPUT_SAVE_DRAFT_ACTION_TYPE to actionType.messageActionTypeValue,
                        KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID to previousSenderAddressId
                    )
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, TEN_SECONDS, TimeUnit.SECONDS)
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
