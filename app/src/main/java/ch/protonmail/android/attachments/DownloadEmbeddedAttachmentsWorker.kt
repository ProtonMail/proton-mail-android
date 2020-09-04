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
import android.os.Environment
import android.text.TextUtils
import android.util.Base64
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProgressListener
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadata
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.crypto.Crypto
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.DownloadedAttachmentEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.servers.notification.NotificationServer
import ch.protonmail.android.storage.AttachmentClearingService
import ch.protonmail.android.utils.AppUtil
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import javax.inject.Inject

// region constants
private const val ATTACHMENT_UNKNOWN_FILE_NAME = "attachment"
private const val NOTIFICATION_ID = 213412

private const val KEY_INPUT_DATA_MESSAGE_ID_STRING = "KEY_INPUT_DATA_MESSAGE_ID_STRING"
private const val KEY_INPUT_DATA_USERNAME_STRING = "KEY_INPUT_DATA_USERNAME_STRING"
private const val KEY_INPUT_DATA_ATTACHMENT_ID_STRING = "KEY_INPUT_DATA_ATTACHMENT_ID_STRING"
// endregion

/**
 * Represents one unit of work downloading embedded attachments for [Message][ch.protonmail.android.api.models.room.messages.Message] and saving them to local app storage.
 *
 * InputData has to contain non-null values for:
 * - messageId
 *
 * @see androidx.work.WorkManager
 * @see androidx.work.Data
 */

class DownloadEmbeddedAttachmentsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    @Inject
    internal lateinit var userManager: UserManager

    @Inject
    internal lateinit var api: ProtonMailApiManager

    @Inject
    internal lateinit var messageDetailsRepository: MessageDetailsRepository

    @Inject
    internal lateinit var attachmentMetadataDatabase: AttachmentMetadataDatabase

    private val notificationManager by lazy { applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private lateinit var notificationBuilder: NotificationCompat.Builder

    init {
        (applicationContext as ProtonMailApplication).appComponent.inject(this)
    }

    override fun doWork(): Result {

        // sanitize input
        val messageId = inputData.getString(KEY_INPUT_DATA_MESSAGE_ID_STRING)
            ?: return Result.failure()
        val username = inputData.getString(KEY_INPUT_DATA_USERNAME_STRING)
            ?: return Result.failure()

        val singleAttachmentId = inputData.getString(KEY_INPUT_DATA_ATTACHMENT_ID_STRING)

        var attachments: List<Attachment>
        var message = messageDetailsRepository.findSearchMessageById(messageId)
        if (message != null) { // use search or standard message database, if Message comes from search
            attachments = messageDetailsRepository.findSearchAttachmentsByMessageId(messageId)
        } else {
            message = messageDetailsRepository.findMessageById(messageId)
            attachments = messageDetailsRepository.findAttachmentsByMessageId(messageId)
        }

        if (message == null) return Result.failure()

        val addressCrypto = Crypto.forAddress(userManager, username, message.addressID!!)
                ?: return Result.failure()
        // We need this outside of this because the embedded attachments are set once the message is actually decrypted
        try {
            message.decrypt(addressCrypto)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (message.isPGPMime) {
            attachments = message.Attachments
        }

        val embeddedImages = attachments.mapNotNull { EmbeddedImage.fromAttachment(it, message.embeddedImagesArray) }
        val otherAttachments = attachments.filter { attachment -> embeddedImages.find { attachment.attachmentId == it.attachmentId } == null }
        val singleAttachment = otherAttachments.find { it.attachmentId == singleAttachmentId }

        return if (singleAttachment != null) {
            val attachmentDirectoryFile = File(applicationContext.filesDir.toString() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS + messageId + "/" + singleAttachmentId)
            if (!createAttachmentFolderIfNeeded(attachmentDirectoryFile)) {
                AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.FAILED))
                return Result.failure()
            }
            handleSingleAttachment(singleAttachment, addressCrypto, attachmentDirectoryFile, messageId)
        } else {
            val attachmentsDirectoryFile = File(applicationContext.filesDir.toString() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS + messageId)
            if (!createAttachmentFolderIfNeeded(attachmentsDirectoryFile)) {
                AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.FAILED))
                return Result.failure()
            }
            handleEmbeddedImages(embeddedImages, addressCrypto, attachmentsDirectoryFile, messageId)
        }
    }

    private fun handleSingleAttachment(attachment: Attachment, crypto: AddressCrypto, attachmentsDirectoryFile: File, messageId: String): ListenableWorker.Result {

        AppUtil.postEventOnUi(DownloadedAttachmentEvent(Status.STARTED, attachment.fileName, attachment.attachmentId, messageId, false))

        val filenameInCache = attachment.fileName!!.replace(" ", "_").replace("/", ":")
        val attachmentFile = File(attachmentsDirectoryFile, filenameInCache)
        val uniqueFilenameInDownloads = createUniqueFilename(attachment.fileName ?: ATTACHMENT_UNKNOWN_FILE_NAME, Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))

        try {

            initializeNotificationBuilder(uniqueFilenameInDownloads)

            val decryptedByteArray = getAttachmentData(crypto, attachment.mimeData, attachment.attachmentId!!, attachment.keyPackets, attachment.fileSize)
            FileOutputStream(attachmentFile).use {
                it.write(decryptedByteArray)
            }

            val attachmentMetadata = AttachmentMetadata(attachment.attachmentId!!, attachment.fileName!!, attachment.fileSize, attachment.messageId + "/" + attachment.attachmentId + "/" + filenameInCache, attachment.messageId, System.currentTimeMillis())
            attachmentMetadataDatabase.insertAttachmentMetadata(attachmentMetadata)

            attachmentFile.copyTo(File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), uniqueFilenameInDownloads))

        } catch (e: Exception) {
            AppUtil.postEventOnUi(DownloadedAttachmentEvent(Status.FAILED, filenameInCache, attachment.attachmentId, messageId, false))
            return Result.failure()
        }

        AppUtil.postEventOnUi(DownloadedAttachmentEvent(Status.SUCCESS, uniqueFilenameInDownloads, attachment.attachmentId, messageId, false))
        AttachmentClearingService.startRegularClearUpService() // TODO don't call it every time we download attachments
        return Result.success()
    }

    private fun handleEmbeddedImages(embeddedImages: List<EmbeddedImage>, crypto: AddressCrypto, attachmentsDirectoryFile: File, messageId: String): ListenableWorker.Result {

        // short-circuit if all attachments are already downloaded
        if (areAllAttachmentsAlreadyDownloaded(attachmentsDirectoryFile, messageId, embeddedImages)) {
            AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.SUCCESS, embeddedImages))
            return Result.success()
        }

        AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.STARTED))

        var failure = false
        embeddedImages.forEachIndexed { index, embeddedImage ->

            val filename = calculateFilename(embeddedImage.fileName!!, index)
            val attachmentFile = File(attachmentsDirectoryFile, filename)

            try {

                val decryptedByteArray = getAttachmentData(crypto, embeddedImage.mimeData, embeddedImage.attachmentId, embeddedImage.key)
                FileOutputStream(attachmentFile).use {
                    it.write(decryptedByteArray)
                }

                embeddedImage.localFileName = filename
                val attachmentMetadata = AttachmentMetadata(embeddedImage.attachmentId, embeddedImage.fileName!!, embeddedImage.size, embeddedImage.messageId + "/" + filename, embeddedImage.messageId, System.currentTimeMillis())
                attachmentMetadataDatabase.insertAttachmentMetadata(attachmentMetadata)

            } catch (e: Exception) {
                failure = true
            }
        }

        return if (failure) {
            AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.FAILED))
            Result.failure()
        } else {
            AttachmentClearingService.startRegularClearUpService() // TODO don't call it every time we download attachments
            AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.SUCCESS, embeddedImages))
            Result.success()
        }
    }

    private fun calculateFilename(originalFilename: String, position: Int): String {
        var filename = originalFilename
        filename = filename.replace(" ", "_").replace("/", ":")
        val filenameArray = filename.split(".").toTypedArray()
        filenameArray[0] = filenameArray[0] + "(" + position + ")"
        filename = TextUtils.join(".", filenameArray).replace(" ", "_").replace("/", ":")
        return filename
    }

    /**
     * If fileSize is provided, progress notification will be shown.
     */
    private fun getAttachmentData(crypto: AddressCrypto, mimeData: ByteArray?, attachmentId: String, key: String?, fileSize: Long = -1L): ByteArray? {
        if (mimeData != null) {
            return mimeData
        }

        return try {
            val byteArray = api.downloadAttachment(attachmentId, object : ProgressListener {

                var currentBytesRead = 0
                var mNotificationProgressStartTime: Long = 0
                var mNotificationProgressElapsedTime = 0L

                override fun update(bytesRead: Long, contentLength: Long, done: Boolean) {
                    val br = (bytesRead.toFloat() / fileSize * 100).toInt()
                    if (br > currentBytesRead) {
                        currentBytesRead = br
                    }
                    if (mNotificationProgressElapsedTime > 500) {
                        if (fileSize != -1L) {
                            notifyOfProgress(currentBytesRead)
                        }
                        mNotificationProgressStartTime = System.currentTimeMillis()
                        mNotificationProgressElapsedTime = 0L
                    } else {
                        mNotificationProgressElapsedTime = Date().time - mNotificationProgressStartTime
                    }
                }
            })
            val keyBytes = Base64.decode(key, Base64.DEFAULT)
            crypto.decryptAttachment(CipherText(keyBytes, byteArray)).decryptedData
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            notifyOfProgress(100)
        }
    }

    private fun areAllAttachmentsAlreadyDownloaded(attachmentsDirectoryFile: File, messageId: String, embeddedImages: List<EmbeddedImage>): Boolean {

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

    private fun createAttachmentFolderIfNeeded(path: File): Boolean = try {
        if (!path.exists()) {
            path.mkdirs()
        }
        true
    } catch (e: SecurityException) {
        false
    }

    /**
     * Creates unique filename from original one in given directory.
     */
    private fun createUniqueFilename(originalFilename: String, folder: File): String {

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

    private fun initializeNotificationBuilder(filename: String) {
        val channelId = NotificationServer(applicationContext, notificationManager).createAttachmentsChannel()

        notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(filename)
                .setPriority(1000)
                .setContentText(applicationContext.getString(R.string.download_in_progress))
                .setProgress(100, 0, false)
    }

    private fun notifyOfProgress(progress: Int) {
        if (progress == 100) {
            notificationManager.cancel(NOTIFICATION_ID)
        } else {
            notificationBuilder.setProgress(100, progress, false)
            notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    companion object {

        fun enqueue(messageId: String, username: String, attachmentId: String? = null): Operation {

            val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

            val data = Data.Builder()
                    .putString(KEY_INPUT_DATA_MESSAGE_ID_STRING, messageId)
                    .putString(KEY_INPUT_DATA_USERNAME_STRING, username)
                    .putString(KEY_INPUT_DATA_ATTACHMENT_ID_STRING, attachmentId)
                    .build()

            val downloadEmbeddedAttachmentsWork = OneTimeWorkRequest.Builder(DownloadEmbeddedAttachmentsWorker::class.java)
                    .setConstraints(constraints)
                    .setInputData(data)
                    .build()

            return WorkManager.getInstance().enqueue(downloadEmbeddedAttachmentsWork)
        }
    }

}
