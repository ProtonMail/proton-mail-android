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
package ch.protonmail.android.details.presentation

import org.jsoup.Jsoup

internal fun String.parseBodyToPair(): Pair<String, String> {

    val quoteList = arrayOf(
        ".protonmail_quote",
        ".gmail_quote",
        ".yahoo_quoted",
        ".gmail_extra",
        ".zmail_extra", // zoho
        ".moz-cite-prefix",
        "#isForwardContent",
        "#isReplyContent",
        "#mailcontent:not(table)",
        "#origbody",
        "#reply139content",
        "#oriMsgHtmlSeperator",
        "blockquote[type=\"cite\"]",
        "[name=\"quote\"]", // gmx
    )

    val originalMessageHtml = Jsoup.parse(this)
    val messageHtmlWithoutQuote = Jsoup.parse(this)

    for (quoteElement in quoteList) {
        val quotedContentElements = messageHtmlWithoutQuote.select(quoteElement)
        if (!quotedContentElements.isNullOrEmpty()) {
            quotedContentElements.remove()
        }
    }

    return Pair(messageHtmlWithoutQuote.toString(), originalMessageHtml.toString())
}
