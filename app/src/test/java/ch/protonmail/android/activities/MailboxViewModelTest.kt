/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.activities

import android.graphics.Color
import androidx.lifecycle.liveData
import app.cash.turbine.test
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.segments.event.FetchEventsAndReschedule
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.ARCHIVE
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.Constants.MessageLocationType.LABEL
import ch.protonmail.android.core.Constants.MessageLocationType.LABEL_FOLDER
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.PendingSend
import ch.protonmail.android.data.local.model.PendingUpload
import ch.protonmail.android.di.JobEntryPoint
import ch.protonmail.android.domain.loadMoreFlowOf
import ch.protonmail.android.domain.withLoadMore
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.domain.usecase.ObserveLabels
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationsByLocation
import ch.protonmail.android.mailbox.domain.usecase.ObserveMessagesByLocation
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.mailbox.presentation.MailboxState
import ch.protonmail.android.mailbox.presentation.MailboxViewModel
import ch.protonmail.android.mailbox.presentation.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailbox.presentation.model.MailboxItemUiModel
import ch.protonmail.android.notifications.presentation.usecase.ClearNotificationsForUser
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.delete.EmptyFolder
import ch.protonmail.android.usecase.message.ChangeMessagesReadStatus
import ch.protonmail.android.usecase.message.ChangeMessagesStarredStatus
import dagger.hilt.EntryPoints
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MailboxViewModelTest : ArchTest, CoroutinesTest {

    private val testUserId = UserId("8237462347237428")

    private val messageDetailsRepository: MessageDetailsRepository = mockk {
        every { findAllPendingSendsAsync() } returns liveData { emit(emptyList<PendingSend>()) }
        every { findAllPendingUploadsAsync() } returns liveData { emit(emptyList<PendingUpload>()) }
    }
    private val messageDetailsRepositoryFactory: MessageDetailsRepository.AssistedFactory = mockk {
        every { create(any()) } returns messageDetailsRepository
    }

    private val labelRepository: LabelRepository = mockk()

    private val userManager: UserManager = mockk {
        every { currentUserId } returns testUserId
        coEvery { primaryUserId } returns MutableStateFlow(testUserId)
        every { requireCurrentUserId() } returns testUserId
    }

    private val deleteMessage: DeleteMessage = mockk()

    private val verifyConnection: VerifyConnection = mockk()

    private val networkConfigurator: NetworkConfigurator = mockk()

    private val conversationModeEnabled: ConversationModeEnabled = mockk()

    private val observeConversationsByLocation: ObserveConversationsByLocation = mockk()

    private val observeMessagesByLocation: ObserveMessagesByLocation = mockk()

    private val changeMessagesReadStatus: ChangeMessagesReadStatus = mockk()

    private val changeConversationsReadStatus: ChangeConversationsReadStatus = mockk()

    private val changeMessagesStarredStatus: ChangeMessagesStarredStatus = mockk()

    private val changeConversationsStarredStatus: ChangeConversationsStarredStatus = mockk()

    private val moveConversationsToFolder: MoveConversationsToFolder = mockk()

    private val moveMessagesToFolder: MoveMessagesToFolder = mockk()

    private val deleteConversations: DeleteConversations = mockk()

    private val emptyFolder: EmptyFolder = mockk()

    private val getMailSettings: GetMailSettings = mockk()

    private val observeLabels: ObserveLabels = mockk()

    private val mailboxItemUiModelMapper: MailboxItemUiModelMapper = mockk {
        coEvery { toUiModels(any(), any()) } returns emptyList()
        coEvery { toUiModels(any(), any(), any()) } returns emptyList()
    }

    private val clearNotificationsForUser: ClearNotificationsForUser = mockk()

    private val fetchEventsAndReschedule: FetchEventsAndReschedule = mockk {
        coEvery { this@mockk.invoke() } just runs
    }

    private lateinit var viewModel: MailboxViewModel

    private val loadingState = MailboxState.Loading
    private val messagesResponseChannel = Channel<GetMessagesResult>()
    private val conversationsResponseFlow = Channel<GetConversationsResult>()

    private val mailboxItemId1 = "mailboxItemId1"
    private val inboxLocation = INBOX
    private val testColorInt = 871

    @BeforeTest
    fun setUp() {
        every { conversationModeEnabled(INBOX) } returns false // INBOX type to use with messages
        every { conversationModeEnabled(ARCHIVE) } returns true // ARCHIVE type to use with conversations
        every { conversationModeEnabled(LABEL) } returns true // LABEL type to use with conversations
        every { conversationModeEnabled(LABEL_FOLDER) } returns true // LABEL_FOLDER type to use with conversations
        every { conversationModeEnabled(ALL_MAIL) } returns true // ALL_MAIL type to use with conversations
        every { verifyConnection.invoke() } returns flowOf(Constants.ConnectionState.CONNECTED)
        coEvery { observeMessagesByLocation(any(), any(), any()) } returns messagesResponseChannel.receiveAsFlow()
            .withLoadMore(loadMoreFlowOf<GetMessagesResult>()) {}
        every { observeConversationsByLocation(any(), any()) } returns conversationsResponseFlow.receiveAsFlow()
            .withLoadMore(loadMoreFlowOf<GetConversationsResult>()) {}

        val jobEntryPoint = mockk<JobEntryPoint>()
        mockkStatic(EntryPoints::class)
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } returns testColorInt


        every { EntryPoints.get(any(), JobEntryPoint::class.java) } returns jobEntryPoint
        every { jobEntryPoint.userManager() } returns mockk(relaxed = true)

        val allLabels = (0..11).map {
            Label(
                id = LabelId("$it"),
                name = "label $it",
                color = testColorInt.toString(),
                order = 0,
                type = LabelType.MESSAGE_LABEL,
                path = "a/b",
                parentId = "parentId",
            )

        }
        coEvery { labelRepository.findAllLabels(any()) } returns allLabels
        coEvery { labelRepository.findLabels(any()) } answers {
            val labelIds = arg<List<LabelId>>(1)
            allLabels.filter { label -> label.id in labelIds }
        }
        coEvery { observeLabels(any(), any()) } returns flowOf(allLabels)

        viewModel = MailboxViewModel(
            messageDetailsRepositoryFactory = messageDetailsRepositoryFactory,
            userManager = userManager,
            deleteMessage = deleteMessage,
            labelRepository = labelRepository,
            verifyConnection = verifyConnection,
            networkConfigurator = networkConfigurator,
            conversationModeEnabled = conversationModeEnabled,
            observeConversationModeEnabled = mockk(),
            observeMessagesByLocation = observeMessagesByLocation,
            observeConversationsByLocation = observeConversationsByLocation,
            changeMessagesReadStatus = changeMessagesReadStatus,
            changeConversationsReadStatus = changeConversationsReadStatus,
            changeMessagesStarredStatus = changeMessagesStarredStatus,
            changeConversationsStarredStatus = changeConversationsStarredStatus,
            observeAllUnreadCounters = mockk(),
            moveConversationsToFolder = moveConversationsToFolder,
            moveMessagesToFolder = moveMessagesToFolder,
            deleteConversations = deleteConversations,
            emptyFolder = emptyFolder,
            observeLabels = observeLabels,
            observeLabelsAndFoldersWithChildren = mockk(),
            drawerFoldersAndLabelsSectionUiModelMapper = mockk(),
            getMailSettings = getMailSettings,
            mailboxItemUiModelMapper = mailboxItemUiModelMapper,
            fetchEventsAndReschedule = fetchEventsAndReschedule,
            clearNotificationsForUser = clearNotificationsForUser
        )
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(EntryPoints::class)
        unmockkStatic(Color::class)
    }

    @Test
    fun verifyBasicInitFlowWithEmptyMessages() = runBlockingTest {
        // Given
        val messages = emptyList<Message>()
        val expected = emptyList<MailboxItemUiModel>().toMailboxState()

        // When
        viewModel.mailboxState.test {
            // Then
            assertEquals(loadingState, awaitItem())
            messagesResponseChannel.send(GetMessagesResult.Success(messages))
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun verifyBasicInitFlowWithAnError() = runBlockingTest {
        // given
        val errorMessage = "An error!"
        val exception = Exception(errorMessage)
        val expected = MailboxState.Error("Failed getting messages", exception)

        // When
        viewModel.mailboxState.test {
            // Then
            assertEquals(loadingState, awaitItem())
            messagesResponseChannel.close(exception)
            assertEquals(expected.throwable?.message, (awaitItem() as MailboxState.Error).throwable?.message)
        }
    }

    @Test
    fun getMailboxItemsReturnsStateWithMailboxItemsMappedFromMessageDetailsRepositoryWhenFetchingFirstPage() =
        runBlockingTest {
            // Given
            val message = Message()
            val messages = listOf(message)
            val mailboxUiItems = listOf(buildMailboxUiItem())
            coEvery { mailboxItemUiModelMapper.toUiModels(listOf(message), any()) } returns mailboxUiItems
            val expectedState = mailboxUiItems.toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, awaitItem())
                messagesResponseChannel.send(GetMessagesResult.Success(messages))
                assertEquals(expectedState, awaitItem())
            }
        }

    @Test
    fun getMailboxItemsReturnsMailboxItemsMappedFromConversationsWhenGetConversationsUseCaseSucceeds() =
        runBlockingTest {
            val location = ARCHIVE
            viewModel.setNewMailboxLocation(location)
            val conversations = listOf(buildConversation())
            val successResult = GetConversationsResult.Success(conversations)
            val mailboxUiItems = listOf(buildMailboxUiItem())
            coEvery { mailboxItemUiModelMapper.toUiModels(conversations, location.asLabelId(), any()) } returns mailboxUiItems

            val expected = mailboxUiItems.toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, awaitItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, awaitItem())
            }
        }

    @Test
    fun getMailboxItemsReturnsMailboxStateWithErrorWhenGetConversationsUseCaseReturnsError() =
        runBlockingTest {
            // Given
            val location = LABEL
            val expected = MailboxState.Error("Failed getting conversations", null)

            // When
            viewModel.setNewMailboxLocation(location)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, awaitItem())
                conversationsResponseFlow.send(GetConversationsResult.Error())
                assertEquals(expected, awaitItem())
            }
        }

    @Test
    fun isFreshDataIsFalseBeforeDataRefreshAndTrueAfter() = runBlockingTest {
        // given
        val firstExpected = MailboxState.Data(emptyList(), isFreshData = false, shouldResetPosition = true)
        val secondExpected = MailboxState.Data(emptyList(), isFreshData = true, shouldResetPosition = true)

        // when
        viewModel.mailboxState.test {

            // then
            assertEquals(loadingState, awaitItem())

            // first emission from database
            messagesResponseChannel.send(GetMessagesResult.Success(emptyList()))
            assertEquals(firstExpected, awaitItem())

            // emission api refresh
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))
            awaitItem()

            // emission from database after api refresh
            messagesResponseChannel.send(GetMessagesResult.Success(emptyList()))
            assertEquals(secondExpected, awaitItem())
        }
    }

    @Test
    fun `verify conversation is unstarred`() = runBlockingTest {
        // given
        val ids = listOf(mailboxItemId1)
        every { conversationModeEnabled(inboxLocation) } returns true
        coEvery {
            changeConversationsStarredStatus(ids, testUserId, ChangeConversationsStarredStatus.Action.ACTION_UNSTAR)
        } returns ConversationsActionResult.Success

        // when
        viewModel.unstar(ids, testUserId, inboxLocation)

        // then
        coVerify {
            changeConversationsStarredStatus(ids, testUserId, ChangeConversationsStarredStatus.Action.ACTION_UNSTAR)
        }
    }

    @Test
    fun `verify message is unstarred`() = runBlockingTest {
        // given
        val ids = listOf(mailboxItemId1)
        every { conversationModeEnabled(inboxLocation) } returns false
        coEvery {
            changeMessagesStarredStatus(testUserId, ids, ChangeMessagesStarredStatus.Action.ACTION_UNSTAR)
        } just runs

        // when
        viewModel.unstar(ids, testUserId, inboxLocation)

        // then
        coVerify {
            changeMessagesStarredStatus(testUserId, ids, ChangeMessagesStarredStatus.Action.ACTION_UNSTAR)
        }
    }

    @Test
    fun `verify the star action is called when doing an update star swipe action on unstarred mailbox item`() = runBlockingTest {
        // given
        val mailboxUiItem = buildMailboxUiItem(
            itemId = mailboxItemId1,
            isStarred = false
        )
        coEvery {
            viewModel.star(
                listOf(mailboxItemId1),
                testUserId,
                inboxLocation
            )
        } just runs

        // when
        viewModel.handleConversationSwipe(
            SwipeAction.UPDATE_STAR,
            mailboxUiItem,
            inboxLocation,
            inboxLocation.messageLocationTypeValue.toString()
        )

        // then
        coVerify {
            viewModel.star(
                listOf(mailboxItemId1),
                testUserId,
                inboxLocation
            )
        }
    }

    @Test
    fun `verify the unstar action is called when doing an update star swipe action on starred mailbox item`() = runBlockingTest {
        // given
        val mailboxUiItem = buildMailboxUiItem(
            itemId = mailboxItemId1,
            isStarred = true
        )
        coEvery {
            viewModel.unstar(
                listOf(mailboxItemId1),
                testUserId,
                inboxLocation
            )
        } just runs

        // when
        viewModel.handleConversationSwipe(
            SwipeAction.UPDATE_STAR,
            mailboxUiItem,
            inboxLocation,
            inboxLocation.messageLocationTypeValue.toString()
        )

        // then
        coVerify {
            viewModel.unstar(
                listOf(mailboxItemId1),
                testUserId,
                inboxLocation
            )
        }
    }

    @Test
    fun `when refreshed and the data has arrived, should fetch events and reschedule the event loop`() =
        runBlockingTest {
            // when
            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))

            // then
            coVerify { fetchEventsAndReschedule() }
        }

    @Test
    fun `when refreshed and error emitted, should not fetch events nor reschedule the event loop`() =
        runBlockingTest {
            // when
            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.Error())

            // then
            coVerify { fetchEventsAndReschedule wasNot called }
        }

    @Test
    fun `when refreshed and s success state emitted, should not fetch events nor reschedule the event loop`() =
        runBlockingTest {
            // when
            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.Success(emptyList()))

            // then
            coVerify { fetchEventsAndReschedule wasNot called }
        }

    @Test
    fun `when refreshed and loading, should not fetch events nor reschedule the event loop`() =
        runBlockingTest {
            // when
            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.Loading)

            // then
            coVerify { fetchEventsAndReschedule wasNot called }
        }

    @Test
    fun `should fetch events and reschedule the event loop only once after the data has arrived following a refresh`() =
        runBlockingTest {
            // when
            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))

            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))

            // then
            // called once per each refresh
            coVerify(exactly = 2) { fetchEventsAndReschedule() }
        }

    @Test
    fun `verify clear notifications use case is called`() =
        runBlockingTest {
            // when
            viewModel.clearNotifications(testUserId)

            // then
            coVerify {
                clearNotificationsForUser(testUserId)
            }
        }

    private fun List<MailboxItemUiModel>.toMailboxState(): MailboxState.Data =
        MailboxState.Data(this, isFreshData = false, shouldResetPosition = true)

    companion object TestData {

        fun buildConversation() = Conversation(
            id = EMPTY_STRING,
            subject = EMPTY_STRING,
            senders = emptyList(),
            receivers = emptyList(),
            messagesCount = 0,
            unreadCount = 0,
            attachmentsCount = 0,
            expirationTime = 0,
            labels = emptyList(),
            messages = emptyList()
        )

        fun buildMailboxUiItem(
            itemId: String = EMPTY_STRING,
            isStarred: Boolean = false,
        ) = MailboxItemUiModel(
            itemId = itemId,
            senderName = EMPTY_STRING,
            subject = EMPTY_STRING,
            lastMessageTimeMs = 0,
            hasAttachments = false,
            isStarred = isStarred,
            isRead = false,
            expirationTime = 0,
            messagesCount = 0,
            messageData = null,
            messageLabels = emptyList(),
            allLabelsIds = emptyList(),
            recipients = EMPTY_STRING,
            isDraft = false
        )

    }
}
