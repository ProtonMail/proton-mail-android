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

package ch.protonmail.android.activities.messageDetails

import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.body.MessageBodyDecryptor
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.domain.MessageBodyParser
import ch.protonmail.android.details.domain.model.MessageBodyParts
import ch.protonmail.android.details.presentation.MessageDetailsListItem
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.testdata.KeyInformationTestData
import ch.protonmail.android.testdata.MessageTestData
import ch.protonmail.android.testdata.UserIdTestData
import ch.protonmail.android.utils.css.MessageBodyCssProvider
import ch.protonmail.android.utils.resources.StringResourceResolver
import ch.protonmail.android.utils.ui.screen.RenderDimensionsProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MessageBodyLoaderTest {

    private val userManagerMock = mockk<UserManager> {
        every { requireCurrentUserId() } returns UserIdTestData.userId
    }
    private val messageRepositoryMock = mockk<MessageRepository>()
    private val renderDimensionsProviderMock = mockk<RenderDimensionsProvider> {
        every { getRenderWidth() } returns TestData.RENDER_WIDTH
    }
    private val messageBodyCssProviderMock = mockk<MessageBodyCssProvider> {
        every { getMessageBodyCss() } returns TestData.MESSAGE_BODY_CSS
        every { getMessageBodyDarkModeCss() } returns TestData.MESSAGE_BODY_DARK_MODE_CSS
    }
    private val getStringResourceMock = mockk<StringResourceResolver> {
        every { this@mockk.invoke(R.string.request_timeout) } returns TestData.DEFAULT_ERROR_MESSAGE
    }
    private val messageBodyParserMock = mockk<MessageBodyParser> {
        every { splitBody(MessageTestData.MESSAGE_BODY_FORMATTED) } returns TestData.MessageParts.withBodyAndQuote
    }
    private val decryptMessageBodyMock = mockk<MessageBodyDecryptor>()
    private val messageBodyLoader = MessageBodyLoader(
        userManagerMock,
        messageRepositoryMock,
        renderDimensionsProviderMock,
        messageBodyCssProviderMock,
        getStringResourceMock,
        messageBodyParserMock,
        decryptMessageBodyMock
    )

    @Test(expected = IllegalArgumentException::class)
    fun `should fail if id of the expanded message is null`() = runBlockingTest {
        // when
        messageBodyLoader.loadExpandedMessageBody(
            expandedMessage = Message(messageId = null),
            formatMessageHtmlBody = mockk(),
            handleEmbeddedImagesLoading = mockk(),
            publicKeys = KeyInformationTestData.listWithValidKey
        )
    }

    @Test
    fun `should return null and not attempt loading if the expanded message fetching fails`() = runBlockingTest {
        // given
        givenFetchingMessageDetailsReturns(null)

        // when
        val messageWithLoadedBody = messageBodyLoader.loadExpandedMessageBody(
            expandedMessage = Message(messageId = MessageTestData.MESSAGE_ID_RAW),
            formatMessageHtmlBody = mockk(),
            handleEmbeddedImagesLoading = mockk(),
            publicKeys = KeyInformationTestData.listWithValidKey
        )

        // then
        assertNull(messageWithLoadedBody)
        verify(exactly = 0) { decryptMessageBodyMock(any(), any()) }
        verify(exactly = 0) { messageBodyParserMock.splitBody(any()) }
    }

    @Test
    fun `should return a message with body, show image button, and no error when loaded`() = runBlockingTest {
        // given
        val fetchedMessage = Message(
            messageId = MessageTestData.MESSAGE_ID_RAW,
            messageBody = MessageTestData.MESSAGE_BODY
        )
        givenFetchingMessageDetailsReturns(fetchedMessage)
        givenDecryptingSucceedsFor(fetchedMessage)

        // when
        val messageWithLoadedBody = messageBodyLoader.loadExpandedMessageBody(
            expandedMessage = Message(messageId = MessageTestData.MESSAGE_ID_RAW),
            formatMessageHtmlBody = formatMessageBodyFor(fetchedMessage),
            handleEmbeddedImagesLoading = handleEmbeddedImageLoadingFor(fetchedMessage, showButton = true),
            publicKeys = KeyInformationTestData.listWithValidKey
        )

        // then
        coVerify { decryptMessageBodyMock(fetchedMessage, KeyInformationTestData.listWithValidKey) }
        messageWithLoadedBody.assertLoadedBodyProperties(
            shouldShowEmbeddedImagesButton = true,
            shouldShowDecryptionError = false
        )
    }

    @Test
    fun `should return a message with body, not show image button, and show error when loaded`() = runBlockingTest {
        // given
        val fetchedMessage = Message(
            messageId = MessageTestData.MESSAGE_ID_RAW,
            messageBody = MessageTestData.MESSAGE_BODY
        )
        givenFetchingMessageDetailsReturns(fetchedMessage)
        givenDecryptingFailsFor(fetchedMessage)

        // when
        val messageWithLoadedBody = messageBodyLoader.loadExpandedMessageBody(
            expandedMessage = Message(messageId = MessageTestData.MESSAGE_ID_RAW),
            formatMessageHtmlBody = formatMessageBodyFor(fetchedMessage),
            handleEmbeddedImagesLoading = handleEmbeddedImageLoadingFor(fetchedMessage, showButton = false),
            publicKeys = KeyInformationTestData.listWithValidKey
        )

        // then
        coVerify { decryptMessageBodyMock(fetchedMessage, KeyInformationTestData.listWithValidKey) }
        messageWithLoadedBody.assertLoadedBodyProperties(
            shouldShowEmbeddedImagesButton = false,
            shouldShowDecryptionError = true
        )
    }

    private fun givenFetchingMessageDetailsReturns(message: Message?) {
        coEvery {
            messageRepositoryMock.getMessage(
                userId = UserIdTestData.userId,
                messageId = MessageTestData.MESSAGE_ID_RAW,
                shouldFetchMessageDetails = true
            )
        } returns message
    }

    private fun givenDecryptingSucceedsFor(message: Message) {
        every { decryptMessageBodyMock(message, KeyInformationTestData.listWithValidKey) } returns true
    }

    private fun givenDecryptingFailsFor(message: Message) {
        every { decryptMessageBodyMock(message, KeyInformationTestData.listWithValidKey) } returns false
    }

    private fun formatMessageBodyFor(
        message: Message
    ): (Message, Int, String, String, String) -> String {
        val formatMessageBodyMock: (Message, Int, String, String, String) -> String = mockk()
        every {
            formatMessageBodyMock(
                message,
                TestData.RENDER_WIDTH,
                TestData.MESSAGE_BODY_CSS,
                TestData.MESSAGE_BODY_DARK_MODE_CSS,
                TestData.DEFAULT_ERROR_MESSAGE
            )
        } returns MessageTestData.MESSAGE_BODY_FORMATTED
        return formatMessageBodyMock
    }

    private fun handleEmbeddedImageLoadingFor(message: Message, showButton: Boolean) =
        mockk<(Message) -> Boolean> { every { this@mockk(message) } returns showButton }

    private fun MessageDetailsListItem?.assertLoadedBodyProperties(
        shouldShowEmbeddedImagesButton: Boolean,
        shouldShowDecryptionError: Boolean
    ) {
        assertEquals(MessageTestData.MESSAGE_ID_RAW, this?.message?.messageId)
        assertEquals(TestData.MessageParts.BODY, this?.messageFormattedHtml)
        assertEquals(TestData.MessageParts.BODY_WITH_QUOTE, this?.messageFormattedHtmlWithQuotedHistory)
        assertEquals(shouldShowEmbeddedImagesButton, this?.showLoadEmbeddedImagesButton)
        assertEquals(shouldShowDecryptionError, this?.showDecryptionError)
    }
}

private object TestData {
    const val RENDER_WIDTH = 42
    const val MESSAGE_BODY_CSS = "I am css"
    const val MESSAGE_BODY_DARK_MODE_CSS = "And I am a dark css"
    const val DEFAULT_ERROR_MESSAGE = "nope"

    object MessageParts {
        const val BODY = "I am the split message body"
        const val BODY_WITH_QUOTE = "I am the split message body with quote"
        val withBodyAndQuote = MessageBodyParts(BODY, BODY_WITH_QUOTE)
    }
}
