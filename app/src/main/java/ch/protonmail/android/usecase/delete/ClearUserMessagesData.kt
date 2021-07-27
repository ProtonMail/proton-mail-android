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

package ch.protonmail.android.usecase.delete

import android.content.Context
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.storage.AttachmentClearingService
import ch.protonmail.android.storage.MessageBodyClearingService
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * Clear messages and conversations tables for a given user.
 *
 */
class ClearUserMessagesData @Inject constructor(
    private val context: Context,
    private val databaseProvider: DatabaseProvider,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(userId: UserId) {

        val legacyUserId = UserId(userId.id)
        val attachmentMetadataDao = runCatching { databaseProvider.provideAttachmentMetadataDao(legacyUserId) }.getOrNull()
        val messageDao = runCatching { databaseProvider.provideMessageDao(legacyUserId) }.getOrNull()
        //  TODO remove this dependency and use the ConversationRepository.clear()
        //  right now it creates a circular dependency,
        //  so this needs to happen when this use-case is refactored to use only repositories
        val conversationDao = runCatching { databaseProvider.provideConversationDao(legacyUserId) }.getOrNull()

        // Ensure that all the queries run on Io thread, as some are still blocking calls
        withContext(dispatchers.Io) {

            attachmentMetadataDao?.clearAttachmentMetadataCache()
            messageDao?.run {
                clearMessagesCache()
                clearAttachmentsCache()
            }
            conversationDao?.run {
                clear()
            }
        }

        startCleaningServices(legacyUserId)
    }

    private fun startCleaningServices(userId: UserId) {
        AttachmentClearingService.startClearUpImmediatelyService(context, userId)
        MessageBodyClearingService.startClearUpService()
    }
}
