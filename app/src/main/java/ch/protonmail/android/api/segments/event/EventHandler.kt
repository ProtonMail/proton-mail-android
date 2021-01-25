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
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.EventResponse
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.models.MessageCount
import ch.protonmail.android.api.models.UnreadTotalMessagesResponse
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.UserSettings
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.api.models.address.AddressKeyActivationWorker
import ch.protonmail.android.api.models.contacts.receive.ContactLabelFactory
import ch.protonmail.android.api.models.enumerations.MessageFlag
import ch.protonmail.android.api.models.messages.receive.AttachmentFactory
import ch.protonmail.android.api.models.messages.receive.LabelFactory
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.MessageSenderFactory
import ch.protonmail.android.api.models.messages.receive.ServerLabel
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmailContactLabelJoin
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.counters.CountersDao
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessageSender
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase
import ch.protonmail.android.api.segments.RESPONSE_CODE_INVALID_ID
import ch.protonmail.android.api.segments.RESPONSE_CODE_MESSAGE_DOES_NOT_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_MESSAGE_READING_RESTRICTED
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.MessageCountsEvent
import ch.protonmail.android.events.RefreshDrawerEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.events.user.MailSettingsEvent
import ch.protonmail.android.events.user.UserSettingsEvent
import ch.protonmail.android.usecase.fetch.LaunchInitialDataFetch
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.extensions.removeFirst
import ch.protonmail.android.utils.extensions.replaceFirst
import ch.protonmail.android.worker.FetchContactsDataWorker
import ch.protonmail.android.worker.FetchContactsEmailsWorker
import com.google.gson.JsonSyntaxException
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import timber.log.Timber
import javax.inject.Named
import kotlin.collections.set
import kotlin.math.max

