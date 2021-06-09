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
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.Crypto
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.worker.KEY_WORKER_ERROR_DESCRIPTION
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import java.security.GeneralSecurityException
import javax.inject.Inject

// region constants
internal const val KEY_INPUT_DATA_MESSAGE_ID_STRING = "KEY_INPUT_DATA_MESSAGE_ID_STRING"
internal const val KEY_INPUT_DATA_USER_ID_STRING = "KEY_INPUT_DATA_USER_ID_STRING"
internal const val KEY_INPUT_DATA_ATTACHMENT_ID_STRING = "KEY_INPUT_DATA_ATTACHMENT_ID_STRING"
// endregion

/**
 * Represents one unit of work downloading embedded attachments for
 * [Message][ch.protonmail.android.data.local.model.Message] and saving them to local app storage.
 *
 * Downloading of files on Android Q is based on the information from
 * https://commonsware.com/blog/2020/01/11/scoped-storage-stories-diabolical-details-downloads.html
 *
 * InputData has to contain non-null values for:
 * - messageId
 *
 * @see androidx.work.WorkManager
 * @see androidx.work.Data
 */

@HiltWorker
class DownloadEmbeddedAttachmentsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val userManager: UserManager,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val attachmentsHelper: AttachmentsHelper,
    private val handleSingleAttachment: HandleSingleAttachment,
    private val handleEmbeddedImages: HandleEmbeddedImageAttachments
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        // sanitize input
        val messageId = inputData.getString(KEY_INPUT_DATA_MESSAGE_ID_STRING)?.takeIfNotBlank()
            ?: return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty message id")
            )
        val userIdString = inputData.getString(KEY_INPUT_DATA_USER_ID_STRING)?.takeIfNotBlank()
            ?: return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty user id")
            )
        val userId = Id(userIdString)

        val singleAttachmentId = inputData.getString(KEY_INPUT_DATA_ATTACHMENT_ID_STRING)

        val message = messageDetailsRepository.findMessageById(messageId).first()
        var attachments = messageDetailsRepository.findAttachmentsByMessageId(messageId)

        requireNotNull(message)
        val addressId = requireNotNull(message.addressID)

        val addressCrypto = Crypto.forAddress(userManager, userId, Id(addressId))
        // We need this outside of this because the embedded attachments are set once the message is actually decrypted
        try {
            message.decrypt(addressCrypto)
        } catch (exception: GeneralSecurityException) {
            Timber.e(exception, "Decrypt exception")
            Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${exception.message}")
            )
        }

        if (message.isPGPMime) {
            attachments = message.attachments
        }

        val embeddedImages = attachments.mapNotNull {
            attachmentsHelper.fromAttachmentToEmbeddedImage(it, message.embeddedImageIds)
        }
        val otherAttachments = attachments.filter { attachment ->
            embeddedImages.find { attachment.attachmentId == it.attachmentId } == null
        }
        val singleAttachment = otherAttachments.find { it.attachmentId == singleAttachmentId }

        return if (singleAttachment != null) {
            handleSingleAttachment(singleAttachment, addressCrypto, messageId)
        } else {
            handleEmbeddedImages(embeddedImages, addressCrypto, messageId)
        }
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        private val uniqueWorkIdPrefix = "downloadEmbeddedAttachmentsWork"

        fun enqueue(
            messageId: String,
            userId: Id,
            attachmentId: String
        ): Operation {

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = Data.Builder()
                .putString(KEY_INPUT_DATA_MESSAGE_ID_STRING, messageId)
                .putString(KEY_INPUT_DATA_USER_ID_STRING, userId.s)
                .putString(KEY_INPUT_DATA_ATTACHMENT_ID_STRING, attachmentId)
                .build()

            val attachmentsWorkRequest =
                OneTimeWorkRequest.Builder(DownloadEmbeddedAttachmentsWorker::class.java)
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build()

            return workManager.enqueueUniqueWork(
                "$uniqueWorkIdPrefix-$attachmentId",
                ExistingWorkPolicy.KEEP,
                attachmentsWorkRequest
            )
        }
    }
}
