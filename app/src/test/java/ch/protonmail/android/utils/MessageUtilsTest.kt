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

import org.junit.Test
import kotlin.test.assertEquals

class MessageUtilsTest {

    @Test
    fun `isLocalAttachmentId returning true if attachmentId is missing`() {
        // given
        val attachmentId = null

        // when
        val result = MessageUtils.isLocalAttachmentId(attachmentId)

        // then
        assertEquals(true, result)
    }

    @Test
    fun `isLocalAttachmentId returning true if attachmentId is local attachmentId`() {
        // given
        val attachmentId = "-1234567890"

        // when
        val result = MessageUtils.isLocalAttachmentId(attachmentId)

        // then
        assertEquals(true, result)
    }

    @Test
    fun `isLocalAttachmentId returning true if attachmentId is remote attachmentId`() {
        // given
        val attachmentId = "e-aIFdwkb1WtpBk9rTJY63afcvA9yE7lkjApbGzbdvn-Gbugg56h3LwUrD9b-qXWoMcJuNKecwhiv_mKG1ZrzQ=="

        // when
        val result = MessageUtils.isLocalAttachmentId(attachmentId)

        // then
        assertEquals(false, result)
    }
}
