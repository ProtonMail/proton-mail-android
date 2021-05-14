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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.liveData
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.services.MessagesService
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
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.jobs.FetchByLocationJob
import ch.protonmail.android.jobs.FetchMessageCountsJob
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.GetConversations
import ch.protonmail.android.mailbox.domain.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.mailbox.presentation.MailboxState
import ch.protonmail.android.mailbox.presentation.MailboxViewModel
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem
import ch.protonmail.android.mailbox.presentation.model.MessageData
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.ui.view.LabelChipUiModel
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
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.kotlin.EMPTY_STRING
import org.junit.After
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

private const val STARRED_LABEL_ID = "10"

class MailboxViewModelTest : CoroutinesTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var contactsRepository: ContactsRepository

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @MockK
    private lateinit var labelRepository: LabelRepository

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var jobManager: JobManager

    @RelaxedMockK
    private lateinit var deleteMessage: DeleteMessage

    @RelaxedMockK
    private lateinit var verifyConnection: VerifyConnection

    @RelaxedMockK
    private lateinit var networkConfigurator: NetworkConfigurator

    @RelaxedMockK
    private lateinit var messageServiceScheduler: MessagesService.Scheduler

    @RelaxedMockK
    private lateinit var conversationModeEnabled: ConversationModeEnabled

    @RelaxedMockK
    private lateinit var getConversations: GetConversations

    private lateinit var viewModel: MailboxViewModel

    private val currentUserId = Id("8237462347237428")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
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
            getConversations
        )

        val jobEntryPoint = mockk<JobEntryPoint>()
        mockkStatic(EntryPoints::class)

        every { EntryPoints.get(any(), JobEntryPoint::class.java) } returns jobEntryPoint
        every { jobEntryPoint.userManager() } returns mockk(relaxed = true)

        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(emptyList())

        val allLabels = (0..11).map {
            Label(id = "$it", name = "label $it", color = EMPTY_STRING)
        }
        every { labelRepository.findAllLabels(any()) } returns flowOf(allLabels)

        every { userManager.requireCurrentUserId() } returns currentUserId
    }

    @After
    fun tearDown() {
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun messagesToMailboxMapsSenderNameToMessageSenderNameWhenSenderEmailDoesNotExistInContacts() {
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
        every { conversationModeEnabled(any()) } returns false
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

        // Then
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
            isDeleted = false,
            labels = emptyList(),
            recipients = "",
            messageData = MessageData(
                location = INVALID.messageLocationTypeValue,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()!!
        assertEquals(expected, actualMailboxItems.items.first())
    }

    @Test
    fun messagesToMailboxMapsSenderNameToContactNameWhenSenderEmailExistsInContactsList() {
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
        coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
            listOf(ContactEmail("contactId", senderEmailAddress, contactName))
        )
        mockk<MessageUtils> {
            every { toContactsAndGroupsString(recipients) } returns "recipientName"
        }
        every { conversationModeEnabled(any()) } returns false
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

        // Then
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
            isDeleted = false,
            labels = emptyList(),
            recipients = "",
            messageData = MessageData(
                location = INVALID.messageLocationTypeValue,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()!!
        assertEquals(expected, actualMailboxItems.items.first())
        coVerify { contactsRepository.findAllContactEmails() }
    }

    @Test
    fun messagesToMailboxMapsSenderNameToMessageSenderEmailWhenSenderEmailDoesNotExistInContactsAndSenderNameIsNull() {
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
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

        // Then
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
            isDeleted = false,
            labels = emptyList(),
            recipients = "",
            messageData = MessageData(
                location = INVALID.messageLocationTypeValue,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()!!
        assertEquals(expected, actualMailboxItems.items.first())
    }

    @Test
    fun messagesToMailboxMapsSenderNameToMessageSenderEmailWhenSenderEmailDoesNotExistInContactsAndSenderNameIsEmpty() {
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
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

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
            isDeleted = false,
            labels = emptyList(),
            recipients = "",
            messageData = MessageData(
                location = INVALID.messageLocationTypeValue,
                isReplied = false,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()!!
        assertEquals(expected, actualMailboxItems.items.first())
    }

    @Test
    fun messagesToMailboxMapsAllFieldsOfMailboxUiItemFromMessageCorrectly() {
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
                allLabelIDs = listOf("1", "2")
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
        every { messageDetailsRepository.getMessagesByLocationAsync(any()) } returns liveData { emit(messages) }

        // When
        val actual = viewModel.getMailboxItems(
            INBOX,
            "",
            false,
            "",
            false
        ).testObserver()

        // Then
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
            isDeleted = false,
            labels = listOf(
                LabelChipUiModel(Id("1"), Name("label 1"), null),
                LabelChipUiModel(Id("2"), Name("label 2"), null)
            ),
            recipients = toContactsAndGroupsString(
                recipients
            ),
            messageData = MessageData(
                location = SENT.messageLocationTypeValue,
                isReplied = true,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            )
        )
        val actualMailboxItems = actual.observedValues.first()!!
        assertEquals(expected, actualMailboxItems.items.first())
    }

    @Test
    fun getMailboxItemsCallsFetchByLocationForwardingTheGivenParameters() {
        val location = SENT
        val labelId = "labelId923842"
        val includeLabels = true
        val uuid = "9238423bbe2h3423489wssdf"
        val refreshMessages = false

        viewModel.getMailboxItems(
            location,
            labelId,
            includeLabels,
            uuid,
            refreshMessages
        )

        val actual = slot<FetchByLocationJob>()
        verify { jobManager.addJobInBackground(capture(actual)) }
        assertEquals(location, actual.captured.location)
        assertEquals(labelId, actual.captured.labelId)
        assertEquals(includeLabels, actual.captured.includeLabels)
        assertEquals(uuid, actual.captured.uuid)
        assertEquals(false, actual.captured.refreshMessages)
        unmockkStatic(EntryPoints::class)
    }

    @Test
    fun getMailboxItemsReturnsStateWithMailboxItemsLiveDataMappedFromMessageDetailsRepositoryWhenFetchingFirstPage() {
        val message = Message(
            messageId = "messageId9238482",
            sender = MessageSender("senderName", "sender@pm.me"),
            subject = "subject1283"
        )
        coEvery { messageDetailsRepository.getAllMessages() } returns liveData { emit(listOf(message)) }

        val actual = viewModel.getMailboxItems(
            ALL_MAIL,
            "labelId923842",
            true,
            "9238423bbe2h3423489wssdf",
            false
        ).testObserver()

        val expected = listOf(
            fakeMailboxUiData("messageId9238482", "senderName", "subject1283")
        )
        val expectedState = MailboxState(expected, "", false)
        assertEquals(expectedState, actual.observedValues.first())
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
        val userId = Id("userId")
        every { userManager.requireCurrentUserId() } returns userId

        viewModel.loadMailboxItems(
            location,
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
        val userId = Id("userId1")
        every { userManager.requireCurrentUserId() } returns userId

        viewModel.loadMailboxItems(
            location,
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
    fun getMailboxItemsReloadsMessageDatabaseWhenRefreshMessagesIsTrue() {
        // This is a piece of tech debt that we still have to tackle. As beforehand was being done in
        // MailboxActivity.onSwitchedAccounts, we need to "reload" the database to avoid data from the previous
        // account being shown when the user switches the account.
        val refreshMessages = true
        // Represents pagination. Only messages older than the given timestamp will be returned
        val userId = Id("userId")
        every { userManager.requireCurrentUserId() } returns userId

        viewModel.loadMailboxItems(
            ARCHIVE,
            null,
            false,
            "9238423bbe2h3283742h3hh2bjsd",
            refreshMessages,
            123L
        )

        verify { messageDetailsRepository.reloadDependenciesForUser(userId) }
    }

    @Test
    fun getMailboxItemsCallsGetConversationsWithTheCorrectLocationIdWhenTheRequestIsAboutLoadingPagesGreaterThanTheFirst() {
        val location = LABEL
        val labelId = "customLabelIdi2386"
        val uuid = "9238h82388sdfa8sdf8asd3234"
        // Represents pagination. Only messages older than the given timestamp will be returned
        val oldestMessageTimestamp = 1323L
        val userId = Id("userId1")
        every { userManager.requireCurrentUserId() } returns userId
        every { conversationModeEnabled(location) } returns true

        viewModel.loadMailboxItems(
            location,
            labelId,
            false,
            uuid,
            false,
            oldestMessageTimestamp
        )

        verify { getConversations.loadMore(userId, labelId, oldestMessageTimestamp) }
    }

    @Test
    fun getMailboxItemsReturnsMailboxItemsLiveDataMappedFromMessageDetailsRepositoryWhenFetchingSubsequentPages() {
        val message = Message(
            messageId = "messageId92384823",
            sender = MessageSender("senderName1", "sender@pm.me"),
            subject = "subject12834"
        )
        val location = LABEL_FOLDER
        val labelId = "folderIdi2384"
        val includeLabels = false
        val uuid = "9238h82388sdfa8sdf8asd3hh2283"
        val refreshMessages = false
        // Represents pagination. Only messages older than the given timestamp will be returned
        coEvery { messageDetailsRepository.getMessagesByLabelIdAsync(labelId) } returns liveData {
            emit(listOf(message))
        }

        val actual = viewModel.getMailboxItems(
            location,
            labelId,
            includeLabels,
            uuid,
            refreshMessages
        ).testObserver()

        val expected = listOf(
            fakeMailboxUiData("messageId92384823", "senderName1", "subject12834")
        )
        assertEquals(expected, actual.observedValues.first()!!.items)
    }

    @Test
    fun getMailboxItemsCallsGetConversationsUseCaseWhenConversationModeIsEnabled() = runBlockingTest {
        val location = LABEL
        val labelId = "labelId923842"
        val includeLabels = true
        val uuid = "9238423bbe2h3423489wssdf"
        val refreshMessages = false
        every { conversationModeEnabled(location) } returns true

        viewModel.getMailboxItems(
            location,
            labelId,
            includeLabels,
            uuid,
            refreshMessages
        )

        coVerifySequence { getConversations(currentUserId, labelId) }
        verify { messageServiceScheduler wasNot Called }
    }

    @Test
    fun getMailboxItemsReturnsMailboxItemsMappedFromConversationsWhenGetConversationsUseCaseSucceeds() =
        runBlockingTest {
            val location = INBOX
            val locationId = INBOX.messageLocationTypeValue.toString()
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
            val successResult = GetConversationsResult.Success(listOf(conversation))
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(currentUserId, locationId) } returns flowOf(successResult)

            val actual = viewModel.getMailboxItems(
                location,
                null,
                true,
                "9238423bbe2h3423489wssdf",
                false,
            ).testObserver()

            val expected = listOf(
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
                    recipients = ""
                )
            )
            val expectedState = MailboxState(expected, "", false)
            assertEquals(expectedState, actual.observedValues.first())
        }

    @Test
    fun getMailboxItemsMapsConversationsSendersUsingContactNameOrSenderNameOrEmailInThisPreferenceOrder() =
        runBlockingTest {
            val location = INBOX
            val locationId = INBOX.messageLocationTypeValue.toString()
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
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(currentUserId, locationId) } returns flowOf(successResult)
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )

            val actual = viewModel.getMailboxItems(
                location,
                null,
                true,
                "9238423bbe2h3423489wssdf",
                false,
            ).testObserver()

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
                    recipients = "recipient, recipient1"
                )
            )
            assertEquals(expected, actual.observedValues.first()!!.items)
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
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(currentUserId, labelId) } returns flowOf(successResult)
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )

            val actual = viewModel.getMailboxItems(
                location,
                labelId,
                true,
                "9238423bbe2h3423489wssdf",
                false,
            ).testObserver()

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
                    labels = listOf(LabelChipUiModel(Id("10"), Name("label 10"), null)),
                    recipients = ""
                )
            )
            assertEquals(expected, actual.observedValues.first()!!.items)
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
            val labelId = "labelId923843"
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(currentUserId, labelId) } returns flowOf(successResult)
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )

            val actual = viewModel.getMailboxItems(
                location,
                labelId,
                true,
                "9238423bbe4h3423489wssdf",
                false,
            ).testObserver()

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
                    recipients = ""
                )
            )
            assertEquals(expected, actual.observedValues.first()!!.items)
        }

    @Test
    fun getMailboxItemsMapsLastMessageTimeMsToTheContextTimeOfTheLabelRepresentingTheCurrentLocationConvertedToMs() =
        runBlockingTest {
            val location = INBOX
            val locationId = INBOX.messageLocationTypeValue.toString()
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
                    LabelContext(inboxLocationId, 0, 0, 1617982194, 0, 0),
                    LabelContext(archiveLocationId, 0, 0, 0, 0, 0)
                ),
                null
            )
            val successResult = GetConversationsResult.Success(listOf(conversation))
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(currentUserId, locationId) } returns flowOf(successResult)
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )

            val actual = viewModel.getMailboxItems(
                location,
                null,
                true,
                "9238423bbe4h3423489wssdf1",
                false,
            ).testObserver()

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
                        LabelChipUiModel(Id("0"), Name("label 0"), null),
                        LabelChipUiModel(Id("6"), Name("label 6"), null)
                    ),
                    recipients = ""
                )
            )
            assertEquals(expected, actual.observedValues.first()!!.items)
        }

    @Test
    fun getMailboxItemsMapsLastMessageTimeMsToTheContextTimeOfTheLabelRepresentingTheCurrentCustomFolderConvertedToMs() =
        runBlockingTest {
            val location = LABEL
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
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(currentUserId, customLabelId) } returns flowOf(successResult)
            coEvery { contactsRepository.findAllContactEmails() } returns flowOf(
                listOf(ContactEmail("firstContactId", "firstsender@protonmail.com", "firstContactName"))
            )

            val actual = viewModel.getMailboxItems(
                location,
                customLabelId,
                true,
                "9238423bbe4h3423489wssdf2",
                false,
            ).testObserver()

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
                    labels = listOf(LabelChipUiModel(Id("6"), Name("label 6"), null)),
                    recipients = ""
                )
            )
            assertEquals(expected, actual.observedValues.first()!!.items)
        }

    @Test
    fun getMailboxItemsReturnsMailboxStateWithErrorWhenGetConversationsUseCaseReturnsError() =
        runBlockingTest {
            val location = LABEL
            val labelId = "labelId923844"
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(currentUserId, labelId) } returns flowOf(GetConversationsResult.Error)

            val actual = viewModel.getMailboxItems(
                location,
                labelId,
                true,
                "9238423bbe4h3423489wssdf1",
                false,
            ).testObserver()

            val actualItems = actual.observedValues.first()!!.items
            val actualError = actual.observedValues.first()!!.error
            assertEquals(emptyList(), actualItems)
            assertEquals("Failed getting conversations", actualError)
        }

    @Test
    fun getMailboxItemsReturnsStateWithNoMoreItemsWhenGetConversationsUseCaseReturnsNoConversationsFound() =
        runBlockingTest {
            val location = LABEL_FOLDER
            val labelId = "labelId923844"
            every { conversationModeEnabled(location) } returns true
            coEvery { getConversations(currentUserId, labelId) } returns flowOf(
                GetConversationsResult.NoConversationsFound
            )

            val actual = viewModel.getMailboxItems(
                location,
                labelId,
                true,
                "9238423bbe4h3423489wssdf1",
                false,
            ).testObserver()

            val actualItems = actual.observedValues.first()!!.items
            val noMoreResults = actual.observedValues.first()!!.noMoreItems
            assertEquals(emptyList(), actualItems)
            assertEquals(true, noMoreResults)
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
        recipients = ""
    )

}
