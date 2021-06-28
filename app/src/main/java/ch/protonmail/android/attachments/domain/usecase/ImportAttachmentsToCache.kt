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

package ch.protonmail.android.attachments.domain.usecase

import android.content.ContentResolver
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.core.database.getStringOrNull
import ch.protonmail.android.attachments.domain.model.ImportAttachmentResult
import ch.protonmail.android.di.AppCacheDirectory
import ch.protonmail.android.di.AppDataDirectory
import ch.protonmail.android.domain.entity.Bytes
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.MimeType
import ch.protonmail.android.utils.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.equalsNoCase
import me.proton.core.util.kotlin.forEachAsync
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * Importing attachments to internal app's Cache directory
 *  @see AppCacheDirectory
 *
 * Skips file with scheme "file" which are already in [dataDirectory]
 *  @see ContentResolver.SCHEME_FILE
 *  @see AppDataDirectory
 */
class ImportAttachmentsToCache @Inject constructor(
    @AppCacheDirectory private val cacheDirectory: File,
    @AppDataDirectory private val dataDirectory: File,
    private val contentResolver: ContentResolver,
    private val dispatchers: DispatcherProvider
) {

    /**
     * @param deleteOriginalFiles if `true` delete original files,
     *  only if scheme is [ContentResolver.SCHEME_FILE]
     */
    @Suppress(
        "SuspiciousCollectionReassignment", // we want to recreate the list, for avoid concurrency exceptions
        "BlockingMethodInNonBlockingContext"
    )
    operator fun invoke(
        fileUris: List<Uri>,
        deleteOriginalFiles: Boolean = false
    ): Flow<List<ImportAttachmentResult>> = channelFlow {

        // Emit initial state
        val (toSkip, toProcess) = fileUris.partition {
            // Skip files already in app's data directory
            FileScheme.FILE.matches(it) && dataDirectory.path in it.path ?: EMPTY_STRING
        }
        send(toSkip.map(::toSkipped) + toProcess.map(::toIdle))

        // Process
        var result = toProcess
        toProcess.forEachAsync { uri ->

            // Get File info
            val fileInfo = runCatching { getFileInfo(uri) }.getOrNull()
            if (fileInfo != null) {
                send(result.setFileInfo(uri, fileInfo))
            }

            // Read
            val inputStream = contentResolver.openInputStream(uri)

            if (inputStream == null) {
                send(result.setCantRead(uri))
                result -= uri
                return@forEachAsync
            }

            // Write
            val onWriteException: suspend (Exception) -> Unit = { e ->
                Timber.e(e)
                send(result.setCantWrite(uri, fileInfo))
                result -= uri
            }
            val importedFile = try {
                writeTempFile(inputStream)

            } catch (e: IOException) {
                onWriteException(e)
                return@forEachAsync
            } catch (e: SecurityException) {
                onWriteException(e)
                return@forEachAsync
            }

            // Get File info
            val success = ImportAttachmentResult.Success(
                originalFileUri = uri,
                fileInfo = fileInfo ?: getFileInfo(uri, importedFile)
            )
            send(result.setSuccess(uri, success))

            if (deleteOriginalFiles)
                deleteFileIfPossible(uri)

            result -= uri
        }
    }.flowOn(dispatchers.Io)

    /**
     * @throws IOException
     * @throws SecurityException
     */
    private fun writeTempFile(inputStream: InputStream): File {
        val file = File.createTempFile(DateUtil.generateTimestamp(), null, cacheDirectory)
        val output = file.outputStream().buffered()
        inputStream.use { input ->
            output.use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    /**
     * @throws IllegalArgumentException if [importedFile] is `null` and we fail to get info from [uri]
     */
    private fun getFileInfo(uri: Uri, importedFile: File? = null): ImportAttachmentResult.FileInfo {
        return when {
            FileScheme.FILE.matches(uri) -> getFileInfoFromFile(File(requireNotNull(uri.path)))
            FileScheme.CONTENT.matches(uri) -> getFileInfoForContentScheme(uri, importedFile)
            else -> getFileInfoFromFile(requireNotNull(importedFile))
        }
    }

    /**
     * @throws IllegalArgumentException if [importedFileFallback] is `null` and we fail to get info from [uri]
     */
    private fun getFileInfoForContentScheme(uri: Uri, importedFileFallback: File?): ImportAttachmentResult.FileInfo {
        val cursor = contentResolver.query(uri, null, null, null)
            ?: return getFileInfoFromFile(requireNotNull(importedFileFallback))

        return cursor.use {
            if (cursor.moveToFirst()) {

                @Suppress("DEPRECATION") // DATA is deprecated, but correct replacement is unknown
                val fullName = cursor.getStringOrNull(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                    ?: cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA))
                        .split("/")
                        .last()

                val (name, extension) = with(fullName) {
                    Pair(
                        substringBeforeLast("."),
                        substringAfterLast(".", missingDelimiterValue = EMPTY_STRING)
                    )
                }

                val sizeLong = cursor.getLong(cursor.getColumnIndex(OpenableColumns.SIZE))
                    .takeIf { it > 0 }
                    ?: requireNotNull(importedFileFallback).length()

                val mimeTypeString = contentResolver.getType(uri)

                ImportAttachmentResult.FileInfo(
                    fileName = Name(name),
                    extension = extension,
                    size = Bytes(sizeLong.toULong()),
                    mimeType = MimeType.fromString(mimeTypeString)
                )

            } else {
                getFileInfoFromFile(requireNotNull(importedFileFallback))
            }
        }
    }

    private fun getFileInfoFromFile(file: File): ImportAttachmentResult.FileInfo {
        val mimeTypeString = MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)
        return ImportAttachmentResult.FileInfo(
            fileName = Name(file.nameWithoutExtension),
            extension = file.extension,
            size = Bytes(file.length().toULong()),
            mimeType = MimeType.fromString(mimeTypeString)
        )
    }

    private fun deleteFileIfPossible(uri: Uri) {
        if (FileScheme.FILE.matches(uri)) {
            val file = File(checkNotNull(uri.path))
            file.delete()
        }
    }

    enum class FileScheme(val string: String) {

        /**
         * @see ContentResolver.SCHEME_CONTENT
         */
        CONTENT("content"),

        /**
         * @see ContentResolver.SCHEME_FILE
         */
        FILE("file");

        fun matches(uri: Uri): Boolean =
            matches(uri.scheme)

        private fun matches(string: String?): Boolean =
            this.string equalsNoCase string
    }
}

private fun toIdle(uri: Uri) = ImportAttachmentResult.Idle(uri)

private fun toSkipped(uri: Uri) = ImportAttachmentResult.Skipped(uri)


private fun List<Uri>.setFileInfo(uri: Uri, fileInfo: ImportAttachmentResult.FileInfo): List<ImportAttachmentResult> =
    set(uri) { ImportAttachmentResult.OnInfo(uri, fileInfo) }

private fun List<Uri>.setCantRead(uri: Uri): List<ImportAttachmentResult> =
    set(uri, ImportAttachmentResult::CantRead)

private fun List<Uri>.setCantWrite(uri: Uri, fileInfo: ImportAttachmentResult.FileInfo?): List<ImportAttachmentResult> =
    set(uri) { ImportAttachmentResult.CantWrite(uri, fileInfo) }

private fun List<Uri>.setSuccess(uri: Uri, success: ImportAttachmentResult.Success): List<ImportAttachmentResult> =
    set(uri) { success }

private fun List<Uri>.set(uri: Uri, toResult: (Uri) -> ImportAttachmentResult) =
    map {
        if (it == uri)
            toResult(uri)
        else
            ImportAttachmentResult.Idle(it)
    }
