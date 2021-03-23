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
package ch.protonmail.android.attachments

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.PendingActionDao
import ch.protonmail.android.data.local.model.*
import ch.protonmail.android.di.CurrentUserId
import ch.protonmail.android.di.CurrentUserMailSettings
import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val KEY_INPUT_UPLOAD_ATTACHMENTS_ATTACHMENT_IDS = "keyUploadAttachmentAttachmentIds"
internal const val KEY_INPUT_UPLOAD_ATTACHMENTS_MESSAGE_ID = "keyUploadAttachmentMessageId"
internal const val KEY_INPUT_UPLOAD_ATTACHMENTS_IS_MESSAGE_SENDING = "keyUploadAttachmentIsMessageSending"
internal const val KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR = "keyUploadAttachmentResultError"

private const val UPLOAD_ATTACHMENTS_WORK_NAME_PREFIX = "uploadAttachmentUniqueWorkName"
private const val UPLOAD_ATTACHMENTS_MAX_RETRIES = 3

class UploadAttachments @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val dispatchers: DispatcherProvider,
    private val attachmentsRepository: AttachmentsRepository,
    private val pendingActionDao: PendingActionDao,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val addressCryptoFactory: AddressCrypto.Factory,
    @CurrentUserId private val userId: Id,
    @CurrentUserMailSettings private val mailSettings: MailSettings
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result = withContext(dispatchers.Io) {
        val newAttachments = inputData
            .getStringArray(KEY_INPUT_UPLOAD_ATTACHMENTS_ATTACHMENT_IDS)?.toList().orEmpty()
        val messageId = inputData.getString(KEY_INPUT_UPLOAD_ATTACHMENTS_MESSAGE_ID).orEmpty()
        val isMessageSending = inputData.getBoolean(KEY_INPUT_UPLOAD_ATTACHMENTS_IS_MESSAGE_SENDING, false)

        messageDetailsRepository.findMessageById(messageId).first()?.let { message ->
            val addressId = requireNotNull(message.addressID)
            val addressCrypto = addressCryptoFactory.create(userId, Id(addressId))

            return@withContext when (val result = upload(newAttachments, message, addressCrypto, isMessageSending)) {
                is Result.Success -> ListenableWorker.Result.success()
                is Result.Failure -> retryOrFail(result.error)
            }
        }

        return@withContext failureWithError("Message not found")
    }

    private suspend fun upload(
        attachmentIds: List<String>,
        message: Message,
        crypto: AddressCrypto,
        isMessageSending: Boolean
    ): Result =
        withContext(dispatchers.Io) {
            val messageId = requireNotNull(message.messageId)
            Timber.i("UploadAttachments started for messageId $messageId - attachmentIds $attachmentIds")

            pendingActionDao.insertPendingForUpload(PendingUpload(messageId))

            performAttachmentsUpload(attachmentIds, message, crypto, messageId)?.let { failure ->
                return@withContext failure
            }

            val isAttachPublicKey = mailSettings.getAttachPublicKey()
            if (isAttachPublicKey && isMessageSending) {
                Timber.i("UploadAttachments attaching publicKey for messageId $messageId")
                val result = attachmentsRepository.uploadPublicKey(message, crypto)

                if (result is AttachmentsRepository.Result.Failure) {
                    pendingActionDao.deletePendingUploadByMessageId(messageId)
                    return@withContext Result.Failure(result.error)
                }
            }

            pendingActionDao.deletePendingUploadByMessageId(messageId)
            return@withContext Result.Success
        }

    private suspend fun performAttachmentsUpload(
        attachmentIds: List<String>,
        message: Message,
        crypto: AddressCrypto,
        messageId: String
    ): Result.Failure? {
        attachmentIds.forEach { attachmentId ->
            val attachment = messageDetailsRepository.findAttachmentById(attachmentId)

            if (attachment?.filePath == null || attachment.isUploaded || attachment.doesFileExist.not()) {
                Timber.d(
                    "Skipping attachment ${attachment?.attachmentId}: " +
                        "not found, invalid or was already uploaded = ${attachment?.isUploaded}"
                )
                return@forEach
            }
            attachment.setMessage(message)

            val result = attachmentsRepository.upload(attachment, crypto)

            when (result) {
                is AttachmentsRepository.Result.Success -> {
                    Timber.d("UploadAttachment $attachmentId to API for messageId $messageId Succeeded.")
                    updateMessageWithUploadedAttachment(message, result.uploadedAttachmentId)
                }
                is AttachmentsRepository.Result.Failure -> {
                    Timber.e("UploadAttachment $attachmentId to API for messageId $messageId FAILED.")
                    pendingActionDao.deletePendingUploadByMessageId(messageId)
                    return Result.Failure(result.error)
                }
            }

            attachment.deleteLocalFile()
        }
        return null
    }

    private suspend fun updateMessageWithUploadedAttachment(
        message: Message,
        uploadedAttachmentId: String
    ) {
        val uploadedAttachment = messageDetailsRepository.findAttachmentById(uploadedAttachmentId)
        uploadedAttachment?.let {
            val attachments = message.Attachments.toMutableList()
            attachments
                .find { it.fileName == uploadedAttachment.fileName }
                ?.let {
                    attachments.remove(it)
                    attachments.add(uploadedAttachment)
                }
            message.setAttachmentList(attachments)
            messageDetailsRepository.saveMessage(message)
        }
    }

    private fun retryOrFail(
        error: String,
        exception: Throwable? = null
    ): ListenableWorker.Result {
        if (runAttemptCount <= UPLOAD_ATTACHMENTS_MAX_RETRIES) {
            Timber.d("UploadAttachments Worker failed with error = $error, exception = $exception. Retrying...")
            return ListenableWorker.Result.retry()
        }
        return failureWithError(error, exception)
    }

    private fun failureWithError(error: String, exception: Throwable? = null): ListenableWorker.Result {
        Timber.e("UploadAttachments Worker failed permanently. error = $error, exception = $exception. FAILING")
        val errorData = workDataOf(KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to error)
        return ListenableWorker.Result.failure(errorData)
    }

    sealed class Result {
        object Success : Result()
        data class Failure(val error: String) : Result()
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            attachmentIds: List<String>,
            messageId: String,
            isMessageSending: Boolean
        ): Flow<WorkInfo?> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val uploadAttachmentsRequest = OneTimeWorkRequestBuilder<UploadAttachments>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_UPLOAD_ATTACHMENTS_ATTACHMENT_IDS to attachmentIds.toTypedArray(),
                        KEY_INPUT_UPLOAD_ATTACHMENTS_MESSAGE_ID to messageId,
                        KEY_INPUT_UPLOAD_ATTACHMENTS_IS_MESSAGE_SENDING to isMessageSending
                    )
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2 * TEN_SECONDS, TimeUnit.SECONDS)
                .build()

            workManager.enqueueUniqueWork(
                "$UPLOAD_ATTACHMENTS_WORK_NAME_PREFIX-$messageId",
                ExistingWorkPolicy.REPLACE,
                uploadAttachmentsRequest
            )
            return workManager.getWorkInfoByIdLiveData(uploadAttachmentsRequest.id).asFlow()
        }
    }
}
