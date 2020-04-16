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
import androidx.test.platform.app.InstrumentationRegistry
import ch.protonmail.android.adapters.LabelsCirclesAdapter
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.mapper.LabelUiModelMapper
import ch.protonmail.android.testKotlin.CoroutinesTestRule
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.libs.core.utils.EMPTY_STRING
import io.mockk.mockk
import junit.framework.TestCase.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

/**
 * Test suite for [LabelsManagerViewModel]
 * @author Davide Farella
 */
internal class LabelsManagerViewModelTest {

    @get:Rule val archRule = InstantTaskExecutorRule()
    @get:Rule val coroutinesRule = CoroutinesTestRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val messagesDatabase =
            MessagesDatabaseFactory.buildInMemoryDatabase(context).getDatabase()

    private val viewModel by lazy {
        LabelsManagerViewModel(
                jobManager = mockk(),
                messagesDatabase = messagesDatabase,
                type = LabelUiModel.Type.LABELS,
                labelMapper = LabelUiModelMapper(isLabelEditable = false)
        )
    }

    @Test
    fun verify_checked_state_is_updated_correctly_for_adapter_items() {
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
}

