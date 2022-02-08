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

package ch.protonmail.android.notifications.data.mapper

import ch.protonmail.android.notifications.data.local.model.NotificationEntity
import ch.protonmail.android.notifications.domain.model.Notification
import ch.protonmail.android.notifications.domain.model.NotificationId
import ch.protonmail.android.notifications.domain.model.NotificationType
import me.proton.core.domain.entity.UserId
import org.junit.Assert
import org.junit.Test

internal class NotificationEntityDomainMapperTest {

    private val testUserId = UserId("TestUserId")
    private val notificationMapper = NotificationEntityDomainMapper()

    @Test
    fun `mapping NotificationEntity to Notification`() {
        // given
        val notificationEntity = getTestNotificationEntity(userId = testUserId, testId = "ID")

        // when
        val actual = notificationMapper.toNotification(notificationEntity)

        // then
        val expected = getTestNotification(testId = "ID")
        Assert.assertEquals(expected, actual)
    }

    @Test
    fun `mapping Notification to NotificationEntity`() {
        // given
        val notification = getTestNotification(testId = "ID")

        // when
        val actual = notificationMapper.toEntity(notification, userId = testUserId)

        // then
        val expected = getTestNotificationEntity(userId = testUserId, testId = "ID")
        Assert.assertEquals(expected, actual)
    }

    private fun getTestNotificationEntity(userId: UserId, testId: String) = NotificationEntity(
        userId = userId,
        messageId = testId,
        notificationTitle = "testUser",
        notificationBody = "subject",
        url = "https://www.example.com/",
        type = NotificationType.EMAIL
    )

    private fun getTestNotification(testId: String) = Notification(
        id = NotificationId(testId),
        notificationTitle = "testUser",
        notificationBody = "subject",
        url = "https://www.example.com/",
        type = NotificationType.EMAIL
    )
}
