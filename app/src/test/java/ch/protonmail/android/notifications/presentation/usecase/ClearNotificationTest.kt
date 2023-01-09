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
import android.service.notification.StatusBarNotification
import ch.protonmail.android.notifications.domain.NotificationRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Test

class ClearNotificationTest : CoroutinesTest by CoroutinesTest() {

    private val userId = UserId("id")
    private val notificationId = "messageId"

    private val notificationRepository: NotificationRepository = mockk(relaxed = true)

    private val notificationManager: NotificationManager = mockk(relaxed = true)

    private val clearNotification = ClearNotification(
        notificationRepository,
        notificationManager
    )

    @Test
    fun `verify that parent notification is canceled`() {
        runBlockingTest {
            // given
            every { notificationManager.activeNotifications } returns emptyArray()

            // when
            clearNotification(userId, notificationId)

            // then
            coVerify {
                notificationManager.cancel(userId.hashCode())
            }
        }
    }

    @Test
    fun `verify that only child notification is canceled`() {
        runBlockingTest {
            // given
            every { notificationManager.activeNotifications } returns getMockStatusBarNotificationList()

            // when
            clearNotification(userId, notificationId)

            // then
            coVerify {
                notificationManager.cancel(notificationId.hashCode())
            }
        }
    }

    @Test
    fun `verify that notification is removed from DB`() {
        runBlockingTest {
            // given

            // when
            clearNotification(userId, notificationId)

            // then
            coVerify {
                notificationRepository.deleteNotification(userId, notificationId)
            }
        }
    }

    private fun getMockStatusBarNotificationList(): Array<StatusBarNotification> {
        val mockStatusBarNotification1 = mockk<StatusBarNotification>()
        every { mockStatusBarNotification1.groupKey } returns userId.id
        val mockStatusBarNotification2 = mockk<StatusBarNotification>()
        every { mockStatusBarNotification2.groupKey } returns userId.id
        val mockStatusBarNotification3 = mockk<StatusBarNotification>()
        every { mockStatusBarNotification3.groupKey } returns userId.id

        val mockArrayListStatusBarNotification = arrayListOf<StatusBarNotification>()
        mockArrayListStatusBarNotification.add(mockStatusBarNotification1)
        mockArrayListStatusBarNotification.add(mockStatusBarNotification2)
        mockArrayListStatusBarNotification.add(mockStatusBarNotification3)
        return mockArrayListStatusBarNotification.toTypedArray()
    }
}

