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
package ch.protonmail.android.mailbox.presentation

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.services.MessagesService
import ch.protonmail.android.api.utils.ApplyRemoveLabels
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.Constants.MessageLocationType.LABEL
import ch.protonmail.android.core.Constants.MessageLocationType.LABEL_FOLDER
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.LabelRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.jobs.ApplyLabelJob
import ch.protonmail.android.jobs.FetchByLocationJob
import ch.protonmail.android.jobs.FetchMessageCountsJob
import ch.protonmail.android.jobs.RemoveLabelJob
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.GetConversations
import ch.protonmail.android.mailbox.domain.GetMessagesByLocation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem
import ch.protonmail.android.mailbox.presentation.model.MessageData
import ch.protonmail.android.ui.view.LabelChipUiModel
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.UserUtils
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import java.util.ArrayList
import java.util.HashMap
import javax.inject.Inject
import kotlin.collections.set

const val FLOW_START_ACTIVITY = 1
const val FLOW_USED_SPACE_CHANGED = 2
const val FLOW_TRY_COMPOSE = 3
private const val STARRED_LABEL_ID = "10"
private const val MIN_MESSAGES_TO_SHOW_COUNT = 2

@HiltViewModel
class MailboxViewModel @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository,
    private val userManager: UserManager,
    private val jobManager: JobManager,
    private val deleteMessage: DeleteMessage,
    private val dispatchers: DispatcherProvider,
    private val contactsRepository: ContactsRepository,
    private val labelRepository: LabelRepository,
    verifyConnection: VerifyConnection,
    networkConfigurator: NetworkConfigurator,
    private val messageServiceScheduler: MessagesService.Scheduler,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val getConversations: GetConversations,
    private val changeConversationsReadStatus: ChangeConversationsReadStatus,
    private val getMessagesByLocation: GetMessagesByLocation
) : ConnectivityBaseViewModel(verifyConnection, networkConfigurator) {

    var pendingSendsLiveData = messageDetailsRepository.findAllPendingSendsAsync()
    var pendingUploadsLiveData = messageDetailsRepository.findAllPendingUploadsAsync()

    private val _manageLimitReachedWarning = MutableLiveData<Event<Boolean>>()
    private val _manageLimitApproachingWarning = MutableLiveData<Event<Boolean>>()
    private val _manageLimitBelowCritical = MutableLiveData<Event<Boolean>>()
    private val _manageLimitReachedWarningOnTryCompose = MutableLiveData<Event<Boolean>>()
    private val _toastMessageMaxLabelsReached = MutableLiveData<Event<MaxLabelsReached>>()
    private val _hasSuccessfullyDeletedMessages = MutableLiveData<Boolean>()
    private val mutableMailboxState = MutableStateFlow<MailboxState>(MailboxState.Loading)
    private val mutableMailboxLocation = MutableStateFlow(INBOX)
    private val mutableMailboxLabelId = MutableStateFlow(EMPTY_STRING)
    private val mutableUserId = MutableStateFlow(requireNotNull(userManager.currentUserId))

    val manageLimitReachedWarning: LiveData<Event<Boolean>>
        get() = _manageLimitReachedWarning
    val manageLimitApproachingWarning: LiveData<Event<Boolean>>
        get() = _manageLimitApproachingWarning
    val manageLimitBelowCritical: LiveData<Event<Boolean>>
        get() = _manageLimitBelowCritical
    val manageLimitReachedWarningOnTryCompose: LiveData<Event<Boolean>>
        get() = _manageLimitReachedWarningOnTryCompose
    val toastMessageMaxLabelsReached: LiveData<Event<MaxLabelsReached>>
        get() = _toastMessageMaxLabelsReached

    val hasSuccessfullyDeletedMessages: LiveData<Boolean>
        get() = _hasSuccessfullyDeletedMessages

    val mailboxState = mutableMailboxState.asStateFlow()

    val mailboxLocation = mutableMailboxLocation.asStateFlow()

    init {
        combine(mutableMailboxLocation, mutableMailboxLabelId, mutableUserId) { location, label, userId ->
            Triple(location, label, userId)
        }
            .onEach {
                Timber.v("New location,label,user: $it")
                mutableMailboxState.value = MailboxState.Loading
            }
            .flatMapLatest { triple ->
                val location = triple.first
                val labelId = triple.second
                val userId = triple.third

                if (conversationModeEnabled(location)) {
                    Timber.v("Getting conversations for $location, label: $labelId, user: $userId")
                    conversationsAsMailboxItems(location, labelId, userId)
                } else {
                    Timber.v("Getting messages for $location label: $labelId user: $userId")
                    getMessagesByLocation(location, labelId, userId)
                        .map { result ->
                            when (result) {
                                is GetMessagesResult.Success -> {
                                    MailboxState.Data(
                                        messagesToMailboxItems(result.messages),
                                        false
                                    )
                                }
                                is GetMessagesResult.Error -> {
                                    MailboxState.Error(
                                        "GetMessagesResult Error",
                                        result.throwable
                                    )
                                }
                            }
                        }
                }
            }
            .catch {
                emit(
                    MailboxState.Error(
                        "Failed getting messages, catch",
                        it
                    )
                )
            }
            .onEach {
                mutableMailboxState.value = it
            }
            .launchIn(viewModelScope)
    }

    fun reloadDependenciesForUser() {
        pendingSendsLiveData = messageDetailsRepository.findAllPendingSendsAsync()
        pendingUploadsLiveData = messageDetailsRepository.findAllPendingUploadsAsync()
    }

    fun usedSpaceActionEvent(limitReachedFlow: Int) {
        viewModelScope.launch {
            userManager.setShowStorageLimitReached(true)
            val user = userManager.currentUser
                ?: return@launch
            val (usedSpace, totalSpace) = with(user.dedicatedSpace) { used.l.toLong() to total.l.toLong() }
            val userMaxSpace = if (totalSpace == 0L) Long.MAX_VALUE else totalSpace
            val percentageUsed = usedSpace * 100L / userMaxSpace
            val limitReached = percentageUsed >= 100
            val limitApproaching = percentageUsed >= Constants.STORAGE_LIMIT_WARNING_PERCENTAGE

            when (limitReachedFlow) {
                FLOW_START_ACTIVITY -> {
                    if (limitReached) {
                        _manageLimitReachedWarning.postValue(Event(limitReached))
                    } else if (limitApproaching) {
                        _manageLimitApproachingWarning.postValue(Event(limitApproaching))
                    }
                }
                FLOW_USED_SPACE_CHANGED -> {
                    if (limitReached) {
                        _manageLimitReachedWarning.postValue(Event(limitReached))
                    } else if (limitApproaching) {
                        _manageLimitApproachingWarning.postValue(Event(limitApproaching))
                    } else {
                        _manageLimitBelowCritical.postValue(Event(true))
                    }
                }
                FLOW_TRY_COMPOSE -> {
                    _manageLimitReachedWarningOnTryCompose.postValue(Event(limitReached))
                }
            }
        }
    }

    fun processLabels(messageIds: List<String>, checkedLabelIds: List<String>, unchangedLabels: List<String>) {
        val iterator = messageIds.iterator()

        val labelsToApplyMap = HashMap<String, MutableList<String>>()
        val labelsToRemoveMap = HashMap<String, MutableList<String>>()
        var result: Pair<Map<String, List<String>>, Map<String, List<String>>>? = null

        viewModelScope.launch {
            withContext(dispatchers.Comp) {
                while (iterator.hasNext()) {
                    val messageId = iterator.next()
                    val message = messageDetailsRepository.findMessageById(messageId).first()

                    if (message != null) {
                        val currentLabelsIds = message.labelIDsNotIncludingLocations
                        val labels = getAllLabelsByIds(currentLabelsIds)
                        val applyRemoveLabels = resolveMessageLabels(
                            message, ArrayList(checkedLabelIds),
                            ArrayList(unchangedLabels),
                            labels
                        )
                        val apply = applyRemoveLabels?.labelsToApply
                        val remove = applyRemoveLabels?.labelsToRemove
                        apply?.forEach {
                            var labelsToApply: MutableList<String>? = labelsToApplyMap[it]
                            if (labelsToApply == null) {
                                labelsToApply = ArrayList()
                            }
                            labelsToApply.add(messageId)
                            labelsToApplyMap[it] = labelsToApply
                        }
                        remove?.forEach {
                            var labelsToRemove: MutableList<String>? = labelsToRemoveMap[it]
                            if (labelsToRemove == null) {
                                labelsToRemove = ArrayList()
                            }
                            labelsToRemove.add(messageId)
                            labelsToRemoveMap[it] = labelsToRemove
                        }
                    }
                }

                result = Pair(labelsToApplyMap, labelsToRemoveMap)
            }
            val applyKeySet = result?.first?.keys
            val removeKeySet = result?.second?.keys
            applyKeySet?.forEach {
                jobManager.addJobInBackground(ApplyLabelJob(labelsToApplyMap[it], it))
            }

            removeKeySet?.forEach {
                jobManager.addJobInBackground(RemoveLabelJob(labelsToRemoveMap[it], it))
            }
        }
    }

    fun loadMailboxItems(
        labelId: String?,
        includeLabels: Boolean,
        uuid: String,
        refreshMessages: Boolean,
        oldestItemTimestamp: Long
    ) {

        val location = mailboxLocation.value
        Timber.v("loadMailboxItems location: $location")
        if (conversationModeEnabled(location)) {
            val userId = userManager.currentUserId ?: return
            val locationId = labelId ?: location.messageLocationTypeValue.toString()
            return getConversations.loadMore(userId, locationId, oldestItemTimestamp)
        }

        fetchMessages(
            oldestItemTimestamp,
            location,
            labelId,
            includeLabels,
            uuid,
            refreshMessages
        )
    }

    fun messagesToMailboxItemsBlocking(messages: List<Message>) = runBlocking {
        return@runBlocking messagesToMailboxItems(messages)
    }

    private fun fetchMessages(
        oldestMessageTimestamp: Long?,
        location: Constants.MessageLocationType,
        labelId: String?,
        includeLabels: Boolean,
        uuid: String,
        refreshMessages: Boolean
    ) {
        // UserId is needed. We currently don't support merged inbox.
        val userId = userManager.currentUserId ?: return
        Timber.v("fetchMessages userId: $userId")

        // When oldestMessageTimestamp is valid the request is about paginated messages (page > 1)

        if (refreshMessages) {
            messageDetailsRepository.reloadDependenciesForUser(userId)
        }

        if (oldestMessageTimestamp != null) {
            val isCustomLocation = location == LABEL || location == LABEL_FOLDER

            if (isCustomLocation) {
                messageServiceScheduler.fetchMessagesOlderThanTimeByLabel(
                    location,
                    userId,
                    oldestMessageTimestamp,
                    labelId ?: ""
                )
            } else {
                messageServiceScheduler.fetchMessagesOlderThanTime(
                    location,
                    userId,
                    oldestMessageTimestamp
                )
            }
        } else {
            jobManager.addJobInBackground(
                FetchByLocationJob(
                    location,
                    labelId,
                    includeLabels,
                    uuid,
                    refreshMessages
                )
            )
        }
    }

    private fun conversationsAsMailboxItems(
        location: Constants.MessageLocationType,
        labelId: String?,
        userId: Id
    ): Flow<MailboxState> {
        val locationId = if (!labelId.isNullOrEmpty()) {
            labelId
        } else {
            location.messageLocationTypeValue.toString()
        }
        Timber.v("conversationsAsMailboxItems locationId: $locationId")
        return getConversations(
            userId,
            locationId
        ).map { result ->
            when (result) {
                is GetConversationsResult.Success -> {
                    MailboxState.Data(
                        conversationsToMailboxItems(result.conversations, locationId)
                    )
                }
                is GetConversationsResult.NoConversationsFound -> {
                    MailboxState.Data(noMoreItems = true)
                }
                is GetConversationsResult.Error -> {
                    Timber.e(result.throwable, "Failed getting conversations")
                    MailboxState.Error(error = "Failed getting conversations", throwable = result.throwable)
                }
            }
        }
    }

    private suspend fun conversationsToMailboxItems(
        conversations: List<Conversation>,
        locationId: String
    ): List<MailboxUiItem> {
        // Note for future: Get userId from Conversation (should contain it).
        val userId = userManager.currentUserId ?: return emptyList()

        val contacts = contactsRepository.findAllContactEmails().first()
        val labels = labelRepository.findAllLabels(UserId(userId.s)).first()

        return conversations.map { conversation ->
            val lastMessageTimeMs = conversation.labels.find {
                it.id == locationId
            }?.contextTime?.let { it * 1000 } ?: 0

            val conversationLabelsIds = conversation.labels.map { it.id }
            val labelChipUiModels = labels
                .filter { it.id in conversationLabelsIds }
                .toLabelChipUiModels()

            MailboxUiItem(
                conversation.id,
                conversation.senders.joinToString { getCorrespondentDisplayName(it, contacts) },
                conversation.subject,
                lastMessageTimeMs,
                conversation.attachmentsCount > 0,
                conversation.labels.any { it.id == STARRED_LABEL_ID },
                conversation.unreadCount == 0,
                conversation.expirationTime,
                getDisplayMessageCount(conversation),
                null,
                false,
                labelChipUiModels,
                conversation.receivers.joinToString { it.name }
            )
        }
    }

    private fun getDisplayMessageCount(conversation: Conversation) =
        conversation.messagesCount.let {
            if (it >= MIN_MESSAGES_TO_SHOW_COUNT) {
                it
            } else {
                null
            }
        }

    private suspend fun messagesToMailboxItems(messages: List<Message>): List<MailboxUiItem> {
        Timber.v("messagesToMailboxItems size: ${messages.size}")
        // Note for future: Get userId from Message (should contain it).
        val userId = userManager.currentUserId ?: return emptyList()

        val contacts = contactsRepository.findAllContactEmails().first()
        val labels = labelRepository.findAllLabels(UserId(userId.s)).first()

        return messages.map { message ->
            val senderName = getSenderDisplayName(message, contacts)

            val messageData = MessageData(
                message.location,
                message.isReplied ?: false,
                message.isRepliedAll ?: false,
                message.isForwarded ?: false,
                message.isInline,
            )

            val labelChipUiModels = labels
                .filter { it.id in message.allLabelIDs }
                .toLabelChipUiModels()

            MailboxUiItem(
                message.messageId!!,
                senderName,
                message.subject!!,
                message.timeMs,
                message.numAttachments > 0,
                message.isStarred ?: false,
                message.isRead,
                message.expirationTime,
                null,
                messageData,
                message.deleted,
                labelChipUiModels,
                message.toListStringGroupsAware
            )
        }
    }

    private fun getSenderDisplayName(message: Message, contacts: List<ContactEmail>) =
        getCorrespondentDisplayName(
            Correspondent(message.senderName ?: "", message.senderEmail),
            contacts
        )

    private fun getCorrespondentDisplayName(correspondent: Correspondent, contacts: List<ContactEmail>): String {
        val senderNameFromContacts = contacts.find { correspondent.address == it.email }?.name

        if (!senderNameFromContacts.isNullOrEmpty()) {
            return senderNameFromContacts
        }

        if (correspondent.name.isNotEmpty()) {
            return correspondent.name
        }

        return correspondent.address
    }

    private fun getAllLabelsByIds(labelIds: List<String>) =
        messageDetailsRepository.findAllLabelsWithIds(labelIds)

    private fun resolveMessageLabels(
        message: Message,
        checkedLabelIds: MutableList<String>,
        unchangedLabels: List<String>,
        currentContactLabels: List<Label>?
    ): ApplyRemoveLabels? {
        val labelsToRemove = ArrayList<String>()

        currentContactLabels?.forEach {
            val labelId = it.id
            if (!checkedLabelIds.contains(labelId) && !unchangedLabels.contains(labelId) && !it.exclusive) {
                labelsToRemove.add(labelId)
            } else if (checkedLabelIds.contains(labelId)) {
                checkedLabelIds.remove(labelId)
            }
        }

        val labelList = ArrayList(message.labelIDsNotIncludingLocations)
        labelList.addAll(checkedLabelIds)
        labelList.removeAll(labelsToRemove)
        val labelSet = labelList.toSet()
        val maxLabelsAllowed = UserUtils.getMaxAllowedLabels(userManager)

        if (labelSet.size > maxLabelsAllowed) {
            _toastMessageMaxLabelsReached.value = Event(MaxLabelsReached(message.subject, maxLabelsAllowed))
            return null
        }

        message.addLabels(checkedLabelIds)
        message.removeLabels(labelsToRemove)
        viewModelScope.launch {
            messageDetailsRepository.saveMessage(message)
        }

        return ApplyRemoveLabels(checkedLabelIds, labelsToRemove)
    }

    fun deleteMessages(messageIds: List<String>, currentLabelId: String?) =
        viewModelScope.launch {
            val deleteMessagesResult = deleteMessage(messageIds, currentLabelId)
            _hasSuccessfullyDeletedMessages.postValue(deleteMessagesResult.isSuccessfullyDeleted)
        }

    fun refreshMailboxCount(location: Constants.MessageLocationType) {
        if (conversationModeEnabled(location)) {
            return
        }

        jobManager.addJobInBackground(FetchMessageCountsJob(null))
    }

    private fun List<Label>.toLabelChipUiModels(): List<LabelChipUiModel> =
        filterNot { it.exclusive }.map { label ->
            val labelColor = label.color.takeIfNotBlank()
                ?.let { Color.parseColor(UiUtil.normalizeColor(it)) }

            LabelChipUiModel(Id(label.id), Name(label.name), labelColor)
        }

    fun markRead(ids: List<String>, userId: UserId, location: Constants.MessageLocationType) {
        if (conversationModeEnabled(location)) {
            viewModelScope.launch {
                changeConversationsReadStatus(
                    ids,
                    ChangeConversationsReadStatus.Action.ACTION_MARK_READ,
                    userId,
                    location
                )
            }
        } else {
            messageDetailsRepository.markRead(ids)
        }
    }

    fun markUnRead(ids: List<String>, userId: UserId, location: Constants.MessageLocationType) {
        if (conversationModeEnabled(location)) {
            viewModelScope.launch {
                changeConversationsReadStatus(
                    ids,
                    ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                    userId,
                    location
                )
            }
        } else {
            messageDetailsRepository.markUnRead(ids)
        }
    }

    fun setNewMailboxLocation(newLocation: Constants.MessageLocationType) {
        mutableMailboxLocation.value = newLocation
    }

    fun setNewMailboxLabel(labelId: String) {
        mutableMailboxLabelId.value = labelId
    }

    fun setNewUserId(currentUserId: Id) {
        mutableUserId.value = currentUserId
    }

    data class MaxLabelsReached(val subject: String?, val maxAllowedLabels: Int)
}
