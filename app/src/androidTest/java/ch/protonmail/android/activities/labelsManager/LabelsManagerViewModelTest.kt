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
package ch.protonmail.android.activities.labelsManager

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import androidx.work.WorkRequest
import ch.protonmail.android.adapters.LabelsCirclesAdapter
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.libs.core.utils.EMPTY_STRING
import io.mockk.every
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Test suite for [LabelsManagerViewModel]
 * @author Davide Farella
 */
internal class LabelsManagerViewModelTest : CoroutinesTest {

    @get:Rule
    val archRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val messagesDatabase = MessagesDatabaseFactory.buildInMemoryDatabase(context).getDatabase()

    private lateinit var viewModel: LabelsManagerViewModel

    private val savedState = mockk<SavedStateHandle> {
        every { get<Boolean>(EXTRA_MANAGE_FOLDERS) } returns false
    }

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel =
            LabelsManagerViewModel(
                jobManager = mockk(),
                savedStateHandle = savedState,
                messagesDatabase = messagesDatabase,
                deleteLabel = mockk(),
                workManager = workManager
            )
    }

    @Test
    fun verifyCheckedStateIsUpdatedCorrectlyForAdapterItems() {
        val adapter = LabelsCirclesAdapter()
        viewModel.labels.observeDataForever(adapter::submitList)

        // Assert adapter is empty
        assertEquals(0, adapter.itemCount)

        // Add single label
        val label = Label("1", EMPTY_STRING, EMPTY_STRING)
        messagesDatabase.saveLabel(label)
        runBlocking { delay(50) } // Wait for async delivery
        assertEquals(1, adapter.itemCount)

        // Select label
        assertFalse(adapter.currentList!![0]!!.isChecked)
        viewModel.onLabelSelected("1", true)
        runBlocking { delay(50) } // Wait for async delivery
        assertTrue(adapter.currentList!![0]!!.isChecked)

        // Deselect label
        viewModel.onLabelSelected("1", false)
        runBlocking { delay(50) } // Wait for async delivery
        assertFalse(adapter.currentList!![0]!!.isChecked)
    }

    @Test
    fun saveLabelReturnsObservableWorkRequest() {
        val request = viewModel.saveLabel()

        assertTrue(request is WorkRequest)
    }
}

