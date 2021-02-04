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

package ch.protonmail.android.utils.notifier

import android.content.Context
import ch.protonmail.android.R
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.servers.notification.INotificationServer
import ch.protonmail.android.utils.extensions.showToast
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AndroidUserNotifierTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var notificationServer: INotificationServer

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var userManager: UserManager

    @InjectMockKs
    private lateinit var errorNotifier: AndroidUserNotifier

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun errorNotifierCallsNotificationServerToDisplayErrorInPersistentNotification() {
        val errorMessage = "Failed uploading attachments"
        val subject = "Message subject"
        every { userManager.username } returns "loggedInUsername"

        errorNotifier.showPersistentError(errorMessage, subject)

        verify { notificationServer.notifySaveDraftError(errorMessage, subject, "loggedInUsername") }
    }

    @Test
    fun errorNotifierCallsNotificationServerToDisplaySendMessageErrorInPersistentNotification() {
        val errorMessage = "Failed sending message"
        val subject = "A message subject"
        every { userManager.username } returns "loggedInUsername"

        errorNotifier.showSendMessageError(errorMessage, subject)

        val errorAndSubject = "\"$subject\" - $errorMessage"
        verify { notificationServer.notifySingleErrorSendingMessage(errorAndSubject, "loggedInUsername") }
    }

    @Test
    fun showMessageSentShowsAToastOnMainThread() = runBlockingTest {
        mockkStatic("ch.protonmail.android.utils.extensions.TextExtensions")
        every { context.showToast(any<Int>()) } just Runs

        errorNotifier.showMessageSent()

        verify { context.showToast(R.string.message_sent) }
        unmockkStatic("ch.protonmail.android.utils.extensions.TextExtensions")
    }

    @Test
    fun errorNotifierCallsNotificationServerToDisplayHumanVerificationNeededNotification() {
        val subject = "A message subject 123"
        val messageId = "8234728348"
        val isMessageInline = false
        val messageAddressId = "addressId092384"
        val message = Message().apply {
            this.messageId = messageId
            this.subject = subject
            this.isInline = isMessageInline
            this.addressID = messageAddressId
        }
        every { userManager.username } returns "loggedInUsername"

        errorNotifier.showHumanVerificationNeeded(message)

        verify {
            notificationServer.notifyVerificationNeeded(
                "loggedInUsername",
                subject,
                messageId,
                isMessageInline,
                messageAddressId
            )
        }
    }

}
