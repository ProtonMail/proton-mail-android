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
package ch.protonmail.android.api.segments.event

import android.content.Context
import android.database.sqlite.SQLiteBlobTooBigException
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.models.enumerations.MessageFlag
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.segments.RESPONSE_CODE_INVALID_ID
import ch.protonmail.android.api.segments.RESPONSE_CODE_MESSAGE_DOES_NOT_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_MESSAGE_READING_RESTRICTED
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.details.data.MessageFlagsToEncryptionMapper
import ch.protonmail.android.event.data.remote.model.EventResponse
import ch.protonmail.android.event.domain.model.ActionType
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.mapper.LabelEntityDomainMapper
import ch.protonmail.android.labels.data.mapper.LabelEventApiMapper
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.data.local.UnreadCounterDao
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity.Type
import ch.protonmail.android.mailbox.data.mapper.ApiToDatabaseUnreadCounterMapper
import ch.protonmail.android.mailbox.data.remote.model.CountsApiModel
import ch.protonmail.android.mailbox.domain.HandleChangeToConversations
import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.usecase.fetch.LaunchInitialDataFetch
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.worker.FetchContactsDataWorker
import ch.protonmail.android.worker.FetchContactsEmailsWorker
import ch.protonmail.android.worker.FetchMailSettingsWorker
import ch.protonmail.android.worker.FetchUserAddressesWorker
import ch.protonmail.android.worker.FetchUserWorker
import com.google.gson.JsonSyntaxException
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import me.proton.core.usersettings.domain.usecase.GetUserSettings
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.toBoolean
import timber.log.Timber

