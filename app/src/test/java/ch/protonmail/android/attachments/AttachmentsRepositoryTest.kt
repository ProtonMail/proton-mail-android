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

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.AttachmentHeaders
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import okhttp3.MediaType
import okhttp3.RequestBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class AttachmentsRepositoryTest {

    @MockK
    private lateinit var attachment: Attachment

    @MockK
    private lateinit var armorer: Armorer

    @RelaxedMockK
    private lateinit var mockCipherText: CipherText

    @RelaxedMockK
    private lateinit var crypto: AddressCrypto

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @InjectMockKs
    private lateinit var repository: AttachmentsRepository


    @Test
    fun saveUploadsAttachmentToApiInlineWhenAttachmentIsInlined() {
        val messageId = "messageId"
        val contentId = "contentId"
        val mimeType = "image/jpeg"
        val fileContent = "attachment content".toByteArray()
        val fileName = "picture.jpg"
        val signedFileContent = "signedFileContent"
        val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
        val headers = AttachmentHeaders(
            mimeType,
            "contentTransferEncoding",
            listOf("inline"),
            listOf(contentId),
            "contentLocation",
            "contentEncryption"
        )
        every { crypto.encrypt(fileContent, fileName) } returns mockCipherText
        every { crypto.sign(fileContent) } returns signedFileContent
        every { armorer.unarmor(signedFileContent) } returns unarmoredSignedFileContent
        every { attachment.headers } returns headers
        every { attachment.fileName } returns fileName
        every { attachment.messageId } returns messageId
        every { attachment.mimeType } returns mimeType
        every { attachment.getFileContent() } returns fileContent

        repository.upload(attachment)

        val keyPackageSlot = slot<RequestBody>()
        val dataPackageSlot = slot<RequestBody>()
        val signatureSlot = slot<RequestBody>()
        verify {
            apiManager.uploadAttachmentInline(
                attachment,
                messageId,
                contentId,
                capture(keyPackageSlot),
                capture(dataPackageSlot),
                capture(signatureSlot)
            )
        }
        assertEquals(MediaType.parse("image/jpeg"), keyPackageSlot.captured.contentType())
        assertEquals(MediaType.parse("image/jpeg"), dataPackageSlot.captured.contentType())
        assertEquals(MediaType.parse("application/octet-stream"), signatureSlot.captured.contentType())
    }

    @Test
    fun saveUploadsAttachmentToApiInlinePassesContentIdFormatted() {
        val messageId = "messageId"
        val contentId = "ignoreFirst<content>Id<split<last< "
        val mimeType = "image/jpeg"
        val fileContent = "attachment content".toByteArray()
        val fileName = "picture.jpg"
        val signedFileContent = "signedFileContent"
        val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
        val headers = AttachmentHeaders(
            mimeType,
            "contentTransferEncoding",
            listOf("inline"),
            listOf(contentId),
            "contentLocation",
            "contentEncryption"
        )
        every { crypto.encrypt(fileContent, fileName) } returns mockCipherText
        every { crypto.sign(fileContent) } returns signedFileContent
        every { armorer.unarmor(signedFileContent) } returns unarmoredSignedFileContent
        every { attachment.headers } returns headers
        every { attachment.fileName } returns fileName
        every { attachment.messageId } returns messageId
        every { attachment.mimeType } returns mimeType
        every { attachment.getFileContent() } returns fileContent

        repository.upload(attachment)

        val expectedContentId = "contentId"
        verify {
            apiManager.uploadAttachmentInline(
                attachment,
                messageId,
                expectedContentId,
                any(),
                any(),
                any()
            )
        }
    }
}
