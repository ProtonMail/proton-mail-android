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

package ch.protonmail.android.utils

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * A helper class that serves as a wrapper for file operations.
 */
class FileHelper @Inject constructor(
    private val dispatcherProvider: DispatcherProvider
) {

    fun createFile(parent: String, child: String): File {
        val parentFile = File(parent).also {
            it.mkdirs()
        }
        return File(parentFile, child)
    }

    fun readFromFile(file: File): String? =
        runCatching {
            FileInputStream(file)
                .bufferedReader()
                .use { it.readText() }
        }
            .onFailure { Timber.i(it, "Unable to read file") }
            .onSuccess { Timber.v("File ${file.path} read success") }
            .getOrNull()

    suspend fun writeToFile(file: File, text: String): Boolean = withContext(dispatcherProvider.Io) {
        return@withContext runCatching {
            FileOutputStream(file)
                .use { it.write(text.toByteArray()) }
        }.isSuccess
    }

    fun saveStringToFile(filePath: String, dataToSave: String) =
        File(filePath).sink().buffer().use { sink ->
            sink.writeUtf8(dataToSave)
        }

    fun readStringFromFilePath(filePath: String): String =
        File(filePath)
            .source()
            .buffer()
            .readUtf8()

    /**
     * Saves string content for sharing to the file provider defined in AndroidManifest.
     * [androidx.core.content.FileProvider]
     *
     * @param fileName name of the file to save (with extension)
     * @param dataToSave string content to save
     * @param context Android context
     * @return Uri to the saved data
     */
    suspend fun saveStringToFileProvider(
        fileName: String,
        dataToSave: String,
        context: Context
    ): Uri =
        withContext(dispatcherProvider.Io) {
            val filePath = "${context.cacheDir}${File.separator}$fileName"
            val file = File(filePath)
            file.sink().buffer().use { sink ->
                sink.writeUtf8(dataToSave)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        }
}
