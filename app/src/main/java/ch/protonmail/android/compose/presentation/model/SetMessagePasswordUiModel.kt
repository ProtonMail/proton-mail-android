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

package ch.protonmail.android.compose.presentation.model

import ch.protonmail.android.compose.presentation.ui.SetMessagePasswordActivity
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.any

/**
 * Ui model for [SetMessagePasswordActivity]
 */
data class SetMessagePasswordUiModel(
    val passwordInput: Input,
    val repeatInput: Input,
    val messagePassword: MessagePasswordUiModel,
    val hasErrors: Boolean = any(passwordInput, repeatInput) { it.error != null }
) {

    data class Input(
        val text: String,
        val error: Error?
    )

    sealed class Error {
        object Empty : Error()
        object TooShort : Error()
        object TooLong : Error()
        object DoesNotMatch : Error()
    }

    companion object {

        val Empty = SetMessagePasswordUiModel(
            passwordInput = Input(EMPTY_STRING, Error.Empty),
            repeatInput = Input(EMPTY_STRING, Error.Empty),
            messagePassword = MessagePasswordUiModel.Unset
        )
    }
}
