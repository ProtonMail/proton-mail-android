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
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.compose.send.SendMessageWorkerError.DraftCreationFailed
import ch.protonmail.android.compose.send.SendMessageWorkerError.ErrorPerformingApiRequest
import ch.protonmail.android.compose.send.SendMessageWorkerError.FetchSendPreferencesFailed
import ch.protonmail.android.compose.send.SendMessageWorkerError.InvalidInputMessageSecurityOptions
import ch.protonmail.android.compose.send.SendMessageWorkerError.MessageNotFound
import ch.protonmail.android.compose.send.SendMessageWorkerError.SavedDraftMessageNotFound
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_SENT
import ch.protonmail.android.core.Constants.MessageLocationType.SENT
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.di.CurrentUsername
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.compose.SaveDraftResult
import ch.protonmail.android.utils.notifier.ErrorNotifier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.serialize
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val KEY_INPUT_SEND_MESSAGE_MSG_DB_ID = "keySendMessageMessageDbId"
internal const val KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS = "keySendMessageAttachmentIds"
internal const val KEY_INPUT_SEND_MESSAGE_MESSAGE_ID = "keySendMessageMessageLocalId"
internal const val KEY_INPUT_SEND_MESSAGE_MESSAGE_DECRYPTED_BODY = "keySendMessageMessageDecryptedBody"
internal const val KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID = "keySendMessageMessageParentId"
internal const val KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_ENUM_VAL = "keySendMessageMessageActionTypeEnumValue"
internal const val KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID = "keySendMessagePreviousSenderAddressId"
internal const val KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED = "keySendMessageSecurityOptionsSerialized"

internal const val KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM = "keySendMessageErrorResult"

private const val INPUT_MESSAGE_DB_ID_NOT_FOUND = -1L
private const val SEND_MESSAGE_MAX_RETRIES = 5
private const val NO_CONTACTS_AUTO_SAVE = 0
private const val SEND_MESSAGE_WORK_NAME_PREFIX = "sendMessageUniqueWorkName"


