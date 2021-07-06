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
package ch.protonmail.android.api.segments.event

import android.content.Context
import android.database.sqlite.SQLiteBlobTooBigException
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.EventResponse
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.models.MessageCount
import ch.protonmail.android.api.models.UnreadTotalMessagesResponse
import ch.protonmail.android.api.models.contacts.receive.ContactLabelFactory
import ch.protonmail.android.api.models.enumerations.MessageFlag
import ch.protonmail.android.api.models.messages.receive.LabelFactory
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.ServerLabel
import ch.protonmail.android.api.segments.RESPONSE_CODE_INVALID_ID
import ch.protonmail.android.api.segments.RESPONSE_CODE_MESSAGE_DOES_NOT_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_MESSAGE_READING_RESTRICTED
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.PendingActionDao
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.ContactLabel
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.events.MessageCountsEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.usecase.fetch.LaunchInitialDataFetch
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.worker.FetchContactsDataWorker
import ch.protonmail.android.worker.FetchContactsEmailsWorker
import ch.protonmail.android.worker.FetchUserAddressesWorker
import ch.protonmail.android.worker.FetchUserWorker
import com.google.gson.JsonSyntaxException
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class EventHandler @AssistedInject constructor(
    private val context: Context,
    private val protonMailApiManager: ProtonMailApiManager,
    private val userManager: UserManager,
    messageDetailsRepositoryFactory: MessageDetailsRepository.AssistedFactory,
    private val fetchContactEmails: FetchContactsEmailsWorker.Enqueuer,
    private val fetchContactsData: FetchContactsDataWorker.Enqueuer,
    private val fetchUserWorkerEnqueuer: FetchUserWorker.Enqueuer,
    private val fetchUserAddressesWorkerEnqueuer: FetchUserAddressesWorker.Enqueuer,
    databaseProvider: DatabaseProvider,
    private val launchInitialDataFetch: LaunchInitialDataFetch,
    private val messageFactory: MessageFactory,
    @Assisted val userId: Id
) {

    private val messageDetailsRepository = messageDetailsRepositoryFactory.create(userId)
    private val contactDao = databaseProvider.provideContactDao(userId)
    private val counterDao = databaseProvider.provideCounterDao(userId)
    private val messageDao = databaseProvider.provideMessageDao(userId)
    private val pendingActionDao = databaseProvider.providePendingActionDao(userId)

    @AssistedInject.Factory
    interface AssistedFactory {
        fun create(userId: Id): EventHandler
    }

    private val stagedMessages = HashMap<String, Message>()

    init {
        messageDetailsRepository.reloadDependenciesForUser(userId)
    }

    fun handleRefreshContacts() {
        contactDao.run {
            clearContactDataCache()
            clearContactEmailsLabelsJoin()
            clearContactEmailsCache()
            clearContactGroupsLabelsTableBlocking()
        }
        fetchContactEmails.enqueue()
        fetchContactsData.enqueue()
    }

    /**
     * We should only return after the data has been refreshed so the eventmanager knows we are in
     * the correct state. We can do api requests here, because our data already has been invalidated
     * anyway.
     */
    fun handleRefresh() {
        messageDao.run {
            clearMessagesCache()
            clearAttachmentsCache()
            clearLabelsCache()
        }
        counterDao.run {
            clearUnreadLocationsTable()
            clearUnreadLabelsTable()
            clearTotalLocationsTable()
            clearTotalLabelsTable()
        }
        launchInitialDataFetch(
            userId,
            shouldRefreshDetails = false,
            shouldRefreshContacts = false
        )
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
            val type = EventType.fromInt(event.type)

            if (type != EventType.UPDATE && type != EventType.UPDATE_FLAGS) {
                continue
            }

            if (checkPendingForSending(pendingActionDao, messageID)) {
                continue
            }

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
                        Timber.e("Error when fetching message")
                    }
                }
                true
            }

            Timber.d("isMessageStaged: $isMessageStaged, messages size: ${stagedMessages.size}")
            if (!isMessageStaged) {
                return false
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
        val contacts = response.contactUpdates
        val contactsEmails = response.contactEmailsUpdates

        val user = response.userUpdates

        val mailSettings = response.mailSettingsUpdates
        val labels = response.labelUpdates
        val counts = response.messageCounts
        val addresses = response.addresses

        if (labels != null) {
            writeLabelsUpdates(messageDao, contactDao, labels)
        }
        if (messages != null) {
            messages.sortByDescending { eventMessageSortSelector(it) }
            writeMessagesUpdates(messageDao, pendingActionDao, messages)
        }
        if (contacts != null) {
            writeContactsUpdates(contactDao, contacts)
        }
        if (contactsEmails != null) {
            writeContactEmailsUpdates(contactDao, contactsEmails)
        }
        if (mailSettings != null) {
            writeMailSettings(context, mailSettings)
        }
        if (user != null) {
            // Core is the source of truth. Workaround: Force refresh Core.
            fetchUserWorkerEnqueuer(userId)
        }
        if (addresses != null) {
            // Core is the source of truth. Workaround: Force refresh Core.
            fetchUserAddressesWorkerEnqueuer(userId)
        }
        if (counts != null) {
            writeUnreadUpdates(counts)
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
        val type = EventType.fromInt(event.type)
        if (type != EventType.DELETE && checkPendingForSending(pendingActionDao, messageId)) {
            return
        }
        Timber.v("Update message type: $type Id: $messageId")
        when (type) {
            EventType.CREATE -> {
                try {
                    val savedMessage = messageDetailsRepository.findMessageByIdBlocking(messageId)
                    if (savedMessage == null) {
                        messageDetailsRepository.saveMessageBlocking(messageFactory.createMessage(event.message))
                    } else {
                        updateMessageFlags(messageDao, messageId, event)
                    }
                } catch (syntaxException: JsonSyntaxException) {
                    Timber.w(syntaxException, "unable to create Message object")
                }
            }

            EventType.DELETE -> {
                val message = messageDetailsRepository.findMessageByIdBlocking(messageId)
                if (message != null) {
                    messageDao.deleteMessage(message)
                }
            }

            EventType.UPDATE -> {
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

                updateMessageFlags(messageDao, messageId, event)
            }

            EventType.UPDATE_FLAGS -> {
                updateMessageFlags(messageDao, messageId, event)
            }
        }
        return
    }

    private fun updateMessageFlags(
        messageDao: MessageDao,
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
                message.sender = MessageSender(sender.name, sender.address)
            }
            val toList = newMessage.ToList
            if (toList != null) {
                message.toList = toList
            }
            if (newMessage.Time > 0) {
                message.time = newMessage.Time
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
            if (newMessage.Flags > 0) {
                message.isReplied = newMessage.Flags and MessageFlag.REPLIED.value == MessageFlag.REPLIED.value
                message.isRepliedAll =
                    newMessage.Flags and MessageFlag.REPLIED_ALL.value == MessageFlag.REPLIED_ALL.value
                message.isForwarded = newMessage.Flags and MessageFlag.FORWARDED.value == MessageFlag.FORWARDED.value

                message.Type = MessageUtils.calculateType(newMessage.Flags)
                message.setIsEncrypted(MessageUtils.calculateEncryption(newMessage.Flags))
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
                message.setFolderLocation(messageDao)
            }
            if (expired) {
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
        var pendingForSending = pendingActionDao.findPendingSendByMessageId(messageId)
        if (pendingForSending != null) {
            return true
        }
        pendingForSending = pendingActionDao.findPendingSendByOfflineMessageId(messageId)
        return pendingForSending != null
    }

    private fun writeContactsUpdates(contactDao: ContactDao, events: List<EventResponse.ContactEventBody>) {
        for (event in events) {
            Timber.v("New contacts event type: ${event.type} id: ${event.contactID}")
            when (EventType.fromInt(event.type)) {
                EventType.CREATE -> {
                    val contact = event.contact

                    val contactId = contact.contactId
                    val contactName = contact.name
                    val contactData = ContactData(contactId, contactName!!)
                    contactDao.saveContactData(contactData)
                    contactDao.insertFullContactDetailsBlocking(contact)
                }

                EventType.UPDATE -> {
                    val fullContact = event.contact
                    val contactId = fullContact.contactId
                    val contactData = contactDao.findContactDataById(contactId)
                    if (contactData != null) {
                        contactData.name = event.contact.name!!
                        contactDao.saveContactData(contactData)
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

                EventType.DELETE -> {
                    val contactId = event.contactID
                    val contactData = contactDao.findContactDataById(contactId)
                    if (contactData != null) {
                        contactDao.deleteContactData(contactData)
                    }
                }

                EventType.UPDATE_FLAGS -> {
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
            when (EventType.fromInt(event.type)) {
                EventType.CREATE -> {
                    val contactEmail = event.contactEmail
                    val contactId = event.contactEmail.contactEmailId
                    // get current contact email saved in local DB
                    val oldContactEmail = contactDao.findContactEmailById(contactId)
                    if (oldContactEmail != null) {
                        val contactEmailId = oldContactEmail.contactEmailId
                        val joins = contactDao.fetchJoinsByEmail(contactEmailId).toMutableList()
                        contactDao.saveContactEmail(contactEmail)
                        contactDao.saveContactEmailContactLabelBlocking(joins)
                    } else {
                        contactDao.saveContactEmail(contactEmail)
                        val newJoins = mutableListOf<ContactEmailContactLabelJoin>()
                        contactEmail.labelIds?.forEach { labelId ->
                            newJoins.add(ContactEmailContactLabelJoin(contactEmail.contactEmailId, labelId))
                        }
                        Timber.v("Create new email contact: ${contactEmail.email} newJoins size: ${newJoins.size}")
                        if (newJoins.isNotEmpty()) {
                            contactDao.saveContactEmailContactLabelBlocking(newJoins)
                        }
                    }
                }

                EventType.UPDATE -> {
                    val contactId = event.contactEmail.contactEmailId
                    // get current contact email saved in local DB
                    val oldContactEmail = contactDao.findContactEmailById(contactId)
                    Timber.v("Update contact id: $contactId oldContactEmail: ${oldContactEmail?.email}")
                    if (oldContactEmail != null) {
                        val updatedContactEmail = event.contactEmail
                        val labelIds = updatedContactEmail.labelIds ?: ArrayList()
                        val contactEmailId = updatedContactEmail.contactEmailId
                        contactEmailId.let {
                            contactDao.saveContactEmail(updatedContactEmail)
                            val joins = contactDao.fetchJoinsByEmail(contactEmailId).toMutableList()
                            for (labelId in labelIds) {
                                joins.add(ContactEmailContactLabelJoin(contactEmailId, labelId))
                            }
                            if (joins.isNotEmpty()) {
                                contactDao.saveContactEmailContactLabelBlocking(joins)
                            }
                        }
                    } else {
                        contactDao.saveContactEmail(event.contactEmail)
                    }
                }

                EventType.DELETE -> {
                    val contactId = event.contactID
                    val contactEmail = contactDao.findContactEmailById(contactId)
                    if (contactEmail != null) {
                        Timber.v("Delete contact id: $contactId")
                        contactDao.deleteContactEmail(contactEmail)
                    }
                }

                EventType.UPDATE_FLAGS -> {
                }
            }
        }
    }

    private fun writeLabelsUpdates(
        messageDao: MessageDao,
        contactDao: ContactDao,
        events: List<EventResponse.LabelsEventBody>
    ) {
        for (event in events) {
            val item = event.label
            when (EventType.fromInt(event.type)) {
                EventType.CREATE -> {
                    val labelType = item.type!!
                    val id = item.ID
                    val name = item.name
                    val color = item.color
                    val display = item.display!!
                    val order = item.order!!
                    val exclusive = item.exclusive!!
                    if (labelType == Constants.LABEL_TYPE_MESSAGE) {
                        val label = Label(id!!, name!!, color!!, display, order, exclusive == 1)
                        messageDao.saveLabel(label)
                    } else if (labelType == Constants.LABEL_TYPE_CONTACT_GROUPS) {
                        val label = ContactLabel(id!!, name!!, color!!, display, order, exclusive == 1)
                        contactDao.saveContactGroupLabel(label)
                    }
                }

                EventType.UPDATE -> {
                    val labelType = item.type!!
                    val labelId = item.ID
                    if (labelType == Constants.LABEL_TYPE_MESSAGE) {
                        val label = messageDao.findLabelById(labelId!!)
                        writeMessageLabel(label, item, messageDao)
                    } else if (labelType == Constants.LABEL_TYPE_CONTACT_GROUPS) {
                        val contactLabel = contactDao.findContactGroupByIdBlocking(labelId!!)
                        writeContactGroup(contactLabel, item, contactDao)
                    }
                }

                EventType.DELETE -> {
                    val labelId = event.id
                    messageDao.deleteLabelById(labelId)
                    contactDao.deleteByContactGroupLabelId(labelId)
                }

                EventType.UPDATE_FLAGS -> {
                }
            }
        }
    }

    private fun writeUnreadUpdates(messageCounts: List<MessageCount>) {
        val response = UnreadTotalMessagesResponse(messageCounts)
        AppUtil.postEventOnUi(MessageCountsEvent(Status.SUCCESS, response))
    }

    private fun writeMessageLabel(currentLabel: Label?, updatedLabel: ServerLabel, messageDao: MessageDao) {
        if (currentLabel != null) {
            val labelFactory = LabelFactory()
            val labelToSave = labelFactory.createDBObjectFromServerObject(updatedLabel)
            messageDao.saveLabel(labelToSave)
        }
    }

    private fun writeContactGroup(
        currentGroup: ContactLabel?,
        updatedGroup: ServerLabel,
        contactDao: ContactDao
    ) {
        if (currentGroup != null) {
            val contactLabelFactory = ContactLabelFactory()
            val labelToSave = contactLabelFactory.createDBObjectFromServerObject(updatedGroup)
            val joins = contactDao.fetchJoinsBlocking(labelToSave.ID)
            contactDao.saveContactGroupLabel(labelToSave)
            contactDao.saveContactEmailContactLabelBlocking(joins)
        }
    }

    private enum class EventType(val eventType: Int) {
        DELETE(0),
        CREATE(1),
        UPDATE(2),
        UPDATE_FLAGS(3);

        companion object {
            fun fromInt(eventType: Int) = values().find { eventType == it.eventType } ?: DELETE
        }
    }
}

enum class EventType(val eventType: Int) {
    DELETE(0),
    CREATE(1),
    UPDATE(2),
    UPDATE_FLAGS(3);

    companion object {
        fun fromInt(eventType: Int): EventType {
            return values().find {
                eventType == it.eventType
            } ?: DELETE
        }
    }
}
