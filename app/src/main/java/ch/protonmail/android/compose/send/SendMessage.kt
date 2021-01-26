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

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.ServerTime
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class SendMessage @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository,
    private val dispatchers: DispatcherProvider,
    private val pendingActionsDao: PendingActionsDao
) {

    suspend operator fun invoke(parameters: SendMessageParameters) = withContext(dispatchers.Io) {
        val message = parameters.message
        saveMessageLocally(message)

        val pendingSend = PendingSend(localDatabaseId = message.dbId ?: 0, messageId = message.messageId)
        pendingActionsDao.insertPendingForSend(pendingSend)
    }

    private suspend fun saveMessageLocally(message: Message): Long {
        val currentTimeSeconds = ServerTime.currentTimeMillis() / 1000

        message.location = Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue
        message.time = currentTimeSeconds
        message.isDownloaded = false

        return messageDetailsRepository.saveMessageLocally(message)
    }

    data class SendMessageParameters(
        val message: Message
    )
}
