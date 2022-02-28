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

package ch.protonmail.android.compose.presentation.viewmodel

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.PRIVATE
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.compose.presentation.model.MessagePasswordUiModel
import ch.protonmail.android.compose.presentation.model.SetMessagePasswordUiModel
import ch.protonmail.android.compose.presentation.model.SetMessagePasswordUiModel.Error
import ch.protonmail.android.compose.presentation.model.SetMessagePasswordUiModel.Input
import ch.protonmail.android.compose.presentation.ui.SetMessagePasswordActivity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

@VisibleForTesting(otherwise = PRIVATE)
const val MESSAGE_PASSWORD_MIN_LENGTH = 4

@VisibleForTesting(otherwise = PRIVATE)
const val MESSAGE_PASSWORD_MAX_LENGTH = 21

/**
 * ViewModel for [SetMessagePasswordActivity]
 */
@HiltViewModel
class SetMessagePasswordViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _result: MutableStateFlow<SetMessagePasswordUiModel> =
        MutableStateFlow(SetMessagePasswordUiModel.Empty)

    val result: StateFlow<SetMessagePasswordUiModel> =
        _result.asStateFlow()

    fun validate(password: CharSequence?, repeat: CharSequence?, hint: CharSequence?) {
        viewModelScope.launch(dispatchers.Comp) {

            val passwordInput = validatePassword(password)
            val repeatInput = validateRepeatPassword(password, repeat)
            val hasErrors = passwordInput.error ?: repeatInput.error != null
            val passwordUiModel = if (hasErrors) {
                MessagePasswordUiModel.Unset
            } else {
                MessagePasswordUiModel.Set(
                    checkNotNull(password).toString(),
                    hint.toString()
                )
            }
            _result.emit(SetMessagePasswordUiModel(passwordInput, repeatInput, passwordUiModel))
        }
    }

    private fun validatePassword(password: CharSequence?): Input {
        val error = when {
            password.isNullOrBlank() -> Error.Empty
            password.length < MESSAGE_PASSWORD_MIN_LENGTH -> Error.TooShort
            password.length > MESSAGE_PASSWORD_MAX_LENGTH -> Error.TooLong
            else -> null
        }
        return Input(password?.toString() ?: EMPTY_STRING, error)
    }

    private fun validateRepeatPassword(
        password: CharSequence?,
        repeat: CharSequence?
    ): Input {
        val error = when {
            repeat.isNullOrBlank() -> Error.Empty
            repeat.toString() != password.toString() -> Error.DoesNotMatch
            else -> null
        }
        return Input(repeat?.toString() ?: EMPTY_STRING, error)
    }
}
