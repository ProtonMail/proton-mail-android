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

package ch.protonmail.android.contacts.details.presentation.model

sealed class ContactDetailsViewState {
    object Loading : ContactDetailsViewState()
    data class Data(
        val title: String,
        val initials: String,
        val contactDetailsItems: List<ContactDetailsUiItem>,
        val vCardToShare: String,
        val isType2SignatureValid: Boolean?,
        val isType3SignatureValid: Boolean?,
        val photoUrl: String?,
        val photoBytes: List<Byte>?
    ) : ContactDetailsViewState()

    data class Error(val exception: Throwable) : ContactDetailsViewState()
}
