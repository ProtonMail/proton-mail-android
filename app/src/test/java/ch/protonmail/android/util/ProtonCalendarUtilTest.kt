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

package ch.protonmail.android.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.EmailAddress
import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProtonCalendarUtilTest {

    private val mockPackageManager: PackageManager = mockk {
        every { resolveActivity(any(), any()) } returns null
    }
    private val mockContext: Context = mockk {
        every { packageManager } returns mockPackageManager
        every { startActivity(any()) } just Runs
    }
    private val util = ProtonCalendarUtil(context = mockContext)

    @Test
    fun shouldShowProtonCalendarButtonReturnsTrueIfCalendarIsNotInstalled() {
        withAndroidMocks {
            // given
            every { mockPackageManager.getLaunchIntentForPackage(any()) } returns null

            // when
            val result = util.shouldShowProtonCalendarButton()

            // then
            assertTrue(result)
        }
    }

    @Test
    fun shouldShowProtonCalendarButtonReturnsTrueIfCalendarIsInstalledAndCanHandleIcs() {
        withAndroidMocks {
            // given
            every { mockPackageManager.getLaunchIntentForPackage(any()) } returns mockk()
            every { mockPackageManager.resolveActivity(any(), any()) } returns mockk()

            // when
            val result = util.shouldShowProtonCalendarButton()

            // then
            assertTrue(result)
        }
    }

    @Test
    fun shouldShowProtonCalendarButtonReturnsFalseIfCalendarIsInstalledAndCannotHandleIcs() {
        withAndroidMocks {
            // given
            every { mockPackageManager.getLaunchIntentForPackage(any()) } returns mockk()
            every { mockPackageManager.resolveActivity(any(), any()) } returns null

            // when
            val result = util.shouldShowProtonCalendarButton()

            // then
            assertFalse(result)
        }
    }

    @Test
    fun openIcsInProtonCalendarSetCorrectUriAndEmailAddressesInIntent() {
        withAndroidMocks {
            // given
            val uriString = "I am an Uri!"
            val senderEmail = EmailAddress("sender@pm.me")
            val recipientEmail = EmailAddress("recipient@pm.me")

            val uriSlot = slot<Uri>()
            val senderEmailSlot = slot<EmailAddress>()
            val recipientEmailSlot = slot<EmailAddress>()
            captureIntentParameters(
                uriSlot = uriSlot,
                senderAddressSlot = senderEmailSlot,
                recipientAddressSlot = recipientEmailSlot
            )

            // when
            val uri = Uri.parse(uriString)
            util.openIcsInProtonCalendar(uri, senderEmail, recipientEmail)

            // then
            assertEquals(uriString, uriSlot.captured.toString())
            assertEquals(senderEmail, senderEmailSlot.captured)
            assertEquals(recipientEmail, recipientEmailSlot.captured)
        }
    }

    @Test
    fun hasCalendarAttachmentReturnsTrueIfAnyAttachmentHasCalendarAsOnlyMimetype() {
        // given
        val message = Message().apply {
            attachments = listOf(
                Attachment(mimeType = "image/jpeg"),
                Attachment(mimeType = "text/calendar"),
                Attachment(mimeType = "video/mp4")
            )
        }

        // when
        val result = util.hasCalendarAttachment(message)

        // then
        assertTrue(result)
    }

    @Test
    fun hasCalendarAttachmentReturnsTrueIfAnyAttachmentHasCalendarAsOneOfManyMimetypes() {
        // given
        val message = Message().apply {
            attachments = listOf(
                Attachment(mimeType = "image/jpeg;text/calendar;video/mp4")
            )
        }

        // when
        val result = util.hasCalendarAttachment(message)

        // then
        assertTrue(result)
    }

    @Test
    fun hasCalendarAttachmentReturnsFalseIfNoIcsAttachment() {
        // given
        val message = Message().apply {
            attachments = listOf(
                Attachment(mimeType = "image/jpeg"),
                Attachment(mimeType = "video/mp4")
            )
        }

        // when
        val result = util.hasCalendarAttachment(message)

        // then
        assertFalse(result)
    }

    @Test
    fun extractRecipientEmailOrNullReturnsEmailIfAny() {
        // given
        val emailAddress = "email@pm.me"
        val input = """
            This is a Message Header
            X-Original-To:$emailAddress
            This is the end of the Header
        """.trimIndent()
        val expected = EmailAddress(emailAddress)

        // when
        val result = util.extractRecipientEmailOrNull(input)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun extractRecipientEmailOrNullReturnsNullIfNone() {
        // given
        val input = """
            This is a Message Header
            No Email Address here
            This is the end of the Header
        """.trimIndent()

        // when
        val result = util.extractRecipientEmailOrNull(input)

        // then
        assertNull(result)
    }

    @Test
    fun extractRecipientEmailOrNullTrimsTheWhitespaces() {
        // given
        val emailAddress = "email@pm.me"
        val input = """
            This is a Message Header
            X-Original-To: $emailAddress 
            This is the end of the Header
        """.trimIndent()
        val expected = EmailAddress(emailAddress)

        // when
        val result = util.extractRecipientEmailOrNull(input)

        // then
        assertEquals(expected, result)
    }

    private fun captureIntentParameters(
        uriSlot: CapturingSlot<Uri>,
        senderAddressSlot: CapturingSlot<EmailAddress>,
        recipientAddressSlot: CapturingSlot<EmailAddress>
    ) {
        // Capture Uri
        every { anyConstructed<Intent>().setDataAndType(any(), any()) } answers {
            val uriArg = firstArg<Uri>()
            uriSlot.captured = uriArg
            self as Intent
        }
        // Capture sender address
        every {
            anyConstructed<Intent>().putExtra(
                ProtonCalendarUtil.EXTRA_SENDER_EMAIL,
                any<String>()
            )
        } answers {
            val valueArg = secondArg<String>()
            senderAddressSlot.captured = EmailAddress(valueArg)
            self as Intent
        }
        // Capture recipient address
        every {
            anyConstructed<Intent>().putExtra(
                ProtonCalendarUtil.EXTRA_RECIPIENT_EMAIL,
                any<String>()
            )
        } answers {
            val valueArg = secondArg<String>()
            recipientAddressSlot.captured = EmailAddress(valueArg)
            self as Intent
        }
    }

    private fun withAndroidMocks(block: () -> Unit) {
        mockkStatic(Uri::class) {
            every { Uri.parse(any()) } answers {
                val stringArg = firstArg<String>()
                mockk {
                    every { this@mockk.toString() } returns stringArg
                }
            }

            mockkConstructor(Intent::class) {
                every { anyConstructed<Intent>().setDataAndType(any(), any()) } answers { self as Intent }
                every { anyConstructed<Intent>().setPackage(any()) } answers { self as Intent }
                every { anyConstructed<Intent>().setFlags(any()) } answers { self as Intent }

                block()
            }
        }
    }
}
