/*
 * Copyright (c) 2022 Proton Technologies AG
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
import ch.protonmail.android.attachments.KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR
import ch.protonmail.android.attachments.UploadAttachmentsWorker
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.di.CurrentUserId
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.worker.drafts.CreateDraftWorker
import ch.protonmail.android.worker.drafts.CreateDraftWorkerErrors
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class SaveDraft @Inject constructor(
    private val addressCryptoFactory: AddressCrypto.Factory,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val dispatchers: DispatcherProvider,
    private val pendingActionDao: PendingActionDao,
    private val createDraftWorker: CreateDraftWorker.Enqueuer,
    @CurrentUserId private val userId: UserId,
    private val uploadAttachmentsWorker: UploadAttachmentsWorker.Enqueuer,
    private val userNotifier: UserNotifier
) {

    suspend operator fun invoke(
        params: SaveDraftParameters
    ): SaveDraftResult = withContext(dispatchers.Io) {
        Timber.i("Save Draft for messageId ${params.message.messageId}")

        val message = params.message
        val messageId = requireNotNull(message.messageId)
        val addressId = requireNotNull(message.addressID)

        if (params.trigger != SaveDraftTrigger.SendingMessage) {
            val addressCrypto = addressCryptoFactory.create(userId, AddressId(addressId))
            val encryptedBody = addressCrypto.encrypt(message.decryptedBody ?: "", true).armored
            if (message.decryptedBody == null) {
                Timber.w("Save Draft for messageId $messageId - Decrypted Body was null, proceeding...")
            }

            message.messageBody = encryptedBody
        }

        saveMessageLocallyAsDraft(message)

        return@withContext saveDraftOnline(message, params, messageId)
    }

    private suspend fun saveDraftOnline(
        localDraft: Message,
        params: SaveDraftParameters,
        localDraftId: String
    ): SaveDraftResult {
        return createDraftWorker.enqueue(
            userId,
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

                    if (params.trigger == SaveDraftTrigger.AutoSave) {
                        return@map SaveDraftResult.Success(createdDraftId)
                    }

                    return@map uploadAttachments(params, createdDraftId, localDraft)
                } else {
                    Timber.e("Save Draft to API for messageId $localDraftId FAILED.")

                    val saveDraftError = workInfo?.outputData?.getString(KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM)
                    saveDraftError?.let { errorName ->
                        val error = CreateDraftWorkerErrors.valueOf(errorName)
                        if (error == CreateDraftWorkerErrors.MessageAlreadySent) {
                            return@map SaveDraftResult.MessageAlreadySent
                        }
                    }
                    return@map SaveDraftResult.OnlineDraftCreationFailed
                }
            }
            .flowOn(dispatchers.Io)
            .firstOrNull() ?: SaveDraftResult.OnlineDraftCreationFailed
    }

    private suspend fun uploadAttachments(
        params: SaveDraftParameters,
        createdDraftId: String,
        localDraft: Message
    ): SaveDraftResult {
        val isMessageSending = params.trigger == SaveDraftTrigger.SendingMessage
        return uploadAttachmentsWorker.enqueue(params.newAttachmentIds, createdDraftId, isMessageSending)
            .filter { it?.state?.isFinished == true }
            .map {
                if (it?.state == WorkInfo.State.FAILED) {
                    val errorMessage = requireNotNull(
                        it.outputData.getString(KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR)
                    )
                    userNotifier.showAttachmentUploadError(errorMessage, localDraft.subject)
                    return@map SaveDraftResult.UploadDraftAttachmentsFailed
                }
                if (it?.state == WorkInfo.State.CANCELLED) {
                    return@map SaveDraftResult.UploadDraftAttachmentsFailed
                }

                return@map SaveDraftResult.Success(createdDraftId)
            }.first()
    }

    private fun updatePendingForSendMessage(createdDraftId: String, messageId: String) {
        val pendingForSending = pendingActionDao.findPendingSendByMessageIdBlocking(messageId)
        pendingForSending?.let {
            pendingForSending.messageId = createdDraftId
            pendingActionDao.insertPendingForSend(pendingForSending)
        }
    }

    private suspend fun saveMessageLocallyAsDraft(message: Message) {
        message.addLabels(
            listOf(
                ALL_DRAFT.asLabelIdString(),
                ALL_MAIL.asLabelIdString(),
                DRAFT.asLabelIdString()
            )
        )

        messageDetailsRepository.saveMessage(message)
    }

    data class SaveDraftParameters(
        val message: Message,
        val newAttachmentIds: List<String>,
        val parentId: String?,
        val actionType: Constants.MessageActionType,
        val previousSenderAddressId: String,
        val trigger: SaveDraftTrigger
    )

    enum class SaveDraftTrigger {
        UserRequested,
        AutoSave,
        SendingMessage
    }
}
