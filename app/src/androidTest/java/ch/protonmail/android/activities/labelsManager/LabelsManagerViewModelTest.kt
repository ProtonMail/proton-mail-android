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
import androidx.lifecycle.LiveData
import androidx.lifecycle.SavedStateHandle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkInfo
import androidx.work.WorkManager
import ch.protonmail.android.adapters.LabelsAdapter
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.labels.data.model.LabelType.MESSAGE_LABEL
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test suite for [LabelsManagerViewModel]
 * @author Davide Farella
 */
internal class LabelsManagerViewModelTest : CoroutinesTest {

    @get:Rule
    val archRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @MockK
    private lateinit var accountManager: AccountManager

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @MockK
    private lateinit var labelRepository: LabelRepository

    private lateinit var viewModel: LabelsManagerViewModel

    private val savedState = mockk<SavedStateHandle> {
        every { get<Boolean>(EXTRA_MANAGE_FOLDERS) } returns false
    }
    val userId = UserId("testUser")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(userId)
        every { labelRepository.findAllLabelsPaged(userId) } returns mockk()
        viewModel =
            LabelsManagerViewModel(
                labelRepository = labelRepository,
                savedStateHandle = savedState,
                deleteLabel = mockk(),
                workManager = workManager,
                accountManager = accountManager
            )
    }

    @Test
    fun verifyCheckedStateIsUpdatedCorrectlyForAdapterItems() {

        runBlocking {
            val adapter = LabelsAdapter()
            viewModel.labels.observeDataForever(adapter::submitList)

            // Assert adapter is empty
            assertEquals(0, adapter.itemCount)

            // Add single label
            val label = LabelEntity(
                LabelId("1"), userId, EMPTY_STRING, EMPTY_STRING, 0, MESSAGE_LABEL, EMPTY_STRING, EMPTY_STRING, 0, 0, 0
            )
            labelRepository.saveLabel(label)
            delay(50) // Wait for async delivery
            assertEquals(1, adapter.itemCount)

            // Select label
            assertFalse(adapter.currentList!![0]!!.isChecked)
            viewModel.onLabelSelected("1", true)
            delay(50) // Wait for async delivery
            assertTrue(adapter.currentList!![0]!!.isChecked)

            // Deselect label
            viewModel.onLabelSelected("1", false)
            delay(50)  // Wait for async delivery
            assertFalse(adapter.currentList!![0]!!.isChecked)
        }
    }

    @Test
    fun saveLabelReturnsWorkInfoLiveData() {
        val request = viewModel.saveLabel()

        assertTrue(request is LiveData<WorkInfo>)
    }
}
