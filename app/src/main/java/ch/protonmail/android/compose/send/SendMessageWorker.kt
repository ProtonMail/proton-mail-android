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

package ch.protonmail.android.compose.send

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.factories.MessageSecurityOptions
import ch.protonmail.android.api.models.factories.PackageFactory
import ch.protonmail.android.api.models.factories.SendPreferencesFactory
import ch.protonmail.android.api.models.messages.send.MessageSendBody
import ch.protonmail.android.api.models.messages.send.MessageSendResponse
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_VERIFICATION_NEEDED
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.compose.send.SendMessageWorkerError.ApiRequestReturnedBadBodyCode
import ch.protonmail.android.compose.send.SendMessageWorkerError.DraftCreationFailed
import ch.protonmail.android.compose.send.SendMessageWorkerError.ErrorPerformingApiRequest
import ch.protonmail.android.compose.send.SendMessageWorkerError.FailureBuildingApiRequest
import ch.protonmail.android.compose.send.SendMessageWorkerError.FetchSendPreferencesFailed
import ch.protonmail.android.compose.send.SendMessageWorkerError.MessageAlreadySent
import ch.protonmail.android.compose.send.SendMessageWorkerError.MessageNotFound
import ch.protonmail.android.compose.send.SendMessageWorkerError.SavedDraftMessageNotFound
import ch.protonmail.android.compose.send.SendMessageWorkerError.UserVerificationNeeded
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_SENT
import ch.protonmail.android.core.Constants.MessageLocationType.SENT
import ch.protonmail.android.core.Constants.RESPONSE_CODE_OK
import ch.protonmail.android.core.DetailedException
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.core.apiError
import ch.protonmail.android.core.messageId
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.compose.SaveDraftResult
import ch.protonmail.android.utils.notifier.UserNotifier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.serialize
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val KEY_INPUT_SEND_MESSAGE_MSG_DB_ID = "keySendMessageMessageDbId"
internal const val KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS = "keySendMessageAttachmentIds"
internal const val KEY_INPUT_SEND_MESSAGE_MESSAGE_ID = "keySendMessageMessageLocalId"
internal const val KEY_INPUT_SEND_MESSAGE_CURRENT_USERNAME = "keySendMessageCurrentUsername"
internal const val KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID = "keySendMessageMessageParentId"
internal const val KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_ENUM_VAL = "keySendMessageMessageActionTypeEnumValue"
internal const val KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID = "keySendMessagePreviousSenderAddressId"
internal const val KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED = "keySendMessageSecurityOptionsSerialized"

internal const val KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM = "keySendMessageErrorResult"

private const val INPUT_MESSAGE_DB_ID_NOT_FOUND = -1L
private const val SEND_MESSAGE_MAX_RETRIES = 2
private const val NO_CONTACTS_AUTO_SAVE = 0
private const val SEND_MESSAGE_WORK_NAME_PREFIX = "sendMessageUniqueWorkName"
private const val NO_SUBJECT = ""

