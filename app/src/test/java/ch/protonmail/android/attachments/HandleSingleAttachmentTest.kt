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

import android.content.Context
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.work.ListenableWorker
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.AttachmentMetadataDao
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

class HandleSingleAttachmentTest : ArchTest {

    private val context: Context = mockk()
    private val attachmentsRepository: AttachmentsRepository = mockk()
    private val clearingServiceHelper: AttachmentClearingServiceHelper = mockk()
    private val attachmentsHelper: AttachmentsHelper = mockk()
    private val attachmentsDao: AttachmentMetadataDao = mockk()
    private val testMimeType = "image/jpeg"

    val useCase = HandleSingleAttachment(
        context,
        attachmentsDao,
        attachmentsHelper,
        clearingServiceHelper,
        attachmentsRepository
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
            attachmentsRepository.getAttachmentDataOrNull(
                crypto, testAttachmentId, testKeyPackets
            )
        } returns testBytePackets
        val expected = ListenableWorker.Result.failure()

        // when
        val result = useCase.invoke(attachment, crypto, testMessageId)

        // then
        coVerify(exactly = 3) {
            attachmentsRepository.getAttachmentDataOrNull(
                crypto, testAttachmentId, testKeyPackets
            )
        }
        assertEquals(expected, result)
    }
}
