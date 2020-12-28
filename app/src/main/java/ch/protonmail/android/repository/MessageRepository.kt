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

package ch.protonmail.android.repository

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.core.Constants.FeatureFlags.SAVE_MESSAGE_BODY_TO_FILE
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.MessageBodyFileManager
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject
import javax.inject.Named

const val MAX_BODY_SIZE_IN_DB = 900 * 1024 // 900 KB

/**
 * A repository for getting and saving messages.
 */

class MessageRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    @Named("messages") private var messagesDao: MessagesDao,
    private val protonMailApiManager: ProtonMailApiManager,
    private val messageBodyFileManager: MessageBodyFileManager,
    private val userManager: UserManager
) {

    private suspend fun findMessage(messageId: String): Message? =
        withContext(dispatcherProvider.Io) {
            return@withContext messagesDao.findMessageById(messageId)?.apply {
                messageBody?.let {
                    if (SAVE_MESSAGE_BODY_TO_FILE && it.startsWith("file://"))
                        messageBody = messageBodyFileManager.readMessageBodyFromFile(this)
                }
            }
        }

    private suspend fun saveMessage(message: Message): Long =
        withContext(dispatcherProvider.Io) {
            val messageToBeSaved = message.apply {
                messageBody = messageBody?.let {
                    return@let if (SAVE_MESSAGE_BODY_TO_FILE && it.toByteArray().size > MAX_BODY_SIZE_IN_DB) {
                        messageBodyFileManager.saveMessageBodyToFile(this)
                    } else {
                        null
                    }
                }
            }
            return@withContext messagesDao.saveMessage(messageToBeSaved)
        }

    private suspend fun getMessageDetails(messageId: String, username: String): Message? =
        withContext(dispatcherProvider.Io) {
            val message = findMessage(messageId)

            if (message?.messageBody != null) {
                return@withContext message
            }

            return@withContext runCatching {
                protonMailApiManager.fetchMessageDetails(messageId, RetrofitTag(username))
            }.map { messageResponse ->
                saveMessage(messageResponse.message)
                return@map messageResponse.message
            }.fold(
                onSuccess = { it },
                onFailure = { null }
            )
        }

    private suspend fun getMessageMetadata(messageId: String, username: String): Message? =
        withContext(dispatcherProvider.Io) {
            val message = findMessage(messageId)

            if (message != null) {
                return@withContext message
            }

            return@withContext runCatching {
                protonMailApiManager.fetchMessageMetadata(messageId, RetrofitTag(username))
            }.map { messageResponse ->
                return@map messageResponse.messages.firstOrNull()?.let { message ->
                    saveMessage(message)
                    return@let message
                }
            }.fold(
                onSuccess = { it },
                onFailure = { null }
            )
        }

    /**
     * Returns an instance of Message with the given message id. Returns the message from the database
     * if it exists there. Otherwise, fetches it.
     *
     * @param messageId A message id that should be used to get a message
     * @param username Username for which the message is being retrieved. It is used to check whether the
     *        auto-download-messages setting is turned on or not.
     * @param shouldFetchMessageDetails An optional parameter that should indicate whether message details
     *        should be fetched, even if the user settings say otherwise. Example: When a message is being open.
     * @return An instance of Message
     */
    suspend fun getMessage(messageId: String, username: String, shouldFetchMessageDetails: Boolean = false): Message? =
        withContext(dispatcherProvider.Io) {
            val user = userManager.getUser(username)
            return@withContext if (user.isGcmDownloadMessageDetails || shouldFetchMessageDetails) {
                getMessageDetails(messageId, username)
            } else {
                getMessageMetadata(messageId, username)
            }
        }
}
