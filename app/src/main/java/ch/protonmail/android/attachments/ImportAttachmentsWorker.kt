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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.core.Constants
import ch.protonmail.android.events.PostImportAttachmentEvent
import ch.protonmail.android.events.PostImportAttachmentFailureEvent
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.Logger
import ch.protonmail.android.utils.extensions.serialize
import java.io.File

// region constants
const val KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY = "KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY"
const val KEY_INPUT_DATA_DELETE_ORIGINAL_FILE_BOOLEAN = "KEY_INPUT_DATA_DELETE_ORIGINAL_FILE_BOOLEAN"
const val KEY_INPUT_DATA_COMPOSER_INSTANCE_ID = "KEY_INPUT_DATA_COMPOSER_INSTANCE_ID"
// endregion

/**
 * Represents one unit of work importing attachments to app's cache directory.
 *
 * InputData has to contain non-null values for:
 * - fileUris
 *
 * Optionally:
 * - deleteOriginalFile: if Uri's scheme is [ContentResolver.SCHEME_FILE], delete this file after import
 *
 * OutputData contains:
 * TODO when we move from EventBus to observing Workers
 *
 * @see androidx.work.WorkManager
 * @see androidx.work.Data
 */

class ImportAttachmentsWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {

        val fileUris = inputData.getStringArray(KEY_INPUT_DATA_FILE_URIS_STRING_ARRAY)?.mapNotNull { Uri.parse(it) } ?: return Result.failure()
        val deleteOriginalFile = inputData.getBoolean(KEY_INPUT_DATA_DELETE_ORIGINAL_FILE_BOOLEAN, false)
        val composerInstanceId = inputData.getString(KEY_INPUT_DATA_COMPOSER_INSTANCE_ID)

        val postImportAttachmentEvents = mutableListOf<PostImportAttachmentEvent>()
        val contentResolver = applicationContext.contentResolver

        fileUris.filterNot { it.scheme == "file" && (it.path ?: "").contains(applicationContext.applicationInfo.dataDir) }.forEach { uri ->
            try {
                contentResolver.openInputStream(uri)?.let {
                    AppUtil.createTempFileFromInputStream(applicationContext, it)?.let { importedFile ->

                        var displayName = ""
                        var size: Long = 0
                        var mimeType: String? = ""

                        when (uri.scheme) {
                            ContentResolver.SCHEME_CONTENT -> {
                                mimeType = contentResolver.getType(uri)
                                val cursor = contentResolver.query(uri, null, null, null, null)

                                if (cursor != null && cursor.moveToFirst()) {
                                    displayName = try {
                                        cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                                    } catch (e: java.lang.Exception){
                                        cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)).split("/")[cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA)).split("/").size-1]
                                    }
                                    size = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
                                    size = if (size > 0) size else importedFile.length()
                                }

                                if (cursor != null && !cursor.isClosed) {
                                    cursor.close()
                                }
                            }
                            ContentResolver.SCHEME_FILE -> {
                                val file = File(uri.path)
                                displayName = file.name
                                size = file.length()
                                val extension = MimeTypeMap.getFileExtensionFromUrl(file.name)
                                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

                                if (deleteOriginalFile) {
                                    file.delete()
                                }
                            }
                        }

                        postImportAttachmentEvents.add(PostImportAttachmentEvent(Uri.fromFile(importedFile), displayName, size, mimeType ?: Constants.MIME_TYPE_UNKNOWN_FILE,composerInstanceId))
                    }
                }

            } catch (e: Exception) {
                Logger.doLogException(e)
            }
        }

        if (postImportAttachmentEvents.isEmpty()) {
            AppUtil.postEventOnUi(PostImportAttachmentFailureEvent())
        } else {
            postImportAttachmentEvents.forEach {
                AppUtil.postEventOnUi(it)
            }
        }

        // Mapping the import events by their `composerInstanceId`
        val outputEventPairs = postImportAttachmentEvents
                .map { ( it.composerInstanceId ?: "" ) to it.serialize() }
        return Result.success(workDataOf(*outputEventPairs.toTypedArray()))
    }
}
