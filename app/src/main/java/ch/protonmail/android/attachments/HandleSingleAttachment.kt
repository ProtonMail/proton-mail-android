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

import android.annotation.TargetApi
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.work.ListenableWorker
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadata
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.events.DownloadedAttachmentEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.storage.AttachmentClearingServiceHelper
import ch.protonmail.android.utils.AppUtil
import kotlinx.coroutines.suspendCancellableCoroutine
import me.proton.core.util.kotlin.DispatcherProvider
import okio.buffer
import okio.sink
import timber.log.Timber
import java.io.File
import javax.inject.Inject

private const val ATTACHMENT_UNKNOWN_FILE_NAME = "attachment"

/**
 * Handles single attachments download logic, part of [DownloadEmbeddedAttachmentsWorker].
 */
class HandleSingleAttachment @Inject constructor(
    private val context: Context,
    private val attachmentMetadataDatabase: AttachmentMetadataDatabase,
    private val attachmentsHelper: AttachmentsHelper,
    private val clearingServiceHelper: AttachmentClearingServiceHelper,
    private val dispatchers: DispatcherProvider
) {

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

            attachmentMetadataDatabase.insertAttachmentMetadata(attachmentMetadata)

            AppUtil.postEventOnUi(
                DownloadedAttachmentEvent(
                    Status.SUCCESS, filenameInCache, attachmentUri, attachment.attachmentId, messageId, false
                )
            )
        } else {
            Timber.w("handleSingleAttachment failure")
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

    private suspend fun downloadAttachment(attachment: Attachment, filename: String, crypto: AddressCrypto): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            downloadAttachmentForAndroidQ(attachment, filename, crypto)
        } else {
            downloadAttachmentBeforeQ(attachment, filename, crypto)
        }

    @TargetApi(Build.VERSION_CODES.Q)
    private suspend fun downloadAttachmentForAndroidQ(
        attachment: Attachment,
        filename: String,
        crypto: AddressCrypto
    ): Uri? {
        val decryptedByteArray = attachmentsHelper.getAttachmentData(
            crypto,
            requireNotNull(attachment.attachmentId),
            attachment.keyPackets
        )

        return decryptedByteArray?.inputStream()?.let {
            attachmentsHelper.saveAttachmentInMediaStore(
                context.contentResolver, filename, attachment.mimeType, it
            )
        }
    }

    private suspend fun downloadAttachmentBeforeQ(
        attachment: Attachment,
        filename: String,
        crypto: AddressCrypto
    ): Uri? {

        val decryptedByteArray = attachmentsHelper.getAttachmentData(
            crypto,
            requireNotNull(attachment.attachmentId),
            attachment.keyPackets
        )
        val file = saveBytesToFile(filename, decryptedByteArray)

        val result = awaitUriFromMediaScanned(
            context,
            file,
            attachment.mimeType
        )
        val uri = result.second
        Timber.v("Stored file: $filename path: ${result.first} uri: $uri")

        return uri
    }

    private fun saveBytesToFile(filename: String, decryptedByteArray: ByteArray?): File {
        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            filename
        )

        file.sink().buffer().use { sink ->
            decryptedByteArray?.let {
                sink.write(it)
            }
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
        continuation.invokeOnCancellation { Timber.w("Attachment Uri resolution cancelled") }
    }

}
