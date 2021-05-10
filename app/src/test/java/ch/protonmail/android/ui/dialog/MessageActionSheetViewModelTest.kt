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

package ch.protonmail.android.ui.dialog

import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.labels.presentation.ui.ManageLabelsActionSheet
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.usecase.delete.DeleteMessage
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageActionSheetViewModelTest : ArchTest, CoroutinesTest {

    @MockK
    private lateinit var deleteMessage: DeleteMessage

    @MockK
    private lateinit var moveMessagesToFolder: MoveMessagesToFolder

    @MockK
    private lateinit var repository: MessageRepository
    private lateinit var viewModel: MessageActionSheetViewModel

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel = MessageActionSheetViewModel(
            deleteMessage,
            moveMessagesToFolder,
            repository
        )
    }

    @Test
    fun verifyShowLabelsManagerActionIsExecutedForLabels() = runBlockingTest {

        // given
        val messageId1 = "messageId1"
        val labelId1 = "labelId1"
        val messageId2 = "messageId2"
        val labelId2 = "labelId2"
        val messageIds = listOf(messageId1, messageId2)
        val labelIds = listOf(labelId1, labelId2)
        val currentLocation = Constants.MessageLocationType.INBOX
        val labelsSheetType = ManageLabelsActionSheet.Type.LABEL
        val expected = MessageActionSheetAction.ShowLabelsManager(
            messageIds,
            labelIds,
            currentLocation.messageLocationTypeValue,
            labelsSheetType
        )
        val message1 = mockk<Message> {
            every { messageId } returns messageId1
            every { labelIDsNotIncludingLocations } returns listOf(labelId1)
        }
        val message2 = mockk<Message> {
            every { messageId } returns messageId2
            every { labelIDsNotIncludingLocations } returns listOf(labelId2)
        }
        coEvery { repository.findMessageById(messageId1) } returns message1
        coEvery { repository.findMessageById(messageId2) } returns message2

        // when
        viewModel.showLabelsManager(messageIds, currentLocation)

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyShowLabelsManagerActionIsExecutedForFolders() = runBlockingTest {

        // given
        val messageId1 = "messageId1"
        val labelId1 = "labelId1"
        val messageId2 = "messageId2"
        val labelId2 = "labelId2"
        val messageIds = listOf(messageId1, messageId2)
        val labelIds = listOf(labelId1, labelId2)
        val currentLocation = Constants.MessageLocationType.INBOX
        val labelsSheetType = ManageLabelsActionSheet.Type.FOLDER
        val expected = MessageActionSheetAction.ShowLabelsManager(
            messageIds,
            labelIds,
            currentLocation.messageLocationTypeValue,
            labelsSheetType
        )
        val message1 = mockk<Message> {
            every { messageId } returns messageId1
            every { labelIDsNotIncludingLocations } returns listOf(labelId1)
        }
        val message2 = mockk<Message> {
            every { messageId } returns messageId2
            every { labelIDsNotIncludingLocations } returns listOf(labelId2)
        }
        coEvery { repository.findMessageById(messageId1) } returns message1
        coEvery { repository.findMessageById(messageId2) } returns message2

        // when
        viewModel.showLabelsManager(messageIds, currentLocation, ManageLabelsActionSheet.Type.FOLDER)

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
    }

    @Test
    fun verifyShowMessageHeadersActionProcessing() {

        // given
        val messageId1 = "messageId1"
        val messageHeader = "testMessageHeader"
        val message1 = mockk<Message> {
            every { messageId } returns messageId1
            every { header } returns messageHeader
        }
        coEvery { repository.findMessageById(messageId1) } returns message1
        val expected = MessageActionSheetAction.ShowMessageHeaders(messageHeader)

        // when
        viewModel.showMessageHeaders(messageId1)

        // then
        assertEquals(expected, viewModel.actionsFlow.value)
    }
}
