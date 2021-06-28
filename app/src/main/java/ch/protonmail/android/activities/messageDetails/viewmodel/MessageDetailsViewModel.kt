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
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintManager
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.asLiveData
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.messageDetails.IntentExtrasData
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
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.LabelRepository
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.data.toConversationUiModel
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.ui.view.LabelChipUiModel
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.fetch.FetchVerificationKeys
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.DownloadUtils
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.HTMLTransformer.DefaultTransformer
import ch.protonmail.android.utils.HTMLTransformer.ViewportTransformer
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.crypto.KeyInformation
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.takeIfNotBlank
import okio.buffer
import okio.sink
import okio.source
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
internal class MessageDetailsViewModel @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository,
    private val messageRepository: MessageRepository,
    private val userManager: UserManager,
    private val contactsRepository: ContactsRepository,
    private val labelRepository: LabelRepository,
    private val attachmentMetadataDao: AttachmentMetadataDao,
    private val fetchVerificationKeys: FetchVerificationKeys,
    private val attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer,
    private val dispatchers: DispatcherProvider,
    private val attachmentsHelper: AttachmentsHelper,
    private val downloadUtils: DownloadUtils,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val conversationRepository: ConversationsRepository,
    savedStateHandle: SavedStateHandle,
    messageRendererFactory: MessageRenderer.Factory,
    verifyConnection: VerifyConnection,
    networkConfigurator: NetworkConfigurator
) : ConnectivityBaseViewModel(verifyConnection, networkConfigurator) {

    private val messageOrConversationId: String =
        savedStateHandle.get<String>(MessageDetailsActivity.EXTRA_MESSAGE_OR_CONVERSATION_ID)
            ?: throw IllegalStateException("messageId in MessageDetails is Empty!")

    private val location: Constants.MessageLocationType by lazy {
        Constants.MessageLocationType.fromInt(
            savedStateHandle.get<Int>(MessageDetailsActivity.EXTRA_MESSAGE_LOCATION_ID)
                ?: Constants.MessageLocationType.INVALID.messageLocationTypeValue
        )
    }

    private val messageRenderer
        by lazy { messageRendererFactory.create(viewModelScope) }

    val conversationUiModel: LiveData<ConversationUiModel> =
        if (conversationModeEnabled(location)) {
            getConversationFlow().map { it.toConversationUiModel() }
        } else {
            getMessageFlow().map { it.toConversationUiModel() }
        }.asLiveData().distinctUntilChanged()

    private fun getMessageFlow() = userManager.primaryUserId
        .flatMapLatest { userId ->
            if (userId != null) {
                messageRepository.findMessage(userId, messageOrConversationId)
            } else {
                emptyFlow()
            }
        }
        .filterNotNull()

    private fun getConversationFlow() = userManager.primaryUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                return@flatMapLatest emptyFlow()
            }

            conversationRepository.getConversation(messageOrConversationId, Id(userId.id))
                .filter { it is DataResult.Success }
                .map {
                    return@map (it as DataResult.Success).value
                }
        }

    private var publicKeys: List<KeyInformation>? = null

    var renderingPassed = false
    var hasEmbeddedImages: Boolean = false
    private var fetchingPubKeys: Boolean = false
    private var embeddedImagesAttachments: ArrayList<Attachment> = ArrayList()
    private var embeddedImagesToFetch: ArrayList<EmbeddedImage> = ArrayList()
    private var remoteContentDisplayed: Boolean = false

    var renderedFromCache = AtomicBoolean(false)

    var refreshedKeys: Boolean = true

    private val _prepareEditMessageIntentResult: MutableLiveData<Event<IntentExtrasData>> = MutableLiveData()
    private val _decryptedConversationUiModel: MutableLiveData<ConversationUiModel> = MutableLiveData()
    private val _messageRenderedWithImages: MutableLiveData<Message> = MutableLiveData()
    private val _checkStoragePermission: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _messageDetailsError: MutableLiveData<Event<String>> = MutableLiveData()

    private var bodyString: String? = null
        set(value) {
            field = value
            messageRenderer.messageBody = value
        }

    val labels: Flow<List<Label>> =
        conversationUiModel.asFlow()
            .flatMapLatest { conversation ->
                val userId = UserId(userManager.requireCurrentUserId().s)
                val labelsIds = (conversation.labelIds).map(::Id)
                labelRepository.findLabels(userId, labelsIds)
            }

    val nonExclusiveLabelsUiModels: Flow<List<LabelChipUiModel>> =
        labels.map { labelsList ->
            labelsList.map { label ->
                val labelColor = label.color.takeIfNotBlank()
                    ?.let { Color.parseColor(UiUtil.normalizeColor(it)) }

                LabelChipUiModel(Id(label.id), Name(label.name), labelColor)
            }
        }

    val checkStoragePermission: LiveData<Event<Boolean>>
        get() = _checkStoragePermission

    val messageDetailsError: LiveData<Event<String>>
        get() = _messageDetailsError

    val prepareEditMessageIntent: LiveData<Event<IntentExtrasData>>
        get() = _prepareEditMessageIntentResult

    val decryptedConversationUiModel: LiveData<ConversationUiModel>
        get() = _decryptedConversationUiModel

    val messageRenderedWithImages: LiveData<Message>
        get() = _messageRenderedWithImages

    private var areImagesDisplayed: Boolean = false

    init {
        viewModelScope.launch {
            for (renderedMessage in messageRenderer.renderedMessage) {
                val currentUiModel = _decryptedConversationUiModel.value
                val message = currentUiModel?.messages?.find { it.messageId == renderedMessage.messageId }
                message?.decryptedHTML = renderedMessage.renderedHtmlBody
                _messageRenderedWithImages.value = message
                areImagesDisplayed = true
            }
        }
    }

    fun markUnread() {
        messageRepository.markUnRead(listOf(messageOrConversationId))
    }

    fun loadMessageBody(message: Message) = flow {
        Timber.v("loadMessageBody ${message.messageId} isNotDecrypted: ${message.decryptedHTML.isNullOrEmpty()}")

        if (!message.decryptedHTML.isNullOrEmpty()) {
            emit(message)
        } else {
            val userId = userManager.requireCurrentUserId()
            val messageId = requireNotNull(message.messageId)
            val fetchedMessage = messageRepository.getMessage(userId, messageId, true)
            val isDecrypted = fetchedMessage?.tryDecrypt(publicKeys)
            if (isDecrypted == true) {
                Timber.v("message $messageId isDecrypted, isRead: ${fetchedMessage.isRead}")
                if (!fetchedMessage.isRead) {
                    messageRepository.markRead(listOf(messageId))
                }
                emit(fetchedMessage)
            }
        }
    }.flowOn(dispatchers.Io)

    fun loadMailboxItemDetails() {
        viewModelScope.launch(dispatchers.Io) {
            val userId = userManager.requireCurrentUserId()

            Timber.v("loadMailboxItemDetails conversation: ${conversationModeEnabled(location)}, location: $location")
            if (conversationModeEnabled(location)) {
                loadConversationDetails(userId)
                return@launch
            }

            val message = messageRepository.getMessage(userId, messageOrConversationId, true)
            if (message == null) {
                Timber.d("Failed fetching Message Details for message $messageOrConversationId")
                _messageDetailsError.postValue(Event("Failed getting message details"))
                return@launch
            }
            val contactEmail = contactsRepository.findContactEmailByEmail(message.senderEmail)
            message.senderDisplayName = contactEmail?.name.orEmpty()
            emitConversationUiItem(message.toConversationUiModel())
        }
    }

    private suspend fun loadConversationDetails(userId: Id) {
        conversationRepository.getConversation(messageOrConversationId, userId).map { result ->
            if (result is DataResult.Success) {
                val conversation = result.value
                if (conversation.messages?.isEmpty() == true) {
                    return@map null
                }
                onConversationLoaded(conversation, userId)
                return@map conversation
            } else if (result is DataResult.Error) {
                Timber.d("Error loading conversation $messageOrConversationId - cause: ${result.cause}")
                _messageDetailsError.postValue(Event("Failed getting conversation details"))
            }
            return@map null
        }.first { it?.messages?.isNotEmpty() ?: false }
    }

    private suspend fun onConversationLoaded(
        conversation: Conversation,
        userId: Id
    ) {
        val messages = conversation.messages?.mapNotNull { message ->
            messageRepository.findMessageOnce(userId, message.id)?.let { localMessage ->
                val contactEmail = contactsRepository.findContactEmailByEmail(localMessage.senderEmail)
                localMessage.senderDisplayName = contactEmail?.name.orEmpty()
                localMessage
            }
        }
        Timber.v("Loaded conversation ${conversation.id} with ${messages?.size} messages")
        if (messages.isNullOrEmpty()) {
            Timber.d("Failed fetching Message Details for message $messageOrConversationId")
            _messageDetailsError.postValue(Event("Failed getting conversation's messages"))
            return
        }

        val conversationUiItem = conversation.toConversationUiModel().copy(
            messages = messages.sortedBy { it.time }
        )
        emitConversationUiItem(conversationUiItem)
    }

    private fun emitConversationUiItem(conversationUiModel: ConversationUiModel) {
        refreshedKeys = true
        val lastMessage = conversationUiModel.messages.last()
        if (!lastMessage.isDownloaded) {
            Timber.d("Message detail tried loading a non-downloaded message")
            return
        }

        Timber.v("Emitting ConversationUiItem Detail = ${lastMessage.messageId} keys size: ${publicKeys?.size}")
        _decryptedConversationUiModel.postValue(conversationUiModel)
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

    fun startDownloadEmbeddedImagesJob(message: Message) {
        hasEmbeddedImages = false

        viewModelScope.launch(dispatchers.Io) {

            val messageId = message.messageId ?: return@launch
            val attachmentMetadataList = attachmentMetadataDao.getAllAttachmentsForMessage(messageId)
            val embeddedImages = embeddedImagesAttachments.mapNotNull { embeddedImage ->
                attachmentsHelper.fromAttachmentToEmbeddedImage(embeddedImage, message.embeddedImageIds.toList())
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
                messageDetailsRepository.startDownloadEmbeddedImages(
                    messageId, userManager.requireCurrentUserId()
                )
            }
        }
    }

    fun onEmbeddedImagesDownloaded(event: DownloadEmbeddedImagesEvent) {
        Timber.v("onEmbeddedImagesDownloaded status: ${event.status} images size: ${event.images.size}")
        messageRenderer.images.offer(event.images)
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
                embeddedImagesAttachments,
                dispatchers.Io
            )
            _prepareEditMessageIntentResult.value = Event(intent)
        }
    }

    fun viewOrDownloadAttachment(context: Context, attachment: Attachment) {
        viewModelScope.launch(dispatchers.Io) {
            val attachmentId = requireNotNull(attachment.attachmentId)
            val messageId = attachment.messageId
            val metadata = attachmentMetadataDao.getAttachmentMetadataForMessageAndAttachmentId(messageId, attachmentId)
            Timber.v("viewOrDownloadAttachment Id: $attachmentId metadataId: ${metadata?.id}")
            val uri = metadata?.uri
            // extra check if user has not deleted the file
            if (uri != null && attachmentsHelper.isFileAvailable(context, uri)) {
                if (uri.path?.contains(DIR_EMB_ATTACHMENT_DOWNLOADS) == true) {
                    copyAttachmentToDownloadsAndDisplay(context, metadata.name, uri)
                } else {
                    viewAttachment(context, metadata.name, uri)
                }
            } else {
                Timber.d("Attachment id: $attachmentId file not available, uri: $uri ")
                attachmentsWorker.enqueue(messageId, userManager.requireCurrentUserId(), attachmentId)
            }
        }
    }

    private fun lastMessage() = conversationUiModel.value?.messages?.maxByOrNull { it.time }

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

    fun displayRemoteContent(message: Message) {
        remoteContentDisplayed()
        prepareEmbeddedImages(message)
    }

    fun isEmbeddedImagesDisplayed() = areImagesDisplayed

    fun displayEmbeddedImages(message: Message) {
        areImagesDisplayed = true // this will be passed to edit intent
        startDownloadEmbeddedImagesJob(message)
    }

    fun isAutoShowEmbeddedImages(): Boolean {
        val mailSettings = userManager.getCurrentUserMailSettingsBlocking()
        return mailSettings?.showImagesFrom?.includesEmbedded() ?: false
    }

    fun prepareEmbeddedImages(message: Message): Boolean {
        val attachments = message.attachments
        val embeddedImagesToFetch = ArrayList<EmbeddedImage>()
        val embeddedImagesAttachments = ArrayList<Attachment>()
        for (attachment in attachments) {
            val embeddedImage = attachmentsHelper
                .fromAttachmentToEmbeddedImage(attachment, message.embeddedImageIds) ?: continue
            embeddedImagesToFetch.add(embeddedImage)
            embeddedImagesAttachments.add(attachment)
        }

        this.embeddedImagesToFetch = embeddedImagesToFetch
        this.embeddedImagesAttachments = embeddedImagesAttachments

        if (embeddedImagesToFetch.isNotEmpty()) {
            hasEmbeddedImages = true
        }

        return hasEmbeddedImages
    }

    fun triggerVerificationKeyLoading() {
        if (!fetchingPubKeys && publicKeys == null) {
            val message = lastMessage()
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
        val message = lastMessage()

        publicKeys = pubKeys
        refreshedKeys = false

        fetchingPubKeys = false
        renderedFromCache = AtomicBoolean(false)
        // render with the new verification keys
        if (renderingPassed && message != null) {
            RegisterReloadTask(message).execute()
        }
    }

    fun printMessage(activityContext: Context) {
        val message = lastMessage()
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
        decryptedMessageHtml: String,
        windowWidth: Int,
        css: String,
        defaultErrorMessage: String
    ): String? {
        bodyString = try {
            val contentTransformer = DefaultTransformer()
                .pipe(ViewportTransformer(windowWidth, css))

            contentTransformer.transform(Jsoup.parse(decryptedMessageHtml)).toString()
        } catch (ioException: IOException) {
            Timber.e(ioException, "Jsoup is unable to parse HTML message details")
            defaultErrorMessage
        }

        return bodyString
    }

    fun moveToTrash() {
        val message = lastMessage()
        moveMessagesToFolder(
            listOf(messageOrConversationId),
            Constants.MessageLocationType.TRASH.toString(),
            message?.folderLocation ?: EMPTY_STRING
        )
    }

}
