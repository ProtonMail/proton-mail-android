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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.core.Constants
import ch.protonmail.android.mailbox.domain.usecase.ObserveMessageCountByLocation
import ch.protonmail.android.mailbox.domain.usecase.ObserveShowMovedEnabled
import ch.protonmail.android.navigation.presentation.model.NavigationViewAction
import ch.protonmail.android.navigation.presentation.model.NavigationViewState
import ch.protonmail.android.navigation.presentation.model.TemporaryMessage
import ch.protonmail.android.usecase.IsAppInDarkMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import me.proton.core.domain.entity.UserId
import me.proton.core.report.presentation.entity.BugReportOutput
import javax.inject.Inject

@HiltViewModel
internal class NavigationViewModel @Inject constructor(
    private val isAppInDarkMode: IsAppInDarkMode,
    private val observeShowMovedEnabled: ObserveShowMovedEnabled,
    private val observeMessageCountByLocation: ObserveMessageCountByLocation
) : ViewModel() {

    private val viewStateMutableFlow = MutableStateFlow(NavigationViewState.INITIAL)
    val viewStateFlow = viewStateMutableFlow.asStateFlow()

    val showDynamicItemsOnSideMenuFlow = MutableSharedFlow<SideMenuDynamicLocationData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    fun isAppInDarkMode(context: Context) = isAppInDarkMode.invoke(context)

    fun onBugReportSent(bugReportOutput: BugReportOutput) {
        if (bugReportOutput is BugReportOutput.SuccessfullySent) {
            viewStateMutableFlow.value = nextStateFrom(
                currentState = viewStateFlow.value,
                action = NavigationViewAction.ShowTemporaryMessage(
                    TemporaryMessage(bugReportOutput.successMessage)
                )
            )
            viewStateMutableFlow.value = nextStateFrom(
                currentState = viewStateFlow.value,
                action = NavigationViewAction.HideTemporaryMessage
            )
        }
    }

    fun shouldShowDynamicLocation(userId: UserId?) = combine(
        observeShowMovedEnabled.invoke(userId = checkNotNull(userId)),
        observeMessageCountByLocation.invoke(
            userId = userId, Constants.MessageLocationType.ALL_SCHEDULED.asLabelIdString()
        )
    ) { showMoved, numberOfScheduledMessages ->
        showDynamicItemsOnSideMenuFlow.emit(SideMenuDynamicLocationData(showMoved, numberOfScheduledMessages >= 1))
    }.launchIn(viewModelScope)

    private fun nextStateFrom(
        currentState: NavigationViewState,
        action: NavigationViewAction
    ): NavigationViewState = when (action) {
        is NavigationViewAction.ShowTemporaryMessage -> {
            currentState.copy(
                temporaryMessage = action.message
            )
        }
        NavigationViewAction.HideTemporaryMessage -> {
            currentState.copy(
                temporaryMessage = TemporaryMessage.NONE
            )
        }
    }
}

data class SideMenuDynamicLocationData(val showMoved: Boolean, val showScheduled: Boolean)
