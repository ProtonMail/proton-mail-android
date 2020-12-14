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

package ch.protonmail.android.worker

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessageSender
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.api.utils.Fields
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
import ch.protonmail.android.utils.base64.Base64Encoder
import ch.protonmail.android.utils.extensions.deserialize
import ch.protonmail.android.utils.extensions.serialize
import kotlinx.coroutines.flow.Flow
import java.time.Duration
import javax.inject.Inject

internal const val KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_DB_ID = "keyCreateDraftMessageDbId"
internal const val KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_LOCAL_ID = "keyCreateDraftMessageLocalId"
internal const val KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_PARENT_ID = "keyCreateDraftMessageParentId"
internal const val KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_ACTION_TYPE_SERIALIZED = "keyCreateDraftMessageActionTypeSerialized"
internal const val KEY_INPUT_DATA_CREATE_DRAFT_PREVIOUS_SENDER_ADDRESS_ID = "keyCreateDraftPreviousSenderAddressId"

internal const val KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_ERROR_ENUM = "keyCreateDraftErrorResult"
internal const val KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_MESSAGE_ID = "keyCreateDraftSuccessResultDbId"

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
            createDraftRequest.setParentID(parentId)
            createDraftRequest.action = getInputActionType().messageActionTypeValue
            val parentMessage = messageDetailsRepository.findMessageById(parentId)
            val attachments = parentMessage?.attachments(messageDetailsRepository.databaseProvider.provideMessagesDao())

            buildDraftRequestParentAttachments(attachments, senderAddress).forEach {
                createDraftRequest.addAttachmentKeyPacket(it.key, it.value)
            }
        }

        val encryptedMessage = requireNotNull(message.messageBody)
        createDraftRequest.addMessageBody(Fields.Message.SELF, encryptedMessage);
        createDraftRequest.setSender(buildMessageSender(message, senderAddress))

        return runCatching {
            apiManager.createDraft(createDraftRequest)
        }.fold(
            onSuccess = { response ->
                if (response.code != Constants.RESPONSE_CODE_OK) {
                    return handleFailure()
                }

                val responseDraft = response.message
                updateStoredLocalDraft(responseDraft, message)
                Result.success(
                    workDataOf(KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_MESSAGE_ID to response.messageId)
                )
            },
            onFailure = {
                handleFailure()
            }
        )

        // TODO test whether this is needed, drop otherwise
// set inline attachments from parent message that were inline previously
//        for (Attachment atta : draftMessage.getAttachments()) {
//            if (parentAttachmentList != null && !parentAttachmentList.isEmpty()) {
//                for (Attachment parentAtta : parentAttachmentList) {
//                    if (parentAtta.getKeyPackets().equals(atta.getKeyPackets())) {
//                        atta.setInline(parentAtta.getInline());
//                    }
//                }
//            }
//        }
    }

    private fun updateStoredLocalDraft(apiDraft: Message, localDraft: Message) {
        apiDraft.dbId = localDraft.dbId
        apiDraft.toList = localDraft.toList
        apiDraft.ccList = localDraft.ccList
        apiDraft.bccList = localDraft.bccList
        apiDraft.replyTos = localDraft.replyTos
        apiDraft.sender = localDraft.sender
        apiDraft.setLabelIDs(localDraft.getEventLabelIDs())
        apiDraft.parsedHeaders = localDraft.parsedHeaders
        apiDraft.isDownloaded = true
        apiDraft.setIsRead(true)
        apiDraft.numAttachments = localDraft.numAttachments
        apiDraft.localId = localDraft.messageId

        messageDetailsRepository.saveMessageInDB(apiDraft)
    }

    private fun handleFailure(): Result {
        if (retries <= SAVE_DRAFT_MAX_RETRIES) {
            retries++
            return Result.retry()
        }
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
                attachment.keyPackets!!
            }
            draftAttachments[attachment.attachmentId!!] = keyPackets
        }
        return draftAttachments
    }

    private fun reEncryptAttachment(senderAddress: Address, attachment: Attachment): String {
        val previousSenderAddressId = requireNotNull(getInputPreviousSenderAddressId())
        val addressCrypto = addressCryptoFactory.create(Id(previousSenderAddressId), Name(userManager.username))
        val primaryKey = senderAddress.keys
        val publicKey = addressCrypto.buildArmoredPublicKey(primaryKey.primaryKey!!.privateKey)

        val keyPackage = base64.decode(attachment.keyPackets!!)
        val sessionKey = addressCrypto.decryptKeyPacket(keyPackage)
        val newKeyPackage = addressCrypto.encryptKeyPacket(sessionKey, publicKey)
        return base64.encode(newKeyPackage)
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
        val errorData = workDataOf(KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_ERROR_ENUM to error.name)
        return Result.failure(errorData)
    }

    private fun getInputActionType(): Constants.MessageActionType =
        inputData
            .getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_ACTION_TYPE_SERIALIZED)?.deserialize()
            ?: NONE

    private fun getInputPreviousSenderAddressId() =
        inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_PREVIOUS_SENDER_ADDRESS_ID)

    private fun getInputParentId() =
        inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_PARENT_ID)

    private fun getInputMessageDbId() =
        inputData.getLong(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_DB_ID, INPUT_MESSAGE_DB_ID_NOT_FOUND)

    enum class CreateDraftWorkerErrors {
        MessageNotFound,
        ServerError
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            message: Message,
            parentId: String?,
            actionType: Constants.MessageActionType,
            previousSenderAddressId: String
        ): Flow<WorkInfo> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val createDraftRequest = OneTimeWorkRequestBuilder<CreateDraftWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_DB_ID to message.dbId,
                        KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_LOCAL_ID to message.messageId,
                        KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_PARENT_ID to parentId,
                        KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_ACTION_TYPE_SERIALIZED to actionType.serialize(),
                        KEY_INPUT_DATA_CREATE_DRAFT_PREVIOUS_SENDER_ADDRESS_ID to previousSenderAddressId
                    )
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, Duration.ofSeconds(TEN_SECONDS))
                .build()

            workManager.enqueue(createDraftRequest)
            return workManager.getWorkInfoByIdLiveData(createDraftRequest.id).asFlow()
        }
    }

}
