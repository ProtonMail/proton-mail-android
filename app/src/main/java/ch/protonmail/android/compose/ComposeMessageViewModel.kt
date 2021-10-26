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

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.Spanned
import android.text.TextUtils
import androidx.core.net.MailTo
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.R
import ch.protonmail.android.activities.composeMessage.MessageBuilderData
import ch.protonmail.android.activities.composeMessage.UserAction
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.api.models.factories.MessageSecurityOptions
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.bl.HtmlProcessor
import ch.protonmail.android.compose.presentation.model.AddExpirationTimeToMessage
import ch.protonmail.android.compose.presentation.model.ComposeMessageEventUiModel
import ch.protonmail.android.compose.presentation.model.MessagePasswordUiModel
import ch.protonmail.android.compose.presentation.util.HtmlToSpanned
import ch.protonmail.android.compose.send.SendMessage
import ch.protonmail.android.contacts.PostResult
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.LocalAttachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.events.FetchMessageDetailEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.feature.account.allLoggedInBlocking
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob
import ch.protonmail.android.ui.view.DaysHoursPair
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.compose.SaveDraftResult
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchPublicKeys
import ch.protonmail.android.usecase.model.FetchPublicKeysRequest
import ch.protonmail.android.usecase.model.FetchPublicKeysResult
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.MailToData
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.resources.StringResourceResolver
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel
import com.squareup.otto.Subscribe
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlin.time.days
import kotlin.time.hours
import kotlin.time.seconds

const val NEW_LINE = "<br>"
const val LESS_THAN = "&lt;"
const val GREATER_THAN = "&gt;"

