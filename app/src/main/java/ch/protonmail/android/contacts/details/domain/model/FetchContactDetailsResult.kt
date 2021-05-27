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

package ch.protonmail.android.contacts.details.domain.model

import ezvcard.property.Address
import ezvcard.property.Anniversary
import ezvcard.property.Birthday
import ezvcard.property.Email
import ezvcard.property.Gender
import ezvcard.property.Nickname
import ezvcard.property.Organization
import ezvcard.property.Photo
import ezvcard.property.Role
import ezvcard.property.Telephone
import ezvcard.property.Title
import ezvcard.property.Url

sealed class FetchContactDetailsResult {
    object Loading : FetchContactDetailsResult()
    data class Data(
        val emails: List<Email>,
        val telephoneNumbers: List<Telephone>,
        val addresses: List<Address>,
        val photos: List<Photo>,
        val organizations: List<Organization>,
        val titles: List<Title>,
        val nicknames: List<Nickname>,
        val birthdays: List<Birthday>,
        val anniversaries: List<Anniversary>,
        val roles: List<Role>,
        val urls: List<Url>,
        val gender: Gender?,
        val isType2SignatureValid: Boolean?,
        val isType3SignatureValid: Boolean?
    ) : FetchContactDetailsResult()

    data class Error(val exception: Throwable) : FetchContactDetailsResult()
}
