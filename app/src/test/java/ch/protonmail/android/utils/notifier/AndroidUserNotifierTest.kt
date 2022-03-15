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
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.notifications.presentation.utils.NotificationServer
import ch.protonmail.android.utils.extensions.showToast
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class AndroidUserNotifierTest : CoroutinesTest {

    private val testUserId = UserId("id")
    private val testUserName = Name("name")

    private val notificationServer: NotificationServer = mockk(relaxed = true)

    private val context: Context = mockk()

    private val userManager: UserManager = mockk {
        every { currentUserId } returns testUserId
        every { requireCurrentUserId() } returns testUserId
        every { this@mockk.requireCurrentUser() } returns mockk {
            every { id } returns testUserId
            every { name } returns testUserName
        }
    }

    private val userNotifier = AndroidUserNotifier(
        notificationServer,
        userManager,
        context,
        dispatchers
    )

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun userNotifierCallsNotificationServerToDisplayErrorInPersistentNotification() {
        val errorMessage = "Failed uploading attachments"
        val subject = "Message subject"

        userNotifier.showPersistentError(errorMessage, subject)

        verify { notificationServer.notifySaveDraftError(testUserId, errorMessage, subject, testUserName) }
    }

    @Test
    fun userNotifierCallsNotificationServerToDisplaySendMessageErrorInPersistentNotification() {
        val errorMessage = "Failed sending message"
        val subject = "A message subject"

        userNotifier.showSendMessageError(errorMessage, subject)

        val errorAndSubject = "\"$subject\" - $errorMessage"
        verify { notificationServer.notifySingleErrorSendingMessage(testUserId, testUserName, errorAndSubject) }
    }

    @Test
    fun showMessageSentShowsAToastOnMainThread() = runBlockingTest {
        mockkStatic("ch.protonmail.android.utils.extensions.TextExtensions")
        every { context.showToast(any<Int>()) } just Runs

        userNotifier.showMessageSent()

        verify { context.showToast(R.string.message_sent) }
        unmockkStatic("ch.protonmail.android.utils.extensions.TextExtensions")
    }
}
