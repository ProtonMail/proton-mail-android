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
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.model.*
import ch.protonmail.android.testAndroidInstrumented.ReflectivePropertiesMatcher
import ch.protonmail.android.testAndroidInstrumented.matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Created by Kamil Rajtar on 05.09.18.  */
internal class CounterDaoTest {

    private val context = ApplicationProvider.getApplicationContext<ProtonMailApplication>()
    private var databaseFactory = Room.inMemoryDatabaseBuilder(context, CounterDatabase::class.java).build()
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

    private val totalLocations = listOf(
        TotalLocationCounter(1, 5),
        TotalLocationCounter(2, 7),
        TotalLocationCounter(3, 2),
        TotalLocationCounter(4, 12)
    )
    private val totalLabels = listOf(
        TotalLabelCounter("a", 5),
        TotalLabelCounter("b", 7),
        TotalLabelCounter("c", 2),
        TotalLabelCounter("d", 12)
    )

    private fun assertDatabaseState(expectedUnreadLocations: Iterable<UnreadLocationCounter> = unreadLocations,
                                    expectedUnreadLabels: Iterable<UnreadLabelCounter> = unreadLabels,
                                    expectedTotalLocations: Iterable<TotalLocationCounter> = totalLocations,
                                    expectedTotalLabels: Iterable<TotalLabelCounter> = totalLabels) {
        val expectedUnreadLocationsMatcher =
            expectedUnreadLocations.map { ReflectivePropertiesMatcher(it) }
        val expectedUnreadLabelsMatcher = expectedUnreadLabels.map { ReflectivePropertiesMatcher(it) }
        val expectedTotalLocationsMatcher =
            expectedTotalLocations.map { ReflectivePropertiesMatcher(it) }
        val expectedTotalLabelsMatcher = expectedTotalLabels.map { ReflectivePropertiesMatcher(it) }

        val actualUnreadLocationsSet = database.findAllUnreadLocations().testValue
        val actualUnreadLabelsSet = database.findAllUnreadLabels().testValue
        val actualTotalLocationsSet = database.findAllTotalLocations().testValue
        val actualTotalLabelsSet = database.findAllTotalLabels().testValue
        Assert.assertThat(actualUnreadLocationsSet,
            containsInAnyOrder(expectedUnreadLocationsMatcher))
        Assert.assertThat(actualUnreadLabelsSet, containsInAnyOrder(expectedUnreadLabelsMatcher))
        Assert.assertThat(actualTotalLocationsSet, containsInAnyOrder(expectedTotalLocationsMatcher))
        Assert.assertThat(actualTotalLabelsSet, containsInAnyOrder(expectedTotalLabelsMatcher))
    }

    private fun CounterDao.populate() {
        insertAllUnreadLocations(unreadLocations)
        insertAllUnreadLabels(unreadLabels)
        refreshTotalCounters(totalLocations, totalLabels)
    }

    @BeforeTest
    fun setUp() {
        database.populate()
    }

    @Test
    fun findAllUnreadLabels() {
        val expected = unreadLabels.matchers
        val actual = database.findAllUnreadLabels().testValue
        Assert.assertThat(actual, containsInAnyOrder(expected))
        assertDatabaseState()
    }

    @Test
    fun findUnreadLabelById() {
        val expected = ReflectivePropertiesMatcher(unreadLabels[1])
        val actual = database.findUnreadLabelById(unreadLabels[1].id)
        Assert.assertThat(actual, `is`(expected))
        assertDatabaseState()
    }

    @Test
    fun findUnreadLabelByIdShouldReturnNull() {
        val actual = database.findUnreadLabelById("e")
        Assert.assertNull(actual)
        assertDatabaseState()
    }

    @Test
    fun clearUnreadLabelsTable() {
        database.clearUnreadLabelsTable()
        assertDatabaseState(expectedUnreadLabels = emptyList())
    }

    @Test
    fun insertUnreadLabel() {
        val inserted = UnreadLabelCounter("e", 8)
        val expected = unreadLabels + inserted
        database.insertUnreadLabel(inserted)
        assertDatabaseState(expectedUnreadLabels = expected)
    }

    @Test
    fun insertAllUnreadLabels() {
        val inserted = listOf(UnreadLabelCounter("e", 8), UnreadLabelCounter("f", 7))
        val expected = unreadLabels + inserted
        database.insertAllUnreadLabels(inserted)
        assertDatabaseState(expectedUnreadLabels = expected)
    }

    @Test
    fun unreadLabelsReplace() {
        val replaced = UnreadLabelCounter("a", 100)
        val expected = unreadLabels.toMutableList()
        expected[0] = replaced
        database.insertUnreadLabel(replaced)
        assertDatabaseState(expectedUnreadLabels = expected)
    }

    @Test
    fun unreadLabelsReplaceAll() {
        val replaced = listOf(UnreadLabelCounter("a", 100), UnreadLabelCounter("b", 101))
        val expected = unreadLabels.toMutableList()
        expected[0] = replaced[0]
        expected[1] = replaced[1]
        database.insertAllUnreadLabels(replaced)
        assertDatabaseState(expectedUnreadLabels = expected)
    }

