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

package ch.protonmail.android.mailbox.presentation.viewmodel

import android.graphics.Color
import androidx.lifecycle.liveData
import app.cash.turbine.test
import arrow.core.Either
import arrow.core.left
import arrow.core.right
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
import ch.protonmail.android.di.JobEntryPoint
import ch.protonmail.android.domain.loadMoreFlowOf
import ch.protonmail.android.domain.withLoadMore
import ch.protonmail.android.feature.NotLoggedIn
import ch.protonmail.android.feature.rating.usecase.StartRateAppFlowIfNeeded
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
import ch.protonmail.android.mailbox.presentation.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailbox.presentation.model.MailboxItemUiModel
import ch.protonmail.android.mailbox.presentation.model.MailboxListState
import ch.protonmail.android.mailbox.presentation.model.MailboxState
import ch.protonmail.android.mailbox.presentation.util.ConversationModeEnabled
import ch.protonmail.android.notifications.presentation.usecase.ClearNotificationsForUser
import ch.protonmail.android.pendingaction.data.model.PendingSend
import ch.protonmail.android.pendingaction.data.model.PendingUpload
import ch.protonmail.android.settings.domain.usecase.GetMailSettings
import ch.protonmail.android.testdata.UserTestData
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.delete.EmptyFolder
import ch.protonmail.android.usecase.message.ChangeMessagesReadStatus
import ch.protonmail.android.usecase.message.ChangeMessagesStarredStatus
import dagger.hilt.EntryPoints
import io.mockk.Called
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.domain.type.StringEnum
import me.proton.core.mailsettings.domain.entity.ComposerMode
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.MessageButtons
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.PMSignature
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.mailsettings.domain.entity.ShowImage
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.entity.ViewLayout
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MailboxViewModelTest : ArchTest by ArchTest(),
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

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
        coEvery { toUiModels(messages = any<Collection<Message>>(), any(), allLabels = any()) } returns emptyList()
        coEvery {
            toUiModels(
                userId = any(), conversations = any<Collection<Conversation>>(), currentLabelId = any(),
                allLabels = any()
            )
        } returns emptyList()
    }

    private val clearNotificationsForUser: ClearNotificationsForUser = mockk()

    private val fetchEventsAndReschedule: FetchEventsAndReschedule = mockk {
        coEvery { this@mockk.invoke() } just runs
    }
    private val startRateAppFlowIfNeeded: StartRateAppFlowIfNeeded = mockk()

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
        every { conversationModeEnabled(any(), INBOX.asLabelId()) } returns false // INBOX type to use with messages
        every { conversationModeEnabled(ARCHIVE) } returns true // ARCHIVE type to use with conversations
        every {
            conversationModeEnabled(
                any(), ARCHIVE.asLabelId()
            )
        } returns true // ARCHIVE type to use with conversations
        every { conversationModeEnabled(LABEL) } returns true // LABEL type to use with conversations
        every { conversationModeEnabled(any(), LABEL.asLabelId()) } returns true // LABEL type to use with conversations
        every { conversationModeEnabled(LABEL_FOLDER) } returns true // LABEL_FOLDER type to use with conversations
        every {
            conversationModeEnabled(
                any(), LABEL_FOLDER.asLabelId()
            )
        } returns true // LABEL_FOLDER type to use with conversations
        every { conversationModeEnabled(ALL_MAIL) } returns true // ALL_MAIL type to use with conversations
        every {
            conversationModeEnabled(
                any(), ALL_MAIL.asLabelId()
            )
        } returns true // ALL_MAIL type to use with conversations
        every { verifyConnection.invoke() } returns flowOf(Constants.ConnectionState.CONNECTED)
        coEvery { observeMessagesByLocation(any()) } returns messagesResponseChannel.receiveAsFlow()
            .withLoadMore(loadMoreFlowOf<GetMessagesResult>()) {}
        every { observeConversationsByLocation(any()) } returns conversationsResponseFlow.receiveAsFlow()
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
            clearNotificationsForUser = clearNotificationsForUser,
            startRateAppFlowIfNeeded = startRateAppFlowIfNeeded
        )
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(EntryPoints::class)
        unmockkStatic(Color::class)
    }

    @Test
    fun verifyBasicInitFlowWithEmptyMessages() = runTest(dispatchers.Main) {
        // Given
        val messages = emptyList<Message>()
        val expected = emptyList<MailboxItemUiModel>().toMailboxState()

        // When
        viewModel.mailboxState.test {
            // Then
            assertEquals(loadingState, awaitItem())
            messagesResponseChannel.send(GetMessagesResult.Success(messages))
            assertEquals(expected, awaitItem().list)
        }
    }

    @Test
    fun verifyBasicInitFlowWithAnError() = runTest(dispatchers.Main) {
        // given
        val errorMessage = "An error!"
        val exception = Exception(errorMessage)
        val expected = MailboxListState.Error("Failed getting messages", exception)

        // When
        viewModel.mailboxState.test {
            // Then
            assertEquals(loadingState, awaitItem())
            messagesResponseChannel.close(exception)
            val listState = awaitItem().list
            assertEquals(expected.throwable?.message, (listState as MailboxListState.Error).throwable?.message)
        }
    }

    @Test
    fun getMailboxItemsReturnsStateWithMailboxItemsMappedFromMessageDetailsRepositoryWhenFetchingFirstPage() =
        runTest(dispatchers.Main) {
            // Given
            val message = Message()
            val messages = listOf(message)
            val mailboxUiItems = listOf(buildMailboxUiItem())
            coEvery { mailboxItemUiModelMapper.toUiModels(listOf(message), any(), any()) } returns mailboxUiItems
            val expectedState = mailboxUiItems.toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, awaitItem())
                messagesResponseChannel.send(GetMessagesResult.Success(messages))
                assertEquals(expectedState, awaitItem().list)
            }
        }

    @Test
    fun getMailboxItemsReturnsMailboxItemsMappedFromConversationsWhenGetConversationsUseCaseSucceeds() =
        runTest(dispatchers.Main) {
            val location = ARCHIVE
            viewModel.setNewMailboxLocation(location)
            val conversations = listOf(buildConversation())
            val successResult = GetConversationsResult.Success(conversations)
            val mailboxUiItems = listOf(buildMailboxUiItem())
            coEvery {
                mailboxItemUiModelMapper.toUiModels(any(), conversations, location.asLabelId(), any())
            } returns mailboxUiItems

            val expected = mailboxUiItems.toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, awaitItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, awaitItem().list)
            }
        }

    @Test
    fun getMailboxItemsReturnsMailboxStateWithErrorWhenGetConversationsUseCaseReturnsError() =
        runTest(dispatchers.Main) {
            // Given
            val location = LABEL
            val expected = MailboxListState.Error("Failed getting conversations", null)

            // When
            viewModel.setNewMailboxLocation(location)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, awaitItem())
                conversationsResponseFlow.send(GetConversationsResult.Error())
                assertEquals(expected, awaitItem().list)
            }
        }

    @Test
    fun isFreshDataIsFalseBeforeDataRefreshAndTrueAfter() = runTest(dispatchers.Main) {
        // given
        val firstExpected = MailboxListState.Data(emptyList(), isFreshData = false, shouldResetPosition = true)
        val secondExpected = MailboxListState.Data(emptyList(), isFreshData = true, shouldResetPosition = true)

        // when
        viewModel.mailboxState.test {

            // then
            assertEquals(loadingState, awaitItem())

            // first emission from database
            messagesResponseChannel.send(GetMessagesResult.Success(emptyList()))
            assertEquals(firstExpected, awaitItem().list)

            // emission api refresh
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))
            awaitItem()

            // emission from database after api refresh
            messagesResponseChannel.send(GetMessagesResult.Success(emptyList()))
            assertEquals(secondExpected, awaitItem().list)
        }
    }

    @Test
    fun `verify conversation is unstarred`() = runTest(dispatchers.Main) {
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
    fun `verify message is unstarred`() = runTest(dispatchers.Main) {
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
    fun `verify the star action is called when doing an update star swipe action on unstarred mailbox item`() =
        runTest(dispatchers.Main) {
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
    fun `verify the unstar action is called when doing an update star swipe action on starred mailbox item`() =
        runTest(dispatchers.Main) {
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
        runTest(dispatchers.Main) {
            // when
            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))

            // then
            coVerify { fetchEventsAndReschedule() }
        }

    @Test
    fun `when refreshed and error emitted, should not fetch events nor reschedule the event loop`() =
        runTest(dispatchers.Main) {
            // when
            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.Error())

            // then
            coVerify { fetchEventsAndReschedule wasNot called }
        }

    @Test
    fun `when refreshed and s success state emitted, should not fetch events nor reschedule the event loop`() =
        runTest(dispatchers.Main) {
            // when
            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.Success(emptyList()))

            // then
            coVerify { fetchEventsAndReschedule wasNot called }
        }

    @Test
    fun `when refreshed and loading, should not fetch events nor reschedule the event loop`() =
        runTest(dispatchers.Main) {
            // when
            viewModel.refreshMessages()
            messagesResponseChannel.send(GetMessagesResult.Loading)

            // then
            coVerify { fetchEventsAndReschedule wasNot called }
        }

    @Test
    fun `should fetch events and reschedule the event loop only once after the data has arrived following a refresh`() =
        runTest(dispatchers.Main) {
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
        runTest(dispatchers.Main) {
            // when
            viewModel.clearNotifications(testUserId)

            // then
            coVerify {
                clearNotificationsForUser(testUserId)
            }
        }


    @Test
    fun `verify getmailsettings emits again after login`() =
        runTest(dispatchers.Main) {
            // Given
            val userIdFlow = MutableStateFlow<UserId?>(null)
            coEvery { userManager.primaryUserId } returns userIdFlow
            coEvery { getMailSettings.invoke(UserTestData.userId) } returns MutableStateFlow(
                GetMailSettings.Result.Success(mailSettings = mailSettings())
            )

            // When
            viewModel.getMailSettingsState().test {
                // Then
                val result: Either<NotLoggedIn, GetMailSettings.Result> = NotLoggedIn.left()
                val resultSuccess: Either<NotLoggedIn, GetMailSettings.Result> =
                    GetMailSettings.Result.Success(mailSettings = mailSettings()).right()

                assertEquals(result, awaitItem())
                userIdFlow.emit(UserTestData.userId)
                assertEquals(resultSuccess, awaitItem())
            }
        }

    @Test
    fun `calls to start rate app flow if needed delegates to use case`() = runTest {
        // given
        coEvery { startRateAppFlowIfNeeded.invoke(testUserId) } returns Unit

        // when
        viewModel.startRateAppFlowIfNeeded()

        // then
        coVerify { startRateAppFlowIfNeeded.invoke(testUserId) }
    }

    @Test
    fun `start rate app flow if needed is not called when current user id is invalid`() = runTest {
        // given
        every { userManager.currentUserId } returns null

        // when
        viewModel.startRateAppFlowIfNeeded()

        // then
        verify { startRateAppFlowIfNeeded wasNot Called }
    }

    private fun List<MailboxItemUiModel>.toMailboxState(): MailboxListState.Data =
        MailboxListState.Data(this, isFreshData = false, shouldResetPosition = true)

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
            correspondentsNames = EMPTY_STRING,
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
            isDraft = false,
            isScheduled = false,
            isProton = false
        )


        private fun mailSettings() = MailSettings(
            userId = UserTestData.userId,
            displayName = null,
            signature = null,
            autoSaveContacts = true,
            composerMode = IntEnum(1, ComposerMode.Maximized),
            messageButtons = IntEnum(1, MessageButtons.UnreadFirst),
            showImages = IntEnum(1, ShowImage.Remote),
            showMoved = IntEnum(0, ShowMoved.None),
            viewMode = IntEnum(1, ViewMode.NoConversationGrouping),
            viewLayout = IntEnum(1, ViewLayout.Row),
            swipeLeft = IntEnum(1, me.proton.core.mailsettings.domain.entity.SwipeAction.Spam),
            swipeRight = IntEnum(1, me.proton.core.mailsettings.domain.entity.SwipeAction.Spam),
            shortcuts = true,
            pmSignature = IntEnum(1, PMSignature.Disabled),
            numMessagePerPage = 1,
            draftMimeType = StringEnum("text/plain", MimeType.PlainText),
            receiveMimeType = StringEnum("text/plain", MimeType.PlainText),
            showMimeType = StringEnum("text/plain", MimeType.PlainText),
            enableFolderColor = true,
            inheritParentFolderColor = true,
            rightToLeft = true,
            attachPublicKey = true,
            sign = true,
            pgpScheme = IntEnum(1, PackageType.ProtonMail),
            promptPin = true,
            stickyLabels = true,
            confirmLink = true
        )

    }
}
