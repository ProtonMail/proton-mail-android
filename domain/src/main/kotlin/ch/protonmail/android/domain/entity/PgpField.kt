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

/**
 * Represent a field encrypted by PGP
 * Format: `` "-----BEGIN PGP [type] [content]-----END PGP [type]----- ``
 */
sealed class PgpField(val type: String) {
    abstract val content: NotBlankString
    val prefix = "-----BEGIN PGP $type-----"
    val suffix = "-----END PGP $type-----"
    val string get() = "$prefix$content$suffix"

    data class Message(override val content: NotBlankString) : PgpField("MESSAGE")
    data class PublicKey(override val content: NotBlankString) : PgpField("PUBLIC_KEY_BLOCK")
    data class PrivateKey(override val content: NotBlankString) : PgpField("PRIVATE_KEY_BLOCK")
    data class Signature(override val content: NotBlankString) : PgpField("SIGNATURE_KEY_BLOCK")
}
