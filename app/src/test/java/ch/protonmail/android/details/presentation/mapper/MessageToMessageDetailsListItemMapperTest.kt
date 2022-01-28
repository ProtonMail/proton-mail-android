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

package ch.protonmail.android.details.presentation.mapper

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.domain.MessageBodyParser
import ch.protonmail.android.details.domain.model.MessageBodyParts
import ch.protonmail.android.testdata.MessageTestData
import ch.protonmail.android.util.ProtonCalendarUtil
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
class MessageToMessageDetailsListItemMapperTest(
    private val shouldShowDecryptionError: Boolean,
    private val shouldShowLoadEmbeddedImagesButton: Boolean
) {

    private val messageBodyParserMock = mockk<MessageBodyParser> {
        every {
            splitBody(MessageTestData.MESSAGE_BODY)
        } returns TestData.MessageParts.WITH_BODY_AND_QUOTE
    }
    private val protonCalendarUtil: ProtonCalendarUtil = mockk {
        every { hasCalendarAttachment(any()) } returns false
    }
    private val messageToMessageDetailsListItemMapper = MessageToMessageDetailsListItemMapper(
        messageBodyParser = messageBodyParserMock,
        protonCalendarUtil = protonCalendarUtil
    )

    @Test
    fun `should map message to message details list item`() {
        // given
        val mappedMessage = Message(messageId = MessageTestData.MESSAGE_ID_RAW)

        // when
        val mappedModel = messageToMessageDetailsListItemMapper.toMessageDetailsListItem(
            message = mappedMessage,
            messageBody = MessageTestData.MESSAGE_BODY,
            shouldShowDecryptionError = shouldShowDecryptionError,
            shouldShowLoadEmbeddedImagesButton = shouldShowLoadEmbeddedImagesButton
        )

        // then
        with(mappedModel) {
            assertEquals(mappedMessage, message)
            assertEquals(messageFormattedHtml, TestData.MessageParts.BODY)
            assertEquals(messageFormattedHtmlWithQuotedHistory, TestData.MessageParts.BODY_WITH_QUOTE)
            assertEquals(showDecryptionError, shouldShowDecryptionError)
            assertEquals(showLoadEmbeddedImagesButton, shouldShowLoadEmbeddedImagesButton)
        }
    }

    companion object {

        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Array<Any>> {
            return listOf(
                arrayOf(true, true),
                arrayOf(true, false),
                arrayOf(false, true),
                arrayOf(false, false)
            )
        }
    }
}

object TestData {

    object MessageParts {

        const val BODY = "I am the split message body"
        const val BODY_WITH_QUOTE = "I am the split message body with quote"
        val WITH_BODY_AND_QUOTE = MessageBodyParts(BODY, BODY_WITH_QUOTE)
    }
}
