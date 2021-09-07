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

import androidx.annotation.ColorInt
import ch.protonmail.android.labels.domain.model.LabelId

sealed class ContactDetailsUiItem {

    data class Group(
        val id: LabelId,
        val name: String,
        @ColorInt val colorInt: Int,
        val groupIndex: Int
    ) : ContactDetailsUiItem()

    data class Email(
        val value: String,
        val type: String
    ) : ContactDetailsUiItem()

    data class TelephoneNumber(
        val value: String,
        val type: String
    ) : ContactDetailsUiItem()

    data class Address(
        val type: String,
        var street: String?,
        var locality: String?,
        var region: String?,
        var postalCode: String?,
        var country: String?
    ) : ContactDetailsUiItem()

    data class Organization(
        val values: List<String>,
    ) : ContactDetailsUiItem()

    data class Title(
        val value: String
    ) : ContactDetailsUiItem()

    data class Nickname(
        val value: String,
        val type: String?
    ) : ContactDetailsUiItem()

    data class Birthday(
        val birthdayDate: String
    ) : ContactDetailsUiItem()

    data class Anniversary(
        val anniversaryDate: String
    ) : ContactDetailsUiItem()

    data class Role(
        val value: String
    ) : ContactDetailsUiItem()

    data class Url(
        val value: String
    ) : ContactDetailsUiItem()

    data class Gender(
        val value: String?
    ) : ContactDetailsUiItem()

    data class Note(
        val value: String
    ) : ContactDetailsUiItem()

}
