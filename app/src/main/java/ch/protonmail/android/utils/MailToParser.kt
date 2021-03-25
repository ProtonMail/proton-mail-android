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

package ch.protonmail.android.utils

import me.proton.core.util.kotlin.EMPTY_STRING
import java.net.URL
import javax.inject.Inject

private const val CC = "cc"
private const val BCC = "bcc"
private const val SUBJECT = "subject"
private const val BODY = "body"

class MailToParser @Inject constructor() {

    fun parseUrl(dataString: String): MailTo {
        val url = URL(dataString)

        require(url.protocol == MAILTO_SCHEME) { "Unsupported $MAILTO_SCHEME format" }

        // Addresses
        val addresses = url.path
            ?.split(",")
            ?.map { it.trim() } ?: emptyList()

        val queryMap = url.query
            ?.split("&")
            ?.map { it.split("=") }
            ?.map { param -> param[0] to param[1] }
            ?.toMap()

        val cc : List<String> = queryMap
            ?.get(CC)
            ?.split(",") ?: emptyList()

        val bcc = queryMap
            ?.get(BCC)
            ?.split(",")?: emptyList()

        val subject = queryMap
            ?.get(SUBJECT) ?: EMPTY_STRING

        val body = queryMap
            ?.get(BODY) ?: EMPTY_STRING

        return MailTo(addresses, cc, subject, body, bcc)
    }

}
