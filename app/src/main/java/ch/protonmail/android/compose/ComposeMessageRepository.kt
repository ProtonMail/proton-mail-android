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
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.LocalAttachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.feature.account.allLoggedInBlocking
import ch.protonmail.android.jobs.FetchDraftDetailJob
import ch.protonmail.android.jobs.FetchMessageDetailJob
import ch.protonmail.android.jobs.PostReadJob
import ch.protonmail.android.jobs.ResignContactJob
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.utils.resettableLazy
import ch.protonmail.android.utils.resettableManager
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Flowable
import io.reactivex.Single
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class ComposeMessageRepository @Inject constructor(
    val jobManager: JobManager,
    val api: ProtonMailApiManager,
    val databaseProvider: DatabaseProvider,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val accountManager: AccountManager,
    private val userManager: UserManager,
    private val labelRepository: LabelRepository,
    private val contactRepository: ContactsRepository,
    private val dispatcherProvider: DispatcherProvider,
) {

    val lazyManager = resettableManager()

    private val messageDao: MessageDao
        get() = databaseProvider.provideMessageDao(userManager.requireCurrentUserId())

    private val contactDao: ContactDao
        get() = databaseProvider.provideContactDao(userManager.requireCurrentUserId())

    private val contactDaos: HashMap<UserId, ContactDao> by resettableLazy(lazyManager) {
        val userIds = accountManager.allLoggedInBlocking()
        val listOfDaos: HashMap<UserId, ContactDao> = HashMap()
        for (userId in userIds) {
            listOfDaos[userId] = databaseProvider.provideContactDao(userId)
        }
        listOfDaos
    }

    fun getContactGroupsFromDB(userId: UserId, combinedContacts: Boolean): Flow<List<ContactLabelUiModel>> {
        return labelRepository.observeContactGroups(userId)
            .map { list ->
                list.map { entity ->
                    ContactLabelUiModel(
                        id = entity.id,
                        name = entity.name,
                        color = entity.color,
                        type = entity.type,
                        path = entity.path,
                        parentId = entity.parentId,
                        contactEmailsCount = contactRepository.countContactEmailsByLabelId(entity.id)
                    )
                }
            }
    }

    suspend fun getContactGroupEmailsSync(groupId: String): List<ContactEmail> =
        contactDao.observeAllContactsEmailsByContactGroup(groupId).first()

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
        jobManager.addJobInBackground(FetchMessageDetailJob(messageId, labelRepository))
    }

    fun createAttachmentList(
        attachmentList: List<LocalAttachment>,
    ) = Attachment.createAttachmentList(messageDao, attachmentList.filter { it.doSaveInDB }, false)

    fun prepareMessageData(
        currentObject: MessageBuilderData,
        messageTitle: String,
        attachments: ArrayList<LocalAttachment>
    ): MessageBuilderData {
        return MessageBuilderData.Builder()
            .fromOld(currentObject)
            .messageTitle("")
            .senderEmailAddress("")
            .messageSenderName("")
            .messageTitle(messageTitle)
            .attachmentList(attachments)
            .build()
    }

    fun prepareMessageData(
        currentObject: MessageBuilderData,
        isPgpMime: Boolean,
        addressId: String,
        addressEmailAlias: String? = null
    ): MessageBuilderData {
        return MessageBuilderData.Builder()
            .fromOld(currentObject)
            .messageTitle("")
            .senderEmailAddress("")
            .messageSenderName("")
            .addressId(addressId)
            .addressEmailAlias(addressEmailAlias)
            .isPGPMime(isPgpMime)
            .build()
    }

    fun findAllMessageRecipients(userId: UserId) = contactDaos[userId]!!.findAllMessageRecipients()

    suspend fun markMessageRead(messageId: String) {
        messageDetailsRepository.findMessageById(messageId).first()?.let { savedMessage ->
            val read = savedMessage.isRead
            if (!read) {
                jobManager.addJobInBackground(PostReadJob(listOf(savedMessage.messageId)))
            }
        }
    }

    fun getSendPreference(emailList: List<String>, destination: GetSendPreferenceJob.Destination) {
        jobManager.addJobInBackground(GetSendPreferenceJob(emailList, destination))
    }

    fun resignContactJob(
        contactEmail: String,
        sendPreference: SendPreference,
        destination: GetSendPreferenceJob.Destination
    ) {
        jobManager.addJobInBackground(ResignContactJob(contactEmail, sendPreference, destination))
    }
}
