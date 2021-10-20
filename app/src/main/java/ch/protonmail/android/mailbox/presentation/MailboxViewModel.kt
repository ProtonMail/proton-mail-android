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
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.asLoadMoreFlow
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.loadMoreCombineTransform
import ch.protonmail.android.domain.loadMoreMap
import ch.protonmail.android.drawer.presentation.mapper.DrawerFoldersAndLabelsSectionUiModelMapper
import ch.protonmail.android.drawer.presentation.model.DrawerFoldersAndLabelsSectionUiModel
import ch.protonmail.android.jobs.ApplyLabelJob
import ch.protonmail.android.jobs.PostStarJob
import ch.protonmail.android.jobs.RemoveLabelJob
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
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.usecase.ObserveAllUnreadCounters
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationModeEnabled
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationsByLocation
import ch.protonmail.android.mailbox.domain.usecase.ObserveMessagesByLocation
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem
import ch.protonmail.android.mailbox.presentation.model.MessageData
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.UserUtils
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject
import kotlin.collections.set

const val FLOW_START_ACTIVITY = 1
const val FLOW_USED_SPACE_CHANGED = 2
const val FLOW_TRY_COMPOSE = 3
private const val STARRED_LABEL_ID = "10"
private const val MIN_MESSAGES_TO_SHOW_COUNT = 2
private typealias ApplyRemoveLabelsPair = Pair<List<String>, List<String>>

