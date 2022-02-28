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

package ch.protonmail.android.contacts.domain.usecase

import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Name
import java.util.Locale
import javax.inject.Inject

/**
 * Extract initials ( 2 characters ) for a given User or Contact
 * E.g. "Proton User" -> "PU"
 */
class ExtractInitials @Inject constructor() {

    operator fun invoke(displayName: Name, emailAddress: EmailAddress): String =
        getInitials(displayName.s, Type.NAME)
            ?: getInitials(emailAddress.s, Type.EMAIL)
            // This is not a possible scenario, as an EmailAddress will contains at least 2 letters or digits
            ?: throw AssertionError()

    private fun getInitials(displayNameOrEmailAddress: String, type: Type): String? {
        val string = displayNameOrEmailAddress.toUpperCase(Locale.getDefault())
        return string
            // split words, ignoring everything not letter or digits
            .split(type.splitRegex)
            // take first char of each word
            .mapNotNull { it.firstOrNull() }
            // Valid only if we have 2 or more chars
            .takeIf { it.size >= 2 }?.let { "${it[0]}${it[1]}" }
            // Otherwise take only first 2 letters or digits of the name
            ?: string
                .filter { it.isLetterOrDigit() }
                .takeIf { it.length >= 2 }
                ?.let { "${it[0]}${it[1]}" }
    }

    private enum class Type(val splitRegex: Regex) {
        NAME("[^a-zA-Z0-9]".toRegex()),
        EMAIL("[^a-zA-Z0-9@.]".toRegex())
    }
}
