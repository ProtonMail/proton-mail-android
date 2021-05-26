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
import ch.protonmail.android.di.AppCacheDirectory
import ch.protonmail.android.utils.DateUtil
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

/**
 * Writes a temp file into cache directory
 * @see AppCacheDirectory
 */
class WriteTempFileToCache @Inject constructor(
    @AppCacheDirectory private val cacheDirectory: File,
    private val contentResolver: ContentResolver,
    private val dispatchers: DispatcherProvider
) {

    /**
     * @throws IOException
     * @throws SecurityException
     */
    suspend operator fun invoke(
        @Suppress("UNUSED_PARAMETER") // reference to Uri is needed for being able to mock the function
        inputFileUri: Uri,
        inputStream: InputStream
    ): File = withContext(dispatchers.Io) {
        @Suppress("BlockingMethodInNonBlockingContext")
        val file = File.createTempFile(DateUtil.generateTimestamp(), null, cacheDirectory)
        val output = file.outputStream().buffered()
        inputStream.use { input ->
            output.use { output ->
                input.copyTo(output)
            }
        }
        return@withContext file
    }

    /**
     * @throws IOException
     * @throws SecurityException
     */
    suspend operator fun invoke(inputFileUri: Uri): File = withContext(dispatchers.Io) {
        @Suppress("BlockingMethodInNonBlockingContext")
        val inputStream = checkNotNull(contentResolver.openInputStream(inputFileUri)) {
            "Cannot open input stream for ${inputFileUri.path}"
        }
        return@withContext invoke(inputFileUri, inputStream)
    }
}
