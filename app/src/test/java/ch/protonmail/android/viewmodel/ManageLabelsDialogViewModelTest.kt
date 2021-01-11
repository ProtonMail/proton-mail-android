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
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.HideLabelsView
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.SelectedLabelsArchiveEvent
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.SelectedLabelsChangedEvent
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowApplicableLabelsThresholdExceededError
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowLabelCreatedEvent
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowLabelNameDuplicatedError
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowMissingColorError
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowMissingNameError
import ch.protonmail.android.views.ThreeStateButton.Companion.STATE_CHECKED
import ch.protonmail.android.views.ThreeStateButton.Companion.STATE_UNPRESSED
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import org.junit.Assert.assertEquals
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
            STATE_UNPRESSED,
            "label-name",
            emptyList()
        )

        val showMissingColorError = viewModel.viewState.value as ShowMissingColorError
        verifySequence { mockObserver.onChanged(showMissingColorError) }
    }

    @Test
    fun `show MissingNameError when creating a label without passing a valid name`() {
        viewModel.onDoneClicked(
            true,
            "some-color",
            emptyList(),
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
            STATE_UNPRESSED,
            "new-label-name",
            emptyList()
        )

        val showLabelCreatedEvent = viewModel.viewState.value as ShowLabelCreatedEvent
        verify { mockObserver.onChanged(showLabelCreatedEvent) }
        assertEquals("new-label-name", showLabelCreatedEvent.labelName)
    }

    @Test
    fun `show ShowApplicableLabelsThreasholdExceededError when applying a number of labels that exceeds the allowance for the user`() {
        val checkedLabelIds = emptyList<String>()
        every { userManager.getMaxLabelsAllowed() } returns 3
        every { userManager.didReachLabelsThreshold(checkedLabelIds.size) } returns true

        viewModel.onDoneClicked(
            false,
            "",
            checkedLabelIds,
            STATE_UNPRESSED,
            "",
            emptyList()
        )

        val labelsThresholdError = viewModel.viewState.value as ShowApplicableLabelsThresholdExceededError
        verifySequence { mockObserver.onChanged(labelsThresholdError) }
        assertEquals(3, labelsThresholdError.maxLabelsAllowed)
    }

    @Test
    fun `dispatch SelectedLabelsChanged when labels selection changes`() {
        val checkedLabelIds = emptyList<String>()
        val firedStates = slot<ViewState>()
        every { userManager.didReachLabelsThreshold(checkedLabelIds.size) } returns false
        every { mockObserver.onChanged(capture(firedStates)) } answers { println(firedStates) }

        viewModel.onDoneClicked(
            false,
            "",
            checkedLabelIds,
            STATE_UNPRESSED,
            "",
            emptyList()
        )

        verify(exactly = 2) { mockObserver.onChanged(or(SelectedLabelsChangedEvent, HideLabelsView)) }
    }

    @Test
    fun `dispatch SelectedLabelsChangedArchive when labels selection changes and archive option selected`() {
        val checkedLabelIds = emptyList<String>()
        val firedStates = slot<ViewState>()
        every { userManager.didReachLabelsThreshold(checkedLabelIds.size) } returns false
        every { mockObserver.onChanged(capture(firedStates)) } answers { println(firedStates) }

        viewModel.onDoneClicked(
            false,
            "",
            checkedLabelIds,
            STATE_CHECKED,
            "",
            emptyList()
        )

        verify(exactly = 2) { mockObserver.onChanged(or(SelectedLabelsArchiveEvent, HideLabelsView)) }
    }

    @Test
    fun `onDoneClicked dismisses label manager view when labels changes were applied successfully`() {
        val checkedLabelIds = emptyList<String>()

        viewModel.onDoneClicked(
            false,
            "",
            checkedLabelIds,
            STATE_UNPRESSED,
            "",
            emptyList()
        )

        val hideLabelsView = viewModel.viewState.value as HideLabelsView
        verify { mockObserver.onChanged(hideLabelsView) }
    }

    @Test
    fun `onTextChanged fires ShowLabelCreationViews when labelName is not empty`() {
        viewModel.onTextChanged("label-being-typed", false)

        val showLabelCreationViews = viewModel.viewState.value as ViewState.ShowLabelCreationViews
        verifySequence { mockObserver.onChanged(showLabelCreationViews) }
    }

    @Test
    fun `onTextChanged does not fire ShowLabelCreationViews when label name not empty and views already visible`() {
        viewModel.onTextChanged("label-being-typed", true)

        verify(exactly = 0) { mockObserver.onChanged(any()) }
    }

    @Test
    fun `onTextChanged fires HideLabelCreationViews when labelName is empty`() {
        viewModel.onTextChanged("", false)

        val hideLabelCreationViews = viewModel.viewState.value as ViewState.HideLabelCreationViews
        verifySequence { mockObserver.onChanged(hideLabelCreationViews) }
    }
}
