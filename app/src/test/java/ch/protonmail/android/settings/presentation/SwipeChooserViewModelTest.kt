/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.settings.presentation

import androidx.lifecycle.SavedStateHandle
import ch.protonmail.android.settings.domain.usecase.UpdateSwipeActions
import ch.protonmail.android.settings.swipe.EXTRA_SWIPE_ID
import ch.protonmail.android.settings.swipe.SwipeType
import ch.protonmail.android.settings.swipe.viewmodel.SwipeChooserViewModel
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test

private val TEST_USER_ID = UserId("userId")

class SwipeChooserViewModelTest : ArchTest by ArchTest(),
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val savedStateHandle: SavedStateHandle = mockk()

    private var accountManager: AccountManager = mockk {
        every { getPrimaryUserId() } returns flowOf(TEST_USER_ID)
    }

    private var updateSwipeActions: UpdateSwipeActions = mockk(relaxUnitFun = true)

    private val viewModel by lazy {
        SwipeChooserViewModel(
            savedStateHandle = savedStateHandle,
            accountManager = accountManager,
            updateSwipeActions = updateSwipeActions
        )
    }

    @Test
    fun `invokes use case with correct parameters when updating Swipe Left`() = coroutinesTest {

        // given
        val action = SwipeAction.Archive
        every { savedStateHandle.get<SwipeType>(EXTRA_SWIPE_ID) } returns SwipeType.LEFT
        viewModel.setAction(action)

        // when
        viewModel.onSaveClicked()

        // then
        coVerify { updateSwipeActions(TEST_USER_ID, swipeLeft = action) }
        coVerify(exactly = 0) { updateSwipeActions(TEST_USER_ID, swipeRight = action) }
    }

    @Test
    fun `invokes use case with correct parameters when updating Swipe Right`() =
        coroutinesTest {

            // given
            val action = SwipeAction.Spam
            every { savedStateHandle.get<SwipeType>(EXTRA_SWIPE_ID) } returns SwipeType.RIGHT
            viewModel.setAction(action)

            // when
            viewModel.onSaveClicked()

            // then
            coVerify { updateSwipeActions(TEST_USER_ID, swipeRight = action) }
            coVerify(exactly = 0) { updateSwipeActions(TEST_USER_ID, swipeLeft = action) }
        }

}