    @Test
    fun findUnreadLocationById() {
        unreadLabels.forEach {
            val expected = it
            val actual = database.findUnreadLabelById(it.id)
            Assert.assertThat(actual!!, `is`(ReflectivePropertiesMatcher(expected)))
        }
        assertDatabaseState()
    }

    @Test
    fun findUnreadLocationByIdShouldReturnNull() {
        val actual = database.findUnreadLocationById(5)
        Assert.assertNull(actual)
        assertDatabaseState()
    }

    @Test
    fun findAllUnreadLocations() {
        val expected = unreadLocations.map { ReflectivePropertiesMatcher(it) }
        val actual = database.findAllUnreadLocations().testValue
        Assert.assertThat(actual, containsInAnyOrder(expected))
        assertDatabaseState()
    }

    @Test
    fun clearUnreadLocationsTable() {
        database.clearUnreadLocationsTable()
        assertDatabaseState(expectedUnreadLocations = emptyList())
    }

    @Test
    fun insertUnreadLocation() {
        val inserted = UnreadLocationCounter(5, 8)
        val expected = unreadLocations + inserted
        database.insertUnreadLocation(inserted)
        assertDatabaseState(expectedUnreadLocations = expected)
    }

    @Test
    fun insertAllUnreadLocations() {
        val inserted = listOf(UnreadLocationCounter(5, 8), UnreadLocationCounter(6, 7))
        val expected = unreadLocations + inserted
        database.insertAllUnreadLocations(inserted)
        assertDatabaseState(expectedUnreadLocations = expected)
    }

    @Test
    fun unreadLocationsReplace() {
        val replaced = UnreadLocationCounter(1, 100)
        val expected = unreadLocations.toMutableList()
        expected[0] = replaced
        database.insertUnreadLocation(replaced)
        assertDatabaseState(expectedUnreadLocations = expected)
    }

    @Test
    fun unreadLocationsReplaceAll() {
        val replaced = listOf(UnreadLocationCounter(1, 100), UnreadLocationCounter(2, 101))
        val expected = unreadLocations.toMutableList()
        expected[0] = replaced[0]
        expected[1] = replaced[1]
        database.insertAllUnreadLocations(replaced)
        assertDatabaseState(expectedUnreadLocations = expected)
    }

    @Test
    fun updateUnreadCounters() {
        val insertedLabels = listOf(UnreadLabelCounter("e", 8), UnreadLabelCounter("f", 7))
        val insertedLocations = listOf(UnreadLocationCounter(5, 8), UnreadLocationCounter(6, 7))
        database.updateUnreadCounters(insertedLocations, insertedLabels)
        assertDatabaseState(expectedUnreadLocations = insertedLocations,
            expectedUnreadLabels = insertedLabels)
    }

    @Test
    fun findAllTotalLabels() {
        val expected = totalLabels.matchers
        val actual = database.findAllTotalLabels().testValue
        Assert.assertThat(actual, containsInAnyOrder(expected))
        assertDatabaseState()
    }

    @Test
    fun findTotalLabelById() {
        val expected = totalLabels[1]
        val actual = database.findTotalLabelById(totalLabels[1].id)
        Assert.assertThat(actual!!, `is`(ReflectivePropertiesMatcher(expected)))
        assertDatabaseState()
    }

    @Test
    fun clearTotalLabelsTable() {
        database.clearTotalLabelsTable()
        assertDatabaseState(expectedTotalLabels = emptyList())
    }

    @Test
    fun findTotalLocationById() {
        totalLocations.forEach {
            val expected = it
            val actual = database.findTotalLocationById(it.id)
            Assert.assertThat(actual!!, `is`(ReflectivePropertiesMatcher(expected)))
        }
        assertDatabaseState()
    }

    @Test
    fun findTotalLocationByIdShouldReturnNull() {
        val actual = database.findTotalLocationById(5)
        Assert.assertNull(actual)
        assertDatabaseState()
    }

    @Test
    fun findAllTotalLocations() {
        val expected = totalLocations.matchers
        val actual = database.findAllTotalLocations().testValue!!
        Assert.assertThat(actual, containsInAnyOrder(expected))
        assertDatabaseState()
    }

    @Test
    fun clearTotalLocationsTable() {
        database.clearTotalLocationsTable()
        assertDatabaseState(expectedTotalLocations = emptyList())
    }

    @Test
    fun refreshTotalCounters() {
        val insertedLabels = listOf(TotalLabelCounter("e", 8), TotalLabelCounter("f", 7))
        val insertedLocations = listOf(TotalLocationCounter(5, 8), TotalLocationCounter(6, 7))
        database.refreshTotalCounters(insertedLocations, insertedLabels)
        assertDatabaseState(expectedTotalLocations = insertedLocations,
            expectedTotalLabels = insertedLabels)
    }
}
