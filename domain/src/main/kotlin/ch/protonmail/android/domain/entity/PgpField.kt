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
package ch.protonmail.android.domain.entity

import me.proton.core.util.kotlin.substring

/**
 * Represent a field encrypted by PGP
 * Format: `` "-----BEGIN PGP [type]-----[content]-----END PGP [type]----- ``
 */
@Validated
sealed class PgpField(input: NotBlankString, val type: String) {

    val prefix get() = "-----BEGIN PGP $type-----"
    val suffix get() = "-----END PGP $type-----"

    /**
     * @return the content of the key, WITHOUT the [prefix] and [suffix]
     */
    val content by lazy {
        NotBlankString(
            input.s.substring(
                prefix,
                suffix,
                ignoreCase = true,
                ignoreMissingStart = true,
                ignoreMissingEnd = true
            )
        )
    }

    /**
     * @return the full string, INCLUDING [prefix] and [suffix]
     */
    val string get() = "$prefix${content.s}$suffix"

    class Message(input: NotBlankString) : PgpField(input, "MESSAGE")
    class PublicKey(input: NotBlankString) : PgpField(input, "PUBLIC KEY BLOCK")
    class PrivateKey(input: NotBlankString) : PgpField(input, "PRIVATE KEY BLOCK")
    class Signature(input: NotBlankString) : PgpField(input, "SIGNATURE KEY BLOCK")


    override fun equals(other: Any?) =
        other is PgpField && type == other.type && content == other.content

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }
}