@HiltViewModel
class ComposeMessageViewModel @Inject constructor(
    private val composeMessageRepository: ComposeMessageRepository,
    private val userManager: UserManager,
    accountManager: AccountManager,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val deleteMessage: DeleteMessage,
    private val fetchPublicKeys: FetchPublicKeys,
    private val saveDraft: SaveDraft,
    private val dispatchers: DispatcherProvider,
    private val stringResourceResolver: StringResourceResolver,
    private val sendMessage: SendMessage,
    verifyConnection: VerifyConnection,
    networkConfigurator: NetworkConfigurator,
    private val htmlToSpanned: HtmlToSpanned,
    private val addExpirationTimeToMessage: AddExpirationTimeToMessage
) : ConnectivityBaseViewModel(verifyConnection, networkConfigurator) {

    // region events data
    private val _mergedContactsLiveData: MediatorLiveData<List<MessageRecipient>> = MediatorLiveData()
    private val _contactGroupsResult: MutableLiveData<List<MessageRecipient>> = MutableLiveData()
    private val _pmMessageRecipientsResult: MutableLiveData<List<MessageRecipient>> = MutableLiveData()
    private val _androidMessageRecipientsResult: MutableLiveData<List<MessageRecipient>> = MutableLiveData()
    private val _setupComplete: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _closeComposer: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private var _setupCompleteValue = false
    private val _savingDraftComplete: MutableLiveData<Message> = MutableLiveData()
    private val _savingDraftError: MutableLiveData<String> = MutableLiveData()
    private val _deleteResult: MutableLiveData<Event<PostResult>> = MutableLiveData()
    private val _loadingDraftResult: MutableLiveData<Message> = MutableLiveData()
    private val _messageResultError: MutableLiveData<Event<PostResult>> = MutableLiveData()
    private val _openAttachmentsScreenResult: MutableLiveData<List<LocalAttachment>> = MutableLiveData()
    private val _buildingMessageCompleted: MutableLiveData<Event<Message>> = MutableLiveData()
    private val _dbIdWatcher: MutableLiveData<Long> = MutableLiveData()
    private val fetchKeyDetailsTrigger = MutableLiveData<List<FetchPublicKeysRequest>>()

    private val _androidContacts = java.util.ArrayList<MessageRecipient>()
    private val _protonMailContacts = mutableSetOf<MessageRecipient>()
    private var _protonMailGroups: List<MessageRecipient> = java.util.ArrayList()
    private var _androidContactsLoaded: Boolean = false
    private var _protonMailContactsLoaded: Boolean = false

    private lateinit var _messageDataResult: MessageBuilderData

    private lateinit var _composerGroupCountOf: String

    val messageDataResult: MessageBuilderData
        get() = _messageDataResult

    // endregion
    // region data
    private var _actionType = UserAction.NONE
    var _actionId = Constants.MessageActionType.NONE
    private var _parentId: String? = null
    private val _draftId = AtomicReference<String>()
    private lateinit var _data: List<ContactLabelUiModel>
    private lateinit var _senderAddresses: List<String>
    private val _groupsRecipientsMap = HashMap<ContactLabelUiModel, List<MessageRecipient>>()
    private var _oldSenderAddressId: String = ""
    private lateinit var htmlProcessor: HtmlProcessor
    private var _dbId: Long? = null

    private var sendingInProcess = false
    private var signatureContainsHtml = false

    var compositeDisposable = CompositeDisposable()

    // endregion
    // region events observables
    val mergedContactsLiveData: MediatorLiveData<List<MessageRecipient>>
        get() = _mergedContactsLiveData
    val contactGroupsResult: LiveData<List<MessageRecipient>>
        get() = _contactGroupsResult
    val pmMessageRecipientsResult: LiveData<List<MessageRecipient>>
        get() = _pmMessageRecipientsResult
    val androidMessageRecipientsResult: LiveData<List<MessageRecipient>>
        get() = _androidMessageRecipientsResult
    val setupComplete: LiveData<Event<Boolean>>
        get() = _setupComplete
    val closeComposer: LiveData<Event<Boolean>>
        get() = _closeComposer
    val setupCompleteValue: Boolean
        get() = _setupCompleteValue
    val savingDraftComplete: LiveData<Message>
        get() = _savingDraftComplete
    val savingDraftError: LiveData<String>
        get() = _savingDraftError
    val senderAddresses: List<String>
        get() = _senderAddresses
    val deleteResult: LiveData<Event<PostResult>>
        get() = _deleteResult
    val loadingDraftResult: LiveData<Message>
        get() = _loadingDraftResult
    val openAttachmentsScreenResult: LiveData<List<LocalAttachment>>
        get() = _openAttachmentsScreenResult
    val buildingMessageCompleted: LiveData<Event<Message>>
        get() = _buildingMessageCompleted
    val dbIdWatcher: LiveData<Long>
        get() = _dbIdWatcher
    var androidContactsLoaded: Boolean
        get() = _androidContactsLoaded
        set(value) {
            _androidContactsLoaded = value
        }
    val fetchKeyDetailsResult: LiveData<List<FetchPublicKeysResult>>
        get() = fetchKeyDetailsTrigger.switchMap { request ->
            liveData {
                emit(fetchPublicKeys(request))
            }
        }

    private val _events = MutableSharedFlow<ComposeMessageEventUiModel>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: Flow<ComposeMessageEventUiModel> =
        _events.asSharedFlow()

    // endregion
    // region getters
    var draftId: String
        get() = _draftId.get() ?: ""
        set(value) { // this is temporary until all setters are moved from the activity into this class
            _draftId.set(value)
        }
    var oldSenderAddressId: String
        get() = _oldSenderAddressId
        set(value) {
            _oldSenderAddressId = value
        }

    var actionType: UserAction
        get() = _actionType
        set(value) {
            _actionType = value
        }
    val parentId: String?
        get() = _parentId

    internal var autoSaveJob: Job? = null
    // endregion

    private val user by lazy { userManager.requireCurrentLegacyUser() }

    private val userId by lazy { userManager.requireCurrentUserId() }

    private val loggedInUserIds = if (user.combinedContacts) {
        accountManager.allLoggedInBlocking()
    } else {
        listOf(userManager.currentUserId)
    }

    fun init(processor: HtmlProcessor) {
        htmlProcessor = processor
        composeMessageRepository.lazyManager.reset()
        composeMessageRepository.reloadDependenciesForUser(userId)
        getSenderEmailAddresses()
        // if the user is free user, then we do not fetch contact groups and announce the setup is complete
        if (!user.isPaidUser) {
            _setupCompleteValue = true
            sendingInProcess = false
            _setupComplete.postValue(Event(true))
            if (!_protonMailContactsLoaded) {
                loadPMContacts()
            }
        } else {
            for (userId in loggedInUserIds) {
                userId?.let {
                    fetchContactGroups(it)
                }
            }
        }
    }

    override fun onCleared() {
        compositeDisposable.clear()
        super.onCleared()
    }

    fun setupEditDraftMessage(draftId: String, composerGroupCountOf: String) {
        _draftId.set(draftId)
        _composerGroupCountOf = composerGroupCountOf
        watchForMessageSent()
    }

    fun setupComposingNewMessage(
        actionId: Constants.MessageActionType,
        parentId: String?,
        composerGroupCountOf: String
    ) {
        _actionId = actionId
        _parentId = parentId
        _composerGroupCountOf = composerGroupCountOf
    }

    fun prepareMessageData(
        isPGPMime: Boolean,
        addressId: String,
        addressEmailAlias: String? = null
    ) {
        _messageDataResult =
            composeMessageRepository.prepareMessageData(isPGPMime, addressId, addressEmailAlias)
        getSenderEmailAddresses(addressEmailAlias)
    }

    fun prepareMessageData(messageTitle: String, attachments: ArrayList<LocalAttachment>) {
        _messageDataResult = composeMessageRepository.prepareMessageData(
            _messageDataResult,
            messageTitle,
            attachments
        )
    }

    @SuppressLint("CheckResult")
    fun fetchContactGroups(userId: UserId) {
        if (!isPaidUser()) {
            return
        }
        if (::_data.isInitialized) {
            handleContactGroupsResult()
            return
        }
        composeMessageRepository.getContactGroupsFromDB(userId, user.combinedContacts)
            .onEach { models ->
                for (group in models) {
                    val emails = composeMessageRepository.getContactGroupEmailsSync(group.id.id)
                    val recipients = ArrayList<MessageRecipient>()
                    for (email in emails) {
                        val recipient = MessageRecipient(email.name, email.email)
                        recipient.group = group.name
                        recipient.groupIcon = R.string.contact_group_groups_icon
                        recipient.groupColor =
                            Color.parseColor(UiUtil.normalizeColor(group.color))
                        recipients.add(recipient)
                    }
                    _groupsRecipientsMap[group] = recipients
                }

                _data = models
                handleContactGroupsResult()
                _setupCompleteValue = true
                sendingInProcess = false
                _setupComplete.postValue(Event(true))
                if (!_protonMailContactsLoaded) {
                    loadPMContacts()
                }
            }
            .catch {
                _data = ArrayList()
                _setupCompleteValue = false
                sendingInProcess = false
                _setupComplete.postValue(Event(false))
                if (!_protonMailContactsLoaded) {
                    loadPMContacts()
                }
            }
            .launchIn(viewModelScope)
    }

    fun getContactGroupRecipients(group: ContactLabelUiModel): List<MessageRecipient> =
        _groupsRecipientsMap[group] ?: ArrayList()

    fun getContactGroupByName(groupName: String): ContactLabelUiModel? {
        return _data.find {
            it.name == groupName
        }
    }

    private fun filterUploadedAttachments(
        localAttachments: List<Attachment>,
        uploadAttachments: Boolean
    ): List<String> {
        val result = ArrayList<String>()
        for (i in localAttachments.indices) {
            val attachment = localAttachments[i]
            if (attachment.isUploaded || attachment.isUploading || !attachment.isNew) {
                continue
            }
            if (uploadAttachments) {
                attachment.isUploading = true
            }
            val attachmentId: String? = attachment.attachmentId
            attachmentId?.let {
                result.add(attachmentId)
            }
        }
        return result
    }

    @Subscribe
    fun onFetchMessageDetailEvent(event: FetchMessageDetailEvent) {
        if (event.success) {
            viewModelScope.launch {
                val message = event.message
                message!!.decrypt(userManager, userManager.requireCurrentUserId())
                val decryptedMessage = message.decryptedHTML // todo check if any var should be set
                val messageId = event.messageId
                composeMessageRepository.markMessageRead(messageId)
                MessageBuilderData.Builder()
                    .fromOld(_messageDataResult)
                    .message(message)
                    .decryptedMessage(decryptedMessage!!)
                    .build()
                _actionType = UserAction.SAVE_DRAFT
            }
        }
    }

    @SuppressLint("GlobalCoroutineUsage")
    fun saveDraft(message: Message, hasConnectivity: Boolean) {
        val uploadAttachments = _messageDataResult.uploadAttachments

        // This coroutine **needs** to be launched in `GlobalScope` to allow the process of saving a
        // draft to complete without depending on this VM's lifecycle. See MAILAND-1301 for more details
        // and notes on the plan to remove this GlobalScope usage
        GlobalScope.launch(dispatchers.Main) {
            if (_dbId == null) {
                _dbId = saveMessage(message)
                message.dbId = _dbId
            } else {
                message.dbId = _dbId
                saveMessage(message)
            }
            val saveDraftTrigger = if (uploadAttachments) {
                SaveDraft.SaveDraftTrigger.UserRequested
            } else {
                SaveDraft.SaveDraftTrigger.AutoSave
            }
            if (draftId.isNotEmpty()) {
                if (MessageUtils.isLocalMessageId(_draftId.get()) && hasConnectivity) {
                    return@launch
                }
                //region update existing draft here
                message.messageId = draftId
                val newAttachments = calculateNewAttachments(uploadAttachments)

                invokeSaveDraftUseCase(
                    message, newAttachments, parentId, _actionId, _oldSenderAddressId, saveDraftTrigger
                )

                // overwrite "old sender ID" when updating draft
                _oldSenderAddressId = message.addressID ?: _messageDataResult.addressId
                setIsDirty(false)
                //endregion
            } else {
                //region new draft here
                if (draftId.isEmpty() && message.messageId.isNullOrEmpty()) {
                    val newDraftId = UUID.randomUUID().toString()
                    _draftId.set(newDraftId)
                    message.messageId = newDraftId
                    saveMessage(message)
                    watchForMessageSent()
                }
                var newAttachmentIds: List<String> = ArrayList()
                val listOfAttachments = ArrayList(message.attachments)
                if (uploadAttachments && listOfAttachments.isNotEmpty()) {
                    message.numAttachments = listOfAttachments.size
                    saveMessage(message)
                    newAttachmentIds = filterUploadedAttachments(
                        composeMessageRepository.createAttachmentList(
                            _messageDataResult.attachmentList, dispatchers.Io
                        ),
                        uploadAttachments
                    )
                }
                invokeSaveDraftUseCase(
                    message, newAttachmentIds, parentId, _actionId, _oldSenderAddressId, saveDraftTrigger
                )

                _oldSenderAddressId = ""
                setIsDirty(false)
                //endregion
            }

            _messageDataResult = MessageBuilderData.Builder().fromOld(_messageDataResult).isDirty(false).build()
        }
    }

    private suspend fun invokeSaveDraftUseCase(
        message: Message,
        newAttachments: List<String>,
        parentId: String?,
        messageActionType: Constants.MessageActionType,
        oldSenderAddress: String,
        saveDraftTrigger: SaveDraft.SaveDraftTrigger
    ) {
        val saveDraftResult = saveDraft(
            SaveDraft.SaveDraftParameters(
                message,
                newAttachments,
                parentId,
                messageActionType,
                oldSenderAddress,
                saveDraftTrigger
            )
        )

        when (saveDraftResult) {
            is SaveDraftResult.Success -> onDraftSaved(saveDraftResult.draftId)
            SaveDraftResult.OnlineDraftCreationFailed -> {
                val errorMessage = stringResourceResolver(
                    R.string.failed_saving_draft_online
                ).format(message.subject)
                _savingDraftError.postValue(errorMessage)
            }
            SaveDraftResult.UploadDraftAttachmentsFailed -> {
                val errorMessage = stringResourceResolver(R.string.attachment_failed) + message.subject
                _savingDraftError.postValue(errorMessage)
            }
        }
    }

    private suspend fun onDraftSaved(savedDraftId: String) {
        val draft = requireNotNull(messageDetailsRepository.findMessageById(savedDraftId).first())

        viewModelScope.launch(dispatchers.Main) {
            _draftId.set(draft.messageId)
            watchForMessageSent()
        }
        _savingDraftComplete.postValue(draft)
    }

    private suspend fun calculateNewAttachments(uploadAttachments: Boolean): List<String> {
        var newAttachments: List<String> = ArrayList()
        val localAttachmentsList =
            _messageDataResult.attachmentList.filter { !it.isUploaded } // these are composer attachments

        // we need to compare them and find out which are new attachments
        if (uploadAttachments && localAttachmentsList.isNotEmpty()) {
            newAttachments = filterUploadedAttachments(
                composeMessageRepository.createAttachmentList(localAttachmentsList, dispatchers.Io), uploadAttachments
            )
        }
        val currentAttachmentsList = messageDataResult.attachmentList
        setAttachmentList(currentAttachmentsList)
        return newAttachments
    }

    private suspend fun saveMessage(message: Message): Long =
        messageDetailsRepository.saveMessage(message)

    private fun getSenderEmailAddresses(userEmailAlias: String? = null) {
        val senderAddresses = user.senderEmailAddresses
        if (senderAddresses.isEmpty()) {
            senderAddresses.add(user.defaultAddressEmail)
        }
        userEmailAlias?.let {
            if (userEmailAlias.isNotEmpty()) {
                senderAddresses.add(0, userEmailAlias)
            }
        }
        _senderAddresses = senderAddresses
    }

    fun getPositionByAddressId(): Int = user.getPositionByAddressId(_messageDataResult.addressId)

    fun isPaidUser(): Boolean = user.isPaidUser

    fun getUserAddressByIdFromOnlySendAddresses(): Int = user.addressByIdFromOnlySendAddresses

    fun setSenderAddressIdByEmail(email: String) {
        // sanitize alias address so it points to original address
        val nonAliasAddress = "${email.substringBefore("+", email.substringBefore("@"))}@${email.substringAfter("@")}"
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .addressId(user.getSenderAddressIdByEmail(nonAliasAddress))
            .build()
    }

    fun getAddressById(): Address = user.getAddressById(_messageDataResult.addressId)

    fun getNewSignature(): String {
        return if (user.isShowSignature) {
            user.getSignatureForAddress(_messageDataResult.addressId)
        } else ""
    }

    fun startFetchMessageDetailJob(draftId: String) {
        composeMessageRepository.startFetchMessageDetail(draftId)
    }

    fun startFetchPublicKeys(request: List<FetchPublicKeysRequest>) {
        Timber.v("startFetchPublicKeys $request")
        fetchKeyDetailsTrigger.value = request
    }

    fun startSendPreferenceJob(
        emailList: List<String>,
        destination: GetSendPreferenceJob.Destination
    ) {
        composeMessageRepository.getSendPreference(emailList, destination)
    }

    fun startResignContactJobJob(
        contactEmail: String,
        sendPreference: SendPreference,
        destination: GetSendPreferenceJob.Destination
    ) {
        composeMessageRepository.resignContactJob(contactEmail, sendPreference, destination)
    }

    private fun buildMessage() {
        viewModelScope.launch {
            var message: Message? = null
            if (draftId.isNotEmpty()) {
                message = composeMessageRepository.findMessage(draftId)
            }
            if (message != null) {
                _draftId.set(message.messageId)
                watchForMessageSent()
                // region here in this block we are updating local view model attachments with the latest data for the attachments filled from the API
                val savedAttachments = message.attachments // already saved attachments in DB
                val iterator = _messageDataResult.attachmentList.iterator() // current attachments in view model
                val listLocalAttachmentsAlreadySavedInDb = ArrayList<LocalAttachment>()
                while (iterator.hasNext()) {
                    val localAtt = iterator.next()
                    var found = false
                    var att: Attachment? = null
                    for (savedAtt in savedAttachments) {
                        if (savedAtt.fileName == localAtt.displayName) {
                            att = savedAtt
                            found = true
                            break
                        }
                    }
                    if (found) {
                        iterator.remove()
                        val localAttach = LocalAttachment.fromAttachment(att!!)
                        localAttach.uri = localAtt.uri
                        listLocalAttachmentsAlreadySavedInDb.add(localAttach)
                    }
                }
                _messageDataResult.attachmentList.addAll(listLocalAttachmentsAlreadySavedInDb)
                val newAttachments = composeMessageRepository.createAttachmentList(
                    _messageDataResult.attachmentList,
                    dispatchers.Io
                )

                message.setAttachmentList(newAttachments)
                // endregion
                _buildingMessageCompleted.postValue(Event(message))
            } else {
                _buildingMessageCompleted.postValue(Event(Message()))
            }
        }
    }

    @SuppressLint("CheckResult")
    fun findDraftMessageById() {
        val disposable = composeMessageRepository.findMessageByIdSingle(_draftId.get())
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.io())
            .flatMap {
                if (it.messageBody.isNullOrEmpty()) {
                    composeMessageRepository.startFetchDraftDetail(_draftId.get())
                } else {
                    it.isDownloaded = true
                    val attachments = composeMessageRepository.getAttachmentsBlocking(it)
                    it.setAttachmentList(attachments)
                    _messageDataResult = MessageBuilderData.Builder()
                        .fromOld(_messageDataResult)
                        .attachmentList(ArrayList(LocalAttachment.createLocalAttachmentList(attachments)))
                        .message(it)
                        .messageId()
                        .build()
                    _dbId = it.dbId
                }
                Single.just(it)
            }
            .subscribe(
                {
                    _loadingDraftResult.postValue(_messageDataResult.message)
                },
                {
                    composeMessageRepository.startFetchDraftDetail(_draftId.get())
                    _messageResultError.postValue(
                        Event(PostResult(it.message ?: "", Status.FAILED))
                    )
                }
            )

        compositeDisposable.add(disposable)
    }

    fun openAttachmentsScreen() {
        val oldList = _messageDataResult.attachmentList

        viewModelScope.launch {
            if (draftId.isNotEmpty()) {
                val message = composeMessageRepository.findMessage(draftId)

                if (message != null) {
                    val messageAttachments =
                        composeMessageRepository.getAttachments(message, dispatchers.Io)
                    if (oldList.size <= messageAttachments.size) {
                        val attachments = LocalAttachment.createLocalAttachmentList(messageAttachments)
                        _messageDataResult = MessageBuilderData.Builder()
                            .fromOld(_messageDataResult)
                            .attachmentList(ArrayList(attachments))
                            .build()
                        _openAttachmentsScreenResult.postValue(attachments)
                        return@launch
                    }
                }
            }
            _messageDataResult = MessageBuilderData.Builder()
                .fromOld(_messageDataResult)
                .attachmentList(ArrayList(oldList))
                .build()
            _openAttachmentsScreenResult.postValue(oldList)
        }
    }

    fun deleteDraft() {
        viewModelScope.launch {
            if (_draftId.get().isNotEmpty()) {
                deleteMessage(
                    listOf(_draftId.get()),
                    Constants.MessageLocationType.DRAFT.messageLocationTypeValue.toString(),
                    userId
                )
            }
        }
    }

    @Synchronized
    fun sendMessage(message: Message) {
        setIsDirty(false)
        val messageWithExpirationTime = addExpirationTimeToMessage(message, _messageDataResult.expiresAfterInSeconds)

        if (sendingInProcess) {
            return
        }
        sendingInProcess = true
        GlobalScope.launch {
            _messageDataResult = MessageBuilderData.Builder()
                .fromOld(_messageDataResult)
                .message(messageWithExpirationTime)
                .build()
            if (_dbId == null) {
                // if db ID is null this means we do not have local DB row of the message we are about to send
                // and we are saving it. also draftId should be null
                messageWithExpirationTime.messageId = UUID.randomUUID().toString()
                _dbId = saveMessage(messageWithExpirationTime)
            } else {
                // this will ensure the message get latest message id if it was already saved in a create/update draft job
                // and also that the message has all the latest edits in between draft saving (creation) and sending the message
                val savedMessage = messageDetailsRepository.findMessageByDatabaseId(_dbId!!).first()
                messageWithExpirationTime.dbId = _dbId
                savedMessage?.let {
                    if (!TextUtils.isEmpty(it.localId)) {
                        messageWithExpirationTime.messageId = it.messageId
                    } else {
                        messageWithExpirationTime.messageId = _draftId.get()
                    }
                    saveMessage(messageWithExpirationTime)
                }
            }

            if (_dbId != null) {
                val newAttachments = calculateNewAttachments(true)

                sendMessage(
                    SendMessage.SendMessageParameters(
                        messageWithExpirationTime,
                        newAttachments,
                        parentId,
                        _actionId,
                        _oldSenderAddressId,
                        MessageSecurityOptions(
                            messageDataResult.messagePassword,
                            messageDataResult.passwordHint,
                            messageDataResult.expiresAfterInSeconds
                        )
                    )
                )
            } else {
                sendingInProcess = false
            }

            _dbIdWatcher.postValue(_dbId)
        }
    }

    fun createLocalAttachments(loadedMessage: Message) {
        viewModelScope.launch {
            val messageAttachments =
                composeMessageRepository.getAttachments(loadedMessage, dispatchers.Io)
            val localAttachments = LocalAttachment.createLocalAttachmentList(messageAttachments).toMutableList()
            _messageDataResult = MessageBuilderData.Builder()
                .fromOld(_messageDataResult)
                .attachmentList(ArrayList(localAttachments))
                .build()
        }
    }

    fun initSignatures(): StringBuilder {
        var signature = ""
        var mobileFooter = ""
        val signatureBuilder = StringBuilder()
        signatureBuilder.append(NEW_LINE)
        signatureBuilder.append(NEW_LINE)
        signatureBuilder.append(NEW_LINE)
        signature = if (_messageDataResult.addressId.isNotEmpty()) {
            user.getSignatureForAddress(_messageDataResult.addressId)
        } else {
            val senderAddresses = user.senderEmailAddresses
            if (senderAddresses.isNotEmpty()) {
                val selectedEmail = senderAddresses[0]
                user.getSignatureForAddress(user.getSenderAddressIdByEmail(selectedEmail))
            } else {
                val selectedEmail = user.defaultAddressEmail
                user.getSignatureForAddress(user.getSenderAddressIdByEmail(selectedEmail))
            }
        }
        mobileFooter = user.mobileFooter

        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .signature(signature)
            .mobileFooter(mobileFooter)
            .build()

        return signatureBuilder
    }

    fun calculateAttachmentFileSize(): Long {
        var totalFileSize: Long = 0
        for (attachment in _messageDataResult.attachmentList) {
            totalFileSize += attachment.size
        }
        return totalFileSize
    }

    fun setSignature(signature: String) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .signature(signature)
            .build()

        signatureContainsHtml = with(_messageDataResult.signature) {
            val afterTagOpen = substringAfter("<", "")
            var openingTag = afterTagOpen.substringBefore(" ", "")
            if (openingTag.contains(">")) {
                openingTag = afterTagOpen.substringBefore(">", "")
            }
            val afterTagClose = afterTagOpen.substringAfter(">", "")
            val betweenTag = afterTagClose.substringBeforeLast("</$openingTag>", "")
            betweenTag.isNotEmpty()
        }
    }

    fun processSignature(): String {
        return processSignature(_messageDataResult.signature)
    }

    fun calculateSignature(signature: String): String = htmlProcessor.digestMessage(signature)

    fun processSignature(signature: String): String {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .signature(signature)
            .build()
        return signature
    }

    fun setContent(content: String) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .content(content)
            .build()
    }

    fun setIsDirty(isDirty: Boolean) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .isDirty(isDirty)
            .build()
    }

    fun setSender(senderName: String, senderAddress: String) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .messageSenderName(senderName)
            .senderEmailAddress(senderAddress)
            .build()
    }

    fun setRespondInline(respondInline: Boolean) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .isRespondInlineChecked(respondInline)
            .build()
    }

    fun setIsRespondInlineButtonVisible(isRespondInlineButtonVisible: Boolean) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .isRespondInlineButtonVisible(isRespondInlineButtonVisible)
            .build()
    }

    fun setIsMessageBodyVisible(isMessageBodyVisible: Boolean) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .isMessageBodyVisible(isMessageBodyVisible)
            .build()
    }

    fun setAttachmentList(attachments: ArrayList<LocalAttachment>) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .attachmentList(attachments)
            .build()
    }

    fun setEmbeddedAttachmentList(embeddedAttachments: ArrayList<LocalAttachment>?) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .embeddedAttachmentsList(embeddedAttachments ?: ArrayList())
            .build()
    }

    private fun existsAsPMContact(email: String): Boolean {
        for (messageRecipient in _protonMailContacts) {
            if (messageRecipient.emailAddress.equals(email, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun createMessageRecipient(name: String, email: String): MessageRecipient? {
        val item = MessageRecipient(name, email)
        if (existsAsPMContact(item.emailAddress)) {
            return null
        }
        if (item.name != null) {
            _androidContacts.add(item)
        }
        return item
    }

    @SuppressLint("CheckResult")
    fun loadPMContacts() {
        if (_protonMailContactsLoaded) {
            return
        }
        if (!setupCompleteValue) {
            return
        }
        _protonMailContactsLoaded = true
        for (userId in loggedInUserIds) {
            fetchContactGroups(userId!!)
            val disposable = composeMessageRepository.findAllMessageRecipients(userId)
                .subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.main())
                .subscribe {
                    if (it != null) {
                        _protonMailContacts.addAll(it) // no groups
                        val groupedContactsAndGroups = ArrayList<MessageRecipient>(_protonMailContacts)
                        groupedContactsAndGroups.addAll(0, _protonMailGroups)
                        _pmMessageRecipientsResult.postValue(groupedContactsAndGroups)
                        _mergedContactsLiveData.removeSource(pmMessageRecipientsResult)
                        _mergedContactsLiveData.addSource(pmMessageRecipientsResult) { value ->
                            _mergedContactsLiveData.postValue(value)
                        }
                    }
                }
            compositeDisposable.add(disposable)
        }
    }

    private fun handleContactGroupsResult() {
        val messageRecipientList = java.util.ArrayList<MessageRecipient>()
        for (contactLabel in _data) {
            val recipient = MessageRecipient(
                String.format(
                    _composerGroupCountOf, contactLabel.name, contactLabel.contactEmailsCount,
                    contactLabel.contactEmailsCount
                ),
                ""
            )
            recipient.group = contactLabel.name
            recipient.groupRecipients = getContactGroupRecipients(contactLabel)
            recipient.groupIcon = R.string.contact_group_groups_icon
            recipient.groupColor = Color.parseColor(UiUtil.normalizeColor(contactLabel.color))
            messageRecipientList.add(recipient)
        }
        _protonMailGroups = messageRecipientList
        val groupedContactsAndGroups = ArrayList<MessageRecipient>(_protonMailContacts)
        groupedContactsAndGroups.addAll(0, _protonMailGroups)
        _contactGroupsResult.postValue(groupedContactsAndGroups)
        _mergedContactsLiveData.removeSource(contactGroupsResult)
        _mergedContactsLiveData.addSource(contactGroupsResult) { value -> _mergedContactsLiveData.postValue(value) }
    }

    fun getContent(content: String): String {
        return content.replace("<", LESS_THAN).replace(">", GREATER_THAN).replace("\n", NEW_LINE)
    }

    @JvmOverloads
    fun setMessageBody(
        composerBody: String? = null,
        messageBody: String,
        setComposerContent: Boolean,
        isPlainText: Boolean,
        senderNameAddressFormat: String,
        originalMessageDividerString: String,
        replyPrefixOnString: String,
        formattedDateTimeString: String
    ): MessageBodySetup {
        val messageBodySetup = MessageBodySetup()
        val builder = StringBuilder()
        if (setComposerContent) {
            var signatureBuilder = StringBuilder()
            if (composerBody == null) {
                signatureBuilder = initSignatures()
                if (!TextUtils.isEmpty(messageDataResult.signature) && MessageUtils.containsRealContent(
                        messageDataResult.signature
                    ) && user.isShowSignature
                ) {
                    signatureBuilder.append(messageDataResult.signature)
                    signatureBuilder.append(NEW_LINE)
                    signatureBuilder.append(NEW_LINE)
                }
                if (user.isShowMobileFooter) {
                    signatureBuilder.append(messageDataResult.mobileFooter.replace("\n", NEW_LINE))
                }
                signatureBuilder.append(NEW_LINE)
                signatureBuilder.append(NEW_LINE)
                signatureBuilder.append(NEW_LINE)
            }
            composerBody?.let {
                signatureBuilder.insert(0, composerBody)
            }
            messageBodySetup.composeBody = htmlToSpanned(signatureBuilder.toString())
        }
        if (!TextUtils.isEmpty(messageBody)) {
            messageBodySetup.webViewVisibility = true
            val sender =
                String.format(
                    senderNameAddressFormat, _messageDataResult.senderName, _messageDataResult.senderEmailAddress
                )
            setQuotationHeader(sender, originalMessageDividerString, replyPrefixOnString, formattedDateTimeString)
            builder.append("<blockquote class=\"protonmail_quote\">")
            builder.append(NEW_LINE)
            builder.append(messageBody)
            builder.append("</div>")
            messageBodySetup.respondInlineVisibility = true
        } else {
            messageBodySetup.webViewVisibility = false
            messageBodySetup.respondInlineVisibility = false
        }
        setInitialMessageContent(builder.toString())
        setContent(builder.toString())
        messageBodySetup.respondInline = false
        messageBodySetup.isPlainText = isPlainText

        return messageBodySetup
    }

    private fun setQuotationHeader(
        sender: String,
        originalMessageDividerString: String,
        replyPrefixOnString: String,
        formattedDateTimeString: String
    ) {
        val originalMessageBuilder = StringBuilder()
        originalMessageBuilder.append(NEW_LINE)
        originalMessageBuilder.append(originalMessageDividerString)
        if (0L != _messageDataResult.messageTimestamp) {
            originalMessageBuilder.append(NEW_LINE)
            originalMessageBuilder.append(replyPrefixOnString)
            originalMessageBuilder.append(" ")
            originalMessageBuilder.append(formattedDateTimeString)
            originalMessageBuilder.append(", ")
            originalMessageBuilder.append(sender)
        }
        setQuotedHeader(htmlToSpanned(originalMessageBuilder.toString()))
    }

    fun onAndroidContactsLoaded() {
        if (_androidContactsLoaded) {
            return
        }
        _androidContactsLoaded = true
        if (_androidContacts.size > 0) {
            _protonMailContacts.addAll(_androidContacts)
            _androidMessageRecipientsResult.postValue(_protonMailContacts.toList())
            _mergedContactsLiveData.removeSource(contactGroupsResult)
            _mergedContactsLiveData.addSource(androidMessageRecipientsResult) { value ->
                _mergedContactsLiveData.postValue(value)
            }
        }
    }

    fun setShowImages(showImages: Boolean) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .showImages(showImages)
            .build()
    }

    fun setShowRemoteContent(showRemoteContent: Boolean) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .showRemoteContent(showRemoteContent)
            .build()
    }

    fun setQuotedHeader(quotedHeaderString: Spanned) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .quotedHeader(quotedHeaderString)
            .build()
    }

    fun setMessageTimestamp(messageTimestamp: Long) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .messageTimestamp(messageTimestamp)
            .build()
    }

    fun setInitialMessageContent(initialMessageContent: String) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .initialMessageContent(initialMessageContent)
            .build()
    }

    fun addSendPreferences(sendPreference: SendPreference) {
        var sendPreferencesTemp: Map<String, SendPreference> = _messageDataResult.sendPreferences
        sendPreferencesTemp += sendPreference.emailAddress to sendPreference

        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .sendPreferences(sendPreferencesTemp)
            .build()
    }

    class MessageBodySetup {

        var composeBody: Spanned? = null
        var webViewVisibility: Boolean = false
        var respondInlineVisibility: Boolean = false
        var respondInline: Boolean = false
        var isPlainText: Boolean = false
    }

    @JvmOverloads
    fun setBeforeSaveDraft(
        uploadAttachments: Boolean,
        contentFromComposeBodyEditText: String,
        userAction: UserAction = UserAction.SAVE_DRAFT
    ) {
        setUploadAttachments(uploadAttachments)

        _actionType = userAction
        var content = contentFromComposeBodyEditText
        content = UiUtil.toHtml(content)
        content.replace("   ", "&nbsp;&nbsp;&nbsp;").replace("  ", "&nbsp;&nbsp;")
        content = content.replace("<", LESS_THAN).replace(">", GREATER_THAN).replace("\n", NEW_LINE)

        content = UiUtil.createLinksSending(content)

        if (signatureContainsHtml) {
            val fromHtmlSignature = UiUtil.createLinksSending(
                htmlToSpanned(_messageDataResult.signature).toString().replace("\n", NEW_LINE)
            )
            if (!TextUtils.isEmpty(fromHtmlSignature)) {
                content = content.replace(fromHtmlSignature, _messageDataResult.signature)
            }
        }

        val fromHtmlMobileFooter = htmlToSpanned(_messageDataResult.mobileFooter)
        if (fromHtmlMobileFooter.isNotEmpty()) {
            content = content.replace(fromHtmlMobileFooter.toString(), _messageDataResult.mobileFooter)
        }

        if ((_messageDataResult.isRespondInlineChecked || _messageDataResult.isRespondInlineButtonVisible.not()) && _messageDataResult.isMessageBodyVisible.not()) {
            setContent(content)
            setRespondInline(true)
        } else {
            val quotedHeader = messageDataResult.quotedHeader.toString().replace("\n", NEW_LINE)
            setContent(content + quotedHeader + _messageDataResult.initialMessageContent)
            setRespondInline(false)
        }
        buildMessage()
    }

    fun finishBuildingMessage(contentFromComposeBodyEditText: String) {
        setUploadAttachments(true)
        setIsDirty(false)

        _actionType = UserAction.FINISH_EDIT
        var content = contentFromComposeBodyEditText
        content.replace("   ", "&nbsp;&nbsp;&nbsp;").replace("  ", "&nbsp;&nbsp;")
        content = content.replace("<", LESS_THAN).replace(">", GREATER_THAN).replace("\n", NEW_LINE)

        content = UiUtil.createLinksSending(content)

        if (signatureContainsHtml) {
            val fromHtmlSignature = UiUtil.createLinksSending(
                (htmlToSpanned(_messageDataResult.signature).toString()).replace("\n", NEW_LINE)
            )
            if (!TextUtils.isEmpty(fromHtmlSignature)) {
                content = content.replace(fromHtmlSignature, _messageDataResult.signature)
            }
        }

        val fromHtmlMobileFooter = htmlToSpanned(_messageDataResult.mobileFooter)
        if (!TextUtils.isEmpty(fromHtmlMobileFooter)) {
            content = content.replace(fromHtmlMobileFooter.toString(), _messageDataResult.mobileFooter)
        }

        if (_messageDataResult.isRespondInlineChecked || _messageDataResult.isRespondInlineButtonVisible.not()) {
            setContent(content)
            setRespondInline(true)
        } else {
            val quotedHeader = messageDataResult.quotedHeader.toString().replace("\n", NEW_LINE)
            setContent(content + quotedHeader + _messageDataResult.initialMessageContent)
            setRespondInline(false)
        }
        buildMessage()
    }

    fun setUploadAttachments(uploadAttachments: Boolean) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .uploadAttachments(uploadAttachments)
            .build()
    }

    fun getSignatureByEmailAddress(email: String): String {
        val nonAliasAddress = "${email.substringBefore("+", email.substringBefore("@"))}@${email.substringAfter("@")}"
        return user.addresses.find { it.email == nonAliasAddress }?.signature ?: ""
    }

    @SuppressLint("CheckResult")
    fun watchForMessageSent() {
        if (_draftId.get().isNotEmpty()) {
            val disposable = composeMessageRepository.findMessageByIdObservable(_draftId.get()).toObservable()
                .subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.main())
                .subscribe(
                    {
                        if (Constants.MessageLocationType.fromInt(
                                it.location
                            ) == Constants.MessageLocationType.SENT || Constants.MessageLocationType.fromInt(
                                    it.location
                                ) == Constants.MessageLocationType.ALL_SENT
                        ) {
                            _closeComposer.postValue(Event(true))
                        }
                    },
                    { }
                )

            compositeDisposable.add(disposable)
        }
    }

    fun onMessageLoaded(message: Message) {
        val messageId = message.messageId
        val isLocalMessageId = MessageUtils.isLocalMessageId(messageId)
        if (!isLocalMessageId) {
            viewModelScope.launch {
                draftId = messageId!!
                message.isDownloaded = true
                val attachments = message.attachments
                message.setAttachmentList(attachments)
                setAttachmentList(ArrayList(LocalAttachment.createLocalAttachmentList(attachments)))
                _dbId = message.dbId
            }
        } else {
            setBeforeSaveDraft(false, messageDataResult.content, UserAction.SAVE_DRAFT)
        }
    }

    // region password
    fun requestCurrentPasswordForUpdate() {
        viewModelScope.launch {
            val currentPassword = messageDataResult.messagePassword
            val currentHint = messageDataResult.passwordHint
            val uiModel = MessagePasswordUiModel.from(currentPassword, currentHint)
            _events.emit(ComposeMessageEventUiModel.OnPasswordChangeRequest(uiModel))
        }
    }

    fun setPassword(password: MessagePasswordUiModel) {
        viewModelScope.launch {
            val (newPassword, newHint) = when (password) {
                is MessagePasswordUiModel.Set -> password.password to password.hint
                MessagePasswordUiModel.Unset -> null to null
            }
            _messageDataResult = MessageBuilderData.Builder()
                .fromOld(_messageDataResult)
                .messagePassword(newPassword)
                .passwordHint(newHint)
                .build()
            _events.emit(ComposeMessageEventUiModel.OnPasswordChange(newPassword != null))
        }
    }
    // endregion

    // region expiration
    fun requestCurrentExpirationForUpdate() {
        viewModelScope.launch {
            val currentExpiration = (messageDataResult.expiresAfterInSeconds).seconds
            val days = currentExpiration.inDays
            val hours = (currentExpiration - days.days).inHours
            val uiModel = DaysHoursPair(
                days = days.roundToInt(),
                hours = hours.roundToInt()
            )
            _events.emit(ComposeMessageEventUiModel.OnExpirationChangeRequest(uiModel))
        }
    }

    fun setExpiresAfterInSeconds(expiration: DaysHoursPair) {
        viewModelScope.launch {
            val newExpiresAfter = expiration.days.days + expiration.hours.hours
            val newExpiresAfterInSeconds = newExpiresAfter.inSeconds.toLong()
            _messageDataResult = MessageBuilderData.Builder()
                .fromOld(_messageDataResult)
                .expiresAfterInSeconds(newExpiresAfterInSeconds)
                .build()
            _events.emit(ComposeMessageEventUiModel.OnExpirationChange(hasExpiration = newExpiresAfterInSeconds > 0))
        }
    }
    // endregion

    fun autoSaveDraft(messageBody: String) {
        Timber.v("Draft auto save scheduled!")

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(dispatchers.Io) {
            delay(1000)
            Timber.d("Draft auto save triggered")
            setBeforeSaveDraft(false, messageBody)
        }
    }

    fun parseMailTo(dataString: String?): MailToData {
        requireNotNull(dataString)
        val mailTo = MailTo.parse(dataString)

        // Addresses
        val addresses = mailTo.to
            ?.split(",")
            ?.map { it.trim() } ?: emptyList()

        val cc: List<String> = mailTo.cc
            ?.split(",") ?: emptyList()

        val bcc: List<String> = mailTo.bcc
            ?.split(",") ?: emptyList()

        val subject = mailTo.subject ?: EMPTY_STRING

        val body = mailTo.body ?: EMPTY_STRING

        val mailToData = MailToData(addresses, cc, subject, body, bcc)
        Timber.v("Parsed mailto: $dataString to $mailToData")
        return mailToData
    }
}
