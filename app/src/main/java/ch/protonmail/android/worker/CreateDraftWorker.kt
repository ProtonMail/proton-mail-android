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
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessageSender
import ch.protonmail.android.api.utils.Fields
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.utils.base64.Base64Encoder
import ch.protonmail.android.utils.extensions.deserialize
import ch.protonmail.android.utils.extensions.serialize
import javax.inject.Inject

internal const val KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_DB_ID = "keyCreateDraftMessageDbId"
internal const val KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_LOCAL_ID = "keyCreateDraftMessageLocalId"
internal const val KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_PARENT_ID = "keyCreateDraftMessageParentId"
internal const val KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_ACTION_TYPE_SERIALIZED = "keyCreateDraftMessageActionTypeSerialized"
internal const val KEY_INPUT_DATA_CREATE_DRAFT_PREVIOUS_SENDER_ADDRESS_ID = "keyCreateDraftPreviousSenderAddressId"

internal const val KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_ERROR_ENUM = "keyCreateDraftErrorResult"

private const val INPUT_MESSAGE_DB_ID_NOT_FOUND = -1L

class CreateDraftWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val messageFactory: MessageFactory,
    private val userManager: UserManager,
    private val addressCryptoFactory: AddressCrypto.Factory,
    private val base64: Base64Encoder
) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {
        val message = messageDetailsRepository.findMessageByMessageDbId(getInputMessageDbId())
            ?: return failureWithError(CreateDraftWorkerErrors.MessageNotFound)
        val senderAddressId = requireNotNull(message.addressID)
        val senderAddress = requireNotNull(getSenderAddress(senderAddressId))
        val inputParentId = getInputParentId()
        val createDraftRequest = messageFactory.createDraftApiRequest(message)

        inputParentId?.let {
            createDraftRequest.setParentID(inputParentId);
            createDraftRequest.action = getInputActionType().messageActionTypeValue
            val parentMessage = messageDetailsRepository.findMessageById(inputParentId)
            if (isSenderAddressChanged()) {
                val encryptedAttachments = reEncryptParentAttachments(parentMessage, senderAddress)
                encryptedAttachments.forEach {
                    createDraftRequest.addAttachmentKeyPacket(it.key, it.value)
                }
            }
        }

        val encryptedMessage = requireNotNull(message.messageBody)
        createDraftRequest.addMessageBody(Fields.Message.SELF, encryptedMessage);
        createDraftRequest.setSender(getMessageSender(message, senderAddress))

        return Result.failure()
    }

    private fun isSenderAddressChanged(): Boolean {
        val previousSenderAddressId = getInputPreviousSenderAddressId()
        return previousSenderAddressId?.isNotEmpty() == true
    }

    private fun reEncryptParentAttachments(
        parentMessage: Message?,
        senderAddress: Address
    ): MutableMap<String, String> {
        val attachments = parentMessage?.attachments(messageDetailsRepository.databaseProvider.provideMessagesDao())
        val previousSenderAddressId = requireNotNull(getInputPreviousSenderAddressId())
        val encryptedAttachments = mutableMapOf<String, String>()

        val addressCrypto = addressCryptoFactory.create(Id(previousSenderAddressId), Name(userManager.username))
        val primaryKey = senderAddress.keys
        val publicKey = addressCrypto.buildArmoredPublicKey(primaryKey.primaryKey!!.privateKey)

        attachments?.forEach { attachment ->
            val actionType = getInputActionType()
            if (
                actionType === Constants.MessageActionType.FORWARD ||
                ((actionType === Constants.MessageActionType.REPLY ||
                    actionType === Constants.MessageActionType.REPLY_ALL) &&
                    attachment.inline)
            ) {
                val keyPackets = attachment.keyPackets
                if (!keyPackets.isNullOrEmpty()) {
                    val keyPackage = base64.decode(keyPackets)
                    val sessionKey = addressCrypto.decryptKeyPacket(keyPackage)
                    val newKeyPackage = addressCrypto.encryptKeyPacket(sessionKey, publicKey)
                    val newKeyPackets = base64.encode(newKeyPackage)
                    encryptedAttachments[attachment.attachmentId!!] = newKeyPackets
                }
            }
        }
        return encryptedAttachments
    }

    private fun getSenderAddress(senderAddressId: String): Address? {
        val user = userManager.getUser(userManager.username).loadNew(userManager.username)
        return user.findAddressById(Id(senderAddressId))
    }

    private fun getMessageSender(message: Message, senderAddress: Address): MessageSender {
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
            ?: Constants.MessageActionType.NONE


    private fun getInputPreviousSenderAddressId() =
        inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_PREVIOUS_SENDER_ADDRESS_ID)

    private fun getInputParentId() =
        inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_PARENT_ID)

    private fun getInputMessageDbId() =
        inputData.getLong(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_DB_ID, INPUT_MESSAGE_DB_ID_NOT_FOUND)

    enum class CreateDraftWorkerErrors {
        MessageNotFound
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            message: Message,
            parentId: String?,
            actionType: Constants.MessageActionType,
            previousSenderAddressId: String
        ): LiveData<WorkInfo> {
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
                ).build()

            workManager.enqueue(createDraftRequest)
            return workManager.getWorkInfoByIdLiveData(createDraftRequest.id)
        }
    }

}
