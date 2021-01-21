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
package ch.protonmail.android.api.models.room.notifications

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.NotificationDao
import ch.protonmail.android.data.local.NotificationDatabase
import ch.protonmail.android.testAndroidInstrumented.ReflectivePropertiesMatcher
import ch.protonmail.android.testAndroidInstrumented.matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Created by Kamil Rajtar on 05.09.18.  */
internal class NotificationDaoTest {
    private val context = ApplicationProvider.getApplicationContext<ProtonMailApplication>()
    private val databaseFactory = Room.inMemoryDatabaseBuilder(context,
        NotificationDatabase::class.java).build()
    private val database = databaseFactory.getDao()

    private val notifications = listOf(
        Notification("a", "aa", "aaa").apply { dbId = 3 },
        Notification("b", "bb", "bbb").apply { dbId = 2 },
        Notification("c", "cc", "ccc").apply { dbId = 1 },
        Notification("d", "dd", "ddd").apply { dbId = 8 },
        Notification("e", "ee", "eee").apply { dbId = 7 }
    )

    private fun NotificationDao.populate() {
        notifications.forEach(this::insertNotification)
    }

    @BeforeTest
    fun setUp() {
        database.populate()
    }

    private fun assertDatabaseState(expectedNotifications: List<Notification> = notifications) {
        val expected = expectedNotifications.matchers
        val actual = database.findAllNotifications()
        Assert.assertThat(actual, containsInAnyOrder(expected))
    }

    @Test
    fun findByMessageId() {
        notifications.forEach {
            val expected = it
            val actual = database.findByMessageId(it.messageId)
            Assert.assertThat(actual!!, `is`(ReflectivePropertiesMatcher(expected)))
        }
        assertDatabaseState()
    }

    @Test
    fun findByMessageIdShouldReturnNull() {
        val actual = database.findByMessageId("asfsagsg")
        Assert.assertNull(actual)
        assertDatabaseState()
    }

    @Test
    fun deleteByMessageId() {
        val deletedId = notifications[0].messageId
        val expected = notifications.filterNot { it.messageId == deletedId }
        database.deleteByMessageId(deletedId)
        assertDatabaseState(expectedNotifications = expected)
    }

    @Test
    fun insertNotification() {
        val inserted = Notification("j", "jj", "jjj").apply {}
        database.insertNotification(inserted)
        inserted.dbId = database.findByMessageId("j")!!.dbId
        assertDatabaseState(expectedNotifications = notifications + inserted)
    }

    @Test
    fun clearNotificationCache() {
        database.clearNotificationCache()
        assertDatabaseState(expectedNotifications = emptyList())
    }


    @Test
    fun deleteNotifications() {
        val deleted = listOf(notifications[2], notifications[1])
        val expected = notifications - deleted
        database.deleteNotifications(deleted)
        assertDatabaseState(expectedNotifications = expected)
    }
}
