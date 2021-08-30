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

package ch.protonmail.android.activities

import app.cash.turbine.test
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.services.MessagesService
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.ARCHIVE
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.Constants.MessageLocationType.INVALID
import ch.protonmail.android.core.Constants.MessageLocationType.LABEL
import ch.protonmail.android.core.Constants.MessageLocationType.LABEL_FOLDER
import ch.protonmail.android.core.Constants.MessageLocationType.SENT
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.LabelRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.di.JobEntryPoint
import ch.protonmail.android.domain.entity.LabelId
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.jobs.FetchMessageCountsJob
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.GetConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.domain.usecase.ObserveMessagesByLocation
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.mailbox.presentation.MailboxState
import ch.protonmail.android.mailbox.presentation.MailboxViewModel
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem
import ch.protonmail.android.mailbox.presentation.model.MessageData
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.MessageUtils.toContactsAndGroupsString
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.EntryPoints
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.After
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val STARRED_LABEL_ID = "10"
private const val ALL_DRAFT_LABEL_ID = "1"
private const val DRAFT_LABEL_ID = "8"

class MailboxViewModelTest : ArchTest, CoroutinesTest {

    @RelaxedMockK
    private lateinit var contactsRepository: ContactsRepository

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @MockK
    private lateinit var labelRepository: LabelRepository

    @MockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var jobManager: JobManager

    @RelaxedMockK
    private lateinit var deleteMessage: DeleteMessage

    @MockK
    private lateinit var verifyConnection: VerifyConnection

    @RelaxedMockK
    private lateinit var networkConfigurator: NetworkConfigurator

    @RelaxedMockK
    private lateinit var messageServiceScheduler: MessagesService.Scheduler

    @MockK
    private lateinit var conversationModeEnabled: ConversationModeEnabled

    @RelaxedMockK
    private lateinit var getConversations: GetConversations

    @RelaxedMockK
    private lateinit var observeMessagesByLocation: ObserveMessagesByLocation

    @RelaxedMockK
    private lateinit var changeConversationsReadStatus: ChangeConversationsReadStatus

    @RelaxedMockK
    private lateinit var changeConversationsStarredStatus: ChangeConversationsStarredStatus

    @RelaxedMockK
    private lateinit var moveConversationsToFolder: MoveConversationsToFolder

    @RelaxedMockK
    private lateinit var moveMessagesToFolder: MoveMessagesToFolder

    @RelaxedMockK
    private lateinit var deleteConversations: DeleteConversations

    @RelaxedMockK
    private lateinit var getMailSettings: GetMailSettings

    private lateinit var viewModel: MailboxViewModel

    private val loadingState = MailboxState.Loading
    private val messagesResponseChannel = Channel<GetMessagesResult>()
    private val conversationsResponseFlow = Channel<GetConversationsResult>()

    private val currentUserId = UserId("8237462347237428")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        val userId = UserId("testUserId1")
        every { userManager.currentUserId } returns userId
        every { conversationModeEnabled(INBOX) } returns false // INBOX type to use with messages
        every { conversationModeEnabled(ARCHIVE) } returns true // ARCHIVE type to use with conversations
        every { conversationModeEnabled(LABEL) } returns true // LABEL type to use with conversations
        every { conversationModeEnabled(LABEL_FOLDER) } returns true // LABEL_FOLDER type to use with conversations
        every { conversationModeEnabled(ALL_MAIL) } returns true // ALL_MAIL type to use with conversations
        every { verifyConnection.invoke() } returns flowOf(Constants.ConnectionState.CONNECTED)
        coEvery { getConversations(any(), any()) } returns conversationsResponseFlow.receiveAsFlow()
        coEvery { observeMessagesByLocation(any(), any(), any()) } returns messagesResponseChannel.receiveAsFlow()
        viewModel = MailboxViewModel(
            messageDetailsRepository,
            userManager,
            jobManager,
            deleteMessage,
            dispatchers,
            contactsRepository,
            labelRepository,
            verifyConnection,
            networkConfigurator,
            messageServiceScheduler,
            conversationModeEnabled,
            getConversations,
            changeConversationsReadStatus,
            changeConversationsStarredStatus,
            observeMessagesByLocation,
            moveConversationsToFolder,
            moveMessagesToFolder,
            deleteConversations,
            getMailSettings
        )

