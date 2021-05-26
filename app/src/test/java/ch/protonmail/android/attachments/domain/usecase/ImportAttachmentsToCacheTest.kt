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
import android.webkit.MimeTypeMap
import androidx.core.net.toFile
import ch.protonmail.android.attachments.domain.model.ImportAttachmentResult
import ch.protonmail.android.domain.entity.Bytes
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.MimeType
import io.mockk.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import kotlin.test.*

/**
 * Test suite for [ImportAttachmentsToCache]
 */
class ImportAttachmentsToCacheTest : CoroutinesTest {

    // region test data
    private val testUri1 = mockUri("file://folder/photo1.jpg")
    private val testUri2 = mockUri("file://folder/photo2.jpg")
    private val testUri3 = mockUri("file://folder/photo3.jpg")

    private val allTestUris = listOf(testUri1, testUri2, testUri3)

    private val importedUri1 = mockUri("file://cache/photo1.jpg")
    private val importedUri2 = mockUri("file://cache/photo2.jpg")
    private val importedUri3 = mockUri("file://cache/photo3.jpg")

    private val fileInfo1 = ImportAttachmentResult.FileInfo(
        fileName = Name("photo1"),
        extension = "jpg",
        size = Bytes(0uL),
        mimeType = MimeType.MULTIPART_MIXED
    )
    private val fileInfo2 = ImportAttachmentResult.FileInfo(
        fileName = Name("photo2"),
        extension = "jpg",
        size = Bytes(0uL),
        mimeType = MimeType.MULTIPART_MIXED
    )
    private val fileInfo3 = ImportAttachmentResult.FileInfo(
        fileName = Name("photo3"),
        extension = "jpg",
        size = Bytes(0uL),
        mimeType = MimeType.MULTIPART_MIXED
    )
    // endregion

    @get:Rule val cacheFolder = TemporaryFolder().also { it.create() }
    @get:Rule val dataFolder = TemporaryFolder().also { it.create() }

    private val dataDirectory = dataFolder.root
    private val writeTempFileToCache: WriteTempFileToCache = mockk {
        coEvery { this@mockk(testUri1, any()) } returns importedUri1.toFile()
        coEvery { this@mockk(testUri2, any()) } returns importedUri2.toFile()
        coEvery { this@mockk(testUri3, any()) } returns importedUri3.toFile()
    }
    private val contentResolver: ContentResolver = mockk {
        every { query(any(), any(), any(), any()) } returns null
        every { openInputStream(any()) } returns mockk {
            every { read(any()) } returns -1
            every { close() } returns Unit
        }
    }
    private val importAttachments = ImportAttachmentsToCache(
        dataDirectory = dataDirectory,
        writeTempFileToCache = writeTempFileToCache,
        contentResolver = contentResolver,
        dispatchers = dispatchers
    )

    @BeforeTest
    fun setup() {
        mockkStatic(MimeTypeMap::class, Uri::class)
        every { MimeTypeMap.getSingleton().getMimeTypeFromExtension(any()) } returns MimeType.MULTIPART_MIXED.string
        every { Uri.fromFile(any()) } answers {
            when (val name = firstArg<File>().name) {
                "photo1.jpg" -> importedUri1
                "photo2.jpg" -> importedUri2
                "photo3.jpg" -> importedUri3
                else -> mockUri(name)
            }
        }
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(MimeTypeMap::class, Uri::class)
    }

    @Test
    fun happyPath() = coroutinesTest {

        // given
        val expected = listOf(
            ImportAttachmentResult.Success(testUri1, importedUri1, fileInfo1),
            ImportAttachmentResult.Success(testUri2, importedUri2, fileInfo2),
            ImportAttachmentResult.Success(testUri3, importedUri3, fileInfo3)
        )

        // when
        val result = importAttachments(allTestUris).toTerminalList()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun allTheResultsArePublishedCorrectlyIfSuccess() = coroutinesTest {

        // given
        val expected = listOf(
            ImportAttachmentResult.Idle(testUri1),
            ImportAttachmentResult.OnInfo(testUri1, fileInfo1),
            ImportAttachmentResult.Success(testUri1, importedUri1, fileInfo1)
        )

        // when
        val result = importAttachments(listOf(testUri1)).toList()
            .flatten()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun allTheResultsArePublishedCorrectlyIfCantWrite() = coroutinesTest {

        // given
        val expected = listOf(
            ImportAttachmentResult.Idle(testUri1),
            ImportAttachmentResult.OnInfo(testUri1, fileInfo1),
            ImportAttachmentResult.CantWrite(testUri1, fileInfo1)
        )
        coEvery { writeTempFileToCache.invoke(testUri1, any()) } answers {
            throw IOException()
        }

        // when
        val result = importAttachments(listOf(testUri1)).toList()
            .flatten()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun attachmentsAlreadyInAppDataDirectoryAreSkipped() = coroutinesTest {

        // given
        val uri1 = mockUri("file://${dataDirectory.path}/photo1.jpg")
        val uris = listOf(uri1, testUri2, testUri3)

        // when
        val result = importAttachments(uris).toTerminalList()

        // then
        assertEquals(
            listOf(
                ImportAttachmentResult.Skipped(uri1, fileInfo1),
                ImportAttachmentResult.Success(testUri2, importedUri2, fileInfo2),
                ImportAttachmentResult.Success(testUri3, importedUri3, fileInfo3),
            ),
            result
        )
    }

    @Test
    fun emitsCorrectlyCantRead() = coroutinesTest {

        // given
        every { contentResolver.openInputStream(testUri2) } returns null

        // when
        val result = importAttachments(allTestUris).toTerminalList()

        // then
        assertEquals(
            listOf(
                ImportAttachmentResult.Success(testUri1, importedUri1, fileInfo1),
                ImportAttachmentResult.CantRead(testUri2),
                ImportAttachmentResult.Success(testUri3, importedUri3, fileInfo3),
            ),
            result
        )
    }

    @Test
    fun emitsCorrectlyCantWrite() = coroutinesTest {

        // given
        coEvery { writeTempFileToCache.invoke(testUri3, any()) } answers {
            throw IOException()
        }

        // when
        val result = importAttachments(allTestUris).toTerminalList()

        // then
        assertEquals(
            listOf(
                ImportAttachmentResult.Success(testUri1, importedUri1, fileInfo1),
                ImportAttachmentResult.Success(testUri2, importedUri2, fileInfo2),
                ImportAttachmentResult.CantWrite(testUri3, fileInfo3),
            ),
            result
        )
    }

    private suspend fun <T : ImportAttachmentResult> Flow<List<T>>.toTerminalList(): List<T> =
        toList().reduce { acc, list ->
            acc.map { result ->
                list.find { it.originalFileUri == result.originalFileUri } ?: result
            }
        }

    private fun mockUri(path: String): Uri = mockk {
        every { scheme } returns path.substringBefore("://")
        every { this@mockk.path } returns path
        every { this@mockk == any() } answers { firstArg<Uri?>()?.path == path }
        every { this@mockk.toString() } returns path
    }
}
