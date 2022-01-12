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

package ch.protonmail.android.settings.presentation

import androidx.lifecycle.SavedStateHandle
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.settings.domain.UpdateSwipeActions
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Test
import kotlin.test.BeforeTest

class SwipeChooserViewModelTest : ArchTest, CoroutinesTest {

    private var accountManager: AccountManager = mockk(relaxed = true)

    private var updateSwipeActions: UpdateSwipeActions = mockk(relaxed = true)

    private var getMailSettings: GetMailSettings = mockk(relaxed = true)

    private var viewModel: SwipeChooserViewModel = mockk(relaxed = true)

    private val savedState = mockk<SavedStateHandle> {
        every { get<SwipeAction>(EXTRA_CURRENT_ACTION) } returns SwipeAction.MarkRead
        every { get<SwipeType>(EXTRA_SWIPE_ID) } returns SwipeType.LEFT
    }

    private val userId = UserId("userId")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel =
            SwipeChooserViewModel(
                savedStateHandle = savedState,
                accountManager = accountManager,
                updateSwipeActions = updateSwipeActions
            )
    }

    @Test
    fun verifyThatWhenWeChangeLeftSwipeActionUpdateSwipeActionsUseCaseIsCalled() = runBlockingTest {

        // given
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(userId)

        // when
        viewModel.onSaveClicked()

        // then
        coVerify { updateSwipeActions(userId, swipeLeft = SwipeAction.MarkRead) }
    }

    @Test
    fun verifyThatWhenWeChangeLeftSwipeActionUpdateSwipeActionsUseCaseIsNotCalledWithRightSwipeActionParameter() =
        runBlockingTest {

            // given
            coEvery { accountManager.getPrimaryUserId() } returns flowOf(userId)

            // when
            viewModel.onSaveClicked()

            // then
            coVerify(exactly = 0) { updateSwipeActions(userId, swipeRight = SwipeAction.MarkRead) }
        }

}
