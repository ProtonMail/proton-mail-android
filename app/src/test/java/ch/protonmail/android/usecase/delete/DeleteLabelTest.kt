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

package ch.protonmail.android.usecase.delete

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import androidx.work.workDataOf
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.worker.DeleteLabelWorker
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DeleteLabelTest {

    @get:Rule
    val archRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var workScheduler: DeleteLabelWorker.Enqueuer

    @MockK
    private lateinit var messagesDatabase: MessagesDatabase

    @MockK
    private lateinit var contactsDatabase: ContactsDatabase

    private lateinit var deleteLabel: DeleteLabel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        deleteLabel = DeleteLabel(TestDispatcherProvider, contactsDatabase, messagesDatabase, workScheduler)
    }

    @Test
    fun verifyThatMessageIsSuccessfullyDeleted() {
        runBlockingTest {
            // given
            val labelId = "Id1"
            val contactLabel = mockk<ContactLabel>()
            val finishState = WorkInfo.State.SUCCEEDED
            val outputData = workDataOf("a" to "b")
            val workInfo = WorkInfo(
                UUID.randomUUID(),
                finishState,
                outputData,
                emptyList(),
                outputData,
                0
            )
            val workerStatusLiveData = MutableLiveData<List<WorkInfo>>()
            workerStatusLiveData.value = listOf(workInfo)
            val expected = true

            every { contactsDatabase.findContactGroupById(labelId) } returns contactLabel
            every { contactsDatabase.deleteContactGroup(contactLabel) } returns Unit
            every { messagesDatabase.deleteLabelById(labelId) } returns Unit
            every { workScheduler.enqueue(any()) } returns mockk(relaxed = true)
            every { workScheduler.getWorkStatusLiveData() } returns workerStatusLiveData

            // when
            val response = deleteLabel(listOf(labelId))
            response.observeForever { }

            // then
            assertNotNull(response.value)
            assertEquals(expected, response.value)
        }
    }

    @Test
    fun verifyThatMessageDeleteHasFailed() {
        runBlockingTest {
            // given
            val labelId = "Id1"
            val contactLabel = mockk<ContactLabel>()
            val finishState = WorkInfo.State.FAILED
            val outputData = workDataOf()
            val workInfo = WorkInfo(
                UUID.randomUUID(),
                finishState,
                outputData,
                emptyList(),
                outputData,
                0
            )
            val workerStatusLiveData = MutableLiveData<List<WorkInfo>>()
            workerStatusLiveData.value = listOf(workInfo)
            val expected = false

            every { contactsDatabase.findContactGroupById(labelId) } returns contactLabel
            every { contactsDatabase.deleteContactGroup(contactLabel) } returns Unit
            every { messagesDatabase.deleteLabelById(labelId) } returns Unit
            every { workScheduler.enqueue(any()) } returns mockk(relaxed = true)
            every { workScheduler.getWorkStatusLiveData() } returns workerStatusLiveData

            // when
            val response = deleteLabel(listOf(labelId))
            response.observeForever { }

            // then
            assertNotNull(response.value)
            assertEquals(expected, response.value)
        }
    }

}
