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
package ch.protonmail.android.activities.messageDetails.viewmodel

import android.content.Context
import android.print.PrintManager
import android.util.Pair
import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.messageDetails.IntentExtrasData
import ch.protonmail.android.activities.messageDetails.MessageDetailsActivity
import ch.protonmail.android.activities.messageDetails.MessagePrinter
import ch.protonmail.android.activities.messageDetails.MessageRenderer
import ch.protonmail.android.activities.messageDetails.RegisterReloadTask
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.BigContentHolder
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.RESPONSE_CODE_OK
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.FetchMessageDetailEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchVerificationKeys
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.DownloadUtils
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.ServerTime
import ch.protonmail.android.utils.crypto.KeyInformation
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel
import com.squareup.otto.Subscribe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [ViewModel] for `MessageDetailsActivity`
 *
 * TODO reduce [LiveData]s and keep only a single version of the message
 */

internal class MessageDetailsViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val userManager: UserManager,
    private val contactsRepository: ContactsRepository,
    private val attachmentMetadataDatabase: AttachmentMetadataDatabase,
    messageRendererFactory: MessageRenderer.Factory,
    private val deleteMessageUseCase: DeleteMessage,
    private val fetchVerificationKeys: FetchVerificationKeys,
    verifyConnection: VerifyConnection,
    networkConfigurator: NetworkConfigurator
) : ConnectivityBaseViewModel(verifyConnection, networkConfigurator) {

    private val messageId: String = savedStateHandle.get<String>(MessageDetailsActivity.EXTRA_MESSAGE_ID)
        ?: throw IllegalStateException("messageId in MessageDetails is Empty!")
    private val isTransientMessage = savedStateHandle.get<Boolean>(MessageDetailsActivity.EXTRA_TRANSIENT_MESSAGE)
        ?: false

    private val messageRenderer
    by lazy { messageRendererFactory.create(viewModelScope, messageId) }

    lateinit var message: LiveData<Message>

    // TODO: this value was a lateinit, but only initialized with an empty `ArrayList`
    val folderIds: MutableList<String> = mutableListOf()
    lateinit var addressId: String

    var renderingPassed = false
    var hasEmbeddedImages: Boolean = false
    private var fetchingPubKeys: Boolean = false
    private var _embeddedImagesAttachments: ArrayList<Attachment> = ArrayList()
    private var _embeddedImagesToFetch: ArrayList<EmbeddedImage> = ArrayList()
    private var remoteContentDisplayed: Boolean = false

    // region properties and data
    private val requestPending = AtomicBoolean(false)
    var renderedFromCache = AtomicBoolean(false)

    var refreshedKeys: Boolean = true

    private val _messageSavedInDBResult: MutableLiveData<Boolean> = MutableLiveData()
    private val _downloadEmbeddedImagesResult: MutableLiveData<Pair<String, String>> = MutableLiveData()
    private val _prepareEditMessageIntentResult: MutableLiveData<Event<IntentExtrasData>> = MutableLiveData()
    private val _checkStoragePermission: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _reloadRecipientsEvent: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _messageDetailsError: MutableLiveData<Event<String>> = MutableLiveData()

    var nonBrokenEmail: String? = null
    var bodyString: String? = null
        set(value) {
            field = value
            messageRenderer.messageBody = value
        }

    val labels: LiveData<List<Label>> by lazy {
        messageDetailsRepository.findAllLabels(message)
    }
    val messageAttachments: LiveData<List<Attachment>> by lazy {
        if (!isTransientMessage) {
            messageDetailsRepository.findAttachments(decryptedMessageData).distinctUntilChanged()
        } else {
            messageDetailsRepository.findAttachmentsSearchMessage(decryptedMessageData).distinctUntilChanged()
        }
    }
    val pendingSend: LiveData<PendingSend?> by lazy {
        messageDetailsRepository.findPendingSendByOfflineMessageIdAsync(messageId)
    }

    val messageSavedInDBResult: LiveData<Boolean>
        get() = _messageSavedInDBResult

    val checkStoragePermission: LiveData<Event<Boolean>>
        get() = _checkStoragePermission

    val reloadRecipientsEvent: LiveData<Event<Boolean>>
        get() = _reloadRecipientsEvent

    val messageDetailsError: LiveData<Event<String>>
        get() = _messageDetailsError

    val downloadEmbeddedImagesResult: LiveData<Pair<String, String>>
        get() = _downloadEmbeddedImagesResult

    val prepareEditMessageIntent: LiveData<Event<IntentExtrasData>>
        get() = _prepareEditMessageIntentResult

    val publicKeys = MutableLiveData<List<KeyInformation>>()
    lateinit var decryptedMessageData: MediatorLiveData<Message>

    init {
        tryFindMessage()
        messageDetailsRepository.reloadDependenciesForUser(userManager.username)

        viewModelScope.launch {
            for (body in messageRenderer.renderedBody) {
                // TODO Sending twice the same value, perhaps we could improve this
                _downloadEmbeddedImagesResult.postValue(Pair(body, body))
                mImagesDisplayed = true
            }
        }
    }

    fun tryFindMessage() {
        message = if (isTransientMessage) {
            messageDetailsRepository.findSearchMessageByIdAsync(messageId)
        } else {
            messageDetailsRepository.findMessageByIdAsync(messageId)
        }
        observeDecryption()
    }

    fun saveMessage() {
        // Return if message is null
        val message = message.value ?: return
        viewModelScope.launch(IO) {
            val result = runCatching {
                messageDetailsRepository.saveMessageInDB(message, isTransientMessage)
            }
            _messageSavedInDBResult.postValue(result.isSuccess)
        }
    }

    fun markRead(read: Boolean) {
        val message = message.value
        message?.let {
            message.accessTime = ServerTime.currentTimeMillis()
            message.setIsRead(read)
            saveMessage()
            if (read) {
                messageDetailsRepository.markRead(messageId)
                saveMessage()
            }
        }
    }

    //endregion
    fun findAllLabelsWithIds(checkedLabelIds: MutableList<String>) {
        viewModelScope.launch(IO) {
            messageDetailsRepository.findAllLabelsWithIds(
                decryptedMessageData.value ?: Message(), checkedLabelIds,
                labels.value ?: ArrayList(), isTransientMessage
            )
        }
        message.value!!.setLabelIDs(decryptedMessageData.value!!.allLabelIDs)
    }

    fun startDownloadEmbeddedImagesJob() {
        hasEmbeddedImages = false

        viewModelScope.launch(IO) {

            val attachmentMetadataList = attachmentMetadataDatabase.getAllAttachmentsForMessage(messageId)
            val embeddedImages = _embeddedImagesAttachments.mapNotNull {
                EmbeddedImage.fromAttachment(
                    it, decryptedMessageData.value!!.embeddedImagesArray.toList()
                )
            }

            embeddedImages.forEach { embeddedImage ->
                attachmentMetadataList.find { it.id == embeddedImage.attachmentId }?.let {
                    embeddedImage.localFileName = it.localLocation.substringAfterLast("/")
                }
            }

            // don't download embedded images, if we already have them in local storage
            if (embeddedImages.all { it.localFileName != null }) {
                AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.SUCCESS, embeddedImages))
            } else {
                messageDetailsRepository.startDownloadEmbeddedImages(messageId, userManager.username)
            }
        }
    }

    fun onEmbeddedImagesDownloaded(event: DownloadEmbeddedImagesEvent) {
        if (bodyString.isNullOrEmpty()) {
            _downloadEmbeddedImagesResult.value = Pair(bodyString ?: "", nonBrokenEmail ?: "")
            return
        }

        messageRenderer.images.offer(event.images)
    }

    fun prepareEditMessageIntent(
        messageAction: Constants.MessageActionType,
        message: Message,
        newMessageTitle: String?,
        content: String,
        mBigContentHolder: BigContentHolder
    ) {
        val user: User = userManager.user
        viewModelScope.launch {
            val intent = messageDetailsRepository.prepareEditMessageIntent(
                messageAction,
                message,
                user,
                newMessageTitle,
                content,
                mBigContentHolder,
                mImagesDisplayed,
                remoteContentDisplayed,
                _embeddedImagesAttachments,
                IO,
                isTransientMessage
            )
            _prepareEditMessageIntentResult.value = Event(intent)
        }
    }

    fun removeMessageLabels() {
        val message = requireNotNull(message.value)
        message.removeLabels(folderIds)
    }

    private fun observeDecryption() {
        decryptedMessageData = object : MediatorLiveData<Message>() {
            var message: Message? = null
            var keys: List<KeyInformation>? = null
            var contact: ContactEmail? = null
            var decrypted: Boolean = false

            init {
                addSource(this@MessageDetailsViewModel.message) {
                    message = it
                    message?.senderEmail?.let { senderEmail ->
                        addSource(contactsRepository.findContactEmailByEmailLiveData(senderEmail)) { contactEmail ->
                            contact = contactEmail ?: ContactEmail("", message?.senderEmail ?: "", message?.senderName)
                            if (!decrypted) {
                                refreshedKeys = true
                                viewModelScope.launch {
                                    withContext(IO) {
                                        tryEmit()
                                    }
                                }
                            }
                        }
                    }
                    if (!decrypted) {
                        refreshedKeys = true
                        viewModelScope.launch {
                            withContext(IO) {
                                tryEmit()
                            }
                        }
                    }
                }
                addSource(publicKeys) {
                    keys = it
                    refreshedKeys = false
                    viewModelScope.launch {
                        withContext(IO) {
                            tryEmit()
                        }
                    }
                }
            }

            private fun Message.tryDecrypt(verificationKeys: List<KeyInformation>?): Boolean? {
                return try {
                    try {
                        decrypt(userManager = userManager, username = userManager.username, verKeys = verificationKeys)
                    } catch (e: Exception) {
                        // signature verification failed with special case, try to decrypt again without verification
                        // and hardcode verification error
                        if (verificationKeys != null && verificationKeys.isNotEmpty() && e.message == "Signature Verification Error: No matching signature") {
                            Timber.d(e, "Decrypting message again without verkeys")
                            decrypt(userManager = userManager, username = userManager.username)
                            this.hasValidSignature = false
                            this.hasInvalidSignature = true
                            //refreshedKeys = true
                            return true
                        } else {
                            Timber.d(e, "Cannot decrypt message even without empty verkeys")
                            return false
                        }
                    }
                    true
                } catch (e: Throwable) {
                    Timber.d(e, "Cannot decrypt message")
                    false
                }
            }

            private fun tryEmit() {
                val message = message ?: return
                if (!message.isDownloaded) {
                    return
                }
                if (!contact?.name.equals(message.sender!!.emailAddress))
                    message.senderDisplayName = contact?.name
                decrypted = message.tryDecrypt(keys) ?: false
                postValue(message)
            }
        }
    }

    val webViewContentWithoutImages = MutableLiveData<String>()
    val webViewContentWithImages = MutableLiveData<String>()
    val webViewContent = object : MediatorLiveData<String>() {
        var contentWithoutImages: String? = null
        var contentWithImages: String? = null

        init {
            addSource(webViewContentWithoutImages) {
                contentWithoutImages = it
                emit()
            }
            addSource(webViewContentWithImages) {
                contentWithImages = it
                emit()
            }
        }

        fun emit() {
            value = contentWithImages ?: contentWithoutImages
        }
    }

    fun fetchMessageDetails(checkForMessageAttachmentHeaders: Boolean) {
        if (requestPending.get()) {
            return
        }
        requestPending.set(true)
        val bgDispatcher: CoroutineDispatcher = IO

        viewModelScope.launch {
            var shouldExit = false
            if (checkForMessageAttachmentHeaders) {
                val attHeadersPresent = message.value?.let {
                    messageDetailsRepository.checkIfAttHeadersArePresent(it, bgDispatcher)
                } ?: false
                shouldExit = checkForMessageAttachmentHeaders && !attHeadersPresent
            }

            if (!shouldExit) {
                withContext(IO) {
                    val messageDetailsResult = runCatching {
                        with(messageDetailsRepository) {
                            if (isTransientMessage) fetchSearchMessageDetails(messageId)
                            else fetchMessageDetails(messageId)
                        }
                    }

                    messageDetailsResult
                        .onFailure {
                            requestPending.set(false)
                            _messageDetailsError.postValue(Event(""))
                        }
                        .onSuccess { messageResponse ->
                            if (messageResponse.code == RESPONSE_CODE_OK) {
                                with(messageDetailsRepository) {

                                    if (isTransientMessage) {
                                        val savedMessage = findSearchMessageById(messageId, bgDispatcher)
                                        if (savedMessage != null) {
                                            messageResponse.message.writeTo(savedMessage)
                                            saveSearchMessageInDB(savedMessage)
                                        } else {
                                            prepareMessage(messageResponse.message)
                                        }

                                    } else {
                                        val savedMessage = findMessageById(messageId, bgDispatcher)
                                        if (savedMessage != null) {
                                            messageResponse.message.writeTo(savedMessage)
                                            saveMessageInDB(savedMessage)
                                        } else {
                                            prepareMessage(messageResponse.message)
                                            setFolderLocation(messageResponse.message)
                                            saveMessageInDB(messageResponse.message, isTransientMessage)
                                        }
                                    }
                                }
                            } else {
                                _messageDetailsError.postValue(Event(messageResponse.error))
                            }
                        }
                }
            }
        }
    }

    private fun prepareMessage(message: Message) { // TODO: it's not clear why message is assigning values to itself
        message.toList = message.toList
        message.ccList = message.ccList
        message.bccList = message.bccList
        message.replyTos = message.replyTos
        message.sender = message.sender
        message.setLabelIDs(message.getEventLabelIDs())
        message.header = message.header
        message.parsedHeaders = message.parsedHeaders
        var location = Constants.MessageLocationType.INBOX
        for (labelId in message.allLabelIDs) {
            if (labelId.length <= 2) {
                location = Constants.MessageLocationType.fromInt(Integer.valueOf(labelId))
                if (location != Constants.MessageLocationType.ALL_MAIL && location != Constants.MessageLocationType.STARRED) {
                    break
                }
            }
        }
        message.location = location.messageLocationTypeValue
    }

    fun tryDownloadingAttachment(context: Context, attachmentToDownloadId: String, messageId: String) {

        viewModelScope.launch(IO) {
            val metadata = attachmentMetadataDatabase.getAttachmentMetadataForMessageAndAttachmentId(messageId, attachmentToDownloadId)
            if (metadata != null) {
                if (metadata.localLocation.endsWith("==")) { // migration for deprecated saving attachments as files with name <attachmentId>
                    attachmentMetadataDatabase.deleteAttachmentMetadata(metadata)
                    DownloadEmbeddedAttachmentsWorker.enqueue(messageId, userManager.username, attachmentToDownloadId)
                } else {
                    DownloadUtils.viewCachedAttachmentFile(context, metadata.name, metadata.localLocation)
                }
            } else {
                DownloadEmbeddedAttachmentsWorker.enqueue(messageId, userManager.username, attachmentToDownloadId)
            }
        }
    }

    @Subscribe
    fun onFetchMessageDetailEvent(event: FetchMessageDetailEvent?) {
        if (event?.messageId == null || event.messageId != messageId) {
            return
        }
        if (event.success) {
            Timber.v("FetchMessage success")
        }
    }

    fun remoteContentDisplayed() {
        remoteContentDisplayed = true
    }

    fun displayRemoteContentClicked() {
        bodyString = nonBrokenEmail
        webViewContentWithImages.value = bodyString
        remoteContentDisplayed()
        prepareEmbeddedImages()
    }

    fun isEmbeddedImagesDisplayed() = mImagesDisplayed

    fun displayEmbeddedImages() {
        mImagesDisplayed = true // this will be passed to edit intent
        startDownloadEmbeddedImagesJob()
    }

    private var mImagesDisplayed: Boolean = false

    fun prepareEmbeddedImages(): Boolean {
        val message = decryptedMessageData.value
        message?.let {
            val attachments = message.Attachments
            val embeddedImagesToFetch = ArrayList<EmbeddedImage>()
            val embeddedImagesAttachments = ArrayList<Attachment>()
            for (attachment in attachments) {
                val embeddedImage = EmbeddedImage
                    .fromAttachment(attachment, message.embeddedImagesArray.toList()) ?: continue
                embeddedImagesToFetch.add(embeddedImage)
                embeddedImagesAttachments.add(attachment)
            }

            this._embeddedImagesToFetch = embeddedImagesToFetch
            this._embeddedImagesAttachments = embeddedImagesAttachments

            if (embeddedImagesToFetch.isNotEmpty()) {
                hasEmbeddedImages = true
            }
        }
        return hasEmbeddedImages
    }

    fun triggerVerificationKeyLoading() {
        if (!fetchingPubKeys && publicKeys.value == null) {
            val message = message.value
            message?.let {
                fetchingPubKeys = true
                viewModelScope.launch {
                    val result = fetchVerificationKeys(message.senderEmail)
                    onFetchVerificationKeysEvent(result)
                }
            }
        }
    }

    private fun onFetchVerificationKeysEvent(pubKeys: List<KeyInformation>) {
        Timber.v("FetchVerificationKeys received $pubKeys")
        val message = message.value
        publicKeys.value = pubKeys
        fetchingPubKeys = false
        renderedFromCache = AtomicBoolean(false)
        _reloadRecipientsEvent.value = Event(true)
        // render with the new verification keys
        if (renderingPassed && message != null) {
            RegisterReloadTask(message, requestPending).execute()
        }
    }

    fun setAttachmentsList(attachments: List<Attachment>) {
        val message = decryptedMessageData.value
        message!!.setAttachmentList(attachments)
    }

    fun isPgpEncrypted(): Boolean = message.value?.messageEncryption?.isPGPEncrypted ?: false

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            deleteMessageUseCase(listOf(messageId))
        }
    }

    fun printMessage(activityContext: Context) {
        val message = message.value
        message?.let {
            MessagePrinter(
                activityContext,
                activityContext.resources,
                activityContext.getSystemService(Context.PRINT_SERVICE) as PrintManager,
                remoteContentDisplayed
            ).printMessage(it, this.bodyString ?: "")
        }
    }
}
