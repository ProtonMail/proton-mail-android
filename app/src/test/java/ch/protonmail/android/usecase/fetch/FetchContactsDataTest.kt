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

package ch.protonmail.android.usecase.fetch

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import androidx.work.workDataOf
import ch.protonmail.android.worker.FetchContactsDataWorker
import ch.protonmail.android.worker.FetchContactsEmailsWorker
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Rule
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FetchContactsDataTest {

    @get:Rule
    val archRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var fetchContactsDataWorker: FetchContactsDataWorker.Enqueuer

    @MockK
    private lateinit var fetchContactsEmailsWorker: FetchContactsEmailsWorker.Enqueuer

    private lateinit var useCase: FetchContactsData

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = FetchContactsData(fetchContactsDataWorker, fetchContactsEmailsWorker)
    }

    @Test
    fun verifyThatContactsDataAndEmailsAreFetchedAndTrueIsReturned() {
        // given
        val workState = WorkInfo.State.SUCCEEDED
        val outputData = workDataOf("a" to "b")
        val workInfo = WorkInfo(
            UUID.randomUUID(),
            workState,
            outputData,
            emptyList(),
            outputData,
            0
        )
        val workerStatusLiveData = MutableLiveData<WorkInfo>()
        workerStatusLiveData.value = workInfo
        every { fetchContactsDataWorker.enqueue() } returns workerStatusLiveData
        every { fetchContactsEmailsWorker.enqueue(any()) } returns workerStatusLiveData
        val expected = true

        // when
        val response = useCase.invoke()
        response.observeForever { }

        // then
        assertNotNull(response.value)
        assertEquals(expected, response.value)
    }

    @Test
    fun verifyThatContactsDataAndEmailsFetchHasFailedAndFalseIsReturned() {
        // given
        val workState = WorkInfo.State.FAILED
        val outputData = workDataOf("a" to "b")
        val workInfo = WorkInfo(
            UUID.randomUUID(),
            workState,
            outputData,
            emptyList(),
            outputData,
            0
        )
        val workerStatusLiveData = MutableLiveData<WorkInfo>()
        workerStatusLiveData.value = workInfo
        every { fetchContactsDataWorker.enqueue() } returns workerStatusLiveData
        every { fetchContactsEmailsWorker.enqueue(any()) } returns workerStatusLiveData
        val expected = false

        // when
        val response = useCase.invoke()
        response.observeForever { }

        // then
        assertNotNull(response.value)
        assertEquals(expected, response.value)
    }
}
