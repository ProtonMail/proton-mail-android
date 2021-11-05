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

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.MessageDetailsListItem
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.testdata.ConversationTestData
import ch.protonmail.android.testdata.KeyInformationTestData
import ch.protonmail.android.testdata.MessageDetailsListItemTestData
import ch.protonmail.android.utils.ui.TYPE_ITEM
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LoadTheLastNonDraftMessageBodyTest {

    private val messageBodyLoaderMock = mockk<MessageBodyLoader>()
    private val markMessageAsReadIfNeededMock = mockk<MarkMessageAsReadIfNeeded> {
        every { this@mockk(any(), any()) } just runs
    }
    private val loadTheLastNonDraftMessageBody = LoadTheLastNonDraftMessageBody(
        messageBodyLoaderMock,
        markMessageAsReadIfNeededMock
    )

    @Test
    fun `should not load message body nor mark it as read when all messages are drafts`() = runBlockingTest {
        // when
        val resultConversation = loadTheLastNonDraftMessageBody.invoke(
            conversation = ConversationTestData.withDraftsOnly,
            formatMessageHtmlBody = mockk(),
            handleEmbeddedImagesLoading = mockk(),
            publicKeys = KeyInformationTestData.listWithValidKey,
            visibleToTheUser = false
        )

        // then
        assertEquals(resultConversation, ConversationTestData.withDraftsOnly)
        coVerify(exactly = 0) { messageBodyLoaderMock.loadExpandedMessageBody(any(), any(), any(), any()) }
        coVerify(exactly = 0) { markMessageAsReadIfNeededMock(any(), any()) }
    }

    @Test
    fun `should return the same conversation and not mark message as read when loading fails`() = runBlockingTest {
        // given
        coEvery { messageBodyLoaderMock.loadExpandedMessageBody(any(), any(), any(), any()) } returns null

        // when
        val resultConversation = loadTheLastNonDraftMessageBody.invoke(
            conversation = ConversationTestData.withNonDraftsOnly,
            formatMessageHtmlBody = mockk(),
            handleEmbeddedImagesLoading = mockk(),
            publicKeys = KeyInformationTestData.listWithValidKey,
            visibleToTheUser = false
        )

        // then
        assertEquals(resultConversation, ConversationTestData.withNonDraftsOnly)
        coVerify(exactly = 0) { markMessageAsReadIfNeededMock(any(), any()) }
    }

    @Test
    fun `should load body of the last non-draft message and mark it as read if needed`() = runBlockingTest {
        // given
        val loadedMessageIndex = ConversationTestData.withDraftAsLastMessage.messages.size - 2
        val messageToLoad = ConversationTestData.withDraftAsLastMessage.messages[loadedMessageIndex]
        val messageWithLoadedBody = MessageDetailsListItemTestData.withLoadedBodyFrom(messageToLoad)
        givenMessageWithLoadedBodyFor(messageToLoad, messageWithLoadedBody)

        // when
        val resultConversation = loadTheLastNonDraftMessageBody.invoke(
            conversation = ConversationTestData.withDraftAsLastMessage,
            formatMessageHtmlBody = mockk(),
            handleEmbeddedImagesLoading = mockk(),
            publicKeys = KeyInformationTestData.listWithValidKey,
            visibleToTheUser = true
        )

        // then
        assertMessageListWithCorrectMessageLoaded(resultConversation, messageWithLoadedBody, loadedMessageIndex)
        coVerify { markMessageAsReadIfNeededMock(messageToLoad, true) }
    }

    private fun givenMessageWithLoadedBodyFor(message: Message, messageWithLoadedBody: MessageDetailsListItem) {
        coEvery {
            messageBodyLoaderMock.loadExpandedMessageBody(
                eq(message),
                any(),
                any(),
                eq(KeyInformationTestData.listWithValidKey)
            )
        } returns messageWithLoadedBody
    }

    private fun assertMessageListWithCorrectMessageLoaded(
        conversation: ConversationUiModel,
        messageWithLoadedBody: MessageDetailsListItem,
        loadedMessageIndex: Int
    ) {
        assertEquals(ConversationTestData.withDraftAsLastMessage.messages.size, conversation.messageListItems.size)
        assertEquals(messageWithLoadedBody, conversation.messageListItems[loadedMessageIndex])
        conversation.messageListItems.forEachIndexed { index, message ->
            if (index != loadedMessageIndex) {
                assertEquals(TYPE_ITEM, message.itemType)
                assertNull(message.messageFormattedHtml)
                assertNull(message.messageFormattedHtmlWithQuotedHistory)
            }
        }
    }
}