class SendMessageWorker @WorkerInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val saveDraft: SaveDraft,
    private val sendPreferencesFactory: SendPreferencesFactory.Factory,
    private val apiManager: ProtonMailApiManager,
    private val packagesFactory: PackageFactory,
    private val userManager: UserManager,
    private val userNotifier: UserNotifier,
    private val pendingActionsDao: PendingActionsDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val messageDatabaseId = getInputMessageDatabaseId()
        val inputMessageId = getInputMessageId()
        Timber.i(
            "Send Message Worker executing with messageDatabaseId $messageDatabaseId - messageID $inputMessageId"
        )
        val message = messageDetailsRepository.findMessageByMessageDbId(messageDatabaseId)
            ?: messageDetailsRepository.findMessageById(inputMessageId)
        if (message == null) {
            showSendMessageError(NO_SUBJECT)
            pendingActionsDao.deletePendingSendByDbId(messageDatabaseId)
            return failureWithError(MessageNotFound)
        }

        Timber.d("Send Message Worker read local message with messageId ${message.messageId}")

        val previousSenderAddressId = requireNotNull(getInputPreviousSenderAddressId())
        val username = requireNotNull(getInputCurrentUsername())

        return when (val result = saveDraft(message, previousSenderAddressId)) {
            is SaveDraftResult.Success -> {
                val messageId = result.draftId
                Timber.i("Send Message Worker saved draft successfully for messageId $messageId")
                val savedDraftMessage = messageDetailsRepository.findMessageById(messageId) ?: return retryOrFail(
                    SavedDraftMessageNotFound,
                    message
                )

                Timber.d("Send Message Worker fetching send preferences for messageId $messageId")
                val sendPreferences = requestSendPreferences(savedDraftMessage, username) ?: return retryOrFail(
                    FetchSendPreferencesFailed,
                    savedDraftMessage
                )

                Timber.d("Send Message Worker building request for messageId $messageId")
                savedDraftMessage.decrypt(userManager, username)
                val requestBody = try {
                    buildSendMessageRequest(savedDraftMessage, sendPreferences, username)
                } catch (t: Throwable) {
                    if (t is CancellationException) throw t
                    return retryOrFail(FailureBuildingApiRequest, savedDraftMessage, t)
                }

                return try {
                    withContext(NonCancellable) {
                        val response = apiManager.sendMessage(messageId, requestBody, RetrofitTag(username))
                        handleSendMessageResponse(messageId, response, savedDraftMessage)
                    }
                } catch (exception: IOException) {
                    retryOrFail(ErrorPerformingApiRequest, savedDraftMessage, exception)
                } catch (exception: Exception) {
                    pendingActionsDao.deletePendingSendByMessageId(messageId)
                    showSendMessageError(message.subject)
                    throw exception
                }
            }

            is SaveDraftResult.MessageAlreadySent -> {
                pendingActionsDao.deletePendingSendByMessageId(message.messageId ?: "")
                saveMessageAsSent(message)
                failureWithError(MessageAlreadySent)
            }

            else -> {
                pendingActionsDao.deletePendingSendByMessageId(message.messageId ?: "")
                showSendMessageError(message.subject)
                failureWithError(DraftCreationFailed)
            }
        }
    }

    private suspend fun handleSendMessageResponse(
        messageId: String,
        response: MessageSendResponse,
        savedDraftMessage: Message
    ): Result {
        pendingActionsDao.deletePendingSendByMessageId(messageId)

        return when (response.code) {
            RESPONSE_CODE_OK -> {
                Timber.i("Send Message API call succeeded for messageId $messageId, Message Sent.")
                handleMessageSentSuccess(response.sent, savedDraftMessage)
            }
            RESPONSE_CODE_ERROR_VERIFICATION_NEEDED -> {
                Timber.w("Send Message API call failed, human verification required for messageId $messageId")
                userNotifier.showHumanVerificationNeeded(savedDraftMessage)
                failureWithError(UserVerificationNeeded)
            }
            else -> {
                Timber.e(
                    DetailedException().apiError(response.code, response.error).messageId(messageId),
                    "Send Message API call failed for messageId $messageId with error ${response.error}"
                )
                showSendMessageError(savedDraftMessage.subject)
                failureWithError(ApiRequestReturnedBadBodyCode)
            }
        }
    }

    private suspend fun handleMessageSentSuccess(
        responseMessage: Message,
        savedDraftMessage: Message
    ): Result {
        responseMessage.writeTo(savedDraftMessage)
        saveMessageAsSent(savedDraftMessage)
        userNotifier.showMessageSent()
        return Result.success()
    }

    private suspend fun saveMessageAsSent(message: Message) {
        message.location = SENT.messageLocationTypeValue
        message.setLabelIDs(
            listOf(
                ALL_SENT.messageLocationTypeValue.toString(),
                ALL_MAIL.messageLocationTypeValue.toString(),
                SENT.messageLocationTypeValue.toString()
            )
        )
        messageDetailsRepository.saveMessageLocally(message)
    }

    // TODO improve error handling for generic exception MAILAND-2003
    private fun buildSendMessageRequest(
        savedDraftMessage: Message,
        sendPreferences: List<SendPreference>,
        username: String
    ): MessageSendBody {
            val securityOptions = requireInputMessageSecurityOptions()
            val packages = packagesFactory.generatePackages(
                savedDraftMessage,
                sendPreferences,
                securityOptions,
                username
            )
            val autoSaveContacts = userManager.getMailSettings(username)?.autoSaveContacts ?: NO_CONTACTS_AUTO_SAVE
            return MessageSendBody(packages, securityOptions.expiresAfterInSeconds, autoSaveContacts)
    }

    private fun requestSendPreferences(message: Message, username: String): List<SendPreference>? {
        return runCatching {
            val emailSet = mutableSetOf<String>()
            message.toListString
                .split(Constants.EMAIL_DELIMITER)
                .filter { it.isNotBlank() }
                .map { emailSet.add(it) }

            message.ccListString
                .split(Constants.EMAIL_DELIMITER)
                .filter { it.isNotBlank() }
                .map { emailSet.add(it) }

            message.bccListString
                .split(Constants.EMAIL_DELIMITER)
                .filter { it.isNotBlank() }
                .map { emailSet.add(it) }

            val sendPreferences = sendPreferencesFactory.create(username).fetch(emailSet.toList())
            sendPreferences.values.toList()
        }.getOrNull()
    }

    private suspend fun saveDraft(message: Message, previousSenderAddressId: String): SaveDraftResult {
        return this.saveDraft(
            SaveDraft.SaveDraftParameters(
                message,
                getInputAttachmentIds(),
                getInputParentId(),
                getInputActionType(),
                previousSenderAddressId,
                SaveDraft.SaveDraftTrigger.SendingMessage
            )
        )
    }

    private fun retryOrFail(
        error: SendMessageWorkerError,
        message: Message,
        exception: Throwable? = null
    ): Result {
        if (runAttemptCount < SEND_MESSAGE_MAX_RETRIES) {
            Timber.d(exception, "Send Message Worker failed with error = ${error.name}. Retrying...")
            return Result.retry()
        }
        pendingActionsDao.deletePendingSendByMessageId(message.messageId ?: "")
        showSendMessageError(message.subject)
        return failureWithError(error, exception)
    }

    private fun showSendMessageError(messageSubject: String?) {
        userNotifier.showSendMessageError(
            context.getString(R.string.message_drafted),
            messageSubject
        )
    }

    private fun failureWithError(error: SendMessageWorkerError, exception: Throwable? = null): Result {
        Timber.e(exception, "Send Message Worker failed permanently. error = ${error.name}. FAILING")
        val errorData = workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to error.name)
        return Result.failure(errorData)
    }

    private fun getInputCurrentUsername() =
        inputData.getString(KEY_INPUT_SEND_MESSAGE_CURRENT_USERNAME)

    private fun requireInputMessageSecurityOptions(): MessageSecurityOptions =
        requireNotNull(
            inputData
                .getString(KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED)
                ?.deserialize(MessageSecurityOptions.serializer())
        ) {
            "Security options not found as input parameter - $KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED"
        }

    private fun getInputActionType(): Constants.MessageActionType =
        Constants.MessageActionType.fromInt(inputData.getInt(KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_ENUM_VAL, -1))

    private fun getInputPreviousSenderAddressId() =
        inputData.getString(KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID)

    private fun getInputParentId() = inputData.getString(KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID)

    private fun getInputMessageId() =
        inputData.getString(KEY_INPUT_SEND_MESSAGE_MESSAGE_ID) ?: EMPTY_STRING

    private fun getInputMessageDatabaseId() =
        inputData.getLong(KEY_INPUT_SEND_MESSAGE_MSG_DB_ID, INPUT_MESSAGE_DB_ID_NOT_FOUND)

    private fun getInputAttachmentIds() =
        inputData.getStringArray(KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS)?.asList().orEmpty()

    class Enqueuer @Inject constructor(
        private val workManager: WorkManager,
        private val userManager: UserManager
    ) {

        fun enqueue(
            message: Message,
            attachmentIds: List<String>,
            parentId: String?,
            actionType: Constants.MessageActionType,
            previousSenderAddressId: String,
            securityOptions: MessageSecurityOptions
        ): Flow<WorkInfo?> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val sendMessageRequest = OneTimeWorkRequestBuilder<SendMessageWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_SEND_MESSAGE_MSG_DB_ID to message.dbId,
                        KEY_INPUT_SEND_MESSAGE_MESSAGE_ID to message.messageId,
                        KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS to attachmentIds.toTypedArray(),
                        KEY_INPUT_SEND_MESSAGE_CURRENT_USERNAME to userManager.username,
                        KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID to parentId,
                        KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_ENUM_VAL to actionType.messageActionTypeValue,
                        KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID to previousSenderAddressId,
                        KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED to securityOptions.serialize()
                    )
                )
                .setBackoffCriteria(BackoffPolicy.LINEAR, TEN_SECONDS, TimeUnit.SECONDS)
                .build()

            workManager.enqueueUniqueWork(
                "$SEND_MESSAGE_WORK_NAME_PREFIX-${requireNotNull(message.messageId)}",
                ExistingWorkPolicy.REPLACE,
                sendMessageRequest
            )
            return workManager.getWorkInfoByIdLiveData(sendMessageRequest.id).asFlow()
        }
    }
}
