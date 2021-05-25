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

package ch.protonmail.android.compose.presentation.mapper

import android.net.Uri
import assert4k.*
import ch.protonmail.android.attachments.domain.model.ImportAttachmentResult
import ch.protonmail.android.compose.presentation.model.ComposerAttachmentUiModel
import ch.protonmail.android.compose.presentation.model.ComposerAttachmentUiModel.State
import ch.protonmail.android.domain.entity.Bytes
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.MimeType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.*
import me.proton.core.util.kotlin.invoke
import kotlin.test.*

/**
 * Test suite for [ComposerAttachmentUiModelMapper]
 */
class ComposerAttachmentUiModelMapperTest {

    // region test data
    private val testFileName = "photo"
    private val testFileExtension = "jpg"
    private val testFileSize = Bytes(4096uL)
    private val testFileMimeType = MimeType.IMAGE

    private val testOriginalFileUri = mockUri("file://downloads/$testFileName.$testFileExtension")
    private val testImportedFileUri = mockUri("file://data/$testFileName.$testFileExtension")

    private val testFileInfo = ImportAttachmentResult.FileInfo(
        fileName = Name(testFileName),
        extension = testFileExtension,
        size = testFileSize,
        mimeType = testFileMimeType
    )
    // endregion

    private val mapper = ComposerAttachmentUiModelMapper()

    @Test
    fun fromIdle() {

        // given
        val input = ImportAttachmentResult.Idle(
            originalFileUri = testOriginalFileUri
        )
        val expected = ComposerAttachmentUiModel.Idle(
            id = testOriginalFileUri,
        )

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(expected, result)
    }

    @Test
    fun fromOnInfo() {

        // given
        val input = ImportAttachmentResult.OnInfo(
            originalFileUri = testOriginalFileUri,
            fileInfo = testFileInfo
        )
        val expected = ComposerAttachmentUiModel.Data(
            id = testOriginalFileUri,
            displayName = testFileName,
            extension = testFileExtension,
            size = testFileSize,
            icon = 0,
            state = State.Importing
        )

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(expected, result)
    }

    @Test
    fun fromSuccess() {

        // given
        val input = ImportAttachmentResult.Success(
            originalFileUri = testOriginalFileUri,
            importedFileUri = testImportedFileUri,
            fileInfo = testFileInfo
        )
        val expected = ComposerAttachmentUiModel.Data(
            id = testOriginalFileUri,
            displayName = testFileName,
            extension = testFileExtension,
            size = testFileSize,
            icon = 0,
            state = State.Ready
        )

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(expected, result)
    }

    @Test
    fun fromCantRead() {

        // given
        val input = ImportAttachmentResult.CantRead(
            originalFileUri = testOriginalFileUri
        )
        val expected = ComposerAttachmentUiModel.NoFileInfo(
            id = testOriginalFileUri
        )

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(expected, result)
    }

    @Test
    fun fromCantWriteWithInfo() {

        // given
        val input = ImportAttachmentResult.CantWrite(
            originalFileUri = testOriginalFileUri,
            fileInfo = testFileInfo
        )
        val expected = ComposerAttachmentUiModel.Data(
            id = testOriginalFileUri,
            displayName = testFileName,
            extension = testFileExtension,
            size = testFileSize,
            icon = 0,
            state = State.Error
        )

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(expected, result)
    }

    @Test
    fun fromCantWriteWithoutInfo() {

        // given
        val input = ImportAttachmentResult.CantWrite(
            originalFileUri = testOriginalFileUri,
            fileInfo = null
        )
        val expected = ComposerAttachmentUiModel.NoFileInfo(
            id = testOriginalFileUri
        )

        // when
        val result = mapper { input.toUiModel() }

        // then
        assertEquals(expected, result)
    }

    private fun mockUri(path: String): Uri = mockk {
        every { scheme } returns path.substringBefore("://")
        every { this@mockk.path } returns path
        every { this@mockk == any() } answers { firstArg<Uri?>()?.path == path }
    }
}