class SendMessageWorker @WorkerInject constructor(
    @Assisted val context: Context,
    @Assisted params: WorkerParameters,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val saveDraft: SaveDraft,
    private val sendPreferencesFactory: SendPreferencesFactory,
    private val apiManager: ProtonMailApiManager,
    private val packagesFactory: PackageFactory,
    @CurrentUsername private val currentUsername: String,
    private val userManager: UserManager,
    private val errorNotifier: ErrorNotifier,
    private val pendingActionsDao: PendingActionsDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Timber.d("Send Message Worker executing with messageDbId ${getInputMessageDbId()}")
        val message = messageDetailsRepository.findMessageByMessageDbId(getInputMessageDbId())
            ?: return failureWithError(MessageNotFound)
        message.decryptedBody = getInputDecryptedBody()

        Timber.d("Send Message Worker read local message with messageId ${message.messageId}")

        val previousSenderAddressId = requireNotNull(getInputPreviousSenderAddressId())

        return when (val result = saveDraft(message, previousSenderAddressId)) {
            is SaveDraftResult.Success -> {
                val messageId = result.draftId
                val savedDraftMessage = messageDetailsRepository.findMessageById(messageId) ?: return retryOrFail(
                    SavedDraftMessageNotFound,
                    message
                )

                val sendPreferences = requestSendPreferences(savedDraftMessage) ?: return retryOrFail(
                    FetchSendPreferencesFailed,
                    savedDraftMessage
                )

                val requestBody = buildSendMessageRequest(savedDraftMessage, sendPreferences)
                    ?: return failureWithError(InvalidInputMessageSecurityOptions)

                runCatching {
                    apiManager.sendMessage(messageId, requestBody, RetrofitTag(currentUsername))
                }.fold(
                    onSuccess = { messageSendResponse ->
                        messageSendResponse.sent.writeTo(savedDraftMessage)
                        savedDraftMessage.location = SENT.messageLocationTypeValue
                        savedDraftMessage.setLabelIDs(
                            listOf(
                                ALL_SENT.messageLocationTypeValue.toString(),
                                ALL_MAIL.messageLocationTypeValue.toString(),
                                SENT.messageLocationTypeValue.toString()
                            )
                        )
                        messageDetailsRepository.saveMessageLocally(savedDraftMessage)
                        pendingActionsDao.deletePendingSendByMessageId(messageId)
                        errorNotifier.showMessageSent()
                        Result.success()
                    },
                    onFailure = { exception ->
                        retryOrFail(ErrorPerformingApiRequest, savedDraftMessage, exception)
                    }
                )
            }
            else -> failureWithError(DraftCreationFailed)
        }

    }

    private fun buildSendMessageRequest(savedDraftMessage: Message, sendPreferences: List<SendPreference>): MessageSendBody? {
        val securityOptions = getInputMessageSecurityOptions() ?: return null
        val packages = packagesFactory.generatePackages(savedDraftMessage, sendPreferences, securityOptions)
        val autoSaveContacts = userManager.getMailSettings(currentUsername)?.autoSaveContacts ?: NO_CONTACTS_AUTO_SAVE
        return MessageSendBody(packages, securityOptions.expiresAfterInSeconds, autoSaveContacts)
    }

    private fun requestSendPreferences(message: Message): List<SendPreference>? {
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

            val sendPreferences = sendPreferencesFactory.fetch(emailSet.toList())
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
                previousSenderAddressId
            )
        ).first()
    }

    private fun retryOrFail(
        error: SendMessageWorkerError,
        message: Message,
        exception: Throwable? = null
    ): Result {
        if (runAttemptCount <= SEND_MESSAGE_MAX_RETRIES) {
            Timber.d("Send Message Worker FAILED with error = ${error.name}, exception = $exception. Retrying...")
            return Result.retry()
        }
        pendingActionsDao.deletePendingSendByMessageId(message.messageId ?: "")
        errorNotifier.showSendMessageError(context.getString(R.string.message_drafted), message.subject)
        return failureWithError(error, exception)
    }

    private fun failureWithError(error: SendMessageWorkerError, exception: Throwable? = null): Result {
        Timber.e("Send Message Worker failed all the retries. error = $error, exception = $exception. FAILING")
        val errorData = workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to error.name)
        return Result.failure(errorData)
    }

    private fun getInputDecryptedBody() =
        inputData.getString(KEY_INPUT_SEND_MESSAGE_MESSAGE_DECRYPTED_BODY)

    private fun getInputMessageSecurityOptions(): MessageSecurityOptions? =
        inputData
            .getString(KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED)
            ?.deserialize(MessageSecurityOptions.serializer())

    private fun getInputActionType(): Constants.MessageActionType =
        Constants.MessageActionType.fromInt(inputData.getInt(KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_ENUM_VAL, -1))

    private fun getInputPreviousSenderAddressId() =
        inputData.getString(KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID)

    private fun getInputParentId() = inputData.getString(KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID)

    private fun getInputMessageDbId() =
        inputData.getLong(KEY_INPUT_SEND_MESSAGE_MSG_DB_ID, INPUT_MESSAGE_DB_ID_NOT_FOUND)

    private fun getInputAttachmentIds() =
        inputData.getStringArray(KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS)?.asList().orEmpty()

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            message: Message,
            decryptedMessageBody: String,
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
                        KEY_INPUT_SEND_MESSAGE_MESSAGE_DECRYPTED_BODY to decryptedMessageBody,
                        KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS to attachmentIds.toTypedArray(),
                        KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID to parentId,
                        KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_ENUM_VAL to actionType.messageActionTypeValue,
                        KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID to previousSenderAddressId,
                        KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED to securityOptions.serialize()
                    )
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2 * TEN_SECONDS, TimeUnit.SECONDS)
                .build()

            workManager.enqueueUniqueWork(
                "${SEND_MESSAGE_WORK_NAME_PREFIX}-${requireNotNull(message.messageId)}",
                ExistingWorkPolicy.REPLACE,
                sendMessageRequest
            )
            return workManager.getWorkInfoByIdLiveData(sendMessageRequest.id).asFlow()
        }
    }

}
