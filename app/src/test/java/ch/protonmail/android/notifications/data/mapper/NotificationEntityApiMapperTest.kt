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
import ch.protonmail.android.notifications.data.remote.model.PushNotification
import ch.protonmail.android.notifications.data.remote.model.PushNotificationData
import ch.protonmail.android.notifications.data.remote.model.PushNotificationSender
import ch.protonmail.android.notifications.domain.model.NotificationType
import me.proton.core.domain.entity.UserId
import org.junit.Assert
import org.junit.Test

internal class NotificationEntityApiMapperTest {

    private val testUserId = UserId("TestUserId")
    private val notificationMapper = NotificationApiEntityMapper()

    @Test
    fun `mapping notification api model to NotificationEntity succeeds when all fields are valid`() {
        // given
        val pushNotification = getTestNotificationApiModel(testId = "ID")

        // when
        val actual = notificationMapper.toEntity(pushNotification, testUserId)

        // then
        val expected = getTestNotificationEntity(testUserId, testId = "ID")
        Assert.assertEquals(expected, actual)
    }

    private fun getTestNotificationApiModel(testId: String) = PushNotification(
        type = "email",
        version = 2,
        data = getTestNotificationDataApiModel(testId),
        action = "message_created"
    )

    private fun getTestNotificationDataApiModel(testId: String) = PushNotificationData(
        title = "ProtonMail",
        subtitle = "",
        body = "subject",
        vibrate = 1,
        sound = 1,
        largeIcon = "large_icon",
        smallIcon = "small_icon",
        badge = 123,
        messageId = testId,
        customId = "123-abc",
        sender = PushNotificationSender("testUser@protonmail.com", "testUser", ""),
        url = "https://www.example.com/"
    )

    private fun getTestNotificationEntity(userId: UserId, testId: String) = NotificationEntity(
        userId = userId,
        messageId = testId,
        notificationTitle = "testUser",
        notificationBody = "subject",
        url = "https://www.example.com/",
        type = NotificationType.EMAIL
    )
}
