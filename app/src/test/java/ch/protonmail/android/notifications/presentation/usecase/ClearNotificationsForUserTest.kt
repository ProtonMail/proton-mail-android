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

package ch.protonmail.android.notifications.presentation.usecase

import android.app.NotificationManager
import ch.protonmail.android.notifications.domain.NotificationRepository
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Test

class ClearNotificationsForUserTest : CoroutinesTest by CoroutinesTest() {

    private val testId = UserId("id")

    private val notificationRepository: NotificationRepository = mockk(relaxed = true)

    private val notificationManager: NotificationManager = mockk(relaxed = true)

    private val clearNotificationsForUser = ClearNotificationsForUser(
        notificationRepository,
        notificationManager
    )

    @Test
    fun `verify that notification is canceled`() {
        runBlockingTest {
            // given

            // when
            clearNotificationsForUser(testId)

            // then
            coVerify {
                notificationManager.cancel(testId.hashCode())
            }
        }
    }

    @Test
    fun `verify that notifications are removed from DB`() {
        runBlockingTest {
            // given

            // when
            clearNotificationsForUser(testId)

            // then
            coVerify {
                notificationRepository.deleteAllNotificationsByUserId(testId)
            }
        }
    }
}
