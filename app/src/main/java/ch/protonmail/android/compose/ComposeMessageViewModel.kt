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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.composeMessage.MessageBuilderData
import ch.protonmail.android.activities.composeMessage.UserAction
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.LocalAttachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.bl.HtmlProcessor
import ch.protonmail.android.compose.send.SendMessage
import ch.protonmail.android.contacts.PostResult
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.FetchMessageDetailEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.compose.SaveDraftResult
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchPublicKeys
import ch.protonmail.android.usecase.model.FetchPublicKeysRequest
import ch.protonmail.android.usecase.model.FetchPublicKeysResult
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.resources.StringResourceResolver
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel
import ch.protonmail.android.worker.drafts.SAVE_DRAFT_UNIQUE_WORK_ID_PREFIX
import com.squareup.otto.Subscribe
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.collections.set
import kotlin.time.seconds

const val NEW_LINE = "<br>"
const val LESS_THAN = "&lt;"
const val GREATER_THAN = "&gt;"


class ComposeMessageViewModel @Inject constructor(
    private val composeMessageRepository: ComposeMessageRepository,
    private val userManager: UserManager,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val deleteMessage: DeleteMessage,
    private val fetchPublicKeys: FetchPublicKeys,
    private val saveDraft: SaveDraft,
    private val dispatchers: DispatcherProvider,
    private val stringResourceResolver: StringResourceResolver,
    private val workManager: WorkManager,
    private val sendMessageUseCase: SendMessage,
    verifyConnection: VerifyConnection,
    networkConfigurator: NetworkConfigurator
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
    private val _fetchMessageDetailsEvent: MutableLiveData<Event<MessageBuilderData>> = MutableLiveData()
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
    private var _verify: Boolean = false
    private val _draftId = AtomicReference<String>()
    private lateinit var _data: List<ContactLabel>
    private lateinit var _senderAddresses: List<String>
    private val _groupsRecipientsMap = HashMap<ContactLabel, List<MessageRecipient>>()
    private var _oldSenderAddressId: String = ""
    private lateinit var htmlProcessor: HtmlProcessor
    private var _dbId: Long? = null

    private var sendingInProcess = false
    private var signatureContainsHtml = false

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
    val fetchMessageDetailsEvent: LiveData<Event<MessageBuilderData>>
        get() = _fetchMessageDetailsEvent
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

    // endregion
    // region getters
    val verify: Boolean
        get() = _verify
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

    private val loggedInUsernames = if (userManager.user.combinedContacts) {
        AccountManager.getInstance(ProtonMailApplication.getApplication().applicationContext).getLoggedInUsers()
    } else {
        listOf(userManager.username)
    }


    fun init(processor: HtmlProcessor) {
        htmlProcessor = processor
        composeMessageRepository.lazyManager.reset()
        composeMessageRepository.reloadDependenciesForUser(userManager.username)
        getSenderEmailAddresses()
        // if the user is free user, then we do not fetch contact groups and announce the setup is complete
        if (!userManager.user.isPaidUser) {
            _setupCompleteValue = true
            sendingInProcess = false
            _setupComplete.postValue(Event(true))
            if (!_protonMailContactsLoaded) {
                loadPMContacts()
            }
        } else {
            for (username in loggedInUsernames) {
                fetchContactGroups(username)
            }
        }
    }

    fun setupEditDraftMessage(verify: Boolean, draftId: String, composerGroupCountOf: String) {
        _verify = verify
        _draftId.set(draftId)
        _composerGroupCountOf = composerGroupCountOf
        watchForMessageSent()
    }

    fun setupComposingNewMessage(
        verify: Boolean,
        actionId: Constants.MessageActionType,
        parentId: String?,
        composerGroupCountOf: String
    ) {
        _verify = verify
        _actionId = actionId
        _parentId = parentId
        _composerGroupCountOf = composerGroupCountOf
    }

    fun prepareMessageData(
        isPGPMime: Boolean,
        addressId: String,
        addressEmailAlias: String? = null,
        isTransient: Boolean
    ) {
        _messageDataResult =
            composeMessageRepository.prepareMessageData(isPGPMime, addressId, addressEmailAlias, isTransient)
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
    fun fetchContactGroups(username: String) {
        if (!isPaidUser()) {
            return
        }
        if (::_data.isInitialized) {
            handleContactGroupsResult()
            return
        }
        composeMessageRepository.getContactGroupsFromDB(username, userManager.user.combinedContacts)
            .flatMap {
                for (group in it) {
                    val emails = composeMessageRepository.getContactGroupEmailsSync(group.ID)
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
                Observable.just(it)
            }
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.main())
            .subscribe(
                {
                    _data = it
                    handleContactGroupsResult()
                    _setupCompleteValue = true
                    sendingInProcess = false
                    _setupComplete.postValue(Event(true))
                    if (!_protonMailContactsLoaded) {
                        loadPMContacts()
                    }
                },
                {
                    _data = ArrayList()
                    _setupCompleteValue = false
                    sendingInProcess = false
                    _setupComplete.postValue(Event(false))
                    if (!_protonMailContactsLoaded) {
                        loadPMContacts()
                    }
                }
            )
    }

    fun getContactGroupRecipients(group: ContactLabel): List<MessageRecipient> =
        _groupsRecipientsMap[group] ?: ArrayList()

    fun getContactGroupByName(groupName: String): ContactLabel? {
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
            val message = event.message
            message!!.decrypt(userManager, userManager.username)
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
            if (draftId.isNotEmpty()) {
                if (MessageUtils.isLocalMessageId(_draftId.get()) && hasConnectivity) {
                    return@launch
                }
                //region update existing draft here
                message.messageId = draftId
                val newAttachments = calculateNewAttachments(uploadAttachments)

                invokeSaveDraftUseCase(message, newAttachments, parentId, _actionId, _oldSenderAddressId)

                if (newAttachments.isNotEmpty() && uploadAttachments) {
                    _oldSenderAddressId = message.addressID
                        ?: _messageDataResult.addressId // overwrite "old sender ID" when updating draft
                }
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
                val listOfAttachments = ArrayList(message.Attachments)
                if (uploadAttachments && listOfAttachments.isNotEmpty()) {
                    message.numAttachments = listOfAttachments.size
                    saveMessage(message)
                    newAttachmentIds = filterUploadedAttachments(
                        composeMessageRepository.createAttachmentList(_messageDataResult.attachmentList, dispatchers.Io),
                        uploadAttachments
                    )
                }
                invokeSaveDraftUseCase(message, newAttachmentIds, parentId, _actionId, _oldSenderAddressId)

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
        oldSenderAddress: String
    ) {
        saveDraft(
            SaveDraft.SaveDraftParameters(
                message,
                newAttachments,
                parentId,
                messageActionType,
                oldSenderAddress
            )
        ).collect { saveDraftResult ->
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
                SaveDraftResult.SendingInProgressError -> {
                }
            }
        }
    }

    private suspend fun onDraftSaved(savedDraftId: String) {
        val draft = requireNotNull(messageDetailsRepository.findMessageById(savedDraftId))

        viewModelScope.launch(dispatchers.Main) {
            _draftId.set(draft.messageId)
            watchForMessageSent()
        }
        _savingDraftComplete.postValue(draft)
    }

    private suspend fun calculateNewAttachments(uploadAttachments: Boolean): List<String> {
        var newAttachments: List<String> = ArrayList()
        val localAttachmentsList = _messageDataResult.attachmentList.filter { !it.isUploaded } // these are composer attachments

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
        withContext(dispatchers.Io) {
            messageDetailsRepository.saveMessageInDB(message)
        }

    private fun getSenderEmailAddresses(userEmailAlias: String? = null) {
        val user = userManager.user
        val senderAddresses = user.senderEmailAddresses
        if (senderAddresses.isEmpty()) {
            senderAddresses.add(user.defaultEmail)
        }
        userEmailAlias?.let {
            if (userEmailAlias.isNotEmpty()) {
                senderAddresses.add(0, userEmailAlias)
            }
        }
        _senderAddresses = senderAddresses
    }

    fun getPositionByAddressId(): Int = userManager.user.getPositionByAddressId(_messageDataResult.addressId)

    fun isPaidUser(): Boolean = userManager.user.isPaidUser

    fun getUserAddressByIdFromOnlySendAddresses(): Int = userManager.user.addressByIdFromOnlySendAddresses

    fun setSenderAddressIdByEmail(email: String) {
        // sanitize alias address so it points to original address
        val nonAliasAddress = "${email.substringBefore("+", email.substringBefore("@"))}@${email.substringAfter("@")}"
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .addressId(userManager.user.getSenderAddressIdByEmail(nonAliasAddress))
            .build()
    }

    fun getAddressById(): Address = userManager.user.getAddressById(_messageDataResult.addressId)

    fun getNewSignature(): String {
        val user = userManager.user
        return if (user.isShowSignature) {
            user.getSignatureForAddress(_messageDataResult.addressId)
        } else ""
    }

    fun startGetAvailableDomainsJob() {
        composeMessageRepository.startGetAvailableDomains()
    }

    fun startFetchHumanVerificationOptionsJob() {
        composeMessageRepository.startFetchHumanVerificationOptions()
    }

    fun startFetchMessageDetailJob(draftId: String) {
        composeMessageRepository.startFetchMessageDetail(draftId)
    }

    fun startFetchPublicKeys(request: List<FetchPublicKeysRequest>) {
        Timber.v("startFetchPublicKeys $request")
        fetchKeyDetailsTrigger.value = request
    }

    fun startSendPreferenceJob(emailList: List<String>, destination: GetSendPreferenceJob.Destination) {
        composeMessageRepository.getSendPreference(emailList, destination)
    }

    fun startResignContactJobJob(contactEmail: String, sendPreference: SendPreference, destination: GetSendPreferenceJob.Destination) {
        composeMessageRepository.resignContactJob(contactEmail, sendPreference, destination)
    }

    private fun buildMessage() {
        viewModelScope.launch {
            var message: Message? = null
            if (draftId.isNotEmpty()) {
                message = composeMessageRepository.findMessage(draftId, dispatchers.Io)
            }
            if (message != null) {
                _draftId.set(message.messageId)
                watchForMessageSent()
                // region here in this block we are updating local view model attachments with the latest data for the attachments filled from the API
                val savedAttachments = message.Attachments // already saved attachments in DB
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
        composeMessageRepository.findMessageByIdSingle(_draftId.get())
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.io())
            .flatMap {
                if (it.messageBody.isNullOrEmpty()) {
                    composeMessageRepository.startFetchDraftDetail(_draftId.get())
                } else {
                    it.isDownloaded = true
                    val attachments = composeMessageRepository.getAttachments2(it, _messageDataResult.isTransient)
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
    }

    fun openAttachmentsScreen() {
        val oldList = _messageDataResult.attachmentList

        viewModelScope.launch {
            if (draftId.isNotEmpty()) {
                val message = composeMessageRepository.findMessage(draftId, dispatchers.Io)

                if (message != null) {
                    val messageAttachments = composeMessageRepository.getAttachments(message, _messageDataResult.isTransient, dispatchers.Io)
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
                deleteMessage(listOf(_draftId.get()))
            }
        }
    }

    fun startPostHumanVerification(tokenType: Constants.TokenType, token: String) {
        composeMessageRepository.startPostHumanVerification(tokenType, token)
    }

    @Synchronized
    fun sendMessage(message: Message) {
        setIsDirty(false)

        if (sendingInProcess) {
            return
        }
        sendingInProcess = true
        GlobalScope.launch {
            _messageDataResult = MessageBuilderData.Builder().fromOld(_messageDataResult).message(message).build()
            if (_dbId == null) {
                // if db ID is null this means we do not have local DB row of the message we are about to send
                // and we are saving it. also draftId should be null
                message.messageId = UUID.randomUUID().toString()
                _dbId = saveMessage(message)
            } else {
                // this will ensure the message get latest message id if it was already saved in a create/update draft job
                // and also that the message has all the latest edits in between draft saving (creation) and sending the message
                val savedMessage = messageDetailsRepository.findMessageByMessageDbId(_dbId!!, dispatchers.Io)
                message.dbId = _dbId
                savedMessage?.let {
                    if (!TextUtils.isEmpty(it.localId)) {
                        message.messageId = it.messageId
                    } else {
                        message.messageId = _draftId.get()
                    }
                    saveMessage(message)
                }
            }

            if (_dbId != null) {
                val newAttachments = calculateNewAttachments(true)

                // Cancel scheduled save draft work to allow attachments removal while offline
                // This is needed to replace the logic to block draft creation while sending, which being in
                // SaveDraft use case has no effect when CreateDraft is scheduled offline. As this logic
                // was removed in the send refactor, this solution was adopted over moving the check in CreateDraftWorker
                val saveDraftUniqueWorkId = "$SAVE_DRAFT_UNIQUE_WORK_ID_PREFIX-${message.messageId})"
                workManager.cancelUniqueWork(saveDraftUniqueWorkId)

                sendMessageUseCase(
                    SendMessage.SendMessageParameters(
                        message,
                        newAttachments,
                        parentId,
                        _actionId,
                        _oldSenderAddressId
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
            val messageAttachments = composeMessageRepository.getAttachments(loadedMessage, _messageDataResult.isTransient, dispatchers.Io)
            val localAttachments = LocalAttachment.createLocalAttachmentList(messageAttachments).toMutableList()
            _messageDataResult = MessageBuilderData.Builder()
                .fromOld(_messageDataResult)
                .attachmentList(ArrayList(localAttachments))
                .build()
        }
    }

    fun initSignatures(): StringBuilder {
        val user = userManager.user
        var signature = ""
        var mobileSignature = ""
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
                val selectedEmail = user.defaultEmail
                user.getSignatureForAddress(user.getSenderAddressIdByEmail(selectedEmail))
            }
        }
        mobileSignature = user.mobileSignature

        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .signature(signature)
            .mobileSignature(mobileSignature)
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

    fun setMessagePassword(
        messagePassword: String?,
        passwordHint: String?,
        isPasswordValid: Boolean,
        expiresIn: Long?,
        isRespondInlineButtonVisible: Boolean
    ) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .messagePassword(messagePassword)
            .passwordHint(passwordHint)
            .isPasswordValid(isPasswordValid)
            .expirationTime(expiresIn)
            .isRespondInlineButtonVisible(isRespondInlineButtonVisible)
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
        for (username in loggedInUsernames) {
            fetchContactGroups(username)
            composeMessageRepository.findAllMessageRecipients(username)
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
        }
    }

    private fun handleContactGroupsResult() {
        val messageRecipientList = java.util.ArrayList<MessageRecipient>()
        for (contactLabel in _data) {
            val recipient = MessageRecipient(String.format(_composerGroupCountOf, contactLabel.name, contactLabel.contactEmailsCount, contactLabel.contactEmailsCount), "")
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
        val user = userManager.user
        val builder = StringBuilder()
        if (setComposerContent) {
            var signatureBuilder = StringBuilder()
            if (composerBody == null) {
                signatureBuilder = initSignatures()
                if (!TextUtils.isEmpty(messageDataResult.signature) && MessageUtils.containsRealContent(messageDataResult.signature) && user.isShowSignature) {
                    signatureBuilder.append(messageDataResult.signature)
                    signatureBuilder.append(NEW_LINE)
                    signatureBuilder.append(NEW_LINE)
                }
                if (user.isShowMobileSignature) {
                    signatureBuilder.append(messageDataResult.mobileSignature.replace("\n", NEW_LINE))
                }
                signatureBuilder.append(NEW_LINE)
                signatureBuilder.append(NEW_LINE)
                signatureBuilder.append(NEW_LINE)
            }
            composerBody?.let {
                signatureBuilder.insert(0, composerBody)
            }
            messageBodySetup.composeBody = UiUtil.fromHtml(signatureBuilder.toString())
        }
        if (!TextUtils.isEmpty(messageBody)) {
            messageBodySetup.webViewVisibility = true
            val sender =
                String.format(senderNameAddressFormat, _messageDataResult.senderName, _messageDataResult.senderEmailAddress)
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
        setQuotedHeader(UiUtil.fromHtml(originalMessageBuilder.toString()))
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
            val fromHtmlSignature = UiUtil.createLinksSending(UiUtil.fromHtml(_messageDataResult.signature).toString().replace("\n", NEW_LINE))
            if (!TextUtils.isEmpty(fromHtmlSignature)) {
                content = content.replace(fromHtmlSignature, _messageDataResult.signature)
            }
        }

        val fromHtmlMobileSignature = UiUtil.fromHtml(_messageDataResult.mobileSignature)
        if (fromHtmlMobileSignature.isNotEmpty()) {
            content = content.replace(fromHtmlMobileSignature.toString(), _messageDataResult.mobileSignature)
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
            val fromHtmlSignature = UiUtil.createLinksSending((UiUtil.fromHtml(_messageDataResult.signature).toString()).replace("\n", NEW_LINE))
            if (!TextUtils.isEmpty(fromHtmlSignature)) {
                content = content.replace(fromHtmlSignature, _messageDataResult.signature)
            }
        }

        val fromHtmlMobileSignature = UiUtil.fromHtml(_messageDataResult.mobileSignature)
        if (!TextUtils.isEmpty(fromHtmlMobileSignature)) {
            content = content.replace(fromHtmlMobileSignature.toString(), _messageDataResult.mobileSignature)
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

    private fun setUploadAttachments(uploadAttachments: Boolean) {
        _messageDataResult = MessageBuilderData.Builder()
            .fromOld(_messageDataResult)
            .uploadAttachments(uploadAttachments)
            .build()
    }

    fun getSignatureByEmailAddress(email: String): String {
        val nonAliasAddress = "${email.substringBefore("+", email.substringBefore("@"))}@${email.substringAfter("@")}"
        return userManager.user.addresses.find { it.email == nonAliasAddress }?.signature ?: ""
    }

    @SuppressLint("CheckResult")
    fun watchForMessageSent() {
        if (_draftId.get().isNotEmpty()) {
            composeMessageRepository.findMessageByIdObservable(_draftId.get()).toObservable()
                .subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.main())
                .subscribe(
                    {
                        if (Constants.MessageLocationType.fromInt(it.location) == Constants.MessageLocationType.SENT || Constants.MessageLocationType.fromInt(it.location) == Constants.MessageLocationType.ALL_SENT) {
                            _closeComposer.postValue(Event(true))
                        }
                    },
                    { }
                )
        }
    }

    fun onMessageLoaded(message: Message) {
        val messageId = message.messageId
        val isLocalMessageId = MessageUtils.isLocalMessageId(messageId)
        if (!isLocalMessageId) {
            viewModelScope.launch {
                draftId = messageId!!
                message.isDownloaded = true
                val attachments = message.Attachments
                message.setAttachmentList(attachments)
                setAttachmentList(ArrayList(LocalAttachment.createLocalAttachmentList(attachments)))
                _dbId = message.dbId
            }
        } else {
            setBeforeSaveDraft(false, messageDataResult.content, UserAction.SAVE_DRAFT)
        }
    }

    fun autoSaveDraft(messageBody: String) {
        Timber.v("Draft auto save scheduled!")

        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch(dispatchers.Io) {
            delay(1.seconds)
            Timber.d("Draft auto save triggered")
            setBeforeSaveDraft(true, messageBody)
        }
    }
}
