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

package ch.protonmail.android.details.domain

import ch.protonmail.android.details.domain.model.MessageBodyParts
import org.jsoup.Jsoup
import javax.inject.Inject

class MessageBodyParser @Inject constructor() {

    private val quoteDescriptors = listOf(
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

    fun splitBody(messageBody: String): MessageBodyParts {
        val htmlDocumentWithQuote = Jsoup.parse(messageBody)
        val htmlDocumentWithoutQuote = Jsoup.parse(messageBody)
        var htmlQuote: String? = null

        for (quoteElement in quoteDescriptors) {
            val quotedContentElements = htmlDocumentWithoutQuote.select(quoteElement)
            if (quotedContentElements.isNotEmpty()) {
                htmlQuote = quotedContentElements[0].toString()
                quotedContentElements.remove()
            }
        }

        val htmlWithoutQuote = htmlDocumentWithoutQuote.toString()
        val htmlWithQuote = htmlDocumentWithQuote.toString()

        val messageHasQuote = htmlWithQuote != htmlWithoutQuote
        val messageWithQuote = if (messageHasQuote) {
            htmlWithQuote
        } else {
            null
        }
        return MessageBodyParts(htmlWithoutQuote, messageWithQuote, htmlQuote)
    }

}
