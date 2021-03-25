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
@file:JvmName("MailToUtils") // Name for Java
@file:Suppress("FunctionName")

package ch.protonmail.android.utils

import android.content.Intent
import android.net.Uri
import ch.protonmail.android.utils.extensions.substring

// region constants
/**
 * [String] scheme for mailto
 * We don't use [android.net.MailTo.MAILTO_SCHEME] since is "mailto:" but [Uri.getScheme] does not
 * include the semicolon ':'
 */
const val MAILTO_SCHEME = "mailto"

private const val TO = "to"
private const val CC = "cc"
private const val SUBJECT = "subject"
private const val BODY = "body"
// endregion

/** A replacement to [android.net.MailTo] that is pretty broken */
data class MailTo(
    val addresses: List<String>,
    val cc: List<String>,
    val subject: String,
    val body: String,
    val bcc: List<String> = emptyList()
) {
    /** @return [Array] of [String] of [addresses] - Helper for Java */
    val addressesArray get() = addresses.map {
        it.removePrefix("$MAILTO_SCHEME:")
    }.toTypedArray()

    /** @return [Array] of [String] of [cc] - Helper for Java */
    val ccArray get() = cc.map {
        it.removePrefix("$MAILTO_SCHEME:")
    }.toTypedArray()
}

/**
 * @return [MailTo] from the receiver [Intent]
 * @throws IllegalArgumentException if [Uri.getScheme] is not [MAILTO_SCHEME]
 */
@JvmName("parseIntent")
fun Intent.toMailTo(): MailTo {
    if (scheme != MAILTO_SCHEME) throw IllegalArgumentException("Not a $MAILTO_SCHEME scheme")

    // Get the content Uri without the scheme
    val noScheme = dataString?.substring("$MAILTO_SCHEME:") ?: ""
    // if content is blank, use Intent extras
    return if (noScheme.isBlank()) toMailToFromExtras()
    // else parse the content
    else toMailToFromParse()
}

/** @return [MailTo] created using [Intent] extras */
private fun Intent.toMailToFromExtras(): MailTo {
    return MailTo(
        getStringArrayListExtra(Intent.EXTRA_EMAIL) ?: listOf(),
        getStringArrayListExtra(Intent.EXTRA_CC) ?: listOf(),
        getStringExtra(Intent.EXTRA_SUBJECT) ?: "",
        getStringExtra(Intent.EXTRA_TEXT) ?: ""
    )
}

/** @return [MailTo] created parsing [Intent.getDataString] ( [Uri] ) */
private fun Intent.toMailToFromParse(): MailTo {
    val decodedString = Uri.decode(dataString)

    // Addresses
    val addresses = decodedString.substring(start = ":", end = "?")
        .split(",").map { it.trim() }

    // CC
    val cc = decodedString.substring(start = "$CC=", end = "&")
        .split(",").map { it.trim() }

    // Subject
    val subject = decodedString.substring(start = "$SUBJECT=", end = "&")

    // Body
    val body = decodedString.substring(start = "$BODY=")

    return MailTo(addresses, cc, subject, body)
}
