/*
 * Copyright (c) 2022 Proton Technologies AG
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.segments.event.FetchEventsAndReschedule
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.loadMoreBuffer
import ch.protonmail.android.domain.loadMoreCombine
import ch.protonmail.android.domain.loadMoreMap
import ch.protonmail.android.drawer.presentation.mapper.DrawerFoldersAndLabelsSectionUiModelMapper
import ch.protonmail.android.drawer.presentation.model.DrawerFoldersAndLabelsSectionUiModel
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.usecase.ObserveLabels
import ch.protonmail.android.labels.domain.usecase.ObserveLabelsAndFoldersWithChildren
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.usecase.ObserveAllUnreadCounters
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationModeEnabled
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationsByLocation
import ch.protonmail.android.mailbox.domain.usecase.ObserveMessagesByLocation
import ch.protonmail.android.mailbox.presentation.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailbox.presentation.model.MailboxItemUiModel
import ch.protonmail.android.notifications.presentation.usecase.ClearNotificationsForUser
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.delete.EmptyFolder
import ch.protonmail.android.usecase.message.ChangeMessagesReadStatus
import ch.protonmail.android.usecase.message.ChangeMessagesStarredStatus
import ch.protonmail.android.utils.Event
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject

const val FLOW_START_ACTIVITY = 1
const val FLOW_USED_SPACE_CHANGED = 2
const val FLOW_TRY_COMPOSE = 3

@Suppress("LongParameterList") // Every new parameter adds a new issue and breaks the build
@HiltViewModel
internal class MailboxViewModel @Inject constructor(
    private val messageDetailsRepositoryFactory: MessageDetailsRepository.AssistedFactory,
    private val userManager: UserManager,
    private val deleteMessage: DeleteMessage,
    private val labelRepository: LabelRepository,
    verifyConnection: VerifyConnection,
    networkConfigurator: NetworkConfigurator,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val observeConversationModeEnabled: ObserveConversationModeEnabled,
    private val observeMessagesByLocation: ObserveMessagesByLocation,
    private val observeConversationsByLocation: ObserveConversationsByLocation,
    private val changeMessagesReadStatus: ChangeMessagesReadStatus,
    private val changeConversationsReadStatus: ChangeConversationsReadStatus,
    private val changeMessagesStarredStatus: ChangeMessagesStarredStatus,
    private val changeConversationsStarredStatus: ChangeConversationsStarredStatus,
    private val observeAllUnreadCounters: ObserveAllUnreadCounters,
    private val moveConversationsToFolder: MoveConversationsToFolder,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val deleteConversations: DeleteConversations,
    private val emptyFolder: EmptyFolder,
    private val observeLabels: ObserveLabels,
    private val observeLabelsAndFoldersWithChildren: ObserveLabelsAndFoldersWithChildren,
    private val drawerFoldersAndLabelsSectionUiModelMapper: DrawerFoldersAndLabelsSectionUiModelMapper,
    private val getMailSettings: GetMailSettings,
    private val mailboxItemUiModelMapper: MailboxItemUiModelMapper,
    private val fetchEventsAndReschedule: FetchEventsAndReschedule,
    private val clearNotificationsForUser: ClearNotificationsForUser
) : ConnectivityBaseViewModel(verifyConnection, networkConfigurator) {

    private val _manageLimitReachedWarning = MutableLiveData<Event<Boolean>>()
    private val _manageLimitApproachingWarning = MutableLiveData<Event<Boolean>>()
    private val _manageLimitBelowCritical = MutableLiveData<Event<Boolean>>()
    private val _manageLimitReachedWarningOnTryCompose = MutableLiveData<Event<Boolean>>()
    private val _toastMessageMaxLabelsReached = MutableLiveData<Event<MaxLabelsReached>>()
    private val _hasSuccessfullyDeletedMessages = MutableLiveData<Boolean>()
    private val mutableMailboxState = MutableStateFlow<MailboxState>(MailboxState.Loading)
    private val mutableMailboxLocation = MutableStateFlow(INBOX)
    private val mutableMailboxLabelId = MutableStateFlow(EMPTY_STRING)
    private val mutableUserId = userManager.primaryUserId
    private val mutableRefreshFlow = MutableSharedFlow<Boolean>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _exitSelectionModeSharedFlow = MutableSharedFlow<Boolean>()

    private val messageDetailsRepository: MessageDetailsRepository
        get() = messageDetailsRepositoryFactory.create(userManager.requireCurrentUserId())

    var pendingSendsLiveData = mutableUserId.filterNotNull().asLiveData().switchMap {
        messageDetailsRepository.findAllPendingSendsAsync()
    }
    var pendingUploadsLiveData = mutableUserId.filterNotNull().asLiveData().switchMap {
        messageDetailsRepository.findAllPendingUploadsAsync()
    }

    val primaryUserId: Flow<UserId> = mutableUserId.filterNotNull()

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

    val exitSelectionModeSharedFlow: SharedFlow<Boolean>
        get() = _exitSelectionModeSharedFlow

    val mailboxState = mutableMailboxState.asStateFlow()
    val mailboxLocation = mutableMailboxLocation.asStateFlow()

    val drawerLabels: Flow<DrawerFoldersAndLabelsSectionUiModel> = combine(
        mutableUserId.filterNotNull(),
        mutableRefreshFlow.onStart { emit(false) }
    ) { userId, isRefresh -> userId to isRefresh }
        .flatMapLatest { userIdPair ->
            observeLabelsAndFoldersWithChildren(userIdPair.first, userIdPair.second)
        }
        .map { labelsAndFolders ->
            drawerFoldersAndLabelsSectionUiModelMapper.toUiModels(labelsAndFolders)
        }

    val unreadCounters: Flow<List<UnreadCounter>> = combine(
        mutableUserId,
        mutableRefreshFlow.onStart { emit(false) }
    ) { userId, _ -> userId }
        .flatMapLatest { userId ->
            if (userId == null) return@flatMapLatest flowOf(emptyList())
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
            mutableUserId.filterNotNull(),
            mutableRefreshFlow.onStart { emit(false) }
        ) { location, label, userId, isRefresh ->
            Timber.v("New location: $location, label: $label, user: $userId, isRefresh: $isRefresh")
            Triple(location, label, userId)
        }
            .onEach {
                mutableMailboxState.value = MailboxState.Loading
            }
            .flatMapLatest { (location, labelId, userId) ->
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

        val observePullToRefreshEvents = mutableRefreshFlow.filter { it }

        fun Flow<Boolean>.waitForRefreshedDataToArrive() = flatMapLatest {
            mutableMailboxState.filter { it is MailboxState.DataRefresh }.take(1)
        }

        observePullToRefreshEvents
            .waitForRefreshedDataToArrive()
            .onEach { fetchEventsAndReschedule() }
            .launchIn(viewModelScope)
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

    /**
     * Request to fetch more items from API
     */
    fun loadMore() {
        val location = mailboxLocation.value
        Timber.v("loadMailboxItems location: $location")
        mailboxStateFlow.loadMore()
    }

    fun messagesToMailboxItemsBlocking(messages: List<Message>) = runBlocking {
        val currentLabelId = getLabelId(mailboxLocation.value, mutableMailboxLabelId.value)
        return@runBlocking messagesToMailboxItems(messages, currentLabelId, null)
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
        return loadMoreCombine(
            observeLabels(userId),
            observeConversationsByLocation(
                userId,
                locationId
            )
        ) { labels, conversations -> labels to conversations }
            .loadMoreBuffer()
            .loadMoreMap { (labels, result) ->
                when (result) {
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
        return loadMoreCombine(
            observeLabels(userId),
            observeMessagesByLocation(
                userId = userId,
                mailboxLocation = location,
                labelId = labelId
            )
        ) { labels, messages -> labels to messages }
            .loadMoreBuffer()
            .loadMoreMap { pair ->
                val labels = pair.first
                when (val result = pair.second) {
                    is GetMessagesResult.Success -> {
                        val shouldResetPosition = isFirstData || hasReceivedFirstApiRefresh == true
                        isFirstData = false

                        MailboxState.Data(
                            items = messagesToMailboxItems(
                                messages = result.messages,
                                currentLabelId = getLabelId(location, labelId),
                                labelsList = labels
                            ),
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

    private fun getLabelId(location: Constants.MessageLocationType, labelId: String?): LabelId =
        labelId?.let(::LabelId) ?: location.asLabelId()

    private suspend fun conversationsToMailboxItems(
        conversations: List<Conversation>,
        locationId: String,
        labels: List<Label>
    ): List<MailboxItemUiModel> =
        mailboxItemUiModelMapper.toUiModels(
            conversations = conversations,
            currentLabelId = LabelId(locationId),
            allLabels = labels
        )

    private suspend fun messagesToMailboxItems(
        messages: List<Message>,
        currentLabelId: LabelId,
        labelsList: List<Label>?
    ): List<MailboxItemUiModel> {
        Timber.v("messagesToMailboxItems size: ${messages.size}")

        val labelIds = messages.flatMap { message -> message.allLabelIDs }.distinct().map { LabelId(it) }

        val allLabels = labelsList ?: labelIds
            .chunked(Constants.MAX_SQL_ARGUMENTS)
            .flatMap { idsChunk -> labelRepository.findLabels(idsChunk) }

        return mailboxItemUiModelMapper.toUiModels(messages, currentLabelId, allLabels)
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
                val deleteMessagesResult = deleteMessage(
                    ids,
                    currentLocation.messageLocationTypeValue.toString(),
                    userId
                )
                _hasSuccessfullyDeletedMessages.postValue(deleteMessagesResult.isSuccessfullyDeleted)
            }
        }
    }

    fun markRead(
        ids: List<String>,
        userId: UserId,
        location: Constants.MessageLocationType,
        locationId: String
    ) {
        viewModelScope.launch {
            if (conversationModeEnabled(location)) {
                changeConversationsReadStatus(
                    ids,
                    ChangeConversationsReadStatus.Action.ACTION_MARK_READ,
                    userId,
                    locationId
                )
            } else {
                changeMessagesReadStatus(
                    ids,
                    ChangeMessagesReadStatus.Action.ACTION_MARK_READ,
                    userId
                )
            }
        }
    }

    fun markUnRead(
        ids: List<String>,
        userId: UserId,
        location: Constants.MessageLocationType,
        locationId: String
    ) {
        viewModelScope.launch {
            if (conversationModeEnabled(location)) {
                changeConversationsReadStatus(
                    ids,
                    ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                    userId,
                    locationId
                )
            } else {
                changeMessagesReadStatus(
                    ids,
                    ChangeMessagesReadStatus.Action.ACTION_MARK_UNREAD,
                    userId
                )
            }
        }
    }

    fun star(ids: List<String>, userId: UserId, location: Constants.MessageLocationType) {
        viewModelScope.launch {
            if (conversationModeEnabled(location)) {
                changeConversationsStarredStatus(
                    ids,
                    userId,
                    ChangeConversationsStarredStatus.Action.ACTION_STAR
                )
            } else {
                changeMessagesStarredStatus(
                    userId,
                    ids,
                    ChangeMessagesStarredStatus.Action.ACTION_STAR
                )
            }
        }
    }

    fun unstar(ids: List<String>, userId: UserId, location: Constants.MessageLocationType) {
        viewModelScope.launch {
            if (conversationModeEnabled(location)) {
                changeConversationsStarredStatus(
                    ids,
                    userId,
                    ChangeConversationsStarredStatus.Action.ACTION_UNSTAR
                )
            } else {
                changeMessagesStarredStatus(
                    userId,
                    ids,
                    ChangeMessagesStarredStatus.Action.ACTION_UNSTAR
                )
            }
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

    fun emptyFolderAction(userId: UserId, labelId: LabelId) {
        viewModelScope.launch {
            emptyFolder(userId, labelId)
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

    fun refreshMessages() {
        mutableRefreshFlow.tryEmit(true)
    }

    fun exitSelectionMode(areMailboxItemsMovedFromLocation: Boolean) {
        viewModelScope.launch {
            _exitSelectionModeSharedFlow.emit(areMailboxItemsMovedFromLocation)
        }
    }

    fun handleConversationSwipe(
        swipeAction: SwipeAction,
        mailboxUiItem: MailboxItemUiModel,
        mailboxLocation: Constants.MessageLocationType,
        mailboxLocationId: String
    ) {
        when (swipeAction) {
            SwipeAction.TRASH ->
                moveToFolder(
                    listOf(mailboxUiItem.itemId),
                    UserId(userManager.requireCurrentUserId().id),
                    mailboxLocation,
                    Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
                )
            SwipeAction.SPAM ->
                moveToFolder(
                    listOf(mailboxUiItem.itemId),
                    UserId(userManager.requireCurrentUserId().id),
                    mailboxLocation,
                    Constants.MessageLocationType.SPAM.messageLocationTypeValue.toString()
                )
            SwipeAction.UPDATE_STAR ->
                if (mailboxUiItem.isStarred) {
                    unstar(
                        listOf(mailboxUiItem.itemId),
                        UserId(userManager.requireCurrentUserId().id),
                        mailboxLocation
                    )
                } else {
                    star(
                        listOf(mailboxUiItem.itemId),
                        UserId(userManager.requireCurrentUserId().id),
                        mailboxLocation
                    )
                }
            SwipeAction.ARCHIVE ->
                moveToFolder(
                    listOf(mailboxUiItem.itemId),
                    UserId(userManager.requireCurrentUserId().id),
                    mailboxLocation,
                    Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
                )
            SwipeAction.MARK_READ ->
                markRead(
                    listOf(mailboxUiItem.itemId),
                    UserId(userManager.requireCurrentUserId().id),
                    mailboxLocation,
                    mailboxLocationId
                )
        }
    }

    fun clearNotifications(userId: UserId) {
        viewModelScope.launch {
            clearNotificationsForUser.invoke(userId)
        }
    }

    suspend fun getMailSettingsState() = getMailSettings()

    data class MaxLabelsReached(val subject: String?, val maxAllowedLabels: Int)
}
