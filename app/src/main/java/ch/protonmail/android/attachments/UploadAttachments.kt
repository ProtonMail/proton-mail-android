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
    private val messageDetailsRepository: MessageDetailsRepository,
    private val userManager: UserManager
) {

    /**
     * This is only needed to replace existing upload attachments logic with the usage of
     * this usecase from Legacy Java Jobs.
     * Use #UploadAttachments.invoke instead
     */
    @Deprecated("Needed to replace existing logic in legacy java jobs", ReplaceWith("invoke()", ""))
    fun blocking(attachmentIds: List<String>, message: Message, crypto: AddressCrypto) =
        runBlocking {
            invoke(attachmentIds, message, crypto)
        }

    suspend operator fun invoke(attachmentIds: List<String>, message: Message, crypto: AddressCrypto): Result =
        withContext(dispatchers.Io) {
            Timber.i("UploadAttachments started for messageId ${message.messageId} - attachmentIds $attachmentIds")

            attachmentIds.forEach { attachmentId ->
                val attachment = messageDetailsRepository.findAttachmentById(attachmentId)

                if (attachment?.filePath == null || attachment.isUploaded || attachment.doesFileExist.not()) {
                    Timber.d(
                        "Skipping attachment: not found, invalid or was already uploaded = ${attachment?.isUploaded}"
                    )
                    return@forEach
                }
                attachment.setMessage(message)

                val result = attachmentsRepository.upload(attachment, crypto)

                when (result) {
                    is AttachmentsRepository.Result.Success -> {
                        updateMessageWithUploadedAttachment(message, result.uploadedAttachmentId)
                    }
                    is AttachmentsRepository.Result.Failure -> {
                        return@withContext Result.Failure(result.error)
                    }
                }

                attachment.deleteLocalFile()
            }

            val isAttachPublicKey = userManager.getMailSettings(userManager.username)?.getAttachPublicKey() ?: false
            if (isAttachPublicKey) {
                val result = attachmentsRepository.uploadPublicKey(message, crypto)

                if (result is AttachmentsRepository.Result.Failure) {
                    return@withContext Result.Failure(result.error)
                }
            }

            return@withContext Result.Success
        }

    private suspend fun updateMessageWithUploadedAttachment(
        message: Message,
        uploadedAttachmentId: String
    ) {
        val uploadedAttachment = messageDetailsRepository.findAttachmentById(uploadedAttachmentId)
        uploadedAttachment?.let {
            val attachments = message.Attachments.toMutableList()
            attachments.removeIf { it.fileName == uploadedAttachment.fileName }
            attachments.add(uploadedAttachment)
            message.setAttachmentList(attachments)
            messageDetailsRepository.saveMessageLocally(message)
        }
    }

    sealed class Result {
        object Success : Result()
        data class Failure(val error: String) : Result()
    }
}
