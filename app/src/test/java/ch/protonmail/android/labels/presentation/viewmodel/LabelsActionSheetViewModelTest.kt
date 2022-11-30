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

package ch.protonmail.android.labels.presentation.viewmodel

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.SavedStateHandle
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.labels.data.remote.worker.UpdateConversationsLabelsWorker
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.domain.model.ManageLabelActionResult
import ch.protonmail.android.labels.domain.usecase.ObserveLabelsOrFoldersWithChildrenByType
import ch.protonmail.android.labels.domain.usecase.UpdateMessageLabels
import ch.protonmail.android.labels.presentation.mapper.LabelDomainActionItemUiMapper
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.presentation.util.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val TEST_USER_ID = UserId("userId")

class LabelsActionSheetViewModelTest : ArchTest by ArchTest(),
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    @MockK
    private lateinit var moveMessagesToFolder: MoveMessagesToFolder

    @MockK
    private lateinit var updateMessageLabels: UpdateMessageLabels

    @MockK
    private lateinit var userManager: UserManager

    private val observeLabelsOrFoldersWithChildrenByType: ObserveLabelsOrFoldersWithChildrenByType = mockk {
        coEvery { this@mockk(any(), any()) } returns flowOf(emptyList())
    }

    private val accountManager: AccountManager = mockk {
        every { getPrimaryUserId() } returns flowOf(TEST_USER_ID)
    }

    @MockK
    private lateinit var savedStateHandle: SavedStateHandle

    @MockK
    private lateinit var messageRepository: MessageRepository

    @MockK
    private lateinit var moveConversationsToFolder: MoveConversationsToFolder

    @MockK
    private lateinit var conversationModeEnabled: ConversationModeEnabled

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @MockK
    private lateinit var updateConversationsLabels: UpdateConversationsLabelsWorker.Enqueuer

    private val defaultColorInt = 890
    private val testColorInt = 123
    private val context: Context = mockk {
        every { getColor(any()) } returns defaultColorInt
    }

    private val labelDomainUiMapper = LabelDomainActionItemUiMapper(context)

    private lateinit var viewModel: LabelsActionSheetViewModel

    private val messageId1 = "messageId1"
    private val labelId1 = "labelId1"
    private val labelId2 = "labelId2"
    private val iconRes = R.drawable.circle_labels_selection
    private val title = "title"
    private val titleRes = null
    private val color = "YELLOW"
    private val colorInt = testColorInt
    private val message1 = mockk<Message> {
        every { messageId } returns messageId1
        every { labelIDsNotIncludingLocations } returns listOf(labelId1)
    }

    private val model1UiLabel = LabelActonItemUiModel(
        labelId = LabelId(labelId1),
        iconRes = iconRes,
        title = title,
        titleRes = titleRes,
        colorInt = colorInt,
        folderLevel = 0,
        isChecked = true,
        labelType = LabelType.MESSAGE_LABEL
    )
    private val model2UiFolder = LabelActonItemUiModel(
        labelId = LabelId(labelId2),
        iconRes = iconRes,
        title = title,
        titleRes = titleRes,
        colorInt = colorInt,
        folderLevel = 0,
        isChecked = false,
        labelType = LabelType.FOLDER
    )

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } returns testColorInt

        every { savedStateHandle.get<List<String>>(LabelsActionSheet.EXTRA_ARG_MESSAGES_IDS) } returns listOf(
            messageId1
        )
        every {
            savedStateHandle.get<LabelType>(
                LabelsActionSheet.EXTRA_ARG_ACTION_SHEET_TYPE
            )
        } returns LabelType.MESSAGE_LABEL

        every {
            savedStateHandle.get<Int>(LabelsActionSheet.EXTRA_ARG_CURRENT_FOLDER_LOCATION)
        } returns 0

        every {
            savedStateHandle.get<String>(LabelsActionSheet.EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID)
        } returns "0"

        every {
            savedStateHandle.get<ActionSheetTarget>(LabelsActionSheet.EXTRA_ARG_ACTION_TARGET)
        } returns ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN

        coEvery { messageRepository.findMessageById(messageId1) } returns message1
        every { userManager.requireCurrentUserId() } returns TEST_USER_ID
        coEvery { conversationModeEnabled(location = any(), userId = any()) } returns false

        viewModel = buildViewModel()
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun verifyThatAfterOnDoneIsClickedLabelsSuccessIsEmitted() = runBlockingTest {

        // given
        val shallMoveToArchive = true
        val inboxLocationId = "0"
        val archiveLocationId = "6"
        coEvery { updateMessageLabels.invoke(any(), any()) } returns mockk()
        coEvery {
            moveMessagesToFolder(
                listOf(messageId1), Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString(),
                Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString(),
                TEST_USER_ID
            )
        } just Runs

        // when
        viewModel.onDoneClicked(shallMoveToArchive)

        // then
        coVerify { updateMessageLabels.invoke(any(), any()) }
        coVerify {
            moveMessagesToFolder(
                listOf(messageId1),
                archiveLocationId,
                inboxLocationId,
                TEST_USER_ID
            )
        }
        assertEquals(
            ManageLabelActionResult.LabelsSuccessfullySaved(areMailboxItemsMovedFromLocation = true),
            viewModel.actionsResult.value
        )
    }

    @Test
    fun verifyThatAfterDoneIsClickedErrorUpdatingLabelsIsEmittedWhenAConversationActionReturnsErrorResult() {
        // given
        val shallMoveToArchive = true
        every { updateConversationsLabels.enqueue(any(), any(), any()) } returns mockk()
        coEvery {
            moveConversationsToFolder.invoke(
                any(), any(),
                Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
            )
        } returns ConversationsActionResult.Error

        // when
        viewModel.onDoneClicked(shallMoveToArchive)

        // then
        assertEquals(ManageLabelActionResult.ErrorUpdatingLabels, viewModel.actionsResult.value)
    }

    @Test
    fun verifyThatWhenOnLabelClickedIsCalledForLabelTypeAndNumberOfSelectedLabelsIsLessThanThresholdDefaultIsEmitted() =
        runBlockingTest {

            // given
            coEvery { observeLabelsOrFoldersWithChildrenByType(any(), any()) } returns
                flowOf(listOf(buildLabelOrFolderWithChildrenLabel(id = LabelId(labelId1), name = title)))
            coEvery { userManager.didReachLabelsThreshold(any()) } returns false
            val viewModel = buildViewModel()

            // when
            viewModel.onLabelClicked(model1UiLabel)

            // then
            assertEquals(listOf(model1UiLabel.copy(isChecked = false)), viewModel.labels.value)
            assertEquals(ManageLabelActionResult.Default, viewModel.actionsResult.value)
        }

    @Test
    fun verifyThatAfterOnLabelIsClickedForLabelTypeStandardLabelsAreAdded() = runBlockingTest {

        // given
        coEvery { observeLabelsOrFoldersWithChildrenByType(any(), any()) } returns
            flowOf(listOf(buildLabelOrFolderWithChildrenLabel(id = LabelId(labelId1), name = title)))
        coEvery { userManager.didReachLabelsThreshold(any()) } returns false
        val viewModel = buildViewModel()

        // when
        viewModel.onLabelClicked(model1UiLabel)

        // then
        assertEquals(listOf(model1UiLabel.copy(isChecked = false)), viewModel.labels.value)
        assertEquals(ManageLabelActionResult.Default, viewModel.actionsResult.value)
    }

    @Test
    fun verifyThatAfterOnLabelIsClickedForFolderTypeMessagesAreMoved() = runBlockingTest {

        // given
        coEvery { userManager.didReachLabelsThreshold(any()) } returns false
        coEvery { moveMessagesToFolder.invoke(any(), any(), any(), any()) } just Runs
        coEvery { conversationModeEnabled(location = any(), userId = any()) } returns false
        every {
            savedStateHandle.get<ActionSheetTarget>(LabelsActionSheet.EXTRA_ARG_ACTION_TARGET)
        } returns ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN

        // when
        viewModel.onLabelClicked(model2UiFolder)

        // then
        coVerify { moveMessagesToFolder.invoke(any(), any(), any(), any()) }
        assertEquals(
            ManageLabelActionResult.MessageSuccessfullyMoved(
                shouldDismissBackingActivity = true,
                areMailboxItemsMovedFromLocation = true
            ),
            viewModel.actionsResult.value
        )
    }

    @Test
    fun verifyThatWhenLabelIsClickedForFolderTypeWithConversationModeEnabledThenConversationsAreMoved() =
        runBlockingTest {

            // given
            coEvery { userManager.currentUserId } returns TEST_USER_ID
            coEvery { moveMessagesToFolder.invoke(any(), any(), any(), any()) } just Runs
            coEvery { conversationModeEnabled(location = any(), userId = any()) } returns true
            every {
                savedStateHandle.get<ActionSheetTarget>("extra_arg_labels_action_sheet_actions_target")
            } returns ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
            coEvery { moveConversationsToFolder.invoke(any(), any(), any()) } returns ConversationsActionResult.Success

            // when
            viewModel.onLabelClicked(model2UiFolder)

            // then
            coVerify { moveConversationsToFolder(any(), any(), any()) }
            coVerify { moveMessagesToFolder wasNot Called }
            assertEquals(
                ManageLabelActionResult.MessageSuccessfullyMoved(
                    shouldDismissBackingActivity = true,
                    areMailboxItemsMovedFromLocation = true
                ),
                viewModel.actionsResult.value
            )
        }

    @Test
    fun verifyThatWhenLabelIsClickedForFolderTypeWithConversationModeEnabledAndMoveConversationsToFolderReturnsErrorResultErrorMovingToFolderIsEmitted() =
        runBlockingTest {
            // given
            coEvery { userManager.currentUserId } returns TEST_USER_ID
            coEvery { conversationModeEnabled(location = any(), userId = any()) } returns true
            coEvery {
                moveConversationsToFolder.invoke(any(), any(), any())
            } returns ConversationsActionResult.Error

            // when
            viewModel.onLabelClicked(model2UiFolder)

            // then
            assertEquals(ManageLabelActionResult.ErrorMovingToFolder, viewModel.actionsResult.value)
        }

    @Test
    fun verifyThatWhenLabelIsClickedForFolderTypeWithMessageItemWithinConversationAsActionsTargetThenTheMessageIsMoved() =
        runBlockingTest {

            // given

            coEvery { userManager.currentUserId } returns TEST_USER_ID
            coEvery { moveMessagesToFolder.invoke(any(), any(), any(), TEST_USER_ID) } just Runs
            coEvery { conversationModeEnabled(location = any(), userId = any()) } returns true
            every {
                savedStateHandle.get<ActionSheetTarget>("extra_arg_labels_action_sheet_actions_target")
            } returns ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN

            // when
            viewModel.onLabelClicked(model2UiFolder)

            // then
            coVerify { moveMessagesToFolder.invoke(any(), any(), any(), any()) }
            coVerify { moveConversationsToFolder wasNot Called }
            assertEquals(
                ManageLabelActionResult.MessageSuccessfullyMoved(
                    shouldDismissBackingActivity = false,
                    areMailboxItemsMovedFromLocation = true
                ),
                viewModel.actionsResult.value
            )
        }

    private fun buildViewModel() = LabelsActionSheetViewModel(
        savedStateHandle = savedStateHandle,
        observeLabelsOrFoldersWithChildrenByType = observeLabelsOrFoldersWithChildrenByType,
        accountManager = accountManager,
        userManager = userManager,
        updateMessageLabels = updateMessageLabels,
        updateConversationsLabels = updateConversationsLabels,
        moveMessagesToFolder = moveMessagesToFolder,
        moveConversationsToFolder = moveConversationsToFolder,
        conversationModeEnabled = conversationModeEnabled,
        messageRepository = messageRepository,
        conversationsRepository = conversationsRepository,
        labelDomainUiMapper = labelDomainUiMapper
    )

    private fun buildLabelOrFolderWithChildrenLabel(
        name: String = labelId1,
        id: LabelId = LabelId(name)
    ) = LabelOrFolderWithChildren.Label(
        id = id,
        name = name,
        color = color
    )

    private fun buildAListOfMoreThanOneHundredSelectedLabels(): List<LabelOrFolderWithChildren.Label> =
        (0..100).map { index -> buildLabelOrFolderWithChildrenLabel("$index") }
}
