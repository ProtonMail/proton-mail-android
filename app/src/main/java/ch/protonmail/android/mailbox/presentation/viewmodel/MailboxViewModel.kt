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
package ch.protonmail.android.mailbox.presentation.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import arrow.core.Either
import arrow.core.Right
import arrow.core.left
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
import ch.protonmail.android.feature.NotLoggedIn
import ch.protonmail.android.feature.rating.usecase.ShouldStartRateAppFlow
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.usecase.ObserveLabels
import ch.protonmail.android.labels.domain.usecase.ObserveLabelsAndFoldersWithChildren
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.AllUnreadCounters
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters.UnreadStatus
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.usecase.ObserveAllUnreadCounters
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationsByLocation
import ch.protonmail.android.mailbox.domain.usecase.ObserveMessagesByLocation
import ch.protonmail.android.mailbox.presentation.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailbox.presentation.model.MailboxItemUiModel
import ch.protonmail.android.mailbox.presentation.model.MailboxListState
import ch.protonmail.android.mailbox.presentation.model.MailboxState
import ch.protonmail.android.mailbox.presentation.model.UnreadChipState
import ch.protonmail.android.mailbox.presentation.model.UnreadChipUiModel
import ch.protonmail.android.mailbox.presentation.util.ConversationModeEnabled
import ch.protonmail.android.notifications.presentation.usecase.ClearNotificationsForUser
import ch.protonmail.android.settings.domain.usecase.GetMailSettings
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
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
    private val clearNotificationsForUser: ClearNotificationsForUser,
    private val shouldStartRateAppFlow: ShouldStartRateAppFlow
) : ConnectivityBaseViewModel(verifyConnection, networkConfigurator) {

    private val _manageLimitReachedWarning = MutableLiveData<Event<Boolean>>()
    private val _manageLimitApproachingWarning = MutableLiveData<Event<Boolean>>()
    private val _manageLimitBelowCritical = MutableLiveData<Event<Boolean>>()
    private val _manageLimitReachedWarningOnTryCompose = MutableLiveData<Event<Boolean>>()
    private val _toastMessageMaxLabelsReached = MutableLiveData<Event<MaxLabelsReached>>()
    private val _hasSuccessfullyDeletedMessages = MutableLiveData<Boolean>()
    private val mutableMailboxState = MutableStateFlow<MailboxState>(MailboxState.Loading)
    private val mutableMailboxLocation = MutableStateFlow(INBOX)
    private val mutableMailboxLabelId = MutableStateFlow<String?>(null)
    private val mutableUserId = userManager.primaryUserId
    private val mutableRefreshFlow = MutableSharedFlow<Boolean>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _exitSelectionModeSharedFlow = MutableSharedFlow<Boolean>()
    private val _startRateAppFlow = MutableSharedFlow<Unit>()

    private val messageDetailsRepository: MessageDetailsRepository
        get() = messageDetailsRepositoryFactory.create(userManager.requireCurrentUserId())

    private val isUnreadFilterEnabled = MutableStateFlow(false)

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

    val startRateAppFlow: SharedFlow<Unit>
        get() = _startRateAppFlow

    val mailboxState = mutableMailboxState.asStateFlow()
    val mailboxLocation = mutableMailboxLocation.asStateFlow()

    val drawerLabels: Flow<DrawerFoldersAndLabelsSectionUiModel> = combine(
        mutableUserId.filterNotNull(),
        mutableRefreshFlow.onStart { emit(true) }
    ) { userId, isRefresh -> userId to isRefresh }
        .flatMapLatest { userIdPair ->
            observeLabelsAndFoldersWithChildren(userId = userIdPair.first, shallRefresh = userIdPair.second)
        }
        .map { labelsAndFolders ->
            drawerFoldersAndLabelsSectionUiModelMapper.toUiModels(labelsAndFolders)
        }

    private val allCounters = mutableUserId.filterNotNull()
        .flatMapLatest { userId -> observeAllUnreadCounters(userId) }
        .filterIsInstance<DataResult.Success<AllUnreadCounters>>()
        .map { countersDataResult ->
            if (conversationModeEnabled(mailboxLocation.value)) {
                countersDataResult.value.conversationsCounters
            } else {
                countersDataResult.value.messagesCounters
            }
        }

    val unreadCounters: Flow<List<UnreadCounter>> = combine(
        mailboxLocation,
        mutableMailboxLabelId,
        mutableRefreshFlow.onStart { emit(false) },
        allCounters
    ) { _, _, _, allCounters -> allCounters }
        .onEach { allCounters ->
            val currentLabelId = getLabelId(mailboxLocation.value, mutableMailboxLabelId.value).id
            val currentLocationUnreadCounter = allCounters.find { it.labelId == currentLabelId }
                ?.unreadCount
                ?: 0
            val newUnreadChipState = UnreadChipState.Data(
                UnreadChipUiModel(
                    isFilterEnabled = isUnreadFilterEnabled.value,
                    unreadCount = currentLocationUnreadCounter
                )
            )
            val newMailboxState = mailboxState.value.copy(unreadChip = newUnreadChipState)
            mutableMailboxState.emit(newMailboxState)
        }

    private lateinit var mailboxStateFlow: LoadMoreFlow<MailboxListState>

    init {
        combine(
            mutableMailboxLocation,
            mutableMailboxLabelId,
            mutableUserId.filterNotNull(),
            isUnreadFilterEnabled,
            mutableRefreshFlow.onStart { emit(false) }
        ) { location, label, userId, isUnreadFilterEnabled, isRefresh ->
            Timber.v("New location: $location, label: $label, user: $userId, isRefresh: $isRefresh")
            GetMailboxItemsParameters(
                userId = userId,
                labelId = getLabelId(location, label),
                isUnreadFilterEnabled = isUnreadFilterEnabled
            )
        }
            .onEach {
                val newState = mailboxState.value.copy(list = MailboxListState.Loading)
                mutableMailboxState.value = newState
            }
            .flatMapLatest { params ->
                val userId = params.userId
                val labelId = requireNotNull(params.labelId) { "labelId is null" }

                mailboxStateFlow = if (conversationModeEnabled(userId, labelId)) {
                    Timber.v("Getting conversations for label: $labelId, user: $userId")
                    conversationsAsMailboxItems(params.toGetAllConversationsParameters())
                } else {
                    Timber.v("Getting messages for label: $labelId, user: $userId")
                    messagesAsMailboxItems(params.toGetAllMessagesParameters())
                }
                mailboxStateFlow
            }
            .catch {
                emit(
                    MailboxListState.Error(
                        "Failed getting messages, catch",
                        it
                    )
                )
            }
            .onEach { mailboxListState ->
                mutableMailboxState.value = mailboxState.value.copy(list = mailboxListState)
            }
            .launchIn(viewModelScope)

        val observePullToRefreshEvents = mutableRefreshFlow.filter { it }

        fun Flow<Boolean>.waitForRefreshedDataToArrive() = flatMapLatest {
            mutableMailboxState.filter { it.list is MailboxListState.DataRefresh }.take(1)
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

    fun messagesToMailboxItemsBlocking(messages: List<Message>): List<MailboxItemUiModel> = runBlocking {
        val userId = userManager.currentUserId
            ?: return@runBlocking emptyList()
        val currentLabelId = getLabelId(mailboxLocation.value, mutableMailboxLabelId.value)
        return@runBlocking messagesToMailboxItems(userId, messages, currentLabelId, null)
    }

    private fun conversationsAsMailboxItems(params: GetAllConversationsParameters): LoadMoreFlow<MailboxListState> {
        val userId = params.userId
        val labelId = requireNotNull(params.labelId) { "labelId is null" }

        Timber.v("conversationsAsMailboxItems labelId: $labelId")
        var isFirstData = true
        var hasReceivedFirstApiRefresh: Boolean? = null
        return loadMoreCombine(
            observeLabels(userId),
            observeConversationsByLocation(params)
        ) { labels, conversations -> labels to conversations }
            .loadMoreBuffer()
            .loadMoreMap { (labels, result) ->
                when (result) {
                    is GetConversationsResult.Success -> {
                        val shouldResetPosition = isFirstData || hasReceivedFirstApiRefresh == true
                        isFirstData = false

                        MailboxListState.Data(
                            conversationsToMailboxItems(userId, result.conversations, labelId, labels),
                            isFreshData = hasReceivedFirstApiRefresh != null,
                            shouldResetPosition = shouldResetPosition
                        )
                    }
                    is GetConversationsResult.DataRefresh -> {
                        if (hasReceivedFirstApiRefresh == null) hasReceivedFirstApiRefresh = true
                        else if (hasReceivedFirstApiRefresh == true) hasReceivedFirstApiRefresh = false

                        MailboxListState.DataRefresh(
                            lastFetchedItemsIds = result.lastFetchedConversations.map { it.id }
                        )
                    }
                    is GetConversationsResult.Error -> {
                        hasReceivedFirstApiRefresh = false

                        MailboxListState.Error(
                            error = "Failed getting conversations",
                            throwable = result.throwable,
                            isOffline = result.isOffline
                        )
                    }
                    is GetConversationsResult.Loading ->
                        MailboxListState.Loading
                }
            }
    }

    private fun messagesAsMailboxItems(params: GetAllMessagesParameters): LoadMoreFlow<MailboxListState> {
        val labelId = requireNotNull(params.labelId) { "labelId is null" }

        Timber.v("messagesAsMailboxItems labelId: ${params.labelId}")
        var isFirstData = true
        var hasReceivedFirstApiRefresh: Boolean? = null
        return loadMoreCombine(
            observeLabels(params.userId),
            observeMessagesByLocation(params)
        ) { labels, messages -> labels to messages }
            .loadMoreBuffer()
            .loadMoreMap { pair ->
                val labels = pair.first
                when (val result = pair.second) {
                    is GetMessagesResult.Success -> {
                        val shouldResetPosition = isFirstData || hasReceivedFirstApiRefresh == true
                        isFirstData = false

                        MailboxListState.Data(
                            items = messagesToMailboxItems(
                                userId = params.userId,
                                messages = result.messages,
                                currentLabelId = labelId,
                                labelsList = labels
                            ),
                            isFreshData = hasReceivedFirstApiRefresh != null,
                            shouldResetPosition = shouldResetPosition
                        )
                    }
                    is GetMessagesResult.DataRefresh -> {
                        if (hasReceivedFirstApiRefresh == null) hasReceivedFirstApiRefresh = true
                        else if (hasReceivedFirstApiRefresh == true) hasReceivedFirstApiRefresh = false

                        MailboxListState.DataRefresh(
                            lastFetchedItemsIds = result.lastFetchedMessages.mapNotNull { it.messageId }
                        )
                    }
                    is GetMessagesResult.Error -> {
                        hasReceivedFirstApiRefresh = false

                        MailboxListState.Error(
                            error = "GetMessagesResult Error",
                            throwable = result.throwable,
                            isOffline = result.isOffline
                        )
                    }
                    is GetMessagesResult.Loading ->
                        MailboxListState.Loading
                }
            }
    }

    private fun getLabelId(location: Constants.MessageLocationType, labelId: String?): LabelId =
        labelId?.let(::LabelId) ?: location.asLabelId()

    private suspend fun conversationsToMailboxItems(
        userId: UserId,
        conversations: List<Conversation>,
        labelId: LabelId,
        labels: List<Label>
    ): List<MailboxItemUiModel> =
        mailboxItemUiModelMapper.toUiModels(
            userId = userId,
            conversations = conversations,
            currentLabelId = labelId,
            allLabels = labels
        )

    private suspend fun messagesToMailboxItems(
        userId: UserId,
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

    fun enableUnreadFilter() {
        viewModelScope.launch {
            exitSelectionMode(areMailboxItemsMovedFromLocation = true)
            toggleUnreadFilter(isFilterEnabled = true)
        }
    }

    fun disableUnreadFilter() {
        viewModelScope.launch {
            exitSelectionMode(areMailboxItemsMovedFromLocation = true)
            toggleUnreadFilter(isFilterEnabled = false)
        }
    }

    private suspend fun toggleUnreadFilter(isFilterEnabled: Boolean) {
        val prevMailboxState = mailboxState.value
        val prevUnreadChipState = prevMailboxState.unreadChip
        if (prevUnreadChipState is UnreadChipState.Data) {
            val newChipUiModel = prevUnreadChipState.model.copy(isFilterEnabled = isFilterEnabled)
            val newUnreadChipState = prevUnreadChipState.copy(model = newChipUiModel)
            mutableMailboxState.emit(prevMailboxState.copy(unreadChip = newUnreadChipState))
        }

        isUnreadFilterEnabled.emit(isFilterEnabled)
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
            mutableMailboxLabelId.value = labelId.takeIfNotBlank()
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

    fun getMailSettingsState(): Flow<Either<NotLoggedIn, GetMailSettings.Result>> =
        userManager.primaryUserId.flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest flowOf(NotLoggedIn.left())
            }

            getMailSettings(userId).map(::Right)
        }

    fun startRateAppFlowIfNeeded() {
        val userId = userManager.currentUserId ?: return
        viewModelScope.launch {
            shouldStartRateAppFlow(userId).let { startFlow ->
                if (startFlow) {
                    _startRateAppFlow.emit(Unit)
                }
            }
        }
    }

    data class GetMailboxItemsParameters(
        val userId: UserId,
        val labelId: LabelId,
        val isUnreadFilterEnabled: Boolean
    ) {

        fun toGetAllConversationsParameters() = GetAllConversationsParameters(
            userId = userId,
            labelId = labelId
        )

        fun toGetAllMessagesParameters() = GetAllMessagesParameters(
            userId = userId,
            labelId = labelId,
            unreadStatus = if (isUnreadFilterEnabled) UnreadStatus.UNREAD_ONLY else UnreadStatus.ALL,
            sortDirection = if (labelId.id == Constants.MessageLocationType.ALL_SCHEDULED.asLabelIdString()) {
                GetAllMessagesParameters.SortDirection.ASCENDANT
            } else {
                GetAllMessagesParameters.SortDirection.DESCENDANT
            }
        )
    }

    data class MaxLabelsReached(val subject: String?, val maxAllowedLabels: Int)
}
