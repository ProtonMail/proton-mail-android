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

import android.annotation.TargetApi
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.work.ListenableWorker
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.AttachmentMetadata
import ch.protonmail.android.events.DownloadedAttachmentEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.storage.AttachmentClearingServiceHelper
import ch.protonmail.android.utils.AppUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

private const val ATTACHMENT_UNKNOWN_FILE_NAME = "attachment"
private const val MAX_RETRY_ATTEMPTS = 3

/**
 * Handles single attachments download logic, part of [DownloadEmbeddedAttachmentsWorker].
 */
class HandleSingleAttachment @Inject constructor(
    private val context: Context,
    private val userManager: UserManager,
    private val databaseProvider: DatabaseProvider,
    private val attachmentsHelper: AttachmentsHelper,
    private val clearingServiceHelper: AttachmentClearingServiceHelper,
    private val extractAttachmentByteArray: ExtractAttachmentByteArray
) {

    private val attachmentMetadataDao: AttachmentMetadataDao
        get() = databaseProvider.provideAttachmentMetadataDao(userManager.requireCurrentUserId())

    private var runAttemptCount = 0

    suspend operator fun invoke(
        attachment: Attachment,
        crypto: AddressCrypto,
        messageId: String
    ): ListenableWorker.Result {

        val filenameInCache = attachment.fileName?.replace(" ", "_")?.replace("/", ":") ?: ATTACHMENT_UNKNOWN_FILE_NAME
        Timber.v("handleSingleAttachment filename:$filenameInCache messageId: $messageId")

        AppUtil.postEventOnUi(
            DownloadedAttachmentEvent(
                Status.STARTED, filenameInCache, null, attachment.attachmentId, messageId, false
            )
        )

        val attachmentUri = downloadAttachment(attachment, filenameInCache, crypto)

        if (attachmentUri != null) {
            val attachmentMetadata = AttachmentMetadata(
                requireNotNull(attachment.attachmentId),
                requireNotNull(attachment.fileName),
                attachment.fileSize,
                attachment.messageId + "/" + attachment.attachmentId + "/" + filenameInCache,
                attachment.messageId,
                System.currentTimeMillis(),
                attachmentUri
            )

            attachmentMetadataDao.insertAttachmentMetadata(attachmentMetadata)

            AppUtil.postEventOnUi(
                DownloadedAttachmentEvent(
                    Status.SUCCESS, filenameInCache, attachmentUri, attachment.attachmentId, messageId, false
                )
            )
        } else {
            AppUtil.postEventOnUi(
                DownloadedAttachmentEvent(
                    Status.FAILED, filenameInCache, null, attachment.attachmentId, messageId, false
                )
            )
            return ListenableWorker.Result.failure()
        }

        clearingServiceHelper.startRegularClearUpService() // TODO don't call it every time we download attachments
        return ListenableWorker.Result.success()
    }

    private suspend fun downloadAttachment(
        attachment: Attachment,
        filename: String,
        crypto: AddressCrypto
    ): Uri? {
        while (runAttemptCount < MAX_RETRY_ATTEMPTS) {
            try {
                runAttemptCount++
                // Sometimes mime type in attachment.mimeType does not match the file extension type, therefore
                // we determinate is again here, just before saving the file.
                // This is to prevent problems with saving multiple times a file with same name,
                // which was causing errors like saving "invite.ics (1)" instead of "invite (1).ics"
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                    filename.substringAfterLast(".", attachment.mimeType ?: "")
                )
                return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    downloadAttachmentForAndroidQ(attachment, filename, crypto, mimeType)
                } else {
                    downloadAttachmentBeforeQ(attachment, filename, crypto, mimeType)
                }
            } catch (exception: IOException) {
                if (runAttemptCount >= MAX_RETRY_ATTEMPTS) {
                    Timber.d(exception, "Unable to download attachment file $filename retry has failed")
                    runAttemptCount = 0
                    return null
                } else {
                    Timber.i(exception, "Unable to download attachment file $filename")
                }
            }
        }
        return null
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private suspend fun downloadAttachmentForAndroidQ(
        attachment: Attachment,
        filename: String,
        crypto: AddressCrypto,
        mimeType: String?
    ): Uri? {
        return extractAttachmentByteArray(attachment, crypto)
            ?.inputStream()
            ?.let {
                attachmentsHelper.saveAttachmentInMediaStore(
                    context.contentResolver, filename, mimeType, it
                )
            }
    }

    private suspend fun downloadAttachmentBeforeQ(
        attachment: Attachment,
        filename: String,
        crypto: AddressCrypto,
        mimeType: String?
    ): Uri? {
        return extractAttachmentByteArray(attachment, crypto)
            ?.let { bytes ->
                val file = saveBytesToFile(filename, bytes)
                val result = awaitUriFromMediaScanned(
                    context,
                    file,
                    mimeType
                )
                val uri = result.second
                Timber.v("Stored file: $filename path: ${result.first} uri: $uri")
                uri
            }
    }

    private fun saveBytesToFile(filename: String, bytes: ByteArray): File {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            filename
        )

        if (!file.exists()) {
            file.createNewFile()
        }

        file.sink().buffer().use { sink ->
            sink.write(bytes)
        }
        return file
    }

    private suspend fun awaitUriFromMediaScanned(
        context: Context,
        file: File,
        mimeType: String?
    ): Pair<String?, Uri?> = suspendCancellableCoroutine { continuation ->
        val callback = MediaScannerConnection.OnScanCompletedListener { path, uri ->
            continuation.resume(path to uri, null)
        }
        // Register callback with an API
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType),
            callback
        )
        continuation.invokeOnCancellation { Timber.d("Attachment Uri resolution cancelled") }
    }

}
