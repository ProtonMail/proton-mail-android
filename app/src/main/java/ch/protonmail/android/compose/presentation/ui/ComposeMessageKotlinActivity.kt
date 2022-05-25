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

package ch.protonmail.android.compose.presentation.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.activities.BaseContactsActivity
import ch.protonmail.android.compose.ComposeMessageViewModel
import ch.protonmail.android.compose.presentation.model.ComposeMessageEventUiModel.OnExpirationChange
import ch.protonmail.android.compose.presentation.model.ComposeMessageEventUiModel.OnExpirationChangeRequest
import ch.protonmail.android.compose.presentation.model.ComposeMessageEventUiModel.OnPasswordChange
import ch.protonmail.android.compose.presentation.model.ComposeMessageEventUiModel.OnPasswordChangeRequest
import ch.protonmail.android.compose.presentation.model.MessagePasswordUiModel
import ch.protonmail.android.databinding.ActivityComposeMessageBinding
import ch.protonmail.android.ui.view.DaysHoursPair
import ch.protonmail.android.utils.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
abstract class ComposeMessageKotlinActivity : BaseContactsActivity() {

    protected val composeViewModel: ComposeMessageViewModel by viewModels()
    protected lateinit var binding: ActivityComposeMessageBinding

    // region activity results
    // region password
    private val setPasswordLauncher =
        registerForActivityResult(SetMessagePasswordActivity.ResultContract()) {
            composeViewModel.setPassword(it)
        }
    // endregion

    // region expiration
    private val setExpirationLauncher =
        registerForActivityResult(SetMessageExpirationActivity.ResultContract()) {
            composeViewModel.setExpiresAfterInSeconds(it)
        }
    // endregion

    // endregion

    override fun getRootView(): View {
        binding = ActivityComposeMessageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // region setup UI
        binding.composerBottomAppBar.apply {
            onPasswordClick {
                composeViewModel.requestCurrentPasswordForUpdate()
            }
            onExpirationClick {
                composeViewModel.requestCurrentExpirationForUpdate()
            }
            onAttachmentsClick {
                UiUtil.hideKeyboard(this@ComposeMessageKotlinActivity)
                composeViewModel.openAttachmentsScreen()
            }
        }
        // endregion
        // region observe
        composeViewModel.events
            .onEach { event ->
                when (event) {
                    is OnExpirationChange -> binding.composerBottomAppBar.setHasExpiration(event.hasExpiration)
                    is OnExpirationChangeRequest -> openSetExpiration(event.currentExpiration)
                    is OnPasswordChange -> binding.composerBottomAppBar.setHasPassword(event.hasPassword)
                    is OnPasswordChangeRequest -> openSetPassword(event.currentPassword)
                }
            }.launchIn(lifecycleScope)
        // endregion
    }

    // region password
    private fun openSetPassword(currentPassword: MessagePasswordUiModel) {
        setPasswordLauncher.launch(currentPassword)
    }
    // endregion

    // region expiration
    private fun openSetExpiration(currentExpiration: DaysHoursPair) {
        setExpirationLauncher.launch(currentExpiration)
    }
    // endregion
}