class EventHandler @AssistedInject constructor(
    private val context: Context,
    private val protonMailApiManager: ProtonMailApiManager,
    private val userManager: UserManager,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val fetchContactEmails: FetchContactsEmailsWorker.Enqueuer,
    private val fetchContactsData: FetchContactsDataWorker.Enqueuer,
    private val launchInitialDataFetch: LaunchInitialDataFetch,
    private val pendingActionsDao: PendingActionsDao,
    private val contactsDao: ContactsDao,
    private val countersDao: CountersDao,
    @Named("messages") private val messagesDao: MessagesDao,
    @Assisted val username: String
) {

    @AssistedInject.Factory
    interface AssistedFactory {
        fun create(username: String): EventHandler
    }

    private val messageFactory: MessageFactory

    private val stagedMessages = HashMap<String, Message>()

    init {
        val attachmentFactory = AttachmentFactory()
        val messageSenderFactory = MessageSenderFactory()
        messageFactory = MessageFactory(attachmentFactory, messageSenderFactory)
        messageDetailsRepository.reloadDependenciesForUser(username)
    }

    fun handleRefreshContacts() {
        contactsDao.clearContactDataCache()
        contactsDao.clearContactEmailsLabelsJoin()
        contactsDao.clearContactEmailsCache()
        contactsDao.clearContactGroupsLabelsTableBlocking()
        fetchContactEmails.enqueue()
        fetchContactsData.enqueue()
    }

    /**
     * We should only return after the data has been refreshed so the eventmanager knows we are in
     * the correct state. We can do api requests here, because our data already has been invalidated
     * anyway.
     */
    fun handleRefresh() {
        messagesDao.clearMessagesCache()
        messagesDao.clearAttachmentsCache()
        messagesDao.clearLabelsCache()
        countersDao.clearUnreadLocationsTable()
        countersDao.clearUnreadLabelsTable()
        countersDao.clearTotalLocationsTable()
        countersDao.clearTotalLabelsTable()
        launchInitialDataFetch(
            shouldRefreshDetails = false,
            shouldRefreshContacts = false
        )
    }

    /**
     * Does all the pre-processing which does not change the database state
     * @return Whether the staging process was successful or not
     */
    fun stage(messages: MutableList<EventResponse.MessageEventBody>?): Boolean {

        stagedMessages.clear()
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

            if (checkPendingForSending(pendingActionsDao, messageID)) {
                continue
            }

            val messageResponse = protonMailApiManager.messageDetail(messageID, RetrofitTag(username))
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
        unsafeWrite(contactsDao, messagesDao, pendingActionsDao, response)
    }

    private fun eventMessageSortSelector(message: EventResponse.MessageEventBody): Int = message.type

    /**
     * NOTE we should not do api requests here because we are in a transaction
     */
    private fun unsafeWrite(
        contactsDao: ContactsDao,
        messagesDao: MessagesDao,
        pendingActionsDao: PendingActionsDao,
        response: EventResponse
    ) {

        val savedUser = userManager.getUser(username)

        if (response.usedSpace > 0) {
            savedUser.setAndSaveUsedSpace(response.usedSpace)
        }

        val messages = response.messageUpdates
        val contacts = response.contactUpdates
        val contactsEmails = response.contactEmailsUpdates

        val user = response.userUpdates

        val mailSettings = response.mailSettingsUpdates
        val userSettings = response.userSettingsUpdates
        val labels = response.labelUpdates
        val counts = response.messageCounts
        val addresses = response.addresses

        if (labels != null) {
            writeLabelsUpdates(messagesDao, contactsDao, labels)
        }
        if (messages != null) {
            messages.sortByDescending { eventMessageSortSelector(it) }
            writeMessagesUpdates(messagesDao, pendingActionsDao, messages)
        }
        if (contacts != null) {
            writeContactsUpdates(contactsDao, contacts)
        }
        if (contactsEmails != null) {
            writeContactEmailsUpdates(contactsDao, contactsEmails)
        }
        if (mailSettings != null) {
            writeMailSettings(mailSettings)
        }
        if (userSettings != null) {
            writeUserSettings(userSettings)
        }

        if (user != null) {
            user.username = username
            if (addresses != null && addresses.size > 0) {
                writeAddressUpdates(addresses, ArrayList(savedUser.addresses!!), user)
            } else {
                user.setAddresses(ArrayList<Address>(savedUser.addresses!!))
            }
            user.setAddressIdEmail()
            user.notificationSetting = savedUser.notificationSetting
            userManager.user = user
            AppUtil.postEventOnUi(RefreshDrawerEvent())
        } else {
            if (addresses != null && addresses.size > 0) {
                writeAddressUpdates(addresses, savedUser.addresses, savedUser)
                userManager.user = savedUser
                AppUtil.postEventOnUi(RefreshDrawerEvent())
            }
        }
        if (counts != null) {
            writeUnreadUpdates(counts)
        }
    }


    private fun writeMailSettings(mSettings: MailSettings) {
        var mailSettings: MailSettings? = mSettings
        if (mailSettings == null) {
            mailSettings = MailSettings()
        }
        mailSettings.username = username
        mailSettings.showImages = mSettings.showImages
        mailSettings.autoSaveContacts = mSettings.autoSaveContacts
        mailSettings.sign = mSettings.sign
        mailSettings.pgpScheme = mSettings.pgpScheme
        mailSettings.setAttachPublicKey(if (mSettings.getAttachPublicKey()) 1 else 0)
        mailSettings.save()
        AppUtil.postEventOnUi(MailSettingsEvent(mailSettings))
    }

    private fun writeUserSettings(uSettings: UserSettings) {
        var userSettings: UserSettings? = uSettings
        if (userSettings == null) {
            userSettings = UserSettings()
        }
        userSettings.username = username
        userSettings.notificationEmail
        userSettings.save()
        userManager.userSettings = userSettings
        AppUtil.postEventOnUi(UserSettingsEvent(userSettings))
    }

    private fun writeMessagesUpdates(
        messagesDatabase: MessagesDao,
        pendingActionsDatabase: PendingActionsDatabase,
        events: List<EventResponse.MessageEventBody>
    ) {
        var latestTimestamp = userManager.checkTimestamp
        for (event in events) {
            event.message?.let {
                latestTimestamp = max(event.message.Time.toFloat(), latestTimestamp)
            }
            val messageID = event.messageID
            writeMessageUpdate(event, pendingActionsDatabase, messageID, messagesDatabase)
        }
        userManager.checkTimestamp = latestTimestamp
    }

    private fun writeMessageUpdate(
        event: EventResponse.MessageEventBody,
        pendingActionsDatabase: PendingActionsDatabase,
        messageID: String,
        messagesDatabase: MessagesDao
    ) {
        val type = EventType.fromInt(event.type)
        if (type != EventType.DELETE && checkPendingForSending(pendingActionsDatabase, messageID)) {
            return
        }
        Timber.v("Update message type: $type")
        when (type) {
            EventType.CREATE -> {
                try {
                    val savedMessage = messageDetailsRepository.findMessageByIdBlocking(messageID)
                    if (savedMessage == null) {
                        messageDetailsRepository.saveMessageInDB(messageFactory.createMessage(event.message))
                    } else {
                        updateMessageFlags(messagesDatabase, messageID, event)
                    }

                } catch (syntaxException: JsonSyntaxException) {
                    Timber.w(syntaxException, "unable to create Message object")
                }
            }

            EventType.DELETE -> {
                val message = messageDetailsRepository.findMessageByIdBlocking(messageID)
                if (message != null) {
                    messagesDatabase.deleteMessage(message)
                }
            }

            EventType.UPDATE -> {
                // update Message body
                val message = messageDetailsRepository.findMessageByIdBlocking(messageID)
                stagedMessages[messageID]?.let { messageUpdate ->
                    val dbTime = message?.time ?: 0
                    val serverTime = messageUpdate.time

                    if (message != null) {
                        message.Attachments = messageUpdate.Attachments
                    }

                    if (serverTime > dbTime && message != null && messageUpdate.messageBody != null) {
                        message.messageBody = messageUpdate.messageBody
                        messageDetailsRepository.saveMessageInDB(message)
                    }

                }

                updateMessageFlags(messagesDatabase, messageID, event)
            }

            EventType.UPDATE_FLAGS -> {
                updateMessageFlags(messagesDatabase, messageID, event)
            }
        }
        return
    }

    private fun updateMessageFlags(
        messagesDatabase: MessagesDao,
        messageID: String,
        item: EventResponse.MessageEventBody
    ) {
        val message = messageDetailsRepository.findMessageByIdBlocking(messageID)
        val newMessage = item.message

        if (message != null) {

            if (newMessage.Subject != null) {
                message.subject = newMessage.Subject
            }
            if (newMessage.Unread >= 0) {
                message.Unread = newMessage.Unread > 0
            }
            val sender = newMessage.Sender
            if (sender != null) {
                message.sender = MessageSender(sender.Name, sender.Address)
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
                message.setFolderLocation(messagesDatabase)
            }
            if (expired) {
                messageDetailsRepository.deleteMessage(message)
            } else {
                messageDetailsRepository.saveMessageInDB(message)
            }
        } else {
            stagedMessages[messageID]?.let {
                messageDetailsRepository.saveMessageInDB(it)
            }
        }
    }

    private fun checkPendingForSending(pendingActionsDao: PendingActionsDao, messageId: String): Boolean {
        var pendingForSending = pendingActionsDao.findPendingSendByMessageId(messageId)
        if (pendingForSending != null) {
            return true
        }
        pendingForSending = pendingActionsDao.findPendingSendByOfflineMessageId(messageId)
        return pendingForSending != null
    }

    private fun writeAddressUpdates(
        events: List<EventResponse.AddressEventBody>,
        currentAddresses: MutableList<Address>?,
        user: User
    ) {
        val addresses = currentAddresses?.toMutableList() ?: mutableListOf()
        val eventAddresses = mutableListOf<Address>()

        for (event in events) {
            try {
                val matcher = { address: Address -> address.id == event.id }

                when (val type = EventType.fromInt(event.type)) {
                    EventType.CREATE, EventType.UPDATE -> {
                        addresses.replaceFirst(event.address, matcher)
                        eventAddresses.add(event.address)
                    }
                    EventType.DELETE -> addresses.removeFirst(matcher)
                    EventType.UPDATE_FLAGS -> { /* Do nothing */
                    }
                    else -> Timber.w("'$type' not implemented")
                }
            } catch (exception: Exception) {
                Timber.e(exception, "writeAddressUpdates exception")
            }
        }

        AddressKeyActivationWorker.activateAddressKeysIfNeeded(context, eventAddresses, username)
        user.setAddresses(addresses)
    }

    private fun writeContactsUpdates(contactsDatabase: ContactsDatabase, events: List<EventResponse.ContactEventBody>) {
        for (event in events) {
            Timber.v("New contacts event type: ${event.type} id: ${event.contactID}")
            when (EventType.fromInt(event.type)) {
                EventType.CREATE -> {
                    val contact = event.contact

                    val contactId = contact.contactId
                    val contactName = contact.name
                    val contactData = ContactData(contactId, contactName!!)
                    contactsDatabase.saveContactData(contactData)
                    contactsDatabase.insertFullContactDetails(contact)
                }

                EventType.UPDATE -> {
                    val fullContact = event.contact
                    val contactId = fullContact.contactId
                    val contactData = contactsDatabase.findContactDataById(contactId)
                    if (contactData != null) {
                        contactData.name = event.contact.name!!
                        contactsDatabase.saveContactData(contactData)
                    }

                    val localFullContact = contactsDatabase.findFullContactDetailsById(contactId)
                    if (localFullContact != null) {
                        contactsDatabase.deleteFullContactsDetails(localFullContact)
                    }
                    contactsDatabase.insertFullContactDetails(fullContact)
                }

                EventType.DELETE -> {
                    val contactId = event.contactID
                    val contactData = contactsDatabase.findContactDataById(contactId)
                    if (contactData != null) {
                        contactsDatabase.deleteContactData(contactData)
                    }
                }

                EventType.UPDATE_FLAGS -> {
                }
            }
        }
    }

    private fun writeContactEmailsUpdates(
        contactsDatabase: ContactsDatabase,
        events: List<EventResponse.ContactEmailEventBody>
    ) {
        for (event in events) {
            Timber.v("New contacts emails event type: ${event.type} id: ${event.contactID}")
            when (EventType.fromInt(event.type)) {
                EventType.CREATE -> {
                    val contactEmail = event.contactEmail
                    val contactId = event.contactEmail.contactEmailId
                    // get current contact email saved in local DB
                    val oldContactEmail = contactsDatabase.findContactEmailById(contactId)
                    if (oldContactEmail != null) {
                        val contactEmailId = oldContactEmail.contactEmailId
                        val joins = contactsDatabase.fetchJoinsByEmail(contactEmailId).toMutableList()
                        contactsDatabase.saveContactEmail(contactEmail)
                        contactsDatabase.saveContactEmailContactLabel(joins)
                    } else {
                        contactsDatabase.saveContactEmail(contactEmail)
                        val newJoins = mutableListOf<ContactEmailContactLabelJoin>()
                        contactEmail.labelIds?.forEach { labelId ->
                            newJoins.add(ContactEmailContactLabelJoin(contactEmail.contactEmailId, labelId))
                        }
                        Timber.v("Create new email contact: ${contactEmail.email} newJoins size: ${newJoins.size}")
                        if (newJoins.isNotEmpty()) {
                            contactsDatabase.saveContactEmailContactLabel(newJoins)
                        }
                    }
                }

                EventType.UPDATE -> {
                    val contactId = event.contactEmail.contactEmailId
                    // get current contact email saved in local DB
                    val oldContactEmail = contactsDatabase.findContactEmailById(contactId)
                    Timber.v("Update contact id: $contactId oldContactEmail: ${oldContactEmail?.email}")
                    if (oldContactEmail != null) {
                        val updatedContactEmail = event.contactEmail
                        val labelIds = updatedContactEmail.labelIds ?: ArrayList()
                        val contactEmailId = updatedContactEmail.contactEmailId
                        contactEmailId.let {
                            contactsDatabase.saveContactEmail(updatedContactEmail)
                            val joins = contactsDatabase.fetchJoinsByEmail(contactEmailId).toMutableList()
                            for (labelId in labelIds) {
                                joins.add(ContactEmailContactLabelJoin(contactEmailId, labelId))
                            }
                            if (joins.isNotEmpty()) {
                                contactsDatabase.saveContactEmailContactLabel(joins)
                            }
                        }
                    } else {
                        contactsDatabase.saveContactEmail(event.contactEmail)
                    }
                }

                EventType.DELETE -> {
                    val contactId = event.contactID
                    val contactEmail = contactsDatabase.findContactEmailById(contactId)
                    if (contactEmail != null) {
                        Timber.v("Delete contact id: $contactId")
                        contactsDatabase.deleteContactEmail(contactEmail)
                    }
                }

                EventType.UPDATE_FLAGS -> {
                }
            }
        }
    }

    private fun writeLabelsUpdates(
        messagesDatabase: MessagesDao,
        contactsDatabase: ContactsDatabase,
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
                        messagesDatabase.saveLabel(label)
                    } else if (labelType == Constants.LABEL_TYPE_CONTACT_GROUPS) {
                        val label = ContactLabel(id!!, name!!, color!!, display, order, exclusive == 1)
                        contactsDatabase.saveContactGroupLabel(label)
                    }
                }

                EventType.UPDATE -> {
                    val labelType = item.type!!
                    val labelId = item.ID
                    if (labelType == Constants.LABEL_TYPE_MESSAGE) {
                        val label = messagesDatabase.findLabelById(labelId!!)
                        writeMessageLabel(label, item, messagesDatabase)
                    } else if (labelType == Constants.LABEL_TYPE_CONTACT_GROUPS) {
                        val contactLabel = contactsDatabase.findContactGroupByIdBlocking(labelId!!)
                        writeContactGroup(contactLabel, item, contactsDatabase)
                    }
                }

                EventType.DELETE -> {
                    val labelId = event.id
                    messagesDatabase.deleteLabelById(labelId)
                    contactsDatabase.deleteByContactGroupLabelId(labelId)
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

    private fun writeMessageLabel(currentLabel: Label?, updatedLabel: ServerLabel, messagesDatabase: MessagesDao) {
        if (currentLabel != null) {
            val labelFactory = LabelFactory()
            val labelToSave = labelFactory.createDBObjectFromServerObject(updatedLabel)
            messagesDatabase.saveLabel(labelToSave)
        }
    }

    private fun writeContactGroup(
        currentGroup: ContactLabel?,
        updatedGroup: ServerLabel,
        contactsDatabase: ContactsDatabase
    ) {
        if (currentGroup != null) {
            val contactLabelFactory = ContactLabelFactory()
            val labelToSave = contactLabelFactory.createDBObjectFromServerObject(updatedGroup)
            val joins = contactsDatabase.fetchJoins(labelToSave.ID)
            contactsDatabase.saveContactGroupLabel(labelToSave)
            contactsDatabase.saveContactEmailContactLabel(joins)
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
