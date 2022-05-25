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

sealed class MessagePasswordUiModel {

    data class Set(
        val password: String,
        val hint: String?
    ) : MessagePasswordUiModel()

    object Unset : MessagePasswordUiModel()

    companion object {

        fun from(password: String?, hint: String?): MessagePasswordUiModel =
            if (!password.isNullOrBlank()) Set(password, hint)
            else Unset
    }
}
