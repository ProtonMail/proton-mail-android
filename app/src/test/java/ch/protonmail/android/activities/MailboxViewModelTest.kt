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

import android.graphics.Color
import androidx.lifecycle.liveData
import app.cash.turbine.test
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.MessageRecipient
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
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.data.local.model.PendingSend
import ch.protonmail.android.data.local.model.PendingUpload
import ch.protonmail.android.di.JobEntryPoint
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.loadMoreFlowOf
import ch.protonmail.android.domain.withLoadMore
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.domain.usecase.ObserveLabels
import ch.protonmail.android.mailbox.data.mapper.MessageRecipientToCorrespondentMapper
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationsByLocation
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
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.EntryPoints
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val STARRED_LABEL_ID = "10"
private const val ALL_DRAFT_LABEL_ID = "1"
private const val DRAFT_LABEL_ID = "8"

class MailboxViewModelTest : ArchTest, CoroutinesTest {

    private val testUserId = UserId("8237462347237428")

    private val contactsRepository: ContactsRepository = mockk()

    @Suppress("RemoveExplicitTypeArguments") // Explicit arguments are required for lists. IDE bug?
    private val messageDetailsRepository: MessageDetailsRepository = mockk {
        every { findAllPendingSendsAsync() } returns liveData { emit(emptyList<PendingSend>()) }
        every { findAllPendingUploadsAsync() } returns liveData { emit(emptyList<PendingUpload>()) }
    }

    private val labelRepository: LabelRepository = mockk()

    private val userManager: UserManager = mockk {
        every { currentUserId } returns testUserId
    }

    private val jobManager: JobManager = mockk {
        every { addJobInBackground(any()) } just Runs
    }

    private val deleteMessage: DeleteMessage = mockk()

    private val verifyConnection: VerifyConnection = mockk()

    private val networkConfigurator: NetworkConfigurator = mockk()

    private val conversationModeEnabled: ConversationModeEnabled = mockk()

    private val observeConversationsByLocation: ObserveConversationsByLocation = mockk()

    private val observeMessagesByLocation: ObserveMessagesByLocation = mockk()

    private val changeConversationsReadStatus: ChangeConversationsReadStatus = mockk()

    private val changeConversationsStarredStatus: ChangeConversationsStarredStatus = mockk()

    private val moveConversationsToFolder: MoveConversationsToFolder = mockk()

    private val moveMessagesToFolder: MoveMessagesToFolder = mockk()

    private val deleteConversations: DeleteConversations = mockk()

    private val getMailSettings: GetMailSettings = mockk()

    private val observeLabels: ObserveLabels = mockk()

    private val messageRecipientToCorrespondentMapper = MessageRecipientToCorrespondentMapper()

    private lateinit var viewModel: MailboxViewModel

    private val loadingState = MailboxState.Loading
    private val messagesResponseChannel = Channel<GetMessagesResult>()
    private val conversationsResponseFlow = Channel<GetConversationsResult>()

    private val currentUserId = UserId("8237462347237428")
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

        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(emptyList())
        coEvery { contactsRepository.findContactsByEmail(any()) } returns flowOf(emptyList())

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
            messageDetailsRepository = messageDetailsRepository,
            userManager = userManager,
            jobManager = jobManager,
            deleteMessage = deleteMessage,
            dispatchers = dispatchers,
            contactsRepository = contactsRepository,
            labelRepository = labelRepository,
            verifyConnection = verifyConnection,
            networkConfigurator = networkConfigurator,
            conversationModeEnabled = conversationModeEnabled,
            observeConversationModeEnabled = mockk(),
            observeMessagesByLocation = observeMessagesByLocation,
            observeConversationsByLocation = observeConversationsByLocation,
            changeConversationsReadStatus = changeConversationsReadStatus,
            changeConversationsStarredStatus = changeConversationsStarredStatus,
            observeAllUnreadCounters = mockk(),
            moveConversationsToFolder = moveConversationsToFolder,
            moveMessagesToFolder = moveMessagesToFolder,
            deleteConversations = deleteConversations,
            observeLabels = observeLabels,
            observeLabelsAndFoldersWithChildren = mockk(),
            drawerFoldersAndLabelsSectionUiModelMapper = mockk(),
            getMailSettings = getMailSettings,
            messageRecipientToCorrespondentMapper = messageRecipientToCorrespondentMapper
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
        val expected = emptyList<MailboxUiItem>().toMailboxState()

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

