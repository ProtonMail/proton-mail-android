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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.worker.KEY_WORKER_ERROR_DESCRIPTION
import ch.protonmail.android.worker.failure
import timber.log.Timber
import java.security.GeneralSecurityException
import javax.inject.Inject

// region constants
internal const val KEY_INPUT_DATA_MESSAGE_ID_STRING = "KEY_INPUT_DATA_MESSAGE_ID_STRING"
internal const val KEY_INPUT_DATA_USERNAME_STRING = "KEY_INPUT_DATA_USERNAME_STRING"
internal const val KEY_INPUT_DATA_ATTACHMENT_ID_STRING = "KEY_INPUT_DATA_ATTACHMENT_ID_STRING"
// endregion

/**
 * Represents one unit of work downloading embedded attachments for
 * [Message][ch.protonmail.android.api.models.room.messages.Message] and saving them to local app storage.
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

class DownloadEmbeddedAttachmentsWorker @WorkerInject constructor(
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
        val messageId = requireNotNull(inputData.getString(KEY_INPUT_DATA_MESSAGE_ID_STRING))
        val username = requireNotNull(inputData.getString(KEY_INPUT_DATA_USERNAME_STRING))
        val singleAttachmentId = inputData.getString(KEY_INPUT_DATA_ATTACHMENT_ID_STRING)

        if (messageId.isEmpty() || username.isEmpty()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty messageId or username")
            )
        }

        var message = messageDetailsRepository.findSearchMessageById(messageId)
        var attachments = if (message != null) {
            // use search or standard message database, if Message comes from search
            messageDetailsRepository.findSearchAttachmentsByMessageId(messageId)
        } else {
            message = messageDetailsRepository.findMessageById(messageId)
            messageDetailsRepository.findAttachmentsByMessageId(messageId)
        }

        requireNotNull(message)
        val addressId = requireNotNull(message.addressID)

        val addressCrypto = AddressCrypto(userManager, userManager.openPgp, Name(username), Id(addressId))
        // We need this outside of this because the embedded attachments are set once the message is actually decrypted
        try {
            message.decrypt(addressCrypto)
        } catch (exception: GeneralSecurityException) {
            Timber.e(exception, "Decrypt exception")
            failure(exception)
        }

        if (message.isPGPMime) {
            attachments = message.Attachments
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
            username: String,
            attachmentId: String
        ): Operation {

            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val attachmentsWorkRequest =
                OneTimeWorkRequest.Builder(DownloadEmbeddedAttachmentsWorker::class.java)
                    .setConstraints(constraints)
                    .setInputData(
                        workDataOf(
                            KEY_INPUT_DATA_MESSAGE_ID_STRING to messageId,
                            KEY_INPUT_DATA_USERNAME_STRING to username,
                            KEY_INPUT_DATA_ATTACHMENT_ID_STRING to attachmentId
                        )
                    )
                    .build()

            return workManager.enqueueUniqueWork(
                "$uniqueWorkIdPrefix-$attachmentId",
                ExistingWorkPolicy.KEEP,
                attachmentsWorkRequest
            )
        }
    }
}
