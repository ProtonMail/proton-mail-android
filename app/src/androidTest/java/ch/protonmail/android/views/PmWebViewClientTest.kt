/*
 * Copyright (c) 2020 Proton Technologies AG
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

package ch.protonmail.android.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import androidx.test.filters.SmallTest
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.settings.data.AccountSettingsRepository
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertTrue
import java.util.Arrays
import kotlin.test.Test
import kotlin.test.assertEquals

private const val EXTRA_TO_RECIPIENTS = "to_recipients"
private const val EXTRA_CC_RECIPIENTS = "cc_recipients"
private const val EXTRA_MAIL_TO = "mail_to"
private const val EXTRA_MESSAGE_TITLE = "message_title"
private const val EXTRA_MESSAGE_BODY = "message_body"

@SmallTest
class PmWebViewClientTest {

    private val userManager: UserManager = mockk(relaxed = true)

    private val activity: Activity = mockk(relaxed = true)

    private val mockWebView: WebView = mockk(relaxed = true)

    private val mockContext: Context = mockk(relaxed = true)

    private val accountSettingsRepository: AccountSettingsRepository = mockk()

    private val loadRemote = false

    private val webViewClient = PmWebViewClient(
        userManager, accountSettingsRepository, activity, loadRemote
    )

    @Test
    fun shouldOverrideUrlLoadingStartsComposeMessageActivityWhenAMailToLinkWithoutCcRecipientsIsLoaded() {
        // given
        val url = "mailto:marino-test@protonmail.com"
        val expected = Intent(mockContext, ComposeMessageActivity::class.java)
            .putExtra(EXTRA_TO_RECIPIENTS, arrayOf("marino-test@protonmail.com"))
            .putExtra(EXTRA_MAIL_TO, true)

        // when
        webViewClient.shouldOverrideUrlLoading(mockWebView, url)

        // then
        val actual = slot<Intent>()
        verify { activity.startActivity(capture(actual)) }
        assertIntentsEquals(expected, actual.captured)
    }

    @Test
    fun shouldOverrideUrlLoadingStartsComposeMessageActivityWhenAMailToLinkWithAllDetailsIsLoaded() {
        // given
        val url =
            """mailto:marino-test@protonmail.com?cc=marino-test-1@protonmail.com&bcc=test12345@gmail.com&subject=The%20subject%20of%20the%20email&body=The%20body%20of%20the%20email"""
        val expected = Intent(mockContext, ComposeMessageActivity::class.java)
            .putExtra(EXTRA_TO_RECIPIENTS, arrayOf("marino-test@protonmail.com"))
            .putExtra(EXTRA_CC_RECIPIENTS, arrayOf("marino-test-1@protonmail.com"))
            .putExtra(EXTRA_MESSAGE_TITLE, "The subject of the email")
            .putExtra(EXTRA_MESSAGE_BODY, "The body of the email")
            .putExtra(EXTRA_MAIL_TO, true)

        // when
        webViewClient.shouldOverrideUrlLoading(mockWebView, url)

        // then
        val actual = slot<Intent>()
        verify { activity.startActivity(capture(actual)) }
        assertIntentsEquals(expected, actual.captured)
    }


    private fun assertIntentsEquals(expected: Intent, actual: Intent) {
        val expectedExtras = expected.extras!!
        val actualExtras = actual.extras!!

        assertEquals(expected.action, actual.action)

        assertTrue(
            "Intents EXTRA_TO_RECIPIENTS not equal!",
            Arrays.equals(
                expectedExtras.getStringArray(EXTRA_TO_RECIPIENTS),
                actualExtras.getStringArray(EXTRA_TO_RECIPIENTS)
            )
        )
        assertEquals(
            expectedExtras.getBoolean(EXTRA_MAIL_TO),
            actualExtras.getBoolean(EXTRA_MAIL_TO),
            "Intents EXTRA_MAIL_TO not equal!"
        )
        assertEquals(
            expectedExtras.getString(EXTRA_MESSAGE_TITLE),
            actualExtras.getString(EXTRA_MESSAGE_TITLE),
            "Intents EXTRA_MESSAGE_TITLE not equal!"
        )
        assertEquals(
            expectedExtras.getString(EXTRA_MESSAGE_BODY),
            actualExtras.getString(EXTRA_MESSAGE_BODY),
            "Intents EXTRA_MESSAGE_BODY not equal!"
        )
    }
}
