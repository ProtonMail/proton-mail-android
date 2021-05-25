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

package ch.protonmail.android.attachments.domain.model

import android.net.Uri
import ch.protonmail.android.attachments.domain.usecase.ImportAttachmentsToCache
import ch.protonmail.android.domain.entity.Bytes
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.MimeType
import me.proton.core.util.kotlin.unsupported

/**
 * Result for [ImportAttachmentsToCache]
 *
 * @param isTerminal if `true` means that the given [originalFileUri] won't have any sequential state
 */
sealed class ImportAttachmentResult(val isTerminal: Boolean) {

    abstract val originalFileUri: Uri

    /**
     * Attachment is about to be imported
     */
    data class Idle(override val originalFileUri: Uri) : ImportAttachmentResult(isTerminal = false)

    /**
     * Information for file has been loaded
     */
    data class OnInfo(
        override val originalFileUri: Uri,
        val fileInfo: FileInfo
    ) : ImportAttachmentResult(isTerminal = false)

    /**
     * Attachment has been imported correctly
     *
     * @property importedFileUri [Uri] of the imported file
     * @property skipped `true` if the file was already in the app's folder
     */
    data class Success(
        override val originalFileUri: Uri,
        val importedFileUri: Uri,
        val fileInfo: FileInfo,
        val skipped: Boolean = originalFileUri == importedFileUri
    ) : ImportAttachmentResult(isTerminal = true)

    /**
     * File can't be read
     */
    data class CantRead(override val originalFileUri: Uri) : ImportAttachmentResult(isTerminal = true)

    /**
     * File can't be written
     *
     * @property fileInfo in nullable, because in some case, it will be possible to get Info only after we write the
     *  file in our cache directory
     */
    data class CantWrite(
        override val originalFileUri: Uri,
        val fileInfo: FileInfo?
    ) : ImportAttachmentResult(isTerminal = true)


    data class FileInfo(
        val fileName: Name,
        val extension: String,
        val size: Bytes,
        val mimeType: MimeType
    )

    companion object {

        /**
         * Attachment has been skipped, no need to be imported
         */
        fun Skipped(
            originalFileUri: Uri,
            fileInfo: FileInfo
        ) = Success(
            originalFileUri = originalFileUri,
            importedFileUri = originalFileUri,
            fileInfo = fileInfo,
            skipped = true
        )
    }
}

val ImportAttachmentResult.FileInfo.fullName get() = "${fileName.s}.$extension"

fun ImportAttachmentResult.requireFileInfo(): ImportAttachmentResult.FileInfo =
    when (this) {
        is ImportAttachmentResult.CantRead -> unsupported
        is ImportAttachmentResult.CantWrite -> requireNotNull(fileInfo)
        is ImportAttachmentResult.Idle -> unsupported
        is ImportAttachmentResult.OnInfo -> fileInfo
        is ImportAttachmentResult.Success -> fileInfo
    }
