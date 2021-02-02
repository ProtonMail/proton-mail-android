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

import android.app.NotificationManager
import android.content.Context
import android.text.TextUtils
import android.util.Base64
import androidx.core.app.NotificationCompat
import ch.protonmail.android.R
import ch.protonmail.android.api.ProgressListener
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.servers.notification.NotificationServer
import ch.protonmail.android.utils.AppUtil
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Date
import javax.inject.Inject

private const val NOTIFICATION_ID = 213_412
private const val FULL_PROGRESS = 100

class DownloadAttachmentsHelper @Inject constructor(
    private val context: Context,
    private val api: ProtonMailApiManager,
    private val notificationManager: NotificationManager
) {
    private var notificationBuilder: NotificationCompat.Builder? = null

    fun areAllAttachmentsAlreadyDownloaded(
        attachmentsDirectoryFile: File,
        messageId: String,
        embeddedImages: List<EmbeddedImage>,
        attachmentMetadataDatabase: AttachmentMetadataDatabase
    ): Boolean {

        if (attachmentsDirectoryFile.exists()) {

            val attachmentMetadataList = attachmentMetadataDatabase.getAllAttachmentsForMessage(messageId)
            attachmentMetadataList.size

            embeddedImages.forEach { embeddedImage ->
                attachmentMetadataList.find { it.id == embeddedImage.attachmentId }?.let {
                    embeddedImage.localFileName = it.localLocation.substringAfterLast("/")
                }
            }

            // all embedded images are in the local filestorage already
            if (embeddedImages.all { it.localFileName != null }) return true
        }

        return false
    }

    fun createAttachmentFolderIfNeeded(path: File): Boolean = try {
        if (!path.exists()) {
            path.mkdirs()
        }
        true
    } catch (exception: SecurityException) {
        Timber.e(exception, "createAttachmentFolderIfNeeded exception")
        AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.FAILED))
        false
    }

    /**
     * Creates unique filename from original one in given directory.
     */
    fun createUniqueFilename(originalFilename: String, folder: File): String {

        val sanitizedOriginalFilename = originalFilename.replace(" ", "_").replace("/", ":")

        if (!File(folder, sanitizedOriginalFilename).exists()) return sanitizedOriginalFilename

        val name = sanitizedOriginalFilename.substringBeforeLast('.', "")
        val extension = sanitizedOriginalFilename.substringAfterLast('.', "")
        var counter = 0
        do {
            counter++
        } while (File(folder, "$name($counter).$extension").exists())

        return "$name($counter).$extension"
    }

    private fun initializeNotificationBuilder(
        filename: String
    ): NotificationCompat.Builder {
        val channelId = NotificationServer(context, notificationManager).createAttachmentsChannel()

        return NotificationCompat.Builder(context, channelId)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(filename)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentText(context.getString(R.string.download_in_progress))
            .setProgress(FULL_PROGRESS, 0, false)
    }

    fun calculateFilename(originalFilename: String, position: Int): String {
        var filename = originalFilename.replace(" ", "_").replace("/", ":")
        val filenameArray = filename.split(".").toTypedArray()
        filenameArray[0] = filenameArray[0] + "(" + position + ")"
        filename = TextUtils.join(".", filenameArray).replace(" ", "_").replace("/", ":")
        return filename
    }

    /**
     * If fileSize is provided, progress notification will be shown.
     */
    fun getAttachmentData(
        crypto: AddressCrypto,
        mimeData: ByteArray?,
        attachmentId: String,
        key: String?,
        fileSize: Long = -1L,
        uniqueFileName: String? = null
    ): ByteArray? {
        if (mimeData != null) {
            return mimeData
        }

        uniqueFileName?.let {
            notificationBuilder = initializeNotificationBuilder(it)
        }

        return try {
            val byteArray = api.downloadAttachment(
                attachmentId,
                object : ProgressListener {

                    var currentBytesRead = 0
                    var notificationProgressStartTime: Long = 0
                    var notificationProgressElapsedTime = 0L

                    override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                        val br = (bytesRead.toFloat() / fileSize * FULL_PROGRESS).toInt()
                        if (br > currentBytesRead) {
                            currentBytesRead = br
                        }
                        if (notificationProgressElapsedTime > 500) {
                            if (fileSize != -1L) {
                                notifyOfProgress(currentBytesRead)
                            }
                            notificationProgressStartTime = System.currentTimeMillis()
                            notificationProgressElapsedTime = 0L
                        } else {
                            notificationProgressElapsedTime = Date().time - notificationProgressStartTime
                        }
                    }
                }
            )
            val keyBytes = Base64.decode(key, Base64.DEFAULT)
            crypto.decryptAttachment(CipherText(keyBytes, byteArray)).decryptedData
        } catch (exception: IOException) {
            Timber.w(exception, "getAttachmentData exception")
            null
        } finally {
            notifyOfProgress(FULL_PROGRESS)
        }
    }

    private fun notifyOfProgress(progress: Int) {
        if (progress == FULL_PROGRESS) {
            notificationManager.cancel(NOTIFICATION_ID)
        } else {
            notificationBuilder?.let { builder ->
                builder.setProgress(FULL_PROGRESS, progress, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }
    }
}
