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
package ch.protonmail.android.api.models.room.pendingActions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import ch.protonmail.android.api.models.room.testValue
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.testAndroidInstrumented.ReflectivePropertiesMatcher
import ch.protonmail.android.testAndroidInstrumented.matchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.containsInAnyOrder
import org.junit.Assert
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Created by Kamil Rajtar on 06.09.18.  */
internal class PendingActionsDatabaseTest {

    private val context = ApplicationProvider.getApplicationContext<ProtonMailApplication>()
    private var databaseFactory = Room.inMemoryDatabaseBuilder(context, PendingActionsDatabaseFactory::class.java).build()
    private var database = databaseFactory.getDatabase()

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val pendingSends = listOf(
        PendingSend("a", "aa", "aaa", false, 5),
        PendingSend("b", "bb", "bbb", true, 7),
        PendingSend("c", "cc", "ccc", false, 3),
        PendingSend("d", "dd", "ddd", true, 2),
        PendingSend("e", "ee", "eee", false, 1),
        PendingSend("f", "ff", "fff", true, 9)
    )
    private val pendingUploads = listOf(
        PendingUpload("a"),
        PendingUpload("b"),
        PendingUpload("c"),
        PendingUpload("d"),
        PendingUpload("e")
    )

    private fun PendingActionsDatabase.populate() {
        pendingSends.forEach(this::insertPendingForSend)
        pendingUploads.forEach(this::insertPendingForUpload)
    }

    @BeforeTest
    fun setUp() {
        database.populate()
    }

    private fun assertDatabaseState(expectedSends: Iterable<PendingSend> = pendingSends,
                                    expectedUploads: Iterable<PendingUpload> = pendingUploads) {
        val expectedSendsMatcher = expectedSends.map { ReflectivePropertiesMatcher(it) }
        val expectedUploadsMatcher = expectedUploads.map { ReflectivePropertiesMatcher(it) }

        val actualSendsSet = database.findAllPendingSendsAsync().testValue
        val actualUploadsSet = database.findAllPendingUploadsAsync().testValue
        Assert.assertThat(actualSendsSet, containsInAnyOrder(expectedSendsMatcher))
        Assert.assertThat(actualUploadsSet, containsInAnyOrder(expectedUploadsMatcher))
    }

    @Test
    fun insertPendingForSend() {
        val inserted = PendingSend("z", "zz", "zzz", true, 101)
        val expected = pendingSends + inserted
        database.insertPendingForSend(inserted)
        assertDatabaseState(expectedSends = expected)
    }

    @Test
    fun findPendingSendByMessageId() {
        val expected = pendingSends[3]
        val actual = database.findPendingSendByMessageId(expected.messageId!!)
        Assert.assertThat(actual!!, `is`(ReflectivePropertiesMatcher(expected)))
        assertDatabaseState()
    }

    @Test
    fun deletePendingSendByMessageId() {
        val deleted = pendingSends[3]
        val expected = pendingSends.filterNot { it.messageId == deleted.messageId }
        database.deletePendingSendByMessageId(deleted.messageId!!)
        assertDatabaseState(expectedSends = expected)
    }

    @Test
    fun findPendingSendByOfflineMessageId() {
        val expected = pendingSends[3]
        val actual = database.findPendingSendByOfflineMessageId(expected.offlineMessageId!!)
        Assert.assertThat(actual!!, `is`(ReflectivePropertiesMatcher(expected)))
        assertDatabaseState()
    }

    @Test
    fun findPendingSendByOfflineMessageIdAsync() {
        val expected = pendingSends[3]
        val actual = database.findPendingSendByOfflineMessageIdAsync(expected.offlineMessageId!!).testValue
        Assert.assertThat(actual!!, `is`(ReflectivePropertiesMatcher(expected)))
        assertDatabaseState()
    }

    @Test
    fun findPendingSendByDbId() {
        val expected = pendingSends[3]
        val actual = database.findPendingSendByDbId(expected.localDatabaseId)
        Assert.assertThat(actual!!, `is`(ReflectivePropertiesMatcher(expected)))
        assertDatabaseState()
    }

    @Test
    fun clearPendingSendCache() {
        database.clearPendingSendCache()
        assertDatabaseState(expectedSends = emptyList())
    }

    @Test
    fun insertPendingForUpload() {
        val inserted = PendingUpload("z")
        val expected = pendingUploads + inserted
        database.insertPendingForUpload(inserted)
        assertDatabaseState(expectedUploads = expected)
    }

    @Test
    fun findAllPendingUploadsAsync() {
        val expected = pendingUploads.matchers
        val actual = database.findAllPendingUploadsAsync().testValue!!
        Assert.assertThat(actual, containsInAnyOrder(expected))
        assertDatabaseState()
    }

    @Test
    fun findPendingUploadByMessageId() {
        val expected = pendingUploads[2]
        val actual = database.findPendingUploadByMessageId(expected.messageId)
        Assert.assertEquals(expected, actual)
        assertDatabaseState()
    }

    @Test
    fun deletePendingUploadByMessageId() {
        val deletedId = pendingUploads[2].messageId
        val expected = pendingUploads.filterNot { it.messageId == deletedId }
        database.deletePendingUploadByMessageId(deletedId)
        assertDatabaseState(expectedUploads = expected)
    }

    @Test
    fun clearPendingUploadCache() {
        database.clearPendingUploadCache()
        assertDatabaseState(expectedUploads = emptyList())
    }
}
