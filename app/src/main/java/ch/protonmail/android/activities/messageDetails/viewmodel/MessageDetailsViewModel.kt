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

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintManager
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.messageDetails.IntentExtrasData
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.activities.messageDetails.MessagePrinter
import ch.protonmail.android.activities.messageDetails.MessageRenderer
import ch.protonmail.android.activities.messageDetails.RegisterReloadTask
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.User
import ch.protonmail.android.attachments.AttachmentsHelper
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.BigContentHolder
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.DIR_EMB_ATTACHMENT_DOWNLOADS
import ch.protonmail.android.core.Constants.RESPONSE_CODE_OK
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.model.*
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.fetch.FetchVerificationKeys
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.DownloadUtils
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.HTMLTransformer.DefaultTransformer
import ch.protonmail.android.utils.HTMLTransformer.ViewportTransformer
import ch.protonmail.android.utils.ServerTime
import ch.protonmail.android.utils.crypto.KeyInformation
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import okio.buffer
import okio.sink
import okio.source
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

/**
 * [ViewModel] for `MessageDetailsActivity`
 *
 * TODO reduce [LiveData]s and keep only a single version of the message
 */
@HiltViewModel
internal class MessageDetailsViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val userManager: UserManager,
    private val contactsRepository: ContactsRepository,
    private val attachmentMetadataDao: AttachmentMetadataDao,
    private val fetchVerificationKeys: FetchVerificationKeys,
    private val attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer,
    private val dispatchers: DispatcherProvider,
    private val attachmentsHelper: AttachmentsHelper,
    private val downloadUtils: DownloadUtils,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    messageRendererFactory: MessageRenderer.Factory,
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
    lateinit var decryptedMessageData: MediatorLiveData<Message>

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
    private val _downloadEmbeddedImagesResult: MutableLiveData<String> = MutableLiveData()
    private val _prepareEditMessageIntentResult: MutableLiveData<Event<IntentExtrasData>> = MutableLiveData()
    private val _checkStoragePermission: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _reloadRecipientsEvent: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _messageDetailsError: MutableLiveData<Event<String>> = MutableLiveData()

    private var bodyString: String? = null
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

    val downloadEmbeddedImagesResult: LiveData<String>
        get() = _downloadEmbeddedImagesResult

    val prepareEditMessageIntent: LiveData<Event<IntentExtrasData>>
        get() = _prepareEditMessageIntentResult

    val publicKeys = MutableLiveData<List<KeyInformation>>()

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

    private var areImagesDisplayed: Boolean = false

    init {
        tryFindMessage()
        messageDetailsRepository.reloadDependenciesForUser(userManager.requireCurrentUserId())

        viewModelScope.launch {
            for (body in messageRenderer.renderedBody) {
                // TODO Sending twice the same value, perhaps we could improve this
                _downloadEmbeddedImagesResult.postValue(body)
                areImagesDisplayed = true
            }
        }
    }

    fun tryFindMessage() {
        messageDetailsRepository.reloadDependenciesForUser(userManager.requireCurrentUserId())
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
        viewModelScope.launch(dispatchers.Io) {
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
                messageDetailsRepository.markRead(listOf(messageId))
                saveMessage()
            }
        }
    }

    //endregion
    fun findAllLabelsWithIds(checkedLabelIds: MutableList<String>) {
        viewModelScope.launch(dispatchers.Io) {
            messageDetailsRepository.findAllLabelsWithIds(
                decryptedMessageData.value ?: Message(), checkedLabelIds,
                labels.value ?: ArrayList(), isTransientMessage
            )
        }
        message.value!!.setLabelIDs(decryptedMessageData.value!!.allLabelIDs)
    }

    fun startDownloadEmbeddedImagesJob() {
        hasEmbeddedImages = false

        viewModelScope.launch(dispatchers.Io) {

            val attachmentMetadataList = attachmentMetadataDao.getAllAttachmentsForMessage(messageId)
            val embeddedImages = _embeddedImagesAttachments.mapNotNull {
                attachmentsHelper.fromAttachmentToEmbeddedImage(
                    it, decryptedMessageData.value!!.embeddedImageIds.toList()
                )
            }
            val embeddedImagesWithLocalFiles = mutableListOf<EmbeddedImage>()
            embeddedImages.forEach { embeddedImage ->
                attachmentMetadataList.find { it.id == embeddedImage.attachmentId }?.let {
                    embeddedImagesWithLocalFiles.add(
                        embeddedImage.copy(localFileName = it.localLocation.substringAfterLast("/"))
                    )
                }
            }

            // don't download embedded images, if we already have them in local storage
            if (
                embeddedImagesWithLocalFiles.isNotEmpty() &&
                embeddedImagesWithLocalFiles.all { it.localFileName != null }
            ) {
                AppUtil.postEventOnUi(DownloadEmbeddedImagesEvent(Status.SUCCESS, embeddedImagesWithLocalFiles))
            } else {
                messageDetailsRepository.startDownloadEmbeddedImages(messageId, userManager.requireCurrentUserId())
            }
        }
    }

    fun onEmbeddedImagesDownloaded(event: DownloadEmbeddedImagesEvent) {
        Timber.v("onEmbeddedImagesDownloaded status: ${event.status} images size: ${event.images.size}")
        if (bodyString.isNullOrEmpty()) {
            _downloadEmbeddedImagesResult.value = bodyString ?: ""
            return
        }

        if (event.status == Status.SUCCESS) {
            messageRenderer.images.offer(event.images)
        }
    }

    fun prepareEditMessageIntent(
        messageAction: Constants.MessageActionType,
        message: Message,
        newMessageTitle: String?,
        content: String,
        mBigContentHolder: BigContentHolder
    ) {
        val user: User = userManager.requireCurrentLegacyUser()
        viewModelScope.launch {
            val intent = messageDetailsRepository.prepareEditMessageIntent(
                messageAction,
                message,
                user,
                newMessageTitle,
                content,
                mBigContentHolder,
                areImagesDisplayed,
                remoteContentDisplayed,
                _embeddedImagesAttachments,
                dispatchers.Io,
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
            var isDecrypted: Boolean = false

            init {
                addSource(this@MessageDetailsViewModel.message) {
                    message = it
                    message?.senderEmail?.let { senderEmail ->
                        addSource(contactsRepository.findContactEmailByEmailLiveData(senderEmail)) { contactEmail ->
                            contact = contactEmail ?: ContactEmail("", message?.senderEmail ?: "", message?.senderName)
                            if (!isDecrypted) {
                                refreshedKeys = true
                                tryEmit()
                            }
                        }
                    }
                    if (!isDecrypted) {
                        refreshedKeys = true
                        tryEmit()
                    }
                }
                addSource(publicKeys) {
                    keys = it
                    refreshedKeys = false
                    tryEmit()
                }
            }

            private fun tryEmit() {
                val message = message ?: return
                if (!message.isDownloaded) {
                    return
                }
                viewModelScope.launch {
                    if (contact?.name != message.sender?.emailAddress)
                        message.senderDisplayName = contact?.name

                    isDecrypted = withContext(dispatchers.Comp) {
                        message.tryDecrypt(keys) ?: false
                    }
                    Timber.v("Message isDecrypted:$isDecrypted, keys size: ${keys?.size}")
                    value = message
                }
            }

            private fun Message.tryDecrypt(verificationKeys: List<KeyInformation>?): Boolean? {
                return try {
                    decrypt(userManager, userManager.requireCurrentUserId(), verificationKeys)
                    true
                } catch (exception: Exception) {
                    // signature verification failed with special case, try to decrypt again without verification
                    // and hardcode verification error
                    if (verificationKeys != null && verificationKeys.isNotEmpty() &&
                        exception.message == "Signature Verification Error: No matching signature"
                    ) {
                        Timber.d(exception, "Decrypting message again without verkeys")
                        decrypt(userManager, userManager.requireCurrentUserId())
                        this.hasValidSignature = false
                        this.hasInvalidSignature = true
                        true
                    } else {
                        Timber.d(exception, "Cannot decrypt message")
                        false
                    }
                }
            }
        }
    }

    fun fetchMessageDetails(checkForMessageAttachmentHeaders: Boolean) {
        if (requestPending.get()) {
            return
        }
        requestPending.set(true)

        viewModelScope.launch {
            var shouldExit = false
            if (checkForMessageAttachmentHeaders) {
                val attHeadersPresent = message.value?.let {
                    messageDetailsRepository.checkIfAttHeadersArePresent(it, dispatchers.Io)
                } ?: false
                shouldExit = checkForMessageAttachmentHeaders && !attHeadersPresent
            }

            if (!shouldExit) {
                withContext(dispatchers.Io) {
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
                                        val savedMessage = findSearchMessageById(messageId).first()
                                        if (savedMessage != null) {
                                            messageResponse.message.writeTo(savedMessage)
                                            saveSearchMessageInDB(savedMessage)
                                        } else {
                                            prepareMessage(messageResponse.message)
                                        }

                                    } else {
                                        val savedMessage = findMessageById(messageId).first()
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
                if (location != Constants.MessageLocationType.ALL_MAIL &&
                    location != Constants.MessageLocationType.STARRED
                ) {
                    break
                }
            }
        }
        message.location = location.messageLocationTypeValue
    }

    fun viewOrDownloadAttachment(context: Context, attachmentToDownloadId: String, messageId: String) {

        viewModelScope.launch(dispatchers.Io) {
            val metadata = attachmentMetadataDao
                .getAttachmentMetadataForMessageAndAttachmentId(messageId, attachmentToDownloadId)
            Timber.v("viewOrDownloadAttachment Id: $attachmentToDownloadId metadataId: ${metadata?.id}")
            if (metadata != null) {
                val uri = metadata.uri
                if (uri != null && attachmentsHelper.isFileAvailable(context, uri)) {
                    if (uri.path?.contains(DIR_EMB_ATTACHMENT_DOWNLOADS) == true) {
                        copyAttachmentToDownloadsAndDisplay(context, metadata.name, uri)
                    } else {
                        viewAttachment(context, metadata.name, uri)
                    }
                } else {
                    Timber.v("No file attachment id: $attachmentToDownloadId downloading again")
                    attachmentsWorker.enqueue(messageId, userManager.requireCurrentUserId(), attachmentToDownloadId)
                }
            } else {
                Timber.v("No metadata found for attachment id: $attachmentToDownloadId")
                attachmentsWorker.enqueue(messageId, userManager.requireCurrentUserId(), attachmentToDownloadId)
            }
        }
    }

    /**
     * Explicitly make a copy of embedded attachment to downloads and display it (product requirement)
     */
    private fun copyAttachmentToDownloadsAndDisplay(
        context: Context,
        filename: String,
        uri: Uri
    ) {
        val newUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getCopiedUriFromQ(filename, uri, context)
        } else {
            getCopiedUriBeforeQ(filename, uri, context)
        }

        Timber.v("Copied attachment file from ${uri.path} to ${newUri?.path}")
        viewAttachment(context, filename, newUri)
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun getCopiedUriFromQ(filename: String, uri: Uri, context: Context): Uri? {
        val contentResolver = context.contentResolver

        return contentResolver.openInputStream(uri)?.let {
            attachmentsHelper.saveAttachmentInMediaStore(
                contentResolver, filename, contentResolver.getType(uri), it
            )
        }
    }

    private fun getCopiedUriBeforeQ(filename: String, uri: Uri, context: Context): Uri {
        val fileInDownloads = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            filename
        )

        context.contentResolver.openInputStream(uri)?.use { stream ->
            fileInDownloads.sink().buffer().use { sink ->
                sink.writeAll(stream.source())
            }
        }

        return FileProvider.getUriForFile(
            context, context.applicationContext.packageName + ".provider", fileInDownloads
        )
    }

    fun viewAttachment(context: Context, filename: String?, uri: Uri?) =
        downloadUtils.viewAttachment(context, filename, uri)

    fun remoteContentDisplayed() {
        remoteContentDisplayed = true
    }

    fun displayRemoteContentClicked() {
        webViewContentWithImages.value = bodyString
        remoteContentDisplayed()
        prepareEmbeddedImages()
    }

    fun isEmbeddedImagesDisplayed() = areImagesDisplayed

    fun displayEmbeddedImages() {
        areImagesDisplayed = true // this will be passed to edit intent
        startDownloadEmbeddedImagesJob()
    }

    fun prepareEmbeddedImages(): Boolean {
        val message = decryptedMessageData.value
        message?.let {
            val attachments = message.Attachments
            val embeddedImagesToFetch = ArrayList<EmbeddedImage>()
            val embeddedImagesAttachments = ArrayList<Attachment>()
            for (attachment in attachments) {
                val embeddedImage = attachmentsHelper
                    .fromAttachmentToEmbeddedImage(attachment, message.embeddedImageIds) ?: continue
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

    fun getParsedMessage(
        decryptedMessage: String,
        windowWidth: Int,
        css: String,
        defaultErrorMessage: String
    ): String? {
        bodyString = try {
            val contentTransformer = DefaultTransformer()
                .pipe(ViewportTransformer(windowWidth, css))

            contentTransformer.transform(Jsoup.parse(decryptedMessage)).toString()
        } catch (ioException: IOException) {
            Timber.e(ioException, "Jsoup is unable to parse HTML message details")
            defaultErrorMessage
        }

        return bodyString
    }

    fun moveToTrash() {
        moveMessagesToFolder(
            listOf(messageId),
            Constants.MessageLocationType.TRASH.toString(),
            message.value?.folderLocation ?: EMPTY_STRING
        )
    }

    fun markUnread() {
        messageDetailsRepository.markUnRead(listOf(messageId))
    }

}
