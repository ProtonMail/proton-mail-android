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
import ch.protonmail.android.attachments.UploadAttachments
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.di.CurrentUsername
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.worker.drafts.CreateDraftWorker
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class SaveDraft @Inject constructor(
    private val addressCryptoFactory: AddressCrypto.Factory,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val dispatchers: DispatcherProvider,
    private val pendingActionsDao: PendingActionsDao,
    private val createDraftWorker: CreateDraftWorker.Enqueuer,
    @CurrentUsername private val username: String,
    private val uploadAttachments: UploadAttachments,
    private val userNotifier: UserNotifier
) {

    suspend operator fun invoke(
        params: SaveDraftParameters
    ): Flow<SaveDraftResult> = withContext(dispatchers.Io) {
        Timber.i("Save Draft for messageId ${params.message.messageId}")

        val message = params.message
        val messageId = requireNotNull(message.messageId)
        val addressId = requireNotNull(message.addressID)

        val addressCrypto = addressCryptoFactory.create(Id(addressId), Name(username))
        val encryptedBody = addressCrypto.encrypt(message.decryptedBody ?: "", true).armored
        if (message.decryptedBody == null) {
            Timber.w("Save Draft for messageId $messageId - Decrypted Body was null, proceeding...")
        }

        message.messageBody = encryptedBody

        saveMessageLocallyAsDraft(message)

        return@withContext saveDraftOnline(message, params, messageId, addressCrypto)
    }

    private suspend fun saveDraftOnline(
        localDraft: Message,
        params: SaveDraftParameters,
        localDraftId: String,
        addressCrypto: AddressCrypto
    ): Flow<SaveDraftResult> {
        return createDraftWorker.enqueue(
            localDraft,
            params.parentId,
            params.actionType,
            params.previousSenderAddressId
        )
            .filter { it?.state?.isFinished == true && it.state != WorkInfo.State.CANCELLED }
            .map { workInfo ->
                if (workInfo?.state == WorkInfo.State.SUCCEEDED) {
                    val createdDraftId = requireNotNull(
                        workInfo.outputData.getString(KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID)
                    )
                    Timber.d(
                        "Save Draft to API for messageId $localDraftId succeeded. Created draftId = $createdDraftId"
                    )

                    updatePendingForSendMessage(createdDraftId, localDraftId)

                    messageDetailsRepository.findMessageById(createdDraftId)?.let {
                        val uploadResult = uploadAttachments(params.newAttachmentIds, it, addressCrypto, false)

                        if (uploadResult is UploadAttachments.Result.Failure) {
                            userNotifier.showPersistentError(uploadResult.error, localDraft.subject)
                            return@map SaveDraftResult.UploadDraftAttachmentsFailed
                        }
                        return@map SaveDraftResult.Success(createdDraftId)
                    }
                }

                Timber.e("Save Draft to API for messageId $localDraftId FAILED.")
                return@map SaveDraftResult.OnlineDraftCreationFailed
            }
            .flowOn(dispatchers.Io)
    }

    private fun updatePendingForSendMessage(createdDraftId: String, messageId: String) {
        val pendingForSending = pendingActionsDao.findPendingSendByMessageId(messageId)
        pendingForSending?.let {
            pendingForSending.messageId = createdDraftId
            pendingActionsDao.insertPendingForSend(pendingForSending)
        }
    }

    private suspend fun saveMessageLocallyAsDraft(message: Message) {
        message.setLabelIDs(
            listOf(
                ALL_DRAFT.messageLocationTypeValue.toString(),
                ALL_MAIL.messageLocationTypeValue.toString(),
                DRAFT.messageLocationTypeValue.toString()
            )
        )

        messageDetailsRepository.saveMessageLocally(message)
    }

    data class SaveDraftParameters(
        val message: Message,
        val newAttachmentIds: List<String>,
        val parentId: String?,
        val actionType: Constants.MessageActionType,
        val previousSenderAddressId: String
    )
}