            val expected = MailboxUiItem(
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
            ).toMailboxState()

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

        every { conversationModeEnabled(any()) } returns false
        val expected = MailboxUiItem(
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
        ).toMailboxState()

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
            every { conversationModeEnabled(any()) } returns false

            val expected = MailboxUiItem(
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
            ).toMailboxState()

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
            val messages = listOf(
                Message().apply {
                    messageId = "messageId"
                    sender = MessageSender("", "anySenderEmail8437@protonmail.ch")
                    subject = "subject"
                }
            )
            every { conversationModeEnabled(any()) } returns false

            // Then
            val expected = MailboxUiItem(
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
            ).toMailboxState()
            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                messagesResponseChannel.send(GetMessagesResult.Success(messages))
                assertEquals(expected, expectItem())
            }
        }

    @Test
    fun messagesToMailboxMapsRecipientNameToContactNameWhenRecipientEmailExistsInContactsList() = runBlockingTest {
        // Given
        val contactName = "contactNameTest"
        val recipientEmailAddress = "recipient@pm.ch"
        val recipients = listOf(MessageRecipient("recipientName", recipientEmailAddress))
        val messages = listOf(
            Message().apply {
                messageId = "messageId"
                subject = "subject"
                toList = recipients
            }
        )
        coEvery {
            contactsRepository.findContactsByEmail(match { emails -> emails.contains(recipientEmailAddress) })
        } returns flowOf(
            listOf(ContactEmail("contactId", recipientEmailAddress, contactName))
        )

        every { conversationModeEnabled(any()) } returns false
        val expected = MailboxUiItem(
            itemId = "messageId",
            senderName = "",
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
            recipients = "recipientName",
            isDraft = false
        ).toMailboxState()

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
        every { conversationModeEnabled(any()) } returns false
        coEvery {
            labelRepository.findLabels(
                listOf(LabelId("0"), LabelId("2"))
            )
        } returns listOf(
            Label(LabelId("0"), "label 0", "blue", 0, LabelType.MESSAGE_LABEL, "", ""),
            Label(LabelId("2"), "label 2", "blue", 0, LabelType.MESSAGE_LABEL, "", "")
        )

        val expected = MailboxUiItem(
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
                LabelChipUiModel(LabelId("0"), Name("label 0"), testColorInt),
                LabelChipUiModel(LabelId("2"), Name("label 2"), testColorInt)
            ),
            recipients = "recipientName",
            isDraft = false
        ).toMailboxState()

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
            val expectedState = expected.toMailboxState()

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
        // Represents pagination. Only messages older than the given timestamp will be returned
        val userId = UserId("userId")
        every { userManager.currentUserId } returns userId
        every { conversationModeEnabled(location) } returns false

        viewModel.setNewMailboxLocation(location)
        viewModel.loadMore()

        verify(exactly = 0) { jobManager.addJobInBackground(any()) }
    }

    @Test
    fun getMailboxItemsCallsMessageServiceStartFetchMessagesByLabelWhenTheRequestIsAboutLoadingPagesGreaterThanTheFirstAndLocationIsALabelOrFolder() {
        val location = LABEL_FOLDER
        val userId = UserId("userId1")
        every { userManager.currentUserId } returns userId
        every { conversationModeEnabled(location) } returns false

        viewModel.setNewMailboxLocation(location)
        viewModel.loadMore()

        verify(exactly = 0) { messageDetailsRepository.reloadDependenciesForUser(userId) }
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

            val expected = MailboxUiItem(
                "conversationId124",
                "firstSender",
                "subject2345",
                lastMessageTimeMs = 0,
                hasAttachments = true,
                isStarred = false,
                isRead = true,
                expirationTime = 823_764_623,
                messagesCount = 4,
                messageData = null,
                isDeleted = false,
                labels = emptyList(),
                recipients = "",
                isDraft = false
            ).toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, expectItem())
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

            val expected = MailboxUiItem(
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
            ).toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, expectItem())
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

            val expected = MailboxUiItem(
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
                labels = listOf(LabelChipUiModel(LabelId("10"), Name("label 10"), testColorInt)),
                recipients = "",
                isDraft = false
            ).toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, expectItem())
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

            val expected = MailboxUiItem(
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
            ).toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, expectItem())
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

            val expected = MailboxUiItem(
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
                    LabelChipUiModel(LabelId("0"), Name("label 0"), testColorInt),
                    LabelChipUiModel(LabelId("6"), Name("label 6"), testColorInt)
                ),
                recipients = "",
                isDraft = false
            ).toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, expectItem())
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
            val expected = MailboxUiItem(
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
                labels = listOf(LabelChipUiModel(LabelId("6"), Name("label 6"), testColorInt)),
                recipients = "",
                isDraft = false
            ).toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, expectItem())
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

            val expected = MailboxUiItem(
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
                    LabelChipUiModel(LabelId("1"), Name("label 1"), testColorInt),
                    LabelChipUiModel(LabelId("8"), Name("label 8"), testColorInt)
                ),
                recipients = "",
                true
            ).toMailboxState()
            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, expectItem())
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

            val expected = MailboxUiItem(
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
                    LabelChipUiModel(LabelId("1"), Name("label 1"), testColorInt),
                    LabelChipUiModel(LabelId("8"), Name("label 8"), testColorInt)
                ),
                recipients = "",
                false
            ).toMailboxState()

            // When
            viewModel.mailboxState.test {
                // Then
                assertEquals(loadingState, expectItem())
                conversationsResponseFlow.send(successResult)
                assertEquals(expected, expectItem())
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
            every { conversationModeEnabled(any()) } returns false
            coEvery {
                labelRepository.findLabels(
                    listOf(LabelId(ALL_DRAFT_LABEL_ID), LabelId(DRAFT_LABEL_ID))
                )
            } returns listOf(
                Label(
                    LabelId(ALL_DRAFT_LABEL_ID), "label 1", "blue", 0, LabelType.MESSAGE_LABEL, "", "",
                ),
                Label(
                    LabelId(DRAFT_LABEL_ID), "label 8", "blue", 0, LabelType.MESSAGE_LABEL, "", "",
                )
            )

            // Then
            val expected = MailboxUiItem(
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
                    LabelChipUiModel(LabelId("1"), Name("label 1"), testColorInt),
                    LabelChipUiModel(LabelId("8"), Name("label 8"), testColorInt)
                ),
                recipients = "",
                isDraft = true
            ).toMailboxState()
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
    fun isFreshDataIsFalseBeforeDataRefreshAndTrueAfter() = runBlockingTest {
        // given
        val firstExpected = MailboxState.Data(emptyList(), isFreshData = false, shouldResetPosition = true)
        val secondExpected = MailboxState.Data(emptyList(), isFreshData = true, shouldResetPosition = true)

        // when
        viewModel.mailboxState.test {

            // then
            assertEquals(loadingState, expectItem())

            // first emission from database
            messagesResponseChannel.send(GetMessagesResult.Success(emptyList()))
            assertEquals(firstExpected, expectItem())

            // emission api refresh
            messagesResponseChannel.send(GetMessagesResult.DataRefresh(emptyList()))
            expectItem()

            // emission from database after api refresh
            messagesResponseChannel.send(GetMessagesResult.Success(emptyList()))
            assertEquals(secondExpected, expectItem())
        }
    }

    private fun MailboxUiItem.toMailboxState() = listOf(this).toMailboxState()

    private fun List<MailboxUiItem>.toMailboxState(): MailboxState.Data =
        MailboxState.Data(this, isFreshData = false, shouldResetPosition = true)

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
