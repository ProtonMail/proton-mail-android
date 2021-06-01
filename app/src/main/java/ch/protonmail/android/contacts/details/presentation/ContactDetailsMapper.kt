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

package ch.protonmail.android.contacts.details.presentation

import android.graphics.Color
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.contacts.details.domain.model.FetchContactGroupsResult
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsUiItem
import ch.protonmail.android.contacts.details.presentation.model.ContactDetailsViewState
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.VCardUtil
import ezvcard.parameter.VCardParameter
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import javax.inject.Inject

class ContactDetailsMapper @Inject constructor() {

    fun mapToContactViewData(
        fetchResult: FetchContactDetailsResult,
        groupsResult: FetchContactGroupsResult
    ): ContactDetailsViewState.Data {
        val items = mutableListOf<ContactDetailsUiItem>()

        val groups = groupsResult.groupsList.mapIndexed { index, group ->
            ContactDetailsUiItem.Group(
                group.ID,
                group.name,
                Color.parseColor(UiUtil.normalizeColor(group.color)),
                index
            )
        }
        items.addAll(groups)

        val emailItems = fetchResult.emails.map { email ->
            ContactDetailsUiItem.Email(
                value = email.value,
                type = getType(email.types)
            )
        }

        items.addAll(emailItems)

        val telephoneNumbers = fetchResult.telephoneNumbers.map { telephone ->
            ContactDetailsUiItem.TelephoneNumber(
                value = telephone.text,
                type = getType(telephone.types)
            )
        }

        items.addAll(telephoneNumbers)

        val addresses = fetchResult.addresses.map { address ->
            ContactDetailsUiItem.Address(
                type = getType(address.types),
                street = address.streetAddress,
                locality = address.locality,
                region = address.region,
                postalCode = address.postalCode,
                country = address.country
            )
        }
        items.addAll(addresses)

        val organizations = fetchResult.organizations.map { organization ->
            ContactDetailsUiItem.Organization(
                organization.values
            )
        }
        items.addAll(organizations)

        val titles = fetchResult.titles.map { title ->
            ContactDetailsUiItem.Title(
                title.value
            )
        }
        items.addAll(titles)

        val nicknames = fetchResult.nicknames.map { nickname ->
            ContactDetailsUiItem.Nickname(
                nickname.values[0],
                nickname.type
            )
        }
        items.addAll(nicknames)

        val birthdays = fetchResult.birthdays.map { birthday ->
            val dateString = when {
                birthday.date != null -> DateUtil.formatDate(birthday.date)
                birthday.partialDate != null -> birthday.partialDate.toISO8601(false)
                else -> birthday.text
            }

            ContactDetailsUiItem.Birthday(
                dateString
            )
        }
        items.addAll(birthdays)

        val anniversaries = fetchResult.anniversaries.map { anniversary ->
            val dateString = when {
                anniversary.date != null -> DateUtil.formatDate(anniversary.date)
                anniversary.partialDate != null -> anniversary.partialDate.toISO8601(false)
                else -> anniversary.text
            }

            ContactDetailsUiItem.Anniversary(
                dateString
            )
        }
        items.addAll(anniversaries)

        val roles = fetchResult.roles.map { role ->
            ContactDetailsUiItem.Role(
                role.value
            )
        }
        items.addAll(roles)

        val urls = fetchResult.urls.map { url ->
            ContactDetailsUiItem.Url(
                url.value
            )
        }
        items.addAll(urls)

        if (!fetchResult.gender?.text.isNullOrEmpty()) {
            val gender = ContactDetailsUiItem.Gender(
                fetchResult.gender?.text
            )
            items.add(gender)
        }

        Timber.v("Ui Contacts details: $items")

        return ContactDetailsViewState.Data(
            fetchResult.contactName,
            UiUtil.extractInitials(fetchResult.contactName).take(2),
            items,
            fetchResult.vCardToShare,
            fetchResult.isType2SignatureValid,
            fetchResult.isType3SignatureValid,
            fetchResult.photos.firstOrNull()?.url,
            fetchResult.photos.firstOrNull()?.data?.toList()
        )
    }

    private fun getType(parameter: List<VCardParameter>) = if (parameter.isNotEmpty()) {
        VCardUtil.capitalizeType(
            VCardUtil.removeCustomPrefixForCustomType(parameter[0].value)
        )
    } else {
        EMPTY_STRING
    }

}
