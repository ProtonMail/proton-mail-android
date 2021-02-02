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

package ch.protonmail.android.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.webkit.WebView
import androidx.test.filters.SmallTest
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.core.UserManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertTrue
import java.util.Arrays
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals


private const val EXTRA_IN_APP = "extra_in_app"
private const val EXTRA_TO_RECIPIENTS = "to_recipients"
private const val EXTRA_CC_RECIPIENTS = "cc_recipients"
private const val EXTRA_MAIL_TO = "mail_to"
private const val EXTRA_MESSAGE_TITLE = "message_title"
private const val EXTRA_MESSAGE_BODY = "message_body"


@SmallTest
class PMWebViewClientTest {

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var activity: Activity

    @RelaxedMockK
    private lateinit var mockWebView: WebView

    @RelaxedMockK
    private lateinit var mockContext: Context

    @InjectMockKs
    private lateinit var webViewClient: PMWebViewClient

    // Injected into webViewClient
    private val loadRemote = false

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { userManager.context } returns mockContext
    }

    @Test
    fun shouldOverrideUrlLoadingStartsComposeMessageActivityWhenAMailToLinkWithoutCcRecipientsIsLoaded() {
        val url = "mailto:marino-test@protonmail.com"

        webViewClient.shouldOverrideUrlLoading(mockWebView, url)

        val expected = Intent(mockContext, ComposeMessageActivity::class.java)
        expected.putExtra(EXTRA_IN_APP, true)
        expected.putExtra(EXTRA_TO_RECIPIENTS, arrayOf("marino-test@protonmail.com"))
        expected.putExtra(EXTRA_MAIL_TO, true)
        val actual = slot<Intent>()
        verify { activity.startActivity(capture(actual)) }
        assertIntentsEquals(expected, actual.captured)
    }

    @Test
    fun shouldOverrideUrlLoadingStartsComposeMessageActivityWhenAMailToLinkWithAllDetailsIsLoaded() {
        val url = """mailto:marino-test@protonmail.com?cc=marino-test-1@protonmail.com&bcc=test12345@gmail.com&subject=The%20subject%20of%20the%20email&body=The%20body%20of%20the%20email"""

        webViewClient.shouldOverrideUrlLoading(mockWebView, url)

        val expected = Intent(mockContext, ComposeMessageActivity::class.java)
        expected.putExtra(EXTRA_IN_APP, true)
        expected.putExtra(EXTRA_TO_RECIPIENTS, arrayOf("marino-test@protonmail.com"))
        expected.putExtra(EXTRA_CC_RECIPIENTS, arrayOf("marino-test-1@protonmail.com"))
        expected.putExtra(EXTRA_MESSAGE_TITLE, "The subject of the email")
        expected.putExtra(EXTRA_MESSAGE_BODY, "The body of the email")
        expected.putExtra(EXTRA_MAIL_TO, true)
        val actual = slot<Intent>()
        verify { activity.startActivity(capture(actual)) }
        assertIntentsEquals(expected, actual.captured)
    }


    private fun assertIntentsEquals(expected: Intent, actual: Intent) {
        val expectedExtras = expected.extras!!
        val actualExtras = actual.extras!!

        assertEquals(expected.action, actual.action)

        assertEquals(
            expectedExtras.getBoolean(EXTRA_IN_APP),
            actualExtras.getBoolean(EXTRA_IN_APP),
            "Intents EXTRA_IN_APP not equal!"
        )
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
