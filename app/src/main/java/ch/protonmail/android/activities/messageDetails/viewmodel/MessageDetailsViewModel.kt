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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.SavedStateHandle
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
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.data.toConversationUiModel
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.details.presentation.model.MessageBodyState
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.ReportPhishingJob
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchVerificationKeys
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.DownloadUtils
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.HTMLTransformer.DefaultTransformer
import ch.protonmail.android.utils.HTMLTransformer.ViewportTransformer
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.crypto.KeyInformation
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel
import com.birbit.android.jobqueue.JobManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.mapSecond
import me.proton.core.util.kotlin.takeIfNotBlank
import okio.buffer
import okio.sink
import okio.source
import org.jsoup.Jsoup
import timber.log.Timber
import java.io.File
import java.io.IOException
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
    private val moveConversationsToFolder: MoveConversationsToFolder,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val conversationRepository: ConversationsRepository,
    private val changeConversationsReadStatus: ChangeConversationsReadStatus,
    private val changeConversationsStarredStatus: ChangeConversationsStarredStatus,
    private val deleteMessage: DeleteMessage,
    private val deleteConversations: DeleteConversations,
    private val savedStateHandle: SavedStateHandle,
    messageRendererFactory: MessageRenderer.Factory,
    verifyConnection: VerifyConnection,
    networkConfigurator: NetworkConfigurator
) : ConnectivityBaseViewModel(verifyConnection, networkConfigurator), LifecycleObserver {

    private val messageOrConversationId: String =
        savedStateHandle.get<String>(MessageDetailsActivity.EXTRA_MESSAGE_OR_CONVERSATION_ID)
            ?: throw IllegalStateException("messageId in MessageDetails is Empty!")

    private val location: Constants.MessageLocationType
        get() = Constants.MessageLocationType.fromInt(
            savedStateHandle.get<Int>(MessageDetailsActivity.EXTRA_MESSAGE_LOCATION_ID)
                ?: Constants.MessageLocationType.INVALID.messageLocationTypeValue
        )

    private val mailboxLocationId: String? by lazy {
        savedStateHandle.get<String>(MessageDetailsActivity.EXTRA_MAILBOX_LABEL_ID)
    }

    private val messageRenderer
        by lazy { messageRendererFactory.create(viewModelScope) }

    private var publicKeys: List<KeyInformation>? = null

    var renderingPassed = false
    var hasEmbeddedImages: Boolean = false
    private var fetchingPubKeys: Boolean = false
    private var embeddedImagesAttachments: ArrayList<Attachment> = ArrayList()
    private var embeddedImagesToFetch: ArrayList<EmbeddedImage> = ArrayList()
    private var remoteContentDisplayed: Boolean = false
    var refreshedKeys: Boolean = true

    private val _prepareEditMessageIntentResult: MutableLiveData<Event<IntentExtrasData>> = MutableLiveData()
    private val _decryptedConversationUiModel: MutableLiveData<ConversationUiModel> = MutableLiveData()
    private val _messageRenderedWithImages: MutableLiveData<Message> = MutableLiveData()
    private val _checkStoragePermission: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _messageDetailsError: MutableLiveData<Event<String>> = MutableLiveData()
    private val _showPermissionMissingDialog: MutableLiveData<Unit> = MutableLiveData()
    private val _conversationUiFlow = MutableSharedFlow<ConversationUiModel>(replay = 1)

    val conversationUiModel: SharedFlow<ConversationUiModel>
        get() = _conversationUiFlow

    val checkStoragePermission: LiveData<Event<Boolean>>
        get() = _checkStoragePermission

    val messageDetailsError: LiveData<Event<String>>
        get() = _messageDetailsError

    val showPermissionMissingDialog: LiveData<Unit>
        get() = _showPermissionMissingDialog

    val prepareEditMessageIntent: LiveData<Event<IntentExtrasData>>
        get() = _prepareEditMessageIntentResult

    val decryptedConversationUiModel: LiveData<ConversationUiModel>
        get() = _decryptedConversationUiModel

    val messageRenderedWithImages: LiveData<Message>
        get() = _messageRenderedWithImages

    private var areImagesDisplayed: Boolean = false

    private var visibleToTheUser = true

    init {
        // message render flow
        userManager.primaryUserId
            .filterNotNull()
            .flatMapLatest { userId ->
                if (isConversationEnabled()) {
                    getConversationFlow(userId)
                } else {
                    getMessageFlow(userId)
                }
            }
            .filterNotNull()
            .distinctUntilChanged()
            .combineWithLabels()
            .onEach {
                Timber.i("Emit conversation Ui model subject ${it.subject}")
                emitConversationUiItem(it)
            }
            .launchIn(viewModelScope)
    }

    private fun getMessageFlow(userId: UserId): Flow<ConversationUiModel?> =
        messageRepository.observeMessage(userId, messageOrConversationId)
            .distinctUntilChanged()
            .map {
                loadMessageDetails(it)
            }

    private fun getConversationFlow(userId: UserId): Flow<ConversationUiModel?> =
        conversationRepository.getConversation(userId, messageOrConversationId)
            .distinctUntilChanged()
            .filterOutIncompleteConversations()
            .map {
                loadConversationDetails(it, userId)
            }

    private fun Flow<DataResult<Conversation>>.filterOutIncompleteConversations() = filterNot { result ->
        result is DataResult.Success && !result.value.isComplete()
    }

    private fun Flow<ConversationUiModel>.combineWithLabels() = flatMapLatest { conversation ->
        val nonExclusiveLabelsHashMap = hashMapOf<String, List<LabelChipUiModel>>()
        val exclusiveLabelsHashMap = hashMapOf<String, List<Label>>()
        conversation.messages.filter { it.allLabelIDs.isNotEmpty() }.forEach { message ->
            val messageId = requireNotNull(message.messageId)
            getAllLabelsFor(message)?.let { (exclusiveLabels, nonExclusiveLabels) ->
                exclusiveLabelsHashMap[messageId] = exclusiveLabels.toList()
                nonExclusiveLabelsHashMap[messageId] = nonExclusiveLabels
            }
        }
        return@flatMapLatest flowOf(
            conversation.copy(
                nonExclusiveLabels = nonExclusiveLabelsHashMap,
                exclusiveLabels = exclusiveLabelsHashMap
            )
        )
    }

    private suspend fun getAllLabelsFor(
        message: Message
    ): Pair<Collection<Label>, List<LabelChipUiModel>>? {
        val allLabelIds = message.allLabelIDs.map { labelId -> LabelId(labelId) }
        return labelRepository.observeLabels(allLabelIds)
            .firstOrNull()
            ?.partition { it.type == LabelType.FOLDER }
            ?.mapSecond { it.toNonExclusiveLabelModel() }
    }

    private fun Label.toNonExclusiveLabelModel(): LabelChipUiModel {
        val labelColor = color.takeIfNotBlank()
            ?.let { Color.parseColor(UiUtil.normalizeColor(it)) }
        return LabelChipUiModel(id, Name(name), labelColor)
    }

    fun markUnread() {
        viewModelScope.launch {
            if (isConversationEnabled() && doesConversationHaveMoreThanOneMessage()) {
                changeConversationsReadStatus(
                    listOf(messageOrConversationId),
                    ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                    userManager.requireCurrentUserId(),
                    mailboxLocationId ?: location.messageLocationTypeValue.toString()
                )
            } else {
                lastMessage()?.let { message ->
                    messageRepository.markUnRead(listOf(requireNotNull(message.messageId)))
                }
            }
        }
    }

    fun loadMessageBody(message: Message) = flow {
        Timber.v("loadMessageBody ${message.messageId} isNotDecrypted: ${message.decryptedHTML.isNullOrEmpty()}")

        if (!message.decryptedHTML.isNullOrEmpty()) {
            emit(MessageBodyState.Success(message))
        } else {
            val userId = userManager.requireCurrentUserId()
            val messageId = requireNotNull(message.messageId)
            val fetchedMessage = messageRepository.getMessage(userId, messageId, true) ?: return@flow

            val isDecrypted = fetchedMessage.tryDecrypt(publicKeys)
            Timber.v("message $messageId isDecrypted, isRead: ${fetchedMessage.isRead}")
            if (!fetchedMessage.isRead && visibleToTheUser) {
                messageRepository.markRead(listOf(messageId))
            }

            if (isDecrypted == true) {
                emit(MessageBodyState.Success(fetchedMessage))
            } else {
                emit(MessageBodyState.Error.DecryptionError(fetchedMessage))
            }
        }
    }.flowOn(dispatchers.Io)

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pause() {
        visibleToTheUser = false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resume() {
        visibleToTheUser = true
    }

    private suspend fun loadMessageDetails(message: Message?): ConversationUiModel? {

        val messageWithDetails = if (message == null || !message.isDownloaded) {
            Timber.v("Message is not downloaded, trying to fetch it")
            val userId = userManager.requireCurrentUserId()
            messageRepository.getMessage(userId, messageOrConversationId, true)
        } else {
            message
        }

        if (messageWithDetails == null || !messageWithDetails.isDownloaded) {
            Timber.i("Failed fetching Message Details for message $messageOrConversationId")
            _messageDetailsError.postValue(Event("Failed getting message details"))
            return null
        }

        val contact = contactsRepository.findContactEmailByEmail(messageWithDetails.senderEmail)
        val contactName = contact?.name?.takeIfNotBlank()
        if (contactName != null && contactName != contact.email) {
            messageWithDetails.senderDisplayName = contact.name
        }
        return messageWithDetails.toConversationUiModel()
    }

    fun isConversationEnabled() = conversationModeEnabled(location)

    fun doesConversationHaveMoreThanOneMessage() = runBlocking {
        val messagesCount = conversationUiModel.first().messagesCount
        if (messagesCount != null) messagesCount > 1 else false
    }

    private suspend fun loadConversationDetails(
        result: DataResult<Conversation>,
        userId: UserId
    ): ConversationUiModel? {
        return when (result) {
            is DataResult.Success -> {
                Timber.v("loadConversationDetails Success")
                val conversation = result.value
                if (conversation.messages?.isEmpty() == true) {
                    Timber.i("Failed getting conversation details, empty messages")
                    null
                } else {
                    onConversationLoaded(conversation, userId)
                }
            }
            is DataResult.Error -> {
                Timber.d("loadConversationDetails $messageOrConversationId Error - cause: ${result.cause}")
                _messageDetailsError.postValue(Event("Failed getting conversation details"))
                null
            }
            else -> {
                Timber.v("loadConversationDetails result ${result.javaClass.canonicalName}")
                null
            }
        }
    }

    private suspend fun onConversationLoaded(
        conversation: Conversation,
        userId: UserId
    ): ConversationUiModel? {
        val messages = conversation.messages?.mapNotNull { message ->
            messageRepository.findMessage(userId, message.id)?.let { localMessage ->
                val contact = contactsRepository.findContactEmailByEmail(localMessage.senderEmail)
                val contactName = contact?.name?.takeIfNotBlank()
                if (contactName != null && contactName != contact.email) {
                    localMessage.senderDisplayName = contact.name
                }
                localMessage
            }
        }
        if (messages.isNullOrEmpty()) {
            Timber.d("Failed fetching Message Details for message $messageOrConversationId")
            _messageDetailsError.postValue(Event("Failed getting conversation's messages"))
            return null
        }

        return conversation.toConversationUiModel().copy(
            messages = messages.sortedBy { it.time }
        )
    }

    private suspend fun emitConversationUiItem(conversationUiModel: ConversationUiModel) {
        refreshedKeys = true
        _decryptedConversationUiModel.postValue(conversationUiModel)
        _conversationUiFlow.emit(conversationUiModel)
    }

    private fun Message.tryDecrypt(verificationKeys: List<KeyInformation>?): Boolean? {
        return try {
            decrypt(userManager, userManager.requireCurrentUserId(), verificationKeys)
            Timber.d("decrypted verificationKeys size: ${verificationKeys?.size}, body size: ${messageBody?.length}")
            true
        } catch (exception: Exception) {
            // signature verification failed with special case, try to decrypt again without verification
            // and hardcode verification error
            if (verificationKeys != null && verificationKeys.isNotEmpty() &&
                exception.message == "Signature Verification Error: No matching signature"
            ) {
                Timber.i(exception, "Decrypting message again without verkeys")
                decrypt(userManager, userManager.requireCurrentUserId())
                this.hasValidSignature = false
                this.hasInvalidSignature = true
                true
            } else {
                Timber.w(exception, "Cannot decrypt message")
                false
            }
        }
    }

    fun startDownloadEmbeddedImagesJob(message: Message, embeddedImageIds: List<String>) {
        hasEmbeddedImages = false

        viewModelScope.launch(dispatchers.Io) {

            val messageId = message.messageId ?: return@launch
            val attachmentMetadataList = attachmentMetadataDao.getAllAttachmentsForMessage(messageId)
            val embeddedImages = embeddedImagesAttachments.mapNotNull { embeddedImage ->
                attachmentsHelper.fromAttachmentToEmbeddedImage(embeddedImage, embeddedImageIds)
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
        viewModelScope.launch {
            Timber.v("onEmbeddedImagesDownloaded status: ${event.status} images size: ${event.images.size}")
            val messageId = event.images.first().messageId
            val renderedMessage = try {
                messageRenderer.setImagesAndProcess(messageId, event.images)
            } catch (e: IllegalStateException) {
                if (e is CancellationException) throw e
                Timber.e(e)
                return@launch
            }

            val updatedMessage = updateUiModelMessageWithFormattedHtml(
                renderedMessage.messageId,
                renderedMessage.renderedHtmlBody
            ) ?: run {
                Timber.e("Cannot update message with formatted html. Message id: $messageId")
                return@launch
            }

            Timber.v("Update rendered HTML message id: ${updatedMessage.messageId}")
            _messageRenderedWithImages.value = updatedMessage
            areImagesDisplayed = true
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

    private suspend fun lastMessage() = conversationUiModel.firstOrNull()?.messages?.maxByOrNull { it.time }

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
        startDownloadEmbeddedImagesJob(message, message.embeddedImageIds)
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
            viewModelScope.launch {
                val message = lastMessage()
                message?.let {
                    fetchingPubKeys = true
                    val result = fetchVerificationKeys(message.senderEmail)
                    onFetchVerificationKeysEvent(result, message)
                }
            }
        }
    }

    private fun onFetchVerificationKeysEvent(pubKeys: List<KeyInformation>, message: Message) {
        Timber.v("FetchVerificationKeys received $pubKeys")

        publicKeys = pubKeys
        refreshedKeys = false

        fetchingPubKeys = false
        // render with the new verification keys
        if (renderingPassed) {
            RegisterReloadTask(message, labelRepository).execute()
        }
    }

    fun printMessage(messageId: String, activityContext: Context) {
        _decryptedConversationUiModel.value?.messages?.find { it.messageId == messageId }?.let {
            MessagePrinter(
                activityContext,
                activityContext.resources,
                activityContext.getSystemService(Context.PRINT_SERVICE) as PrintManager,
                remoteContentDisplayed
            ).printMessage(it, it.decryptedHTML ?: "")
        }
    }

    fun formatMessageHtmlBody(
        message: Message,
        windowWidth: Int,
        css: String,
        defaultErrorMessage: String
    ): String {
        val messageId = requireNotNull(message.messageId) { "message id is null" }
        val formattedHtml = try {
            val contentTransformer = DefaultTransformer()
                .pipe(ViewportTransformer(windowWidth, css))

            contentTransformer.transform(Jsoup.parse(message.decryptedHTML)).toString()
        } catch (ioException: IOException) {
            Timber.e(ioException, "Jsoup is unable to parse HTML message details")
            defaultErrorMessage
        }

        updateUiModelMessageWithFormattedHtml(message.messageId, formattedHtml, message.decryptedBody)
        // Set the body of the message currently being displayed in messageRenderer to allow embedded images loading
        messageRenderer.setMessageBody(messageId, formattedHtml)
        return formattedHtml
    }

    private fun updateUiModelMessageWithFormattedHtml(
        messageId: String?,
        formattedHtml: String?,
        decryptedBody: String? = null
    ): Message? {
        // Needed to ensure the `decryptedHTML` is available on a message when an action is executed on it,
        // since most of the click listeners in `MessageDetailsActivity` that trigger actions (such as
        // reply or printMessage) hold a reference to the message in the `conversationUiModel object.
        // This is considered tech debt and detailed in MAILAND-2119
        val currentUiModel = _decryptedConversationUiModel.value
        val message = currentUiModel?.messages?.find { it.messageId == messageId }
        decryptedBody?.let {
            message?.decryptedBody = it
        }
        message?.decryptedHTML = formattedHtml
        return message
    }

    fun moveLastMessageToTrash() {
        viewModelScope.launch {
            val primaryUserId = userManager.requireCurrentUserId()
            if (isConversationEnabled() && doesConversationHaveMoreThanOneMessage()) {
                moveConversationsToFolder(
                    listOf(messageOrConversationId),
                    primaryUserId,
                    Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
                )
            } else {
                lastMessage()?.let { message ->
                    moveMessagesToFolder(
                        listOf(requireNotNull(message.messageId)),
                        Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString(),
                        message.folderLocation ?: EMPTY_STRING,
                        primaryUserId
                    )
                }
            }

        }
    }

    fun delete() {
        viewModelScope.launch {
            if (isConversationEnabled() && doesConversationHaveMoreThanOneMessage()) {
                val primaryUserId = userManager.requireCurrentUserId()
                deleteConversations(
                    listOf(messageOrConversationId),
                    primaryUserId,
                    location.messageLocationTypeValue.toString()
                )
            } else {
                lastMessage()?.let { message ->
                    deleteMessage(
                        listOf(requireNotNull(message.messageId)),
                        location.messageLocationTypeValue.toString()
                    )
                }
            }
        }
    }

    fun handleStarUnStar(messageOrConversationId: String, isChecked: Boolean) {
        val ids = listOf(messageOrConversationId)

        if (isConversationEnabled()) {
            viewModelScope.launch {
                val starAction = if (isChecked) {
                    ChangeConversationsStarredStatus.Action.ACTION_STAR
                } else {
                    ChangeConversationsStarredStatus.Action.ACTION_UNSTAR
                }
                val primaryUserId = userManager.requireCurrentUserId()
                changeConversationsStarredStatus(
                    ids,
                    primaryUserId,
                    starAction
                )
            }
        } else {
            if (isChecked) {
                messageRepository.starMessages(ids)
            } else {
                messageRepository.unStarMessages(ids)
            }
        }
    }

    fun sendPhishingReport(message: Message, jobManager: JobManager) {
        jobManager.addJobInBackground(
            ReportPhishingJob(
                requireNotNull(message.messageId),
                requireNotNull(message.decryptedBody),
                requireNotNull(message.mimeType)
            )
        )
    }

    fun storagePermissionDenied() {
        _showPermissionMissingDialog.value = Unit
    }

    fun shouldShowDeleteActionInBottomActionBar(): Boolean {
        return if (isConversationEnabled() && doesConversationHaveMoreThanOneMessage()) {
            location == Constants.MessageLocationType.TRASH
        } else {
            location in arrayOf(
                Constants.MessageLocationType.TRASH,
                Constants.MessageLocationType.DRAFT,
                Constants.MessageLocationType.SENT,
                Constants.MessageLocationType.SPAM
            )
        }
    }
}
