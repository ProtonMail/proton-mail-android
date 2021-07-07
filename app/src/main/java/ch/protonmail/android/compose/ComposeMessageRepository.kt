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

import ch.protonmail.android.activities.composeMessage.MessageBuilderData
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactLabel
import ch.protonmail.android.data.local.model.LocalAttachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.feature.account.allLoggedInBlocking
import ch.protonmail.android.jobs.FetchDraftDetailJob
import ch.protonmail.android.jobs.FetchMessageDetailJob
import ch.protonmail.android.jobs.PostReadJob
import ch.protonmail.android.jobs.ResignContactJob
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob
import ch.protonmail.android.utils.resettableLazy
import ch.protonmail.android.utils.resettableManager
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Flowable
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class ComposeMessageRepository @Inject constructor(
    val jobManager: JobManager,
    val api: ProtonMailApiManager,
    val databaseProvider: DatabaseProvider,
    private var messageDao: MessageDao,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val accountManager: AccountManager,
    private val userManager: UserManager
) {

    val lazyManager = resettableManager()

    /**
     * Reloads all statically required dependencies when currently active user changes.
     */
    fun reloadDependenciesForUser(userId: Id) {
        messageDetailsRepository.reloadDependenciesForUser(userId)
        messageDao = databaseProvider.provideMessageDao(userId)
    }

    private val contactDao by resettableLazy(lazyManager) {
        databaseProvider.provideContactDao(userManager.requireCurrentUserId())
    }

    private val contactDaos: HashMap<Id, ContactDao> by resettableLazy(lazyManager) {
        val userIds = accountManager.allLoggedInBlocking()
        val listOfDaos: HashMap<Id, ContactDao> = HashMap()
        for (userId in userIds) {
            listOfDaos[userId] = databaseProvider.provideContactDao(userId)
        }
        listOfDaos
    }

    fun getContactGroupsFromDB(userId: Id, combinedContacts: Boolean): Observable<List<ContactLabel>> {
        var tempContactDao: ContactDao = contactDao
        if (combinedContacts) {
            tempContactDao = contactDaos[userId]!!
        }
        return tempContactDao.findContactGroupsObservable()
            .flatMap { list ->
                Observable.fromIterable(list)
                    .map {
                        it.contactEmailsCount = tempContactDao.countContactEmailsByLabelIdBlocking(it.ID)
                        it
                    }
                    .toList()
                    .toFlowable()
            }
            .toObservable()
    }

    fun getContactGroupFromDB(groupName: String): Single<ContactLabel> {
        return contactDao.findContactGroupByNameAsync(groupName)
    }

    fun getContactGroupEmails(groupId: String): Observable<List<ContactEmail>> {
        return contactDao.findAllContactsEmailsByContactGroupAsyncObservable(groupId).toObservable()
    }

    fun getContactGroupEmailsSync(groupId: String): List<ContactEmail> {
        return contactDao.findAllContactsEmailsByContactGroupBlocking(groupId)
    }

    suspend fun getAttachments(message: Message, dispatcher: CoroutineDispatcher): List<Attachment> =
        withContext(dispatcher) {
            message.attachments(messageDao)
        }

    fun getAttachmentsBlocking(message: Message): List<Attachment> = message.attachmentsBlocking(messageDao)

    fun findMessageByIdSingle(id: String): Single<Message> =
        messageDetailsRepository.findMessageByIdSingle(id)

    fun findMessageByIdObservable(id: String): Flowable<Message> =
        messageDetailsRepository.findMessageByIdObservable(id)

    /**
     * Returns a message for a given draft id. It tries to get it by local id first, if absent then by a regular
     *  message id.
     */
    suspend fun findMessage(draftId: String): Message? {
        draftId.takeIfNotBlank() ?: return null
        return messageDetailsRepository.findMessageById(draftId).first()
    }

    fun startFetchDraftDetail(messageId: String) {
        jobManager.addJobInBackground(FetchDraftDetailJob(messageId))
    }

    fun startFetchMessageDetail(messageId: String) {
        jobManager.addJobInBackground(FetchMessageDetailJob(messageId))
    }

    suspend fun createAttachmentList(
        attachmentList: List<LocalAttachment>,
        dispatcher: CoroutineDispatcher
    ) =
        withContext(dispatcher) {
            Attachment.createAttachmentList(messageDao, attachmentList, false)
        }

    fun prepareMessageData(
        currentObject: MessageBuilderData,
        messageTitle: String,
        attachments: ArrayList<LocalAttachment>
    ): MessageBuilderData {
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

    fun prepareMessageData(
        isPgpMime: Boolean,
        addressId: String,
        addressEmailAlias: String? = null
    ): MessageBuilderData {
        return MessageBuilderData.Builder()
            .message(Message())
            .messageTitle("")
            .senderEmailAddress("")
            .messageSenderName("")
            .addressId(addressId)
            .addressEmailAlias(addressEmailAlias)
            .isPGPMime(isPgpMime)
            .build()
    }

    fun findAllMessageRecipients(userId: Id) = contactDaos[userId]!!.findAllMessageRecipients()

    fun markMessageRead(messageId: String) {
        GlobalScope.launch(Dispatchers.IO) {
            messageDetailsRepository.findMessageByIdBlocking(messageId)?.let { savedMessage ->
                val read = savedMessage.isRead
                if (!read) {
                    jobManager.addJobInBackground(PostReadJob(listOf(savedMessage.messageId)))
                }
            }
        }
    }

    fun getSendPreference(emailList: List<String>, destination: GetSendPreferenceJob.Destination) {
        jobManager.addJobInBackground(GetSendPreferenceJob(contactDao, emailList, destination))
    }

    fun resignContactJob(
        contactEmail: String,
        sendPreference: SendPreference,
        destination: GetSendPreferenceJob.Destination
    ) {
        jobManager.addJobInBackground(ResignContactJob(contactEmail, sendPreference, destination))
    }
}
