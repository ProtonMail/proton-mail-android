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

package ch.protonmail.android.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import ch.protonmail.android.adapters.LabelsAdapter
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.*
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.*
import ch.protonmail.android.views.ThreeStateButton.STATE_UNPRESSED
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ManageLabelsDialogViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var messagesDao: MessagesDao

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var mockObserver: Observer<ViewState>

    @InjectMockKs
    private lateinit var viewModel: ManageLabelsDialogViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel.viewState.observeForever(mockObserver)
    }

    @Test
    fun `show MissingColorError when creating a label without passing a valid color`() {
        viewModel.onDoneClicked(
            true,
            "",
            emptyList(),
            false,
            STATE_UNPRESSED,
            "label-name",
            emptyList()
        )

        verify { mockObserver.onChanged(viewModel.viewState.value as ShowMissingColorError) }
    }

    @Test
    fun `show MissingNameError when creating a label without passing a valid name`() {
        viewModel.onDoneClicked(
            true,
            "some-color",
            emptyList(),
            false,
            STATE_UNPRESSED,
            "",
            emptyList()
        )

        val showMissingNameError = viewModel.viewState.value as ShowMissingNameError
        verifySequence { mockObserver.onChanged(showMissingNameError) }
    }

    @Test
    fun `show LabelNameDuplicatedError when creating a label with a name which is already used`() {
        val labelName = "label-name-already-in-use"
        val labelItem = LabelsAdapter.LabelItem(false)
        labelItem.name = labelName

        viewModel.onDoneClicked(
            true,
            "some-color",
            emptyList(),
            false,
            STATE_UNPRESSED,
            labelName,
            listOf(labelItem)
        )

        val showDuplicatedNameError = viewModel.viewState.value as ShowLabelNameDuplicatedError
        verifySequence { mockObserver.onChanged(showDuplicatedNameError) }
    }

    @Test
    fun `show LabelCreatedEvent when creating a label succeeds`() {
        viewModel.onDoneClicked(
            true,
            "some-color",
            emptyList(),
            false,
            STATE_UNPRESSED,
            "new-label-name",
            emptyList()
        )

        val showLabelCreatedEvent = viewModel.viewState.value as ShowLabelCreatedEvent
        verify { mockObserver.onChanged(showLabelCreatedEvent) }
        assertEquals("new-label-name", showLabelCreatedEvent.labelName)
    }
}
