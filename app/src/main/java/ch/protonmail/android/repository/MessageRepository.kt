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
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.utils.MessageBodyFileManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

const val MAX_BODY_SIZE_IN_DB = 900 * 1024 // 900 KB

/**
 * A repository for getting and saving messages.
 */

class MessageRepository @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private var messageDao: MessageDao,
    private val protonMailApiManager: ProtonMailApiManager,
    private val messageBodyFileManager: MessageBodyFileManager,
    private val userManager: UserManager
) {

    private suspend fun findMessage(messageId: String): Message? =
        withContext(dispatcherProvider.Io) {
            return@withContext messageDao.findMessageById(messageId).first()?.apply {
                messageBody?.let {
                    if (it.startsWith("file://"))
                        messageBody = messageBodyFileManager.readMessageBodyFromFile(this)
                }
            }
        }

    private suspend fun saveMessage(message: Message): Long =
        withContext(dispatcherProvider.Io) {
            val messageToBeSaved = message.apply {
                messageBody = messageBody?.let {
                    return@let if (it.toByteArray().size > MAX_BODY_SIZE_IN_DB) {
                        messageBodyFileManager.saveMessageBodyToFile(this)
                    } else {
                        null
                    }
                }
            }
            return@withContext messageDao.saveMessage(messageToBeSaved)
        }

    private suspend fun getMessageDetails(messageId: String, userId: Id): Message? =
        withContext(dispatcherProvider.Io) {
            val message = findMessage(messageId)

            if (message?.messageBody != null) {
                return@withContext message
            }

            return@withContext runCatching {
                protonMailApiManager.fetchMessageDetails(messageId, UserIdTag(userId))
            }.map { messageResponse ->
                saveMessage(messageResponse.message)
                return@map messageResponse.message
            }.getOrNull()
        }

    private suspend fun getMessageMetadata(messageId: String, userId: Id): Message? =
        withContext(dispatcherProvider.Io) {
            val message = findMessage(messageId)

            if (message != null) {
                return@withContext message
            }

            return@withContext runCatching {
                protonMailApiManager.fetchMessageMetadata(messageId, UserIdTag(userId))
            }.map { messageResponse ->
                return@map messageResponse.messages.firstOrNull()?.let { message ->
                    saveMessage(message)
                    return@let message
                }
            }.getOrNull()
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
    suspend fun getMessage(messageId: String, userId: Id, shouldFetchMessageDetails: Boolean = false): Message? =
        withContext(dispatcherProvider.Io) {
            val user = userManager.getLegacyUser(userId)
            return@withContext if (user.isGcmDownloadMessageDetails || shouldFetchMessageDetails) {
                getMessageDetails(messageId, userId)
            } else {
                getMessageMetadata(messageId, userId)
            }
        }
}
