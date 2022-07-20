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
package ch.protonmail.android.attachments

import android.content.Context
import androidx.hilt.work.HiltWorker
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
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.pendingaction.data.model.PendingUpload
import ch.protonmail.android.settings.domain.usecase.GetMailSettings
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val KEY_INPUT_UPLOAD_ATTACHMENTS_ATTACHMENT_IDS = "keyUploadAttachmentAttachmentIds"
internal const val KEY_INPUT_UPLOAD_ATTACHMENTS_MESSAGE_ID = "keyUploadAttachmentMessageId"
internal const val KEY_INPUT_UPLOAD_ATTACHMENTS_IS_MESSAGE_SENDING = "keyUploadAttachmentIsMessageSending"
internal const val KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR = "keyUploadAttachmentResultError"

private const val UPLOAD_ATTACHMENTS_WORK_NAME_PREFIX = "uploadAttachmentUniqueWorkName"
private const val UPLOAD_ATTACHMENTS_MAX_RETRIES = 1

private const val DATA_URI_PREFIX = "data:"

@HiltWorker
class UploadAttachmentsWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val dispatchers: DispatcherProvider,
    private val attachmentsRepository: AttachmentsRepository,
    private val databaseProvider: DatabaseProvider,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val addressCryptoFactory: AddressCrypto.Factory,
    private val userManager: UserManager,
    private val getMailSettings: GetMailSettings
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): ListenableWorker.Result = withContext(dispatchers.Io) {
        val newAttachments = inputData
            .getStringArray(KEY_INPUT_UPLOAD_ATTACHMENTS_ATTACHMENT_IDS)?.toList().orEmpty()
        val messageId = inputData.getString(KEY_INPUT_UPLOAD_ATTACHMENTS_MESSAGE_ID).orEmpty()
        val isMessageSending = inputData.getBoolean(KEY_INPUT_UPLOAD_ATTACHMENTS_IS_MESSAGE_SENDING, false)

        messageDetailsRepository.findMessageById(messageId).first()?.let { message ->
            val addressId = requireNotNull(message.addressID)
            val userId = userManager.currentUserId ?: return@withContext failureWithError(
                "User logged out", messageId = messageId
            )
            val addressCrypto = addressCryptoFactory.create(userId, AddressId(addressId))

            val result = upload(
                userId = userId,
                attachmentIds = newAttachments,
                message = message,
                crypto = addressCrypto,
                isMessageSending = isMessageSending
            )
            return@withContext when (result) {
                is Result.Success -> ListenableWorker.Result.success()
                is Result.Failure.CantGetMailSettings -> retryOrFail(
                    error = result.error,
                    messageId = message.messageId
                )
                is Result.Failure.UploadAttachment -> retryOrFail(
                    error = result.error,
                    messageId = message.messageId
                )
                is Result.Failure.InvalidAttachment -> failureWithError(
                    error = result.error,
                    messageId = message.messageId
                )
            }
        }

        return@withContext failureWithError("Message not found", messageId = messageId)
    }

    private suspend fun upload(
        userId: UserId,
        attachmentIds: List<String>,
        message: Message,
        crypto: AddressCrypto,
        isMessageSending: Boolean
    ): Result =
        withContext(dispatchers.Io) {
            val messageId = requireNotNull(message.messageId)
            Timber.i("UploadAttachments started for messageId $messageId - attachmentIds $attachmentIds")

            val pendingActionDao = databaseProvider.providePendingActionDao(userId)
            pendingActionDao.insertPendingForUpload(PendingUpload(messageId))

            performAttachmentsUpload(attachmentIds, message, crypto, messageId)?.let { failure ->
                return@withContext failure
            }

            val mailSettings = when (val result = getMailSettings(userId).first()) {
                is GetMailSettings.Result.Error ->
                    return@withContext Result.Failure.CantGetMailSettings(result.message ?: "No error message")
                is GetMailSettings.Result.Success -> result.mailSettings
            }
            val isAttachPublicKey = mailSettings.attachPublicKey ?: false
            if (isAttachPublicKey && isMessageSending) {
                Timber.i("UploadAttachments attaching publicKey for messageId $messageId")
                val result = attachmentsRepository.uploadPublicKey(message, crypto)

                if (result is AttachmentsRepository.Result.Failure) {
                    pendingActionDao.deletePendingUploadByMessageId(messageId)
                    return@withContext Result.Failure.UploadAttachment(result.error)
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
            val attachment = messageDetailsRepository.findAttachmentById(attachmentId) ?: return@forEach

            val filePath = attachment.filePath
            if (!attachment.isUploaded &&
                (filePath == null || (!filePath.startsWith(DATA_URI_PREFIX) && attachment.doesFileExist.not()))
            ) {

                return Result.Failure.InvalidAttachment(
                    String.format(
                        context.getString(R.string.attachment_failed_message_drafted),
                        attachment.fileName
                    )
                )
            }

            if (attachment.isUploaded) {
                Timber.d(
                    "Skipping attachment ${attachment.attachmentId}: " +
                        "was already uploaded = ${attachment.isUploaded}"
                )
                return@forEach
            }
            attachment.setMessage(message)

            when (val result = attachmentsRepository.upload(attachment, crypto)) {
                is AttachmentsRepository.Result.Success -> {
                    Timber.d("UploadAttachment $attachmentId to API for messageId $messageId Succeeded.")
                    updateMessageWithUploadedAttachment(message, result.uploadedAttachmentId)
                }
                is AttachmentsRepository.Result.Failure -> {
                    Timber.e("UploadAttachment $attachmentId to API for messageId $messageId FAILED.")
                    val userId = userManager.currentUserId ?: return Result.Failure.UploadAttachment("User logged out")
                    val pendingActionDao = databaseProvider.providePendingActionDao(userId)
                    pendingActionDao.deletePendingUploadByMessageId(messageId)
                    return Result.Failure.UploadAttachment(result.error)
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
            val attachments = message.attachments.toMutableList()
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
        exception: Throwable? = null,
        messageId: String?
    ): ListenableWorker.Result {
        if (runAttemptCount < UPLOAD_ATTACHMENTS_MAX_RETRIES) {
            Timber.d("UploadAttachments Worker failed with error = $error, exception = $exception. Retrying...")
            return ListenableWorker.Result.retry()
        }
        return failureWithError(error, exception, messageId)
    }

    private fun failureWithError(
        error: String,
        exception: Throwable? = null,
        messageId: String?
    ): ListenableWorker.Result {
        Timber.e(exception, "UploadAttachments Worker failed permanently for $messageId. error = $error. FAILING")
        val errorData = workDataOf(
            KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to error,
        )
        val userId = userManager.currentUserId ?: return ListenableWorker.Result.failure(errorData)
        val pendingActionDao = databaseProvider.providePendingActionDao(userId)

        messageId?.let { pendingActionDao.deletePendingUploadByMessageId(it) }
        return ListenableWorker.Result.failure(errorData)
    }

    sealed interface Result {
        object Success : Result
        sealed interface Failure : Result {

            val error: String

            data class CantGetMailSettings(override val error: String) : Failure
            data class UploadAttachment(override val error: String) : Failure
            data class InvalidAttachment(override val error: String) : Failure
        }
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
            val uploadAttachmentsRequest = OneTimeWorkRequestBuilder<UploadAttachmentsWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_UPLOAD_ATTACHMENTS_ATTACHMENT_IDS to attachmentIds.toTypedArray(),
                        KEY_INPUT_UPLOAD_ATTACHMENTS_MESSAGE_ID to messageId,
                        KEY_INPUT_UPLOAD_ATTACHMENTS_IS_MESSAGE_SENDING to isMessageSending
                    )
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, TEN_SECONDS, TimeUnit.SECONDS)
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
