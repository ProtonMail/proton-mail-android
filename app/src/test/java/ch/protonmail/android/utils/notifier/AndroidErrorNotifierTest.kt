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

import ch.protonmail.android.core.UserManager
import ch.protonmail.android.servers.notification.INotificationServer
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class AndroidErrorNotifierTest {

    @RelaxedMockK
    private lateinit var notificationServer: INotificationServer

    @MockK
    private lateinit var userManager: UserManager

    @InjectMockKs
    private lateinit var errorNotifier: AndroidErrorNotifier

    @Test
    fun errorNotifierCallsNotificationServerToDisplayErrorInPersistentNotification() {
        val errorMessage = "Failed uploading attachments"
        val subject = "Message subject"
        every { userManager.username } returns "loggedInUsername"

        errorNotifier.showPersistentError(errorMessage, subject)

        verify { notificationServer.notifySaveDraftError(errorMessage, subject, "loggedInUsername") }
    }
}
