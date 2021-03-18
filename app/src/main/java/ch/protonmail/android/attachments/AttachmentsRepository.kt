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
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.model.*
import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import okhttp3.MediaType
import okhttp3.RequestBody
import okio.ByteString.Companion.decodeBase64
import okio.buffer
import okio.source
import timber.log.Timber
import java.io.IOException
import java.util.Locale
import java.util.concurrent.CancellationException
import javax.inject.Inject

/**
 * Repository that handles uploading attachments
 *
 * TODO Replace `userManager` and `messageDetailsRepository`
 * dependencies for more robust components. tracked in MAILAND-1195
 */
class AttachmentsRepository @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val apiManager: ProtonMailApiManager,
    private val armorer: Armorer,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val userManager: UserManager
) {

    suspend fun upload(attachment: Attachment, crypto: AddressCrypto): Result {
        val fileContent = attachment.getFileContent()
        return uploadAttachment(attachment, crypto, fileContent)
    }

    suspend fun uploadPublicKey(message: Message, crypto: AddressCrypto): Result {
        val addressId = Id(checkNotNull(message.addressID))
        val user = userManager.getUser(userManager.requireCurrentUserId())
        val address = checkNotNull(user.findAddressById(addressId))
        val primaryKey = requireNotNull(address.keys.primaryKey)

        val publicKey = crypto.buildArmoredPublicKey(primaryKey.privateKey)
        val publicKeyFingerprint = crypto.getFingerprint(publicKey)
        val fingerprintCharsUppercase = publicKeyFingerprint.substring(0, 8).toUpperCase(Locale.getDefault())

        val attachment = Attachment().apply {
            fileName = "publickey - ${address.email} - 0x$fingerprintCharsUppercase.asc"
            mimeType = "application/pgp-keys"
            setMessage(message)
        }
        return uploadAttachment(attachment, crypto, publicKey.toByteArray())
    }

    private suspend fun uploadAttachment(
        attachment: Attachment,
        crypto: AddressCrypto,
        fileContent: ByteArray
    ): Result =
        withContext(dispatchers.Io) {
            val headers = attachment.headers
            val mimeType = attachment.mimeType
            val filename = attachment.fileName
            if (mimeType == null || filename == null) {
                Timber.e("Upload attachment failed: mimeType or filename were null")
                return@withContext Result.Failure("This attachment name / type is invalid. Please retry")
            }

            val encryptedAttachment = crypto.encrypt(fileContent, filename)
            val signedFileContent = armorer.unarmor(crypto.sign(fileContent))

            val attachmentMimeType = MediaType.parse(mimeType)
            val octetStreamMimeType = MediaType.parse("application/octet-stream")
            val keyPackage = RequestBody.create(attachmentMimeType, encryptedAttachment.keyPacket)
            val dataPackage = RequestBody.create(attachmentMimeType, encryptedAttachment.dataPacket)
            val signature = RequestBody.create(octetStreamMimeType, signedFileContent)

            val uploadResult = try {
                if (isAttachmentInline(headers)) {
                    requireNotNull(headers)

                    apiManager.uploadAttachmentInline(
                        attachment,
                        attachment.messageId,
                        contentIdFormatted(headers),
                        keyPackage,
                        dataPackage,
                        signature
                    )
                } else {
                    apiManager.uploadAttachment(
                        attachment,
                        keyPackage,
                        dataPackage,
                        signature
                    )
                }
            } catch (exception: IOException) {
                Timber.e("Upload attachment failed: $exception")
                return@withContext Result.Failure("Upload attachment request failed")
            } catch (cancellationException: CancellationException) {
                Timber.w("Upload attachment was cancelled. $cancellationException. Rethrowing")
                throw cancellationException
            } catch (exception: Exception) {
                Timber.w("Upload attachment failed throwing generic exception: $exception")
                return@withContext Result.Failure("Upload attachment request failed")
            }

            if (uploadResult.code == Constants.RESPONSE_CODE_OK) {
                attachment.attachmentId = uploadResult.attachmentID
                attachment.keyPackets = uploadResult.attachment.keyPackets
                attachment.signature = uploadResult.attachment.signature
                attachment.headers = uploadResult.attachment.headers
                attachment.fileSize = uploadResult.attachment.fileSize
                attachment.isUploaded = true
                messageDetailsRepository.saveAttachment(attachment)
                Timber.i("Upload attachment successful. attachmentId: ${uploadResult.attachmentID}")
                return@withContext Result.Success(uploadResult.attachmentID)
            }

            Timber.e("Upload attachment failed: ${uploadResult.error}")
            return@withContext Result.Failure(uploadResult.error)
        }

    private fun contentIdFormatted(headers: AttachmentHeaders): String {
        val contentId = requireNotNull(headers.contentId)
        val parts = contentId.split("<").dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size > 1) {
            return parts[1].replace(">", "")
        }
        return contentId
    }

    private fun isAttachmentInline(headers: AttachmentHeaders?) =
        headers != null &&
            headers.contentDisposition.contains("inline")

    suspend fun getAttachmentDataOrNull(
        crypto: AddressCrypto,
        attachmentId: String,
        key: String
    ): ByteArray? {
        val responseBody = apiManager.downloadAttachment(attachmentId)

        return responseBody?.let { body ->
            withContext(dispatchers.Io) {
                body.byteStream().source().buffer().use { bufferedSource ->
                    val byteArray = bufferedSource.readByteArray()
                    val keyBytes = requireNotNull(key.decodeBase64()?.toByteArray())
                    crypto.decryptAttachment(keyBytes, byteArray).decryptedData
                }
            }
        }
    }

    sealed class Result {
        data class Success(val uploadedAttachmentId: String) : Result()
        data class Failure(val error: String) : Result()
    }

}
