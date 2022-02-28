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

package ch.protonmail.android.details.domain

import ch.protonmail.android.details.domain.model.MessageBodyParts
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class MessageBodyParserTest {

    lateinit var messageBodyParser: MessageBodyParser

    @Before
    fun setUp() {
        messageBodyParser = MessageBodyParser()
    }

    @Test
    fun returnMessageBodyPartsWithJustMessageBodyWhenGivenMessageHasNoQuotedPart() {
        val messageBody = "<html><div>A message without any quoted part</div></html>"

        val actual = messageBodyParser.splitBody(messageBody)

        val jsoupFormattedBody = """
            <html>
             <head></head>
             <body>
              <div>
               A message without any quoted part
              </div>
             </body>
            </html>
        """.trimIndent()
        val expected = MessageBodyParts(jsoupFormattedBody, null, null)
        assertEquals(expected, actual)
    }

    @Test
    fun returnFullMessageBodyPartsWhenGivenMessageHasQuotedPart() {
        // Need to keep this as oneliner to avoid test failures because of Jsoup formatting.
        // The SUT should be changed to avoid relying on Jsoup directly so that we can mock such behavior
        val messageBody =
            "<html><body><div>A reply to a previous message</div><div class=\"moz-cite-prefix\">Some quoted content that was replied to</div></body></html>"

        val actual = messageBodyParser.splitBody(messageBody)

        val jsoupFormattedBody = """
            <html>
             <head></head>
             <body>
              <div>
               A reply to a previous message
              </div>
             </body>
            </html>
        """.trimIndent()
        val jsoupFormattedBodyWithQuote = """
            <html>
             <head></head>
             <body>
              <div>
               A reply to a previous message
              </div>
              <div class="moz-cite-prefix">
               Some quoted content that was replied to
              </div>
             </body>
            </html>
        """.trimIndent()
        val jsoupFormattedQuote = """
              <div class="moz-cite-prefix">
               Some quoted content that was replied to
              </div>
        """.trimIndent()
        val expected = MessageBodyParts(jsoupFormattedBody, jsoupFormattedBodyWithQuote, jsoupFormattedQuote)
        assertEquals(expected, actual)
    }
}