@HiltViewModel
internal class MailboxViewModel @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository,
    private val userManager: UserManager,
    private val jobManager: JobManager,
    private val deleteMessage: DeleteMessage,
    private val dispatchers: DispatcherProvider,
    private val contactsRepository: ContactsRepository,
    private val labelRepository: LabelRepository,
    verifyConnection: VerifyConnection,
    networkConfigurator: NetworkConfigurator,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val observeConversationModeEnabled: ObserveConversationModeEnabled,
    private val observeMessagesByLocation: ObserveMessagesByLocation,
    private val observeConversationsByLocation: ObserveConversationsByLocation,
    private val changeConversationsReadStatus: ChangeConversationsReadStatus,
    private val changeConversationsStarredStatus: ChangeConversationsStarredStatus,
    private val observeAllUnreadCounters: ObserveAllUnreadCounters,
    private val moveConversationsToFolder: MoveConversationsToFolder,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val deleteConversations: DeleteConversations,
    private val observeLabels: ObserveLabels,
    private val drawerFoldersAndLabelsSectionUiModelMapper: DrawerFoldersAndLabelsSectionUiModelMapper,
    private val getMailSettings: GetMailSettings,
    private val messageRecipientToCorrespondentMapper: MessageRecipientToCorrespondentMapper
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
    private val mutableRefreshFlow = MutableSharedFlow<Boolean>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

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

    val drawerLabels: Flow<DrawerFoldersAndLabelsSectionUiModel> = combine(
        mutableUserId,
        mutableRefreshFlow.onStart { emit(false) }
    ) { userId, isRefresh -> userId to isRefresh }
        .flatMapLatest { userIdPair -> observeLabels(userIdPair.first, userIdPair.second) }
        .map { labels ->
            drawerFoldersAndLabelsSectionUiModelMapper.toUiModel(
                labels.filter { it.type != LabelType.CONTACT_GROUP }
            )
        }

    val unreadCounters: Flow<List<UnreadCounter>> = combine(
        mutableUserId,
        mutableRefreshFlow.onStart { emit(false) }
    ) { userId, _ -> userId }
        .flatMapLatest { userId ->
            combineTransform(
                observeAllUnreadCounters(userId),
                observeConversationModeEnabled(userId)
            ) { allCountersResult, isConversationsModeEnabled ->

                if (allCountersResult is DataResult.Error) {
                    Timber.e(allCountersResult.cause, allCountersResult.message)
                }

                if (allCountersResult is DataResult.Success) {
                    val value = if (isConversationsModeEnabled) {
                        allCountersResult.value.conversationsCounters
                    } else {
                        allCountersResult.value.messagesCounters
                    }
                    emit(value)
                }
            }
        }

    private lateinit var mailboxStateFlow: LoadMoreFlow<MailboxState>

    init {
        combine(
            mutableMailboxLocation,
            mutableMailboxLabelId,
            mutableUserId,
            mutableRefreshFlow.onStart { emit(false) }
        ) { location, label, userId, isRefresh ->
            Timber.v("New location: $location, label: $label, user: $userId, isRefresh: $isRefresh")
            Triple(location, label, userId)
        }
            .onEach {
                mutableMailboxState.value = MailboxState.Loading
            }
            .flatMapLatest { parameters ->
                val location = parameters.first
                val labelId = parameters.second
                val userId = parameters.third

                mailboxStateFlow = if (conversationModeEnabled(location)) {
                    Timber.v("Getting conversations for $location, label: $labelId, user: $userId")
                    conversationsAsMailboxItems(location, labelId, userId)
                } else {
                    Timber.v("Getting messages for $location, label: $labelId, user: $userId")
                    messagesAsMailboxItems(location, labelId, userId)
                }
                mailboxStateFlow
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

    fun processLabels(
        messageIds: List<String>,
        checkedLabelIds: List<String>,
        unchangedLabels: List<String>
    ) {
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
                        val labels = getAllLabelsByIds(currentLabelsIds, userManager.requireCurrentUserId())
                        val applyRemoveLabels = resolveMessageLabels(
                            message, ArrayList(checkedLabelIds),
                            ArrayList(unchangedLabels),
                            labels
                        )
                        val apply = applyRemoveLabels?.first
                        val remove = applyRemoveLabels?.second
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

    /**
     * Request to fetch more items from API
     */
    fun loadMore() {
        val location = mailboxLocation.value
        Timber.v("loadMailboxItems location: $location")
        mailboxStateFlow.loadMore()
    }

    fun messagesToMailboxItemsBlocking(messages: List<Message>) = runBlocking {
        return@runBlocking messagesToMailboxItems(messages, null)
    }

    private fun conversationsAsMailboxItems(
        location: Constants.MessageLocationType,
        labelId: String?,
        userId: UserId
    ): LoadMoreFlow<MailboxState> {
        val locationId = labelId?.takeIfNotBlank() ?: location.messageLocationTypeValue.toString()
        Timber.v("conversationsAsMailboxItems locationId: $locationId")
        var isFirstData = true
        var hasReceivedFirstApiRefresh: Boolean? = null
        return loadMoreCombineTransform<List<Label>, GetConversationsResult, Pair<List<Label>, GetConversationsResult>>(
            observeLabels(userId),
            observeConversationsByLocation(
                userId,
                locationId
            )
        ) { conversations, labels ->
            emit(conversations to labels)
        }
            .loadMoreMap { pair ->
                val labels = pair.first
                when (val result = pair.second) {
                    is GetConversationsResult.Success -> {
                        val shouldResetPosition = isFirstData || hasReceivedFirstApiRefresh == true
                        isFirstData = false

                        MailboxState.Data(
                            conversationsToMailboxItems(result.conversations, locationId, labels),
                            isFreshData = hasReceivedFirstApiRefresh != null,
                            shouldResetPosition = shouldResetPosition
                        )
                    }
                    is GetConversationsResult.DataRefresh -> {
                        if (hasReceivedFirstApiRefresh == null) hasReceivedFirstApiRefresh = true
                        else if (hasReceivedFirstApiRefresh == true) hasReceivedFirstApiRefresh = false

                        MailboxState.DataRefresh(
                            lastFetchedItemsIds = result.lastFetchedConversations.map { it.id }
                        )
                    }
                    is GetConversationsResult.Error -> {
                        hasReceivedFirstApiRefresh = false

                        MailboxState.Error(
                            error = "Failed getting conversations",
                            throwable = result.throwable,
                            isOffline = result.isOffline
                        )
                    }
                    is GetConversationsResult.Loading ->
                        MailboxState.Loading
                }
            }
    }

    private fun messagesAsMailboxItems(
        location: Constants.MessageLocationType,
        labelId: String?,
        userId: UserId
    ): LoadMoreFlow<MailboxState> {
        Timber.v("messagesAsMailboxItems location: $location, labelId: $labelId")
        var isFirstData = true
        var hasReceivedFirstApiRefresh: Boolean? = null
        return loadMoreCombineTransform<List<Label>, GetMessagesResult, Pair<List<Label>, GetMessagesResult>>(
            observeLabels(userId),
            observeMessagesByLocation(
                userId = userId,
                mailboxLocation = location,
                labelId = labelId
            )
        ) { messages, labels ->
            emit(messages to labels)
        }.loadMoreMap { pair ->
            val labels = pair.first
            when (val result = pair.second) {
                is GetMessagesResult.Success -> {
                    val shouldResetPosition = isFirstData || hasReceivedFirstApiRefresh == true
                    isFirstData = false

                    MailboxState.Data(
                        items = messagesToMailboxItems(result.messages, labels),
                        isFreshData = hasReceivedFirstApiRefresh != null,
                        shouldResetPosition = shouldResetPosition
                    )
                }
                is GetMessagesResult.DataRefresh -> {
                    if (hasReceivedFirstApiRefresh == null) hasReceivedFirstApiRefresh = true
                    else if (hasReceivedFirstApiRefresh == true) hasReceivedFirstApiRefresh = false

                    MailboxState.DataRefresh(
                        lastFetchedItemsIds = result.lastFetchedMessages.mapNotNull { it.messageId }
                    )
                }
                is GetMessagesResult.Error -> {
                    hasReceivedFirstApiRefresh = false

                    MailboxState.Error(
                        error = "GetMessagesResult Error",
                        throwable = result.throwable,
                        isOffline = result.isOffline
                    )
                }
                is GetMessagesResult.Loading ->
                    MailboxState.Loading
            }
        }
    }

    private suspend fun conversationsToMailboxItems(
        conversations: List<Conversation>,
        locationId: String,
        labels: List<Label>
    ): List<MailboxUiItem> {
        val contacts = contactsRepository.findAllContactEmails().first()

        return conversations.map { conversation ->
            val lastMessageTimeMs = conversation.labels.find {
                it.id == locationId
            }?.contextTime?.let { it * 1000 } ?: 0

            val conversationLabelsIds = conversation.labels.map { it.id }
            val labelChipUiModels = labels
                .filter { it.id.id in conversationLabelsIds }
                .toLabelChipUiModels()

            val isDraft = conversationContainsSingleDraftMessage(conversation)

            MailboxUiItem(
                itemId = conversation.id,
                senderName = conversation.senders.joinToString { getCorrespondentDisplayName(it, contacts) },
                subject = conversation.subject,
                lastMessageTimeMs = lastMessageTimeMs,
                hasAttachments = conversation.attachmentsCount > 0,
                isStarred = conversation.labels.any { it.id == STARRED_LABEL_ID },
                isRead = conversation.unreadCount == 0,
                expirationTime = conversation.expirationTime,
                messagesCount = getDisplayMessageCount(conversation),
                messageData = null,
                isDeleted = false,
                labels = labelChipUiModels,
                recipients = conversation.receivers.joinToString { it.name },
                isDraft = isDraft
            )
        }
    }

    private fun conversationContainsSingleDraftMessage(
        conversation: Conversation
    ) = conversation.messagesCount == 1 && conversation.labels.any {
        it.id == Constants.MessageLocationType.DRAFT.messageLocationTypeValue.toString() ||
            it.id == Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue.toString()
    }

    private fun getDisplayMessageCount(conversation: Conversation) =
        conversation.messagesCount.let {
            if (it >= MIN_MESSAGES_TO_SHOW_COUNT) {
                it
            } else {
                null
            }
        }

    private suspend fun messagesToMailboxItems(messages: List<Message>, labelsList: List<Label>?): List<MailboxUiItem> {
        Timber.v("messagesToMailboxItems size: ${messages.size}")

        val emails = messages.map { message -> message.senderEmail }.distinct()
        val contacts = emails
            .chunked(Constants.MAX_SQL_ARGUMENTS)
            .flatMap { emailChunk -> contactsRepository.findContactsByEmail(emailChunk).first() }
        val labelIds = messages.flatMap { message -> message.allLabelIDs }.distinct().map { LabelId(it) }

        Timber.v("messagesToMailboxItems labels: $labelIds")

        val labels = labelsList?.toLabelChipUiModels()
            ?: labelIds
                .chunked(Constants.MAX_SQL_ARGUMENTS)
                .flatMap { labelChunk -> labelRepository.findLabels(labelChunk) }
                .toLabelChipUiModels()

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
                .filter { it.id.id in message.allLabelIDs }

            val messageLocation = message.locationFromLabel()
            val isDraft = messageLocation == Constants.MessageLocationType.DRAFT ||
                messageLocation == Constants.MessageLocationType.ALL_DRAFT

            val mailboxUiItem = MailboxUiItem(
                itemId = requireNotNull(message.messageId),
                senderName = senderName,
                subject = requireNotNull(message.subject),
                lastMessageTimeMs = message.timeMs,
                hasAttachments = message.numAttachments > 0,
                isStarred = message.isStarred ?: false,
                isRead = message.isRead,
                expirationTime = message.expirationTime,
                messagesCount = null,
                messageData = messageData,
                isDeleted = message.deleted,
                labels = labelChipUiModels,
                recipients = message.toList.joinToString {
                    getCorrespondentDisplayName(
                        messageRecipientToCorrespondentMapper.toDomainModel(it), contacts
                    )
                },
                isDraft = isDraft
            )
            mailboxUiItem
        }
    }

    private fun getSenderDisplayName(message: Message, contacts: List<ContactEmail>): String {
        val name = message.senderDisplayName?.takeIfNotBlank()
            ?: message.senderName?.takeIfNotBlank()
            ?: message.senderEmail

        return getCorrespondentDisplayName(
            Correspondent(name, message.senderEmail),
            contacts
        )
    }

    private fun getCorrespondentDisplayName(
        correspondent: Correspondent,
        contacts: List<ContactEmail>
    ): String {
        val senderNameFromContacts = contacts.find { correspondent.address == it.email }?.name

        return senderNameFromContacts?.takeIfNotBlank()?.takeIf { it != correspondent.address }
            ?: correspondent.name.takeIfNotBlank()
            ?: correspondent.address
    }

    private suspend fun getAllLabelsByIds(labelIds: List<String>, userId: UserId) =
        messageDetailsRepository.findLabelsWithIds(labelIds)

    private fun resolveMessageLabels(
        message: Message,
        checkedLabelIds: MutableList<String>,
        unchangedLabels: List<String>,
        currentContactLabels: List<Label>?
    ): ApplyRemoveLabelsPair? {
        val labelsToRemove = ArrayList<String>()

        currentContactLabels?.forEach {
            val labelId = it.id.id
            if (!checkedLabelIds.contains(labelId) && !unchangedLabels.contains(
                    labelId
                ) && it.type == LabelType.MESSAGE_LABEL
            ) {
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

        return ApplyRemoveLabelsPair(checkedLabelIds, labelsToRemove)
    }

    fun deleteAction(
        ids: List<String>,
        userId: UserId,
        currentLocation: Constants.MessageLocationType
    ) {
        viewModelScope.launch {
            if (conversationModeEnabled(currentLocation)) {
                deleteConversations(ids, userId, currentLocation.messageLocationTypeValue.toString())
            } else {
                val deleteMessagesResult = deleteMessage(ids, currentLocation.messageLocationTypeValue.toString())
                _hasSuccessfullyDeletedMessages.postValue(deleteMessagesResult.isSuccessfullyDeleted)
            }
        }
    }

    private fun List<Label>.toLabelChipUiModels(): List<LabelChipUiModel> =
        filter { it.type == LabelType.MESSAGE_LABEL }.map { label ->
            val labelColor = label.color.takeIfNotBlank()
                ?.let { Color.parseColor(UiUtil.normalizeColor(it)) }

            LabelChipUiModel(label.id, Name(label.name), labelColor)
        }

    fun markRead(
        ids: List<String>,
        userId: UserId,
        location: Constants.MessageLocationType,
        locationId: String
    ) {
        if (conversationModeEnabled(location)) {
            viewModelScope.launch {
                changeConversationsReadStatus(
                    ids,
                    ChangeConversationsReadStatus.Action.ACTION_MARK_READ,
                    userId,
                    locationId
                )
            }
        } else {
            messageDetailsRepository.markRead(ids)
        }
    }

    fun markUnRead(
        ids: List<String>,
        userId: UserId,
        location: Constants.MessageLocationType,
        locationId: String
    ) {
        if (conversationModeEnabled(location)) {
            viewModelScope.launch {
                changeConversationsReadStatus(
                    ids,
                    ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                    userId,
                    locationId
                )
            }
        } else {
            messageDetailsRepository.markUnRead(ids)
        }
    }

    fun star(ids: List<String>, userId: UserId, location: Constants.MessageLocationType) {
        if (conversationModeEnabled(location)) {
            viewModelScope.launch {
                changeConversationsStarredStatus(
                    ids,
                    userId,
                    ChangeConversationsStarredStatus.Action.ACTION_STAR
                )
            }
        } else {
            jobManager.addJobInBackground(PostStarJob(ids))
        }
    }

    fun moveToFolder(
        ids: List<String>,
        userId: UserId,
        currentLocation: Constants.MessageLocationType,
        destinationFolderId: String
    ) {
        viewModelScope.launch {
            if (conversationModeEnabled(currentLocation)) {
                moveConversationsToFolder(
                    ids,
                    userId,
                    destinationFolderId
                )
            } else {
                moveMessagesToFolder(
                    ids,
                    destinationFolderId,
                    currentLocation.messageLocationTypeValue.toString(),
                    userId
                )
            }
        }
    }

    fun setNewMailboxLocation(newLocation: Constants.MessageLocationType) {
        if (mutableMailboxLocation.value != newLocation) {
            mutableMailboxLocation.value = newLocation
        }
    }

    fun setNewMailboxLabel(labelId: String) {
        if (mutableMailboxLabelId.value != labelId) {
            mutableMailboxLabelId.value = labelId
        }
    }

    fun setNewUserId(currentUserId: UserId) {
        if (mutableUserId.value != currentUserId) {
            mutableUserId.value = currentUserId
        }
    }

    fun refreshMessages() {
        mutableRefreshFlow.tryEmit(true)
    }

    fun handleConversationSwipe(
        swipeAction: SwipeAction,
        conversationId: String,
        mailboxLocation: Constants.MessageLocationType,
        mailboxLocationId: String
    ) {
        when (swipeAction) {
            SwipeAction.TRASH ->
                moveToFolder(
                    listOf(conversationId),
                    UserId(userManager.requireCurrentUserId().id),
                    mailboxLocation,
                    Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
                )
            SwipeAction.SPAM ->
                moveToFolder(
                    listOf(conversationId),
                    UserId(userManager.requireCurrentUserId().id),
                    mailboxLocation,
                    Constants.MessageLocationType.SPAM.messageLocationTypeValue.toString()
                )
            SwipeAction.STAR ->
                star(
                    listOf(conversationId),
                    UserId(userManager.requireCurrentUserId().id),
                    mailboxLocation
                )
            SwipeAction.ARCHIVE ->
                moveToFolder(
                    listOf(conversationId),
                    UserId(userManager.requireCurrentUserId().id),
                    mailboxLocation,
                    Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
                )
            SwipeAction.MARK_READ ->
                markRead(
                    listOf(conversationId),
                    UserId(userManager.requireCurrentUserId().id),
                    mailboxLocation,
                    mailboxLocationId
                )
        }
    }

    suspend fun getMailSettingsState() = getMailSettings()

    data class MaxLabelsReached(val subject: String?, val maxAllowedLabels: Int)
}
