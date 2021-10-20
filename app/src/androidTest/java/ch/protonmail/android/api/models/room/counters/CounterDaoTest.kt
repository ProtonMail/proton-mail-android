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
package ch.protonmail.android.api.models.room.counters

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.CounterDao
import ch.protonmail.android.data.local.CounterDatabase
import ch.protonmail.android.data.local.model.UnreadLabelCounter
import ch.protonmail.android.data.local.model.UnreadLocationCounter
import ch.protonmail.android.testAndroidInstrumented.ReflectivePropertiesMatcher
import org.hamcrest.Matchers.`is`
import org.junit.Assert
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

internal class CounterDaoTest {

    private val context = ApplicationProvider.getApplicationContext<ProtonMailApplication>()
    private var databaseFactory = CounterDatabase.buildInMemoryDatabase(context)
    private var database = databaseFactory.getDao()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val unreadLocations = listOf(
        UnreadLocationCounter(1, 5),
        UnreadLocationCounter(2, 7),
        UnreadLocationCounter(3, 2),
        UnreadLocationCounter(4, 12)
    )

    private val unreadLabels = listOf(
        UnreadLabelCounter("a", 5),
        UnreadLabelCounter("b", 7),
        UnreadLabelCounter("c", 2),
        UnreadLabelCounter("d", 12)
    )

    private fun CounterDao.populate() {
        insertAllUnreadLocations(unreadLocations)
    }

    @BeforeTest
    fun setUp() {
        database.populate()
    }

    @Test
    fun findUnreadLabelById() {
        val expected = ReflectivePropertiesMatcher(unreadLabels[1])
        val actual = database.findUnreadLabelById(unreadLabels[1].id)
        Assert.assertThat(actual, `is`(expected))
    }

    @Test
    fun findUnreadLabelByIdShouldReturnNull() {
        val actual = database.findUnreadLabelById("e")
        Assert.assertNull(actual)
    }

    @Test
    fun findUnreadLocationById() {
        unreadLabels.forEach {
            val expected = it
            val actual = database.findUnreadLabelById(it.id)
            Assert.assertThat(actual!!, `is`(ReflectivePropertiesMatcher(expected)))
        }
    }

    @Test
    fun findUnreadLocationByIdShouldReturnNull() {
        val actual = database.findUnreadLocationByIdBlocking(5)
        Assert.assertNull(actual)
    }
}
