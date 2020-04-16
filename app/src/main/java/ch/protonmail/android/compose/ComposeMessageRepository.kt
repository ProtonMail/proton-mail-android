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
package ch.protonmail.android.compose

import android.text.TextUtils
import ch.protonmail.android.activities.composeMessage.MessageBuilderData
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.LocalAttachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.jobs.*
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob
import ch.protonmail.android.jobs.general.GetAvailableDomainsJob
import ch.protonmail.android.jobs.verification.FetchHumanVerificationOptionsJob
import ch.protonmail.android.jobs.verification.PostHumanVerificationJob
import ch.protonmail.android.utils.resettableLazy
import ch.protonmail.android.utils.resettableManager
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.*
import javax.inject.Inject

/**
 * Created by kadrikj on 9/17/18. */
class ComposeMessageRepository @Inject constructor(
        val jobManager: JobManager,
        val api: ProtonMailApi,
        val databaseProvider: DatabaseProvider,
        var messagesDatabase: MessagesDatabase,
        val searchDatabase: MessagesDatabase,
        val messageDetailsRepository: MessageDetailsRepository // FIXME: this should be removed
) {

    val lazyManager = resettableManager()

    /**
     * Reloads all statically required dependencies when currently active user changes.
     */
    fun reloadDependenciesForUser(username: String) {
        messageDetailsRepository.reloadDependenciesForUser(username)
        messagesDatabase = databaseProvider.provideMessagesDao(username)
    }

    private val contactsDao by resettableLazy(lazyManager) {
        databaseProvider.provideContactsDao()
    }

    private val contactsDaos : HashMap<String, ContactsDao> by resettableLazy(lazyManager) {
        val usernames = AccountManager.getInstance(ProtonMailApplication.getApplication().applicationContext).getLoggedInUsers()
        val listOfDaos : HashMap<String, ContactsDao> = HashMap()
        for(username in usernames){
            listOfDaos[username]= databaseProvider.provideContactsDao(username)
        }
        listOfDaos
    }

    fun getContactGroupsFromDB(username : String, combinedContacts:Boolean): Observable<List<ContactLabel>> {
        var tempContactsDao : ContactsDao = contactsDao
        if(combinedContacts) {
             tempContactsDao = contactsDaos[username]!!
        }
            return tempContactsDao.findContactGroupsObservable()
                    .flatMap { list ->
                        Observable.fromIterable(list)
                                .map {
                                    it.contactEmailsCount = tempContactsDao.countContactEmailsByLabelId(it.ID)
                                    it
                                }
                                .toList()
                                .toFlowable()
                    }
                    .toObservable()
    }

    fun getContactGroupFromDB(groupName: String): Single<ContactLabel> {
        return contactsDao.findContactGroupByNameAsync(groupName)
    }

    fun getContactGroupEmails(groupId: String): Observable<List<ContactEmail>> {
        return contactsDao.findAllContactsEmailsByContactGroupAsyncObservable(groupId).toObservable()
    }

    fun getContactGroupEmailsSync(groupId: String): List<ContactEmail> {
        return contactsDao.findAllContactsEmailsByContactGroup(groupId)
    }

    fun saveAttachment(attachment: Attachment): Long {
        return messagesDatabase.saveAttachment(attachment)
    }

    suspend fun getAttachments(message: Message, isTransient: Boolean, dispatcher: CoroutineDispatcher): List<Attachment> =
            withContext(dispatcher) {
                if (!isTransient) {
                    message.attachments(messagesDatabase)
                } else {
                    message.attachments(searchDatabase)
                }
            }

    fun getAttachments2(message: Message, isTransient: Boolean): List<Attachment> = if (!isTransient) {
        message.attachments(messagesDatabase)
    } else {
        message.attachments(searchDatabase)
    }

    fun findMessageByIdSingle(id: String): Single<Message> {
        return messageDetailsRepository.findMessageByIdSingle(id)
    }

    fun findMessageByIdObservable(id: String): Flowable<Message> {
        return messageDetailsRepository.findMessageByIdObservable(id)
    }

    /**
     * Returns a message for a given draft id. It tries to get it by local id first, if absent then by a regular message id.
     */
    suspend fun findMessage(draftId: String, dispatcher: CoroutineDispatcher): Message? =
        withContext(dispatcher) {
            var message: Message? = null
            if (!TextUtils.isEmpty(draftId)) {
                message = messageDetailsRepository.findMessageById(draftId)
            }
            message
        }


    suspend fun deleteMessageById(messageId: String, dispatcher: CoroutineDispatcher) =
        withContext(dispatcher) {
            messagesDatabase.deleteMessageById(messageId)
        }

    fun startGetAvailableDomains() {
        jobManager.addJobInBackground(GetAvailableDomainsJob(true))
    }

    fun startFetchHumanVerificationOptions() {
        jobManager.addJobInBackground(FetchHumanVerificationOptionsJob())
    }

    fun startFetchDraftDetail(messageId: String) {
        jobManager.addJobInBackground(FetchDraftDetailJob(messageId))
    }

    fun startFetchMessageDetail(messageId: String) {
        jobManager.addJobInBackground(FetchMessageDetailJob(messageId))
    }

    fun startPostDelete(messageId: String) {
        jobManager.addJobInBackground(PostDeleteJob(listOf(messageId)))
    }

    fun startPostHumanVerification(tokenType: Constants.TokenType, token: String) {
        jobManager.addJobInBackground(PostHumanVerificationJob(tokenType, token))
    }

    suspend fun saveMessage(message: Message, dispatcher: CoroutineDispatcher) =
        withContext(dispatcher) {
            messageDetailsRepository.saveMessageInDB(message)
        }

    suspend fun findAttachmentsByMessageId(messageId: String, dispatcher: CoroutineDispatcher) =
        withContext(dispatcher) {
            messagesDatabase.findAttachmentsByMessageId(messageId)
        }

    suspend fun findAttachmentByMessageIdFileNameAndPath(messageId: String, fileName: String, filePath: String, isTransient: Boolean, dispatcher: CoroutineDispatcher) =
            withContext(dispatcher) {
                if(!isTransient) {
                    messagesDatabase.findAttachmentsByMessageIdFileNameAndPath(messageId, fileName, filePath)
                } else {
                    searchDatabase.findAttachmentsByMessageIdFileNameAndPath(messageId, fileName, filePath)
                }
            }

    suspend fun createAttachmentList(attachmentList: List<LocalAttachment>, dispatcher: CoroutineDispatcher) =
        withContext(dispatcher) {
            Attachment.createAttachmentList(messagesDatabase, attachmentList, false)
        }

    fun prepareMessageData(currentObject: MessageBuilderData, messageTitle: String,
                           attachments: ArrayList<LocalAttachment>): MessageBuilderData {
        return MessageBuilderData.Builder()
                .fromOld(currentObject)
                .message(Message())
                .messageTitle("")
                .senderEmailAddress("")
                .messageSenderName("")
                .messageTitle(messageTitle)
                .attachmentList(attachments)
                .build()
    }

    fun prepareMessageData(isPgpMime: Boolean, addressId: String, addressEmailAlias: String? = null, isTransient: Boolean = false): MessageBuilderData  {
        return MessageBuilderData.Builder()
            .message(Message())
            .messageTitle("")
            .senderEmailAddress("")
            .messageSenderName("")
            .addressId(addressId)
            .addressEmailAlias(addressEmailAlias)
            .isPGPMime(isPgpMime)
            .isTransient(isTransient)
            .build()
        }

    fun findAllMessageRecipients(username : String) = contactsDaos[username]!!.findAllMessageRecipients()

    fun markMessageRead(messageId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            messageDetailsRepository.findMessageById(messageId)?.let {savedMessage->
                val read = savedMessage.isRead
                if (!read) {
                    jobManager.addJobInBackground(PostReadJob(listOf(savedMessage.messageId)))
                }
            }
        }
    }

    fun fetchPublicKeys(jobs: List<FetchPublicKeysJob.PublicKeysBatchJob>, retry: Boolean) {
        jobManager.addJobInBackground(FetchPublicKeysJob(jobs, retry))
    }

    fun getSendPreference(emailList: List<String>, destination: GetSendPreferenceJob.Destination) {
        jobManager.addJobInBackground(GetSendPreferenceJob(contactsDao, emailList, destination))
    }

    fun resignContactJob(contactEmail: String, sendPreference: SendPreference, destination: GetSendPreferenceJob.Destination) {
        jobManager.addJobInBackground(ResignContactJob(contactEmail, sendPreference, destination))
    }
}