        val jobEntryPoint = mockk<JobEntryPoint>()
        mockkStatic(EntryPoints::class)

        every { EntryPoints.get(any(), JobEntryPoint::class.java) } returns jobEntryPoint
        every { jobEntryPoint.userManager() } returns mockk(relaxed = true)

        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(emptyList())
        coEvery { contactsRepository.findContactsByEmail(any()) } returns flowOf(emptyList())

        val allLabels = (0..11).map {
            Label(id = "$it", name = "label $it", color = EMPTY_STRING)
        }
        every { labelRepository.findAllLabels(any()) } returns flowOf(allLabels)
        every { labelRepository.findLabels(any(), any()) } answers {
            val labelIds = arg<List<LabelId>>(1)
            flowOf(allLabels.filter { label -> LabelId(label.id) in labelIds })
        }

        every { userManager.currentUserId } returns currentUserId
    }

    @After
    fun tearDown() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun verifyBasicInitFlowWithEmptyMessages() = runBlockingTest {
        // Given
        val messages = emptyList<Message>()
        val expected = MailboxState.Data()

        // When
        viewModel.mailboxState.test {
            // Then
            assertEquals(loadingState, expectItem())
            messagesResponseChannel.send(GetMessagesResult.Success(messages))
            assertEquals(expected, expectItem())
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
            assertEquals(loadingState, expectItem())
            messagesResponseChannel.close(exception)
            assertEquals(expected.throwable?.message, (expectItem() as MailboxState.Error).throwable?.message)
        }
    }

    @Test
    fun messagesToMailboxMapsSenderNameToMessageSenderNameWhenSenderEmailDoesNotExistInContacts() =
        runBlockingTest {
            // Given
            val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
            val messages = listOf(
                Message().apply {
                    messageId = "messageId"
                    sender = MessageSender("senderName9238", "anySenderEmail@pm.me")
                    subject = "subject"
                }
            )
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("contactId", "anotherContact@pm.me", "anotherContactName"))
            )
            mockk<MessageUtils> {
                every { toContactsAndGroupsString(recipients) } returns "recipientName"
            }

            val expected = MailboxState.Data(
                listOf(
                    MailboxUiItem(
                        itemId = "messageId",
                        senderName = "senderName9238",
                        subject = "subject",
                        lastMessageTimeMs = 0,
                        hasAttachments = false,
                        isStarred = false,
                        isRead = true,
                        expirationTime = 0,
                        messagesCount = null,
                        messageData = MessageData(
                            location = INVALID.messageLocationTypeValue,
                            isReplied = false,
                            isRepliedAll = false,
                            isForwarded = false,
                            isInline = false
                        ),
                        isDeleted = false,
                        labels = emptyList(),
                        recipients = "",
                        isDraft = false
                    )
                )
            )

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                messagesResponseChannel.send(GetMessagesResult.Success(messages))
                assertEquals(expected, expectItem())
            }
        }

    @Test
    fun messagesToMailboxMapsSenderNameToContactNameWhenSenderEmailExistsInContactsList() = runBlockingTest {
        // Given
        val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
        val contactName = "contactNameTest"
        val senderEmailAddress = "sender@email.pm"
        val messages = listOf(
            Message().apply {
                messageId = "messageId"
                sender = MessageSender("anySenderName", senderEmailAddress)
                subject = "subject"
            }
        )
        coEvery {
            contactsRepository.findContactsByEmail(match { emails -> emails.contains(senderEmailAddress) })
        } returns flowOf(
            listOf(ContactEmail("contactId", senderEmailAddress, contactName))
        )
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }
        every { conversationModeEnabled(any()) } returns false
        val expected = MailboxState.Data(
            listOf(
                MailboxUiItem(
                    itemId = "messageId",
                    senderName = contactName,
                    subject = "subject",
                    lastMessageTimeMs = 0,
                    hasAttachments = false,
                    isStarred = false,
                    isRead = true,
                    expirationTime = 0,
                    messagesCount = null,
                    messageData = MessageData(
                        location = INVALID.messageLocationTypeValue,
                        isReplied = false,
                        isRepliedAll = false,
                        isForwarded = false,
                        isInline = false
                    ),
                    isDeleted = false,
                    labels = emptyList(),
                    recipients = "",
                    isDraft = false
                )
            )
        )

        // When
        viewModel.mailboxState.test {
            // Then
            assertEquals(loadingState, expectItem())
            messagesResponseChannel.send(GetMessagesResult.Success(messages))
            assertEquals(expected, expectItem())
            coVerify { contactsRepository.findContactsByEmail(listOf(senderEmailAddress)) }
        }
    }

    @Test
    fun messagesToMailboxMapsSenderNameToMessageSenderEmailWhenSenderEmailDoesNotExistInContactsAndSenderNameIsNull() =
        runBlockingTest {
            // Given
            val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
            val messages = listOf(
                Message().apply {
                    messageId = "messageId"
                    sender = MessageSender(null, "anySenderEmail@protonmail.ch")
                    subject = "subject"
                }
            )
            mockk<MessageUtils> {
                every { toContactsAndGroupsString(recipients) } returns "recipientName"
            }
            every { conversationModeEnabled(any()) } returns false

            val expected = MailboxState.Data(
                listOf(
                    MailboxUiItem(
                        itemId = "messageId",
                        senderName = "anySenderEmail@protonmail.ch",
                        subject = "subject",
                        lastMessageTimeMs = 0,
                        hasAttachments = false,
                        isStarred = false,
                        isRead = true,
                        expirationTime = 0,
                        messagesCount = null,
                        messageData = MessageData(
                            location = INVALID.messageLocationTypeValue,
                            isReplied = false,
                            isRepliedAll = false,
                            isForwarded = false,
                            isInline = false
                        ),
                        isDeleted = false,
                        labels = emptyList(),
                        recipients = "",
                        isDraft = false
                    )
                )
            )

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                messagesResponseChannel.send(GetMessagesResult.Success(messages))
                assertEquals(expected, expectItem())
            }
        }

    @Test
    fun messagesToMailboxMapsSenderNameToMessageSenderEmailWhenSenderEmailDoesNotExistInContactsAndSenderNameIsEmpty() =
        runBlockingTest {
            // Given
            val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
            val messages = listOf(
                Message().apply {
                    messageId = "messageId"
                    sender = MessageSender("", "anySenderEmail8437@protonmail.ch")
                    subject = "subject"
                }
            )
            mockk<MessageUtils> {
                every { toContactsAndGroupsString(recipients) } returns "recipientName"
            }
            every { conversationModeEnabled(any()) } returns false

            // Then
            val expected = MailboxState.Data(
                listOf(
                    MailboxUiItem(
                        itemId = "messageId",
                        senderName = "anySenderEmail8437@protonmail.ch",
                        subject = "subject",
                        lastMessageTimeMs = 0,
                        hasAttachments = false,
                        isStarred = false,
                        isRead = true,
                        expirationTime = 0,
                        messagesCount = null,
                        messageData = MessageData(
                            location = INVALID.messageLocationTypeValue,
                            isReplied = false,
                            isRepliedAll = false,
                            isForwarded = false,
                            isInline = false
                        ),
                        isDeleted = false,
                        labels = emptyList(),
                        recipients = "",
                        isDraft = false
                    )
                )
            )
            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                messagesResponseChannel.send(GetMessagesResult.Success(messages))
                assertEquals(expected, expectItem())
            }
        }

    @Test
    fun messagesToMailboxMapsAllFieldsOfMailboxUiItemFromMessageCorrectly() = runBlockingTest {
        // Given
        val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
        val messages = listOf(
            Message().apply {
                messageId = "messageId"
                sender = MessageSender("senderName", "senderEmail@pm.ch")
                subject = "subject"
                time = 1617205075 // Wednesday, March 31, 2021 5:37:55 PM GMT+02:00 in seconds
                numAttachments = 1
                isStarred = true
                Unread = true
                expirationTime = 82334L
                deleted = false
                allLabelIDs = listOf("0", "2")
                toList = recipients
                location = SENT.messageLocationTypeValue
                isReplied = true
                isRepliedAll = false
                isForwarded = false
                isInline = false
            }
        )
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }
        every { conversationModeEnabled(any()) } returns false

        val expected = MailboxState.Data(
            listOf(
                MailboxUiItem(
                    itemId = "messageId",
                    senderName = "senderName",
                    subject = "subject",
                    lastMessageTimeMs = 1617205075000, // Wednesday, March 31, 2021 5:37:55 PM GMT+02:00 in millis
                    hasAttachments = true,
                    isStarred = true,
                    isRead = false,
                    expirationTime = 82334L,
                    messagesCount = null,
                    messageData = MessageData(
                        location = SENT.messageLocationTypeValue,
                        isReplied = true,
                        isRepliedAll = false,
                        isForwarded = false,
                        isInline = false
                    ),
                    isDeleted = false,
                    labels = listOf(
                        LabelChipUiModel(LabelId("0"), Name("label 0"), null),
                        LabelChipUiModel(LabelId("2"), Name("label 2"), null)
                    ),
                    recipients = toContactsAndGroupsString(
                        recipients
                    ),
                    isDraft = false
                )
            )
        )

        // When
        viewModel.mailboxState.test {
            // Then
            assertEquals(loadingState, expectItem())
            messagesResponseChannel.send(GetMessagesResult.Success(messages))
            assertEquals(expected, expectItem())
        }
    }

    @Test
    fun getMailboxItemsReturnsStateWithMailboxItemsMappedFromMessageDetailsRepositoryWhenFetchingFirstPage() =
        runBlockingTest {
            // Given
            val message = Message(
                messageId = "messageId9238482",
                sender = MessageSender("senderName", "sender@pm.me"),
                subject = "subject1283"
            )
            val messages = listOf(message)

            val expected = listOf(
                fakeMailboxUiData("messageId9238482", "senderName", "subject1283")
            )
            val expectedState = MailboxState.Data(expected, false)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                messagesResponseChannel.send(GetMessagesResult.Success(messages))
                assertEquals(expectedState, expectItem())
            }
        }

    @Test
    fun getMailboxItemsCallsMessageServiceStartFetchMessagesWhenTheRequestIsAboutLoadingPagesGreaterThanTheFirstAndLocationIsNotALabelOrFolder() {
        val location = ARCHIVE
        val labelId = "labelId92323"
        val includeLabels = false
        val uuid = "9238423bbe2h3283742h3hh2bjsd"
        val refreshMessages = true
        // Represents pagination. Only messages older than the given timestamp will be returned
        val timestamp = 123L
        val userId = UserId("userId")
        every { userManager.currentUserId } returns userId
        every { conversationModeEnabled(location) } returns false

        viewModel.setNewMailboxLocation(location)
        viewModel.loadMailboxItems(
            labelId,
            includeLabels,
            uuid,
            refreshMessages,
            timestamp
        )

        verifySequence { messageServiceScheduler.fetchMessagesOlderThanTime(location, userId, timestamp) }
        verify(exactly = 0) { jobManager.addJobInBackground(any()) }
    }

    @Test
    fun getMailboxItemsCallsMessageServiceStartFetchMessagesByLabelWhenTheRequestIsAboutLoadingPagesGreaterThanTheFirstAndLocationIsALabelOrFolder() {
        val location = LABEL_FOLDER
        val labelId = "folderIdi2384"
        val includeLabels = false
        val uuid = "9238h82388sdfa8sdf8asd3hh2bjsd"
        val refreshMessages = false
        // Represents pagination. Only messages older than the given timestamp will be returned
        val oldestMessageTimestamp = 1323L
        val userId = UserId("userId1")
        every { userManager.currentUserId } returns userId
        every { conversationModeEnabled(location) } returns false

        viewModel.setNewMailboxLocation(location)
        viewModel.loadMailboxItems(
            labelId,
            includeLabels,
            uuid,
            refreshMessages,
            oldestMessageTimestamp
        )

        verifySequence {
            messageServiceScheduler.fetchMessagesOlderThanTimeByLabel(
                location, userId, oldestMessageTimestamp, labelId
            )
        }
        verify(exactly = 0) { messageDetailsRepository.reloadDependenciesForUser(userId) }
    }

    @Test
    fun getMailboxItemsCallsGetConversationsWithTheCorrectLocationIdWhenTheRequestIsAboutLoadingPagesGreaterThanTheFirst() {
        val location = LABEL
        val labelId = "customLabelIdi2386"
        val uuid = "9238h82388sdfa8sdf8asd3234"
        // Represents pagination. Only messages older than the given timestamp will be returned
        val oldestMessageTimestamp = 1323L
        val userId = UserId("userId1")
        every { userManager.currentUserId } returns userId
        every { conversationModeEnabled(location) } returns true

        viewModel.setNewMailboxLocation(location)
        viewModel.loadMailboxItems(
            labelId,
            false,
            uuid,
            false,
            oldestMessageTimestamp
        )

        // TODO: verify { getConversations.loadMore(userId, labelId, oldestMessageTimestamp) }
    }

    @Test
    fun getMailboxItemsReturnsMailboxItemsMappedFromConversationsWhenGetConversationsUseCaseSucceeds() =
        runBlockingTest {
            val location = ARCHIVE

            val senders = listOf(
                Correspondent("firstSender", "firstsender@protonmail.com")
            )
            val conversation = Conversation(
                "conversationId124",
                "subject2345",
                senders,
                emptyList(),
                4,
                0,
                2,
                823764623,
                listOf(),
                null
            )
            viewModel.setNewMailboxLocation(location)
            val successResult = GetConversationsResult.Success(listOf(conversation))

            val expectedItems = listOf(
                MailboxUiItem(
                    "conversationId124",
                    "firstSender",
                    "subject2345",
                    lastMessageTimeMs = 0,
                    hasAttachments = true,
                    isStarred = false,
                    isRead = true,
                    expirationTime = 823764623,
                    messagesCount = 4,
                    messageData = null,
                    isDeleted = false,
                    labels = emptyList(),
                    recipients = "",
                    isDraft = false
                )
            )
            val expectedState = MailboxState.Data(expectedItems, false)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expectedState, expectItem())
            }
        }

    @Test
    fun getMailboxItemsMapsConversationsSendersUsingContactNameOrSenderNameOrEmailInThisPreferenceOrder() =
        runBlockingTest {
            val location = ARCHIVE
            val senders = listOf(
                Correspondent("firstSender", "firstsender@protonmail.com"),
                Correspondent("secondSender", "anotherSender@protonmail.com"),
                Correspondent("", "thirdsender@pm.me"),
            )
            val recipients = listOf(
                Correspondent("recipient", "recipient@protonmail.com"),
                Correspondent("recipient1", "recipient1@pm.ch")
            )
            val conversation = Conversation(
                "conversationId",
                "subject",
                senders,
                recipients,
                2,
                1,
                2,
                123423423,
                listOf(),
                null
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )
            viewModel.setNewMailboxLocation(location)

            val expected = listOf(
                MailboxUiItem(
                    "conversationId",
                    "firstContactName, secondSender, thirdsender@pm.me",
                    "subject",
                    lastMessageTimeMs = 0,
                    hasAttachments = true,
                    isStarred = false,
                    isRead = false,
                    expirationTime = 123423423,
                    messagesCount = 2,
                    messageData = null,
                    isDeleted = false,
                    labels = emptyList(),
                    recipients = "recipient, recipient1",
                    isDraft = false
                )
            )
            val expectedState = MailboxState.Data(expected, false)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expectedState, expectItem())
            }
        }

    @Test
    fun getMailboxItemsMapsConversationAsStarredIfLabelsContainsStarredLabelId() =
        runBlockingTest {
            val location = LABEL
            val conversation = Conversation(
                "conversationId9238",
                "subject9237472",
                emptyList(),
                emptyList(),
                2,
                1,
                0,
                0,
                listOf(
                    LabelContext(STARRED_LABEL_ID, 0, 0, 0, 0, 0),
                    LabelContext("randomLabelId", 0, 0, 0, 0, 0)
                ),
                null
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            val labelId = "labelId923842"
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )
            viewModel.setNewMailboxLocation(location)

            val expected = listOf(
                MailboxUiItem(
                    "conversationId9238",
                    "",
                    "subject9237472",
                    lastMessageTimeMs = 0,
                    hasAttachments = false,
                    isStarred = true,
                    isRead = false,
                    expirationTime = 0,
                    messagesCount = 2,
                    messageData = null,
                    isDeleted = false,
                    labels = listOf(LabelChipUiModel(LabelId("10"), Name("label 10"), null)),
                    recipients = "",
                    isDraft = false
                )
            )
            val expectedState = MailboxState.Data(expected, false)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expectedState, expectItem())
            }
        }

    @Test
    fun getMailboxItemsMapsMessagesNumberToNullWhenItsLowerThanTwoSoThatItIsNotDisplayed() =
        runBlockingTest {
            val location = LABEL
            val conversation = Conversation(
                "conversationId9239",
                "subject9237473",
                emptyList(),
                emptyList(),
                1,
                1,
                0,
                0,
                listOf(),
                null
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )
            viewModel.setNewMailboxLocation(location)

            val expected = listOf(
                MailboxUiItem(
                    "conversationId9239",
                    "",
                    "subject9237473",
                    lastMessageTimeMs = 0,
                    hasAttachments = false,
                    isStarred = false,
                    isRead = false,
                    expirationTime = 0,
                    messagesCount = null,
                    messageData = null,
                    isDeleted = false,
                    labels = emptyList(),
                    recipients = "",
                    isDraft = false
                )
            )
            val expectedState = MailboxState.Data(expected, false)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expectedState, expectItem())
            }
        }

    @Test
    fun getMailboxItemsMapsLastMessageTimeMsToTheContextTimeOfTheLabelRepresentingTheCurrentLocationConvertedToMs() =
        runBlockingTest {
            val location = ARCHIVE
            val inboxLocationId = "0"
            val archiveLocationId = "6"
            val conversation = Conversation(
                "conversationId9240",
                "subject9237474",
                emptyList(),
                emptyList(),
                2,
                1,
                0,
                0,
                listOf(
                    LabelContext(inboxLocationId, 0, 0, 0, 0, 0),
                    LabelContext(archiveLocationId, 0, 0, 1617982194, 0, 0)
                ),
                null
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )
            viewModel.setNewMailboxLocation(location)

            val expected = listOf(
                MailboxUiItem(
                    "conversationId9240",
                    "",
                    "subject9237474",
                    lastMessageTimeMs = 1617982194000,
                    hasAttachments = false,
                    isStarred = false,
                    isRead = false,
                    expirationTime = 0,
                    messagesCount = 2,
                    messageData = null,
                    isDeleted = false,
                    labels = listOf(
                        LabelChipUiModel(LabelId("0"), Name("label 0"), null),
                        LabelChipUiModel(LabelId("6"), Name("label 6"), null)
                    ),
                    recipients = "",
                    isDraft = false
                )
            )
            val expectedState = MailboxState.Data(expected, false)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expectedState, expectItem())
            }
        }

    @Test
    fun getMailboxItemsMapsLastMessageTimeMsToTheContextTimeOfTheLabelRepresentingTheCurrentCustomFolderConvertedToMs() =
        runBlockingTest {
            val location = ARCHIVE
            val customLabelId = "Aujas8df8asdf727388fsdjfsjdbnj12=="
            val archiveLocationId = "6"
            val conversation = Conversation(
                "conversationId9241",
                "subject9237475",
                emptyList(),
                emptyList(),
                2,
                1,
                0,
                0,
                listOf(
                    LabelContext(customLabelId, 0, 0, 1417982244, 0, 0),
                    LabelContext(archiveLocationId, 0, 0, 0, 0, 0)
                ),
                null
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )

            viewModel.setNewMailboxLabel(customLabelId)
            viewModel.setNewMailboxLocation(location)
            val expected = listOf(
                MailboxUiItem(
                    "conversationId9241",
                    "",
                    "subject9237475",
                    lastMessageTimeMs = 1417982244000,
                    hasAttachments = false,
                    isStarred = false,
                    isRead = false,
                    expirationTime = 0,
                    messagesCount = 2,
                    messageData = null,
                    isDeleted = false,
                    labels = listOf(LabelChipUiModel(LabelId("6"), Name("label 6"), null)),
                    recipients = "",
                    isDraft = false
                )
            )
            val expectedState = MailboxState.Data(expected, false)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expectedState, expectItem())
            }
        }

    @Test
    fun getMailboxItemsMapsIsDraftToTrueWhenConversationContainsOnlyOneMessageWhichIsADraft() =
        runBlockingTest {
            val location = Constants.MessageLocationType.ALL_MAIL
            val conversation = Conversation(
                "conversationId9253",
                "subject9237482",
                emptyList(),
                emptyList(),
                1,
                1,
                0,
                0,
                listOf(
                    LabelContext(ALL_DRAFT_LABEL_ID, 0, 0, 0L, 0, 0),
                    LabelContext(DRAFT_LABEL_ID, 0, 0, 0L, 0, 0)
                ),
                null
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )
            viewModel.setNewMailboxLocation(location)

            val expected = listOf(
                MailboxUiItem(
                    "conversationId9253",
                    "",
                    "subject9237482",
                    lastMessageTimeMs = 0,
                    hasAttachments = false,
                    isStarred = false,
                    isRead = false,
                    expirationTime = 0,
                    messagesCount = null,
                    messageData = null,
                    isDeleted = false,
                    labels = listOf(
                        LabelChipUiModel(LabelId("1"), Name("label 1"), null),
                        LabelChipUiModel(LabelId("8"), Name("label 8"), null)
                    ),
                    recipients = "",
                    true
                )
            )
            val expectedState = MailboxState.Data(expected, false)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expectedState, expectItem())
            }
        }

    @Test
    fun getMailboxItemsMapsIsDraftToFalseWhenConversationContainsMoreThanOneMessage() =
        runBlockingTest {
            val location = Constants.MessageLocationType.ALL_MAIL
            val conversation = Conversation(
                "conversationId9254",
                "subject9237483",
                emptyList(),
                emptyList(),
                2,
                1,
                0,
                0,
                listOf(
                    LabelContext(ALL_DRAFT_LABEL_ID, 0, 0, 0L, 0, 0),
                    LabelContext(DRAFT_LABEL_ID, 0, 0, 0L, 0, 0)
                ),
                null
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )
            viewModel.setNewMailboxLocation(location)

            val expected = listOf(
                MailboxUiItem(
                    "conversationId9254",
                    "",
                    "subject9237483",
                    lastMessageTimeMs = 0,
                    hasAttachments = false,
                    isStarred = false,
                    isRead = false,
                    expirationTime = 0,
                    messagesCount = 2,
                    messageData = null,
                    isDeleted = false,
                    labels = listOf(
                        LabelChipUiModel(LabelId("1"), Name("label 1"), null),
                        LabelChipUiModel(LabelId("8"), Name("label 8"), null)
                    ),
                    recipients = "",
                    false
                )
            )
            val expectedState = MailboxState.Data(expected, false)

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expectedState, expectItem())
            }
        }

    @Test
    fun messagesToMailboxMapsIsDraftToTrueWhenMessageLocationIsDraftOrAllDrafts() =
        runBlockingTest {
            // Given
            val recipients = listOf(MessageRecipient("recipientName", "recipient@pm.ch"))
            val messages = listOf(
                Message().apply {
                    messageId = "messageId"
                    sender = MessageSender("", "anySenderEmail8438@protonmail.ch")
                    subject = "subject"
                    allLabelIDs = listOf(ALL_DRAFT_LABEL_ID, DRAFT_LABEL_ID)
                }
            )
            mockk<MessageUtils> {
                every { toContactsAndGroupsString(recipients) } returns "recipientName"
            }
            every { conversationModeEnabled(any()) } returns false

            // Then
            val expected = MailboxState.Data(
                listOf(
                    MailboxUiItem(
                        itemId = "messageId",
                        senderName = "anySenderEmail8438@protonmail.ch",
                        subject = "subject",
                        lastMessageTimeMs = 0,
                        hasAttachments = false,
                        isStarred = false,
                        isRead = true,
                        expirationTime = 0,
                        messagesCount = null,
                        messageData = MessageData(
                            location = INVALID.messageLocationTypeValue,
                            isReplied = false,
                            isRepliedAll = false,
                            isForwarded = false,
                            isInline = false
                        ),
                        isDeleted = false,
                        labels = listOf(
                            LabelChipUiModel(LabelId("1"), Name("label 1"), null),
                            LabelChipUiModel(LabelId("8"), Name("label 8"), null)
                        ),
                        recipients = "",
                        isDraft = true
                    )
                )
            )
            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                messagesResponseChannel.send(GetMessagesResult.Success(messages))
                assertEquals(expected, expectItem())
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
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(GetConversationsResult.Error())
                assertEquals(expected, expectItem())
            }
        }

    @Test
    fun refreshMailboxCountTriggersFetchMessagesCountJobWhenConversationsModeIsNotEnabled() {
        every { conversationModeEnabled(any()) } returns false

        viewModel.refreshMailboxCount(INBOX)

        val actual = slot<FetchMessageCountsJob>()
        verify { jobManager.addJobInBackground(capture(actual)) }
        assertNotNull(actual.captured)
    }

    @Test
    fun refreshMailboxCountDoesNotTriggerFetchMessagesCountJobWhenConversationsModeIsEnabled() {
        every { conversationModeEnabled(any()) } returns true

        viewModel.refreshMailboxCount(INBOX)

        verify { jobManager wasNot Called }
    }

    private fun fakeMailboxUiData(
        itemId: String,
        senderName: String,
        subject: String
    ) = MailboxUiItem(
        itemId,
        senderName,
        subject,
        0,
        hasAttachments = false,
        isStarred = false,
        isRead = true,
        expirationTime = 0,
        messagesCount = null,
        messageData = MessageData(
            INVALID.messageLocationTypeValue,
            isReplied = false,
            isRepliedAll = false,
            isForwarded = false,
            isInline = false
        ),
        isDeleted = false,
        labels = emptyList(),
        recipients = "",
        isDraft = false
    )
}
