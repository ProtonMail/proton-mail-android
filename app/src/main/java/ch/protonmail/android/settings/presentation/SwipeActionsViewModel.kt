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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.settings.domain.UpdateSwipeActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.mailsettings.domain.entity.SwipeAction
import javax.inject.Inject

@HiltViewModel
class SwipeActionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountManager: AccountManager,
    private var updateSwipeActions: UpdateSwipeActions,
    private val getMailSettings: GetMailSettings
) : ViewModel() {

    var currentAction: SwipeAction =
        savedStateHandle.get<SwipeAction>(EXTRA_CURRENT_ACTION) ?: SwipeAction.Trash
    val swipeId: SwipeType = savedStateHandle.get<SwipeType>(EXTRA_SWIPE_ID) ?: SwipeType.RIGHT

    suspend fun getMailSettings() = getMailSettings.invoke()

    fun onSaveClicked() {
        viewModelScope.launch {
            accountManager.getPrimaryUserId().first()?.let { userId ->
                if (swipeId == SwipeType.LEFT) {
                    updateSwipeActions.invoke(
                        userId,
                        swipeLeft = currentAction
                    )
                } else {
                    updateSwipeActions.invoke(
                        userId,
                        swipeRight = currentAction
                    )
                }
            }
        }
    }
}
