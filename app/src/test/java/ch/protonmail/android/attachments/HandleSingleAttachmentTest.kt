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

package ch.protonmail.android.attachments

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.work.ListenableWorker
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.storage.AttachmentClearingServiceHelper
import ch.protonmail.android.utils.AppUtil
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.ArchTest
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class HandleSingleAttachmentTest : ArchTest by ArchTest() {

    private val context: Context = mockk()
    private val userManager: UserManager = mockk()
    private val databaseProvider: DatabaseProvider = mockk()
    private val clearingServiceHelper: AttachmentClearingServiceHelper = mockk()
    private val attachmentsHelper: AttachmentsHelper = mockk()
    private val testMimeType = "image/jpeg"
    private val extractAttachmentByteArray: ExtractAttachmentByteArray = mockk()

    val useCase = HandleSingleAttachment(
        context,
        userManager,
        databaseProvider,
        attachmentsHelper,
        clearingServiceHelper,
        extractAttachmentByteArray
    )

    @BeforeTest
    fun setup() {
        mockkStatic(AppUtil::class)
        every { AppUtil.postEventOnUi(any()) } answers { mockk<Void>() }

        mockkStatic(MimeTypeMap::class)
        val mockMimeTypeMap: MimeTypeMap = mockk {
            every { getMimeTypeFromExtension(any()) } returns testMimeType
        }
        every { MimeTypeMap.getSingleton() } returns mockMimeTypeMap

        mockkStatic(Environment::class)
        every { Environment.getExternalStoragePublicDirectory(any()) } returns File("Downloads")
    }

    @AfterTest
    fun teardown() {
        unmockkStatic(AppUtil::class)
        unmockkStatic(MimeTypeMap::class)
        unmockkStatic(Environment::class)
    }

    @Test
    fun verifyThatRetryWorksAndDownloadIsRetriedThreeTimes() = runBlockingTest {
        // given
        val testMessageId = "testMessageId"
        val testFileName = "myImage.jpg"
        val testAttachmentId = "testAttachmentId"
        val testKeyPackets = "testAttachmentKeyPackets"
        val testBytePackets = "testAttachmentPackets".toByteArray()
        val crypto: AddressCrypto = mockk()
        val attachment: Attachment = mockk {
            every { fileName } returns testFileName
            every { attachmentId } returns testAttachmentId
            every { messageId } returns testMessageId
            every { mimeType } returns testMimeType
            every { keyPackets } returns testKeyPackets
        }
        coEvery {
            extractAttachmentByteArray(attachment, crypto)
        } returns testBytePackets
        val expected = ListenableWorker.Result.failure()

        // when
        val result = useCase.invoke(attachment, crypto, testMessageId)

        // then
        coVerify(exactly = 3) {
            extractAttachmentByteArray(attachment, crypto)
        }
        assertEquals(expected, result)
    }
}
