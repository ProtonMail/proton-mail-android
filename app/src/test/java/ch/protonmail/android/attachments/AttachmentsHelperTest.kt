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
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.AttachmentHeaders
import ch.protonmail.android.jobs.helper.EmbeddedImage
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals

class AttachmentsHelperTest {

    private val context: Context = mockk()
    private val helper = AttachmentsHelper(
        context = context
    )

    @Test
    fun verifyOrdinaryMappingToEmbeddedImages() {
        // given
        val fileNamePic1 = "pic.jpg"
        val attachmentId = "attachment1"
        val mimeType = "image/jpeg"
        val contentId = "contentId"
        val key1 = "key1"
        val size = 3L
        val contentEncoding1 = "contentEncoding1"
        val messageId1 = "messageId1"
        val headers = AttachmentHeaders(
            mimeType,
            contentEncoding1,
            listOf("inline"),
            contentId,
            "contentLocation",
            "contentEncryption"
        )
        val attachment = Attachment(
            attachmentId,
            fileNamePic1,
            mimeType,
            size,
            key1,
            messageId1,
            inline = true,
            headers = headers
        )
        val embeddedImages = listOf(fileNamePic1)
        val expected = EmbeddedImage(
            attachmentId,
            fileNamePic1,
            key1,
            mimeType,
            contentEncoding1,
            contentId,
            null,
            size,
            messageId1,
            null
        )

        // when
        val actual = helper.fromAttachmentToEmbeddedImage(attachment, embeddedImages)

        // then
        assertEquals(expected, actual)
    }
}