class EventHandler @AssistedInject constructor(
    private val context: Context,
    private val protonMailApiManager: ProtonMailApiManager,
    private val unreadCounterDao: UnreadCounterDao,
    private val apiToDatabaseUnreadCounterMapper: ApiToDatabaseUnreadCounterMapper,
    private val userManager: UserManager,
    messageDetailsRepositoryFactory: MessageDetailsRepository.AssistedFactory,
    private val changeToConversations: HandleChangeToConversations,
    private val fetchContactEmails: FetchContactsEmailsWorker.Enqueuer,
    private val fetchContactsData: FetchContactsDataWorker.Enqueuer,
    private val fetchUserWorkerEnqueuer: FetchUserWorker.Enqueuer,
    private val fetchUserAddressesWorkerEnqueuer: FetchUserAddressesWorker.Enqueuer,
    private val fetchMailSettingsWorker: FetchMailSettingsWorker.Enqueuer,
    databaseProvider: DatabaseProvider,
    private val launchInitialDataFetch: LaunchInitialDataFetch,
    private val messageFactory: MessageFactory,
    @Assisted val userId: UserId,
    private val externalScope: CoroutineScope,
    private val messageFlagsToEncryptionMapper: MessageFlagsToEncryptionMapper,
    private val labelRepository: LabelRepository,
    private val labelEventApiMapper: LabelEventApiMapper,
    private val getUserSettings: GetUserSettings
) {

    private val messageDetailsRepository = messageDetailsRepositoryFactory.create(userId)
    private val contactDao = databaseProvider.provideContactDao(userId)
    private val messageDao = databaseProvider.provideMessageDao(userId)
    private val pendingActionDao = databaseProvider.providePendingActionDao(userId)

    @AssistedInject.Factory
    interface AssistedFactory {
        fun create(userId: UserId): EventHandler
    }

    private val stagedMessages = HashMap<String, Message>()

    fun handleRefreshContacts() {
        contactDao.run {
            clearContactDataCache()
            clearContactEmailsCache()
        }
        fetchContactEmails.enqueue()
        fetchContactsData.enqueue()
        externalScope.launch {
            labelRepository.deleteContactGroups(userId)
        }
    }

    /**
     * We should only return after the data has been refreshed so the eventmanager knows we are in
     * the correct state. We can do api requests here, because our data already has been invalidated
     * anyway.
     */
    fun handleRefresh(userId: UserId) {
        messageDao.run {
            clearMessagesCache()
            clearAttachmentsCache()
        }
        launchInitialDataFetch(
            userId,
            shouldRefreshDetails = false,
            shouldRefreshContacts = false
        )
        fetchUserWorkerEnqueuer(userId)
    }

    /**
     * Does all the pre-processing which does not change the database state
     * @return Whether the staging process was successful or not
     */
    fun stage(messages: MutableList<EventResponse.MessageEventBody>?): Boolean {
        if (!messages.isNullOrEmpty()) {
            return stageMessagesUpdates(messages)
        }
        return true
    }

    private fun stageMessagesUpdates(events: List<EventResponse.MessageEventBody>): Boolean {
        for (event in events) {
            val messageID = event.messageID
            val type = ActionType.fromInt(event.type)

            if ((type != ActionType.UPDATE && type != ActionType.UPDATE_FLAGS) ||
                checkPendingForSending(pendingActionDao, messageID)
            ) {
                continue
            }
            if (type == ActionType.UPDATE_FLAGS) {
                stagedMessages[messageID] = messageFactory.createMessage(event.message)
            }

            if (type == ActionType.UPDATE) {
                val messageResponse = protonMailApiManager.fetchMessageDetailsBlocking(messageID, UserIdTag(userId))
                val isMessageStaged = if (messageResponse == null) {
                    // If the response is null, an exception has been thrown while fetching message details
                    // Return false and with that terminate processing this event any further
                    // We'll try to process the same event again next time
                    false
                } else {
                    // If the response is not null, check the response code and act accordingly
                    when (messageResponse.code) {
                        Constants.RESPONSE_CODE_OK -> {
                            stagedMessages[messageID] = messageResponse.message
                        }
                        RESPONSE_CODE_INVALID_ID,
                        RESPONSE_CODE_MESSAGE_DOES_NOT_EXIST,
                        RESPONSE_CODE_MESSAGE_READING_RESTRICTED -> {
                            Timber.e("Error when fetching message: ${messageResponse.error}")
                        }
                        else -> {
                            Timber.e("Error when fetching message: ${messageResponse.error}")
                        }
                    }
                    true
                }

                Timber.d("isMessageStaged: $isMessageStaged, messages size: ${stagedMessages.size}")
                if (!isMessageStaged) {
                    return false
                }
            }
        }

        return true
    }

    fun write(response: EventResponse) {
        unsafeWrite(contactDao, messageDao, pendingActionDao, response)
    }

    private fun eventMessageSortSelector(message: EventResponse.MessageEventBody): Int = message.type

    /**
     * NOTE we should not do api requests here because we are in a transaction
     */
    private fun unsafeWrite(
        contactDao: ContactDao,
        messageDao: MessageDao,
        pendingActionDao: PendingActionDao,
        response: EventResponse
    ) {

        val savedUser = runBlocking { userManager.getLegacyUser(userId) }

        if (response.usedSpace > 0) {
            savedUser.setAndSaveUsedSpace(response.usedSpace)
        }

        val messages = response.messageUpdates
        val conversations = response.conversationUpdates
        val contacts = response.contactUpdates
        val contactsEmails = response.contactEmailsUpdates

        val user = response.userUpdates
        val userSettings = response.userSettingsUpdates

        val mailSettings = response.mailSettingsUpdates
        val labels = response.labelUpdates
        val messageCounts = response.messageCounts
        val conversationCounts = response.conversationCounts
        val addresses = response.addresses

        if (labels != null) {
            writeLabelsUpdates(labels)
        }
        if (messages != null) {
            messages.sortByDescending { eventMessageSortSelector(it) }
            writeMessagesUpdates(messageDao, pendingActionDao, messages)
        }
        if (conversations != null) {
            externalScope.launch {
                changeToConversations.invoke(userId, conversations)
            }
        }
        if (contacts != null) {
            writeContactsUpdates(contactDao, contacts)
        }
        if (contactsEmails != null) {
            writeContactEmailsUpdates(contactDao, contactsEmails)
        }
        if (mailSettings != null) {
            writeMailSettings(context, mailSettings)
            fetchMailSettingsWorker.enqueue()
        }
        if (userSettings != null) {
            // Core is the source of truth. Workaround: Force refresh Core.
            externalScope.launch {
                getUserSettings(userId, refresh = true)
            }
        }
        if (user != null) {
            // Core is the source of truth. Workaround: Force refresh Core.
            fetchUserWorkerEnqueuer(userId)
        }
        if (addresses != null) {
            // Core is the source of truth. Workaround: Force refresh Core.
            fetchUserAddressesWorkerEnqueuer(userId)
        }
        if (messageCounts != null) {
            writeUnreadCountersUpdates(messageCounts, Type.MESSAGES)
        }
        if (conversationCounts != null) {
            writeUnreadCountersUpdates(conversationCounts, Type.CONVERSATIONS)
        }
    }


    private fun writeMailSettings(context: Context, mSettings: MailSettings) {
        var mailSettings: MailSettings? = mSettings
        if (mailSettings == null) {
            mailSettings = MailSettings()
        }
        mailSettings.showImagesFrom = mSettings.showImagesFrom
        mailSettings.autoSaveContacts = mSettings.autoSaveContacts
        mailSettings.sign = mSettings.sign
        mailSettings.pgpScheme = mSettings.pgpScheme
        mailSettings.setAttachPublicKey(if (mSettings.getAttachPublicKey()) 1 else 0)

        val prefs = SecureSharedPreferences.getPrefsForUser(context, userId)
        mailSettings.saveBlocking(prefs)

    }

    private fun writeMessagesUpdates(
        messageDao: MessageDao,
        pendingActionDao: PendingActionDao,
        events: List<EventResponse.MessageEventBody>
    ) {
        events.forEach { writeMessageUpdate(it, pendingActionDao, messageDao) }
    }

    private fun writeMessageUpdate(
        event: EventResponse.MessageEventBody,
        pendingActionDao: PendingActionDao,
        messageDao: MessageDao
    ) {
        val messageId = event.messageID
        val type = ActionType.fromInt(event.type)
        if (type != ActionType.DELETE && checkPendingForSending(pendingActionDao, messageId)) {
            return
        }
        Timber.v("Update message type: $type Id: $messageId")
        when (type) {
            ActionType.CREATE -> {
                try {
                    val savedMessage = messageDetailsRepository.findMessageByIdBlocking(messageId)
                    if (savedMessage == null) {
                        messageDetailsRepository.saveMessageBlocking(messageFactory.createMessage(event.message))
                    } else {
                        updateMessageFlags(messageId, event)
                    }
                } catch (syntaxException: JsonSyntaxException) {
                    Timber.w(syntaxException, "unable to create Message object")
                }
            }

            ActionType.DELETE -> {
                val message = messageDetailsRepository.findMessageByIdBlocking(messageId)
                if (message != null) {
                    externalScope.launch {
                        messageDetailsRepository.deleteAllAttachments(message.attachments)
                    }
                    messageDao.deleteMessage(message)

                }
            }

            ActionType.UPDATE -> {
                // update Message body
                val message = messageDetailsRepository.findMessageByIdBlocking(messageId)
                stagedMessages[messageId]?.let { messageUpdate ->
                    val dbTime = message?.time ?: 0
                    val serverTime = messageUpdate.time

                    if (message != null) {
                        message.attachments = messageUpdate.attachments
                    }

                    if (serverTime > dbTime && message != null && messageUpdate.messageBody != null) {
                        message.messageBody = messageUpdate.messageBody
                        messageDetailsRepository.saveMessageBlocking(message)
                    }

                    Timber.v("Message Id: $messageId processed, staged size:${stagedMessages.size}")
                    stagedMessages.remove(messageId)
                }

                updateMessageFlags(messageId, event)
            }

            ActionType.UPDATE_FLAGS -> {
                updateMessageFlags(messageId, event)
            }
            ActionType.UNKNOWN -> {
                Timber.i("Unsupported Action type: ${event.type} received")
            }
        }
        return
    }

    private fun updateMessageFlags(
        messageId: String,
        item: EventResponse.MessageEventBody
    ) {
        val message = messageDetailsRepository.findMessageByIdBlocking(messageId)
        val newMessage = item.message
        Timber.v("Update flags message id: $messageId, time: ${message?.time} staged size:${stagedMessages.size}")
        if (message != null) {

            if (newMessage.Subject != null) {
                message.subject = newMessage.Subject
            }
            if (newMessage.Unread >= 0) {
                message.Unread = newMessage.Unread > 0
            }
            val sender = newMessage.Sender
            if (sender != null) {
                message.sender = MessageSender(sender.name, sender.address, sender.isProton.toBoolean())
            }
            val toList = newMessage.ToList
            if (toList != null) {
                message.toList = toList
            }
            if (newMessage.time > 0) {
                message.time = newMessage.time
            }
            if (newMessage.Size > 0) {
                message.totalSize = newMessage.Size
            }
            if (newMessage.NumAttachments > 0) {
                message.numAttachments = newMessage.NumAttachments
            }
            var expired = false
            if (newMessage.ExpirationTime != -1L) {
                message.expirationTime = newMessage.ExpirationTime
                if (message.expirationTime == 1L) {
                    expired = true
                }
            }
            if (newMessage.flags > 0) {
                message.isReplied = newMessage.flags and MessageFlag.REPLIED.flagValue == MessageFlag.REPLIED.flagValue
                message.isRepliedAll =
                    newMessage.flags and MessageFlag.REPLIED_ALL.flagValue == MessageFlag.REPLIED_ALL.flagValue
                message.isForwarded =
                    newMessage.flags and MessageFlag.FORWARDED.flagValue == MessageFlag.FORWARDED.flagValue

                message.Type = MessageUtils.calculateType(newMessage.flags)
                message.messageEncryption = messageFlagsToEncryptionMapper.flagsToMessageEncryption(newMessage.flags)
            }
            if (newMessage.AddressID != null) {
                message.addressID = newMessage.AddressID
            }
            val ccList = newMessage.CCList
            if (ccList != null) {
                message.ccList = ccList
            }
            val bccList = newMessage.BCCList
            if (bccList != null) {
                message.bccList = bccList
            }
            if (newMessage.LabelIDs != null) {
                message.setLabelIDs(newMessage.LabelIDs)
            }
            var locationPotentiallyChanged = false
            val eventLabelsAdded = newMessage.LabelIDsAdded
            if (eventLabelsAdded != null) {
                message.addLabels(eventLabelsAdded)
                locationPotentiallyChanged = true
            }
            val eventLabelsRemoved = newMessage.LabelIDsRemoved
            if (eventLabelsRemoved != null) {
                message.removeLabels(eventLabelsRemoved)
                locationPotentiallyChanged = true
            }
            if (locationPotentiallyChanged) {
                message.calculateLocation()
                message.setFolderLocation(labelRepository)
            }
            if (expired) {
                externalScope.launch {
                    messageDetailsRepository.deleteAllAttachments(message.attachments)
                }
                messageDetailsRepository.deleteMessage(message)
            } else {
                messageDetailsRepository.saveMessageBlocking(message)
            }
        } else {
            stagedMessages[messageId]?.let {
                messageDetailsRepository.saveMessageBlocking(it)
            }
        }
        stagedMessages.remove(messageId)
    }

    private fun checkPendingForSending(pendingActionDao: PendingActionDao, messageId: String): Boolean {
        var pendingForSending = pendingActionDao.findPendingSendByMessageIdBlocking(messageId)
        if (pendingForSending != null) {
            return true
        }
        pendingForSending = pendingActionDao.findPendingSendByOfflineMessageId(messageId)
        return pendingForSending != null
    }

    private fun writeContactsUpdates(
        contactDao: ContactDao,
        events: List<EventResponse.ContactEventBody>
    ) {
        for (event in events) {
            Timber.v("New contacts event type: ${event.type} id: ${event.contactID}")
            when (ActionType.fromInt(event.type)) {
                ActionType.CREATE -> {
                    val contact = event.contact

                    val contactId = contact.contactId
                    val contactName = contact.name
                    val contactData = ContactData(contactId, contactName!!)
                    contactDao.saveContactDataBlocking(contactData)
                    contactDao.insertFullContactDetailsBlocking(contact)
                }

                ActionType.UPDATE -> {
                    val fullContact = event.contact
                    val contactId = fullContact.contactId
                    val contactData = contactDao.findContactDataByIdBlocking(contactId)
                    if (contactData != null) {
                        contactData.name = event.contact.name!!
                        contactDao.saveContactDataBlocking(contactData)
                    }

                    val localFullContact = try {
                        contactDao.findFullContactDetailsByIdBlocking(contactId)
                    } catch (tooBigException: SQLiteBlobTooBigException) {
                        Timber.i(tooBigException, "Data too big to be fetched")
                        null
                    }
                    if (localFullContact != null) {
                        contactDao.deleteFullContactsDetails(localFullContact)
                    }
                    contactDao.insertFullContactDetailsBlocking(fullContact)
                }

                ActionType.DELETE -> {
                    val contactId = event.contactID
                    val contactData = contactDao.findContactDataByIdBlocking(contactId)
                    if (contactData != null) {
                        contactDao.deleteContactData(contactData)
                    }
                }

                ActionType.UPDATE_FLAGS -> {
                }
                ActionType.UNKNOWN -> {
                    Timber.i("Unsupported Action type: ${event.type} received")
                }
            }
        }
    }

    private fun writeContactEmailsUpdates(
        contactDao: ContactDao,
        events: List<EventResponse.ContactEmailEventBody>
    ) {
        for (event in events) {
            Timber.v("New contacts emails event type: ${event.type} id: ${event.contactID}")
            when (ActionType.fromInt(event.type)) {
                ActionType.CREATE,
                ActionType.UPDATE -> externalScope.launch {
                    val contactEmail = event.contactEmail
                    // save or replace any existing contact
                    contactDao.saveContactEmail(contactEmail)
                }
                ActionType.DELETE -> externalScope.launch {
                    val contactId = event.contactID
                    val contactEmail = contactDao.findContactEmailById(contactId)
                    if (contactEmail != null) {
                        Timber.v("Delete contact id: $contactId")
                        contactDao.deleteContactEmail(contactEmail)
                    }
                }

                ActionType.UPDATE_FLAGS,
                ActionType.UNKNOWN -> {
                    Timber.i("Unsupported Action type: ${event.type} received")
                }
            }
        }
    }

    private fun writeLabelsUpdates(
        events: List<EventResponse.LabelsEventBody>
    ) {
        for (event in events) {
            val item = event.label
            when (ActionType.fromInt(event.type)) {
                ActionType.CREATE -> {
                    val label = Label(
                        id = LabelId(item.id),
                        name = item.name,
                        color = item.color,
                        order = item.order ?: 0,
                        type = requireNotNull(LabelType.fromIntOrNull(item.type)),
                        path = item.path,
                        parentId = item.parentId ?: EMPTY_STRING,
                    )
                    externalScope.launch {
                        labelRepository.saveLabel(label, userId)
                    }
                }

                ActionType.UPDATE -> externalScope.launch {
                    val label = labelRepository.findLabel(LabelId(item.id))
                    writeLabel(label, labelEventApiMapper.toApiModel(item))
                }

                ActionType.DELETE -> {
                    val labelId = event.id
                    externalScope.launch {
                        labelRepository.deleteLabel(LabelId(labelId))
                    }
                }

                ActionType.UPDATE_FLAGS,
                ActionType.UNKNOWN -> {
                    Timber.i("Unsupported Action type: ${event.type} received")
                }
            }
        }
    }

    private fun writeUnreadCountersUpdates(messageCounts: List<CountsApiModel>, type: Type) {
        val databaseModels = apiToDatabaseUnreadCounterMapper.toDatabaseModels(messageCounts, userId, type)
        runBlocking {
            unreadCounterDao.insertOrUpdate(databaseModels)
        }
    }

    private fun writeLabel(
        currentLabel: Label?,
        updatedLabel: LabelApiModel
    ) {
        if (currentLabel != null) {
            val mapper = LabelEntityApiMapper()
            val domainMapper = LabelEntityDomainMapper()
            val labelToSave = mapper.toEntity(updatedLabel, userId)
            externalScope.launch {
                labelRepository.saveLabel(domainMapper.toLabel(labelToSave), userId)
            }
        }
    }
}
