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

package ch.protonmail.android.usecase.compose

import androidx.work.WorkInfo
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.attachments.UploadAttachments
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.di.CurrentUsername
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.worker.CreateDraftWorker
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_MESSAGE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class SaveDraft @Inject constructor(
    private val addressCryptoFactory: AddressCrypto.Factory,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val dispatchers: DispatcherProvider,
    private val pendingActionsDao: PendingActionsDao,
    private val createDraftWorker: CreateDraftWorker.Enqueuer,
    @CurrentUsername private val username: String,
    val uploadAttachments: UploadAttachments
) {

    suspend operator fun invoke(
        params: SaveDraftParameters
    ): Flow<Result> = withContext(dispatchers.Io) {
        val message = params.message
        val messageId = requireNotNull(message.messageId)
        val addressId = requireNotNull(message.addressID)

        val addressCrypto = addressCryptoFactory.create(Id(addressId), Name(username))
        val encryptedBody = addressCrypto.encrypt(message.decryptedBody ?: "", true).armored

        message.messageBody = encryptedBody
        message.setLabelIDs(
            listOf(
                ALL_DRAFT.messageLocationTypeValue.toString(),
                ALL_MAIL.messageLocationTypeValue.toString(),
                DRAFT.messageLocationTypeValue.toString()
            )
        )

        val messageDbId = messageDetailsRepository.saveMessageLocally(message)
        messageDetailsRepository.insertPendingDraft(messageDbId)

        if (params.newAttachmentIds.isNotEmpty()) {
            pendingActionsDao.insertPendingForUpload(PendingUpload(messageId))
        }

        pendingActionsDao.findPendingSendByDbId(messageDbId)?.let {
            // TODO allow draft to be saved in this case when starting to use SaveDraft use case in PostMessageJob too
            return@withContext flowOf(Result.SendingInProgressError)
        }

        return@withContext saveDraftOnline(message, params, messageId, addressCrypto)

    }

    private fun saveDraftOnline(
        localDraft: Message,
        params: SaveDraftParameters,
        localDraftId: String,
        addressCrypto: AddressCrypto
    ): Flow<Result> {
        return createDraftWorker.enqueue(
            localDraft,
            params.parentId,
            params.actionType,
            params.previousSenderAddressId
        )
            .filter { it.state.isFinished }
            .map { workInfo ->
                if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                    val createdDraftId = workInfo.outputData.getString(KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_MESSAGE_ID)

                    updatePendingForSendMessage(createdDraftId, localDraftId)
                    deleteOfflineDraft(localDraftId)

                    messageDetailsRepository.findMessageById(createdDraftId.orEmpty())?.let {
                        val uploadResult = uploadAttachments(params.newAttachmentIds, it, addressCrypto)
                        if (uploadResult is UploadAttachments.Result.Failure) {
                            return@map Result.UploadDraftAttachmentsFailed
                        }
                    }

                    return@map Result.Success
                }

                return@map Result.OnlineDraftCreationFailed
            }
    }

    private fun deleteOfflineDraft(localDraftId: String) {
        val offlineDraft = messageDetailsRepository.findMessageById(localDraftId)
        offlineDraft?.let {
            messageDetailsRepository.deleteMessage(offlineDraft)
        }
    }

    private fun updatePendingForSendMessage(createdDraftId: String?, messageId: String) {
        val pendingForSending = pendingActionsDao.findPendingSendByOfflineMessageId(messageId)
        pendingForSending?.let {
            pendingForSending.messageId = createdDraftId
            pendingActionsDao.insertPendingForSend(pendingForSending)
        }
    }

    sealed class Result {
        object Success : Result()
        object SendingInProgressError : Result()
        object OnlineDraftCreationFailed : Result()
        object UploadDraftAttachmentsFailed : Result()
    }

    data class SaveDraftParameters(
        val message: Message,
        val newAttachmentIds: List<String>,
        val parentId: String?,
        val actionType: Constants.MessageActionType,
        val previousSenderAddressId: String
    )
}
