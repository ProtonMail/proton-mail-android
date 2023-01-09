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

package ch.protonmail.android.navigation.presentation

import android.content.Context
import app.cash.turbine.test
import ch.protonmail.android.core.Constants
import ch.protonmail.android.mailbox.domain.usecase.ObserveMessageCountByLocation
import ch.protonmail.android.mailbox.domain.usecase.ObserveShowMovedEnabled
import ch.protonmail.android.navigation.presentation.model.NavigationViewState
import ch.protonmail.android.navigation.presentation.model.TemporaryMessage
import ch.protonmail.android.testdata.UserTestData
import ch.protonmail.android.usecase.IsAppInDarkMode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.report.presentation.entity.BugReportOutput
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NavigationViewModelTest : ArchTest by ArchTest(),
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val isAppInDarkMode: IsAppInDarkMode = mockk()
    private val observeShowMovedEnabled: ObserveShowMovedEnabled = mockk()
    private val observeMessageCountByLocation: ObserveMessageCountByLocation = mockk()

    private val navigationViewModel = NavigationViewModel(
        isAppInDarkMode,
        observeShowMovedEnabled,
        observeMessageCountByLocation
    )

    @Test
    fun `should check if app is in dark mode`() {
        // given
        val contextMock = mockk<Context>()
        every { isAppInDarkMode(contextMock) } returns true

        // when
        val isAppInDarkMode = navigationViewModel.isAppInDarkMode(contextMock)

        // then
        assertTrue(isAppInDarkMode)
    }

    @Test
    fun `should emit initial, show, and hide message states when report sent`() = runBlockingTest {
        navigationViewModel.viewStateFlow.test {

            // when
            navigationViewModel.onBugReportSent(BugReportOutput.SuccessfullySent(SUCCESS_MESSAGE))

            // then
            assertEquals(NavigationViewState.INITIAL, awaitItem())
            assertEquals(ViewState.SHOW_BUG_REPORT_SENT_MESSAGE, awaitItem())
            assertEquals(ViewState.HIDE_BUG_REPORT_SENT_MESSAGE, awaitItem())
        }
    }

    @Test
    fun `should emit only the initial state when sending report cancelled`() = runBlockingTest {
        navigationViewModel.viewStateFlow.test {

            // when
            navigationViewModel.onBugReportSent(BugReportOutput.Cancelled)

            // then
            assertEquals(NavigationViewState.INITIAL, awaitItem())
        }
    }

    @Test
    fun `should show ALL_SENT and ALL_DRAFT locations instead of SENT and DRAFT `() = runBlockingTest {
        // given
        val contextMock = mockk<Context>()
        every { isAppInDarkMode(contextMock) } returns true
        coEvery { observeShowMovedEnabled.invoke(userId = UserTestData.userId) } returns flowOf(true)
        coEvery {
            observeMessageCountByLocation.invoke(
                userId = UserTestData.userId, Constants.MessageLocationType.ALL_SCHEDULED.asLabelIdString()
            )
        } returns flowOf(0)

        // when
        navigationViewModel.shouldShowDynamicLocation(UserTestData.userId)

        // then
        navigationViewModel.showDynamicItemsOnSideMenuFlow.test {
            assertEquals(SideMenuDynamicLocationData(showMoved = true, showScheduled = false), awaitItem())
        }
    }


    @Test
    fun `should show SCHEDULED location`() = runBlockingTest {
        // given
        val contextMock = mockk<Context>()
        every { isAppInDarkMode(contextMock) } returns true
        coEvery { observeShowMovedEnabled.invoke(userId = UserTestData.userId) } returns flowOf(false)
        coEvery {
            observeMessageCountByLocation.invoke(
                userId = UserTestData.userId, Constants.MessageLocationType.ALL_SCHEDULED.asLabelIdString()
            )
        } returns flowOf(3)

        // when
        navigationViewModel.shouldShowDynamicLocation(UserTestData.userId)

        // then
        navigationViewModel.showDynamicItemsOnSideMenuFlow.test {
            assertEquals(SideMenuDynamicLocationData(showMoved = false, showScheduled = true), awaitItem())
        }
    }

    private companion object TestData {

        const val SUCCESS_MESSAGE = "Thank you for the report."

        object ViewState {

            val SHOW_BUG_REPORT_SENT_MESSAGE = NavigationViewState(
                temporaryMessage = TemporaryMessage(SUCCESS_MESSAGE)
            )
            val HIDE_BUG_REPORT_SENT_MESSAGE = SHOW_BUG_REPORT_SENT_MESSAGE.copy(
                temporaryMessage = TemporaryMessage.NONE
            )
        }
    }
}
