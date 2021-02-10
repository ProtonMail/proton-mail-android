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

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class UploadAttachments @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val attachmentsRepository: AttachmentsRepository,
    private val pendingActionsDao: PendingActionsDao,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val userManager: UserManager
) {

    suspend operator fun invoke(attachmentIds: List<String>, message: Message, crypto: AddressCrypto): Result =
        withContext(dispatchers.Io) {
            val messageId = requireNotNull(message.messageId)
            Timber.i("UploadAttachments started for messageId $messageId - attachmentIds $attachmentIds")

            pendingActionsDao.findPendingUploadByMessageId(messageId)?.let {
                Timber.i("UploadAttachments STOPPED for messageId $messageId as already in progress")
                return@withContext Result.UploadInProgress
            }

            pendingActionsDao.insertPendingForUpload(PendingUpload(messageId))

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
                        pendingActionsDao.deletePendingUploadByMessageId(messageId)
                        return@withContext Result.Failure(result.error)
                    }
                }

                attachment.deleteLocalFile()
            }

            val isAttachPublicKey = userManager.getMailSettings(userManager.username)?.getAttachPublicKey() ?: false
            if (isAttachPublicKey) {
                Timber.i("UploadAttachments attaching publicKey for messageId $messageId")
                val result = attachmentsRepository.uploadPublicKey(message, crypto)

                if (result is AttachmentsRepository.Result.Failure) {
                    pendingActionsDao.deletePendingUploadByMessageId(messageId)
                    return@withContext Result.Failure(result.error)
                }
            }

            pendingActionsDao.deletePendingUploadByMessageId(messageId)
            return@withContext Result.Success
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
            messageDetailsRepository.saveMessageLocally(message)
        }
    }

    sealed class Result {
        object Success : Result()
        object UploadInProgress : Result()
        data class Failure(val error: String) : Result()
    }
}
