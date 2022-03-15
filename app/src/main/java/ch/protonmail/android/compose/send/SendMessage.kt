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
import ch.protonmail.android.api.models.factories.MessageSecurityOptions
import ch.protonmail.android.core.Constants
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.di.CurrentUserId
import ch.protonmail.android.pendingaction.data.model.PendingSend
import ch.protonmail.android.utils.ServerTime
import ch.protonmail.android.utils.UuidProvider
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

internal const val MISSING_DB_ID = 0L

class SendMessage @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository,
    private val dispatchers: DispatcherProvider,
    private val pendingActionDao: PendingActionDao,
    private val sendMessageScheduler: SendMessageWorker.Enqueuer,
    private val addressCryptoFactory: AddressCrypto.Factory,
    @CurrentUserId private val currentUserId: UserId,
    private val uuidProvider: UuidProvider
) {

    suspend operator fun invoke(parameters: SendMessageParameters) = withContext(dispatchers.Io) {
        val message = parameters.message
        Timber.d("Send Message use case called with messageId ${message.messageId}")
        val addressId = requireNotNull(message.addressID)

        val addressCrypto = addressCryptoFactory.create(currentUserId, AddressId(addressId))
        val encryptedBody = addressCrypto.encrypt(message.decryptedBody ?: "", true).armored
        message.messageBody = encryptedBody

        saveMessageLocally(message)
        setMessageAsPendingForSend(message)

        sendMessageScheduler.enqueue(
            parameters.message,
            parameters.newAttachmentIds,
            parameters.parentId,
            parameters.actionType,
            parameters.previousSenderAddressId,
            parameters.securityOptions
        )
    }

    private fun setMessageAsPendingForSend(message: Message) {
        val pendingSend = PendingSend(
            id = uuidProvider.randomUuid(),
            localDatabaseId = message.dbId ?: MISSING_DB_ID,
            messageId = message.messageId
        )
        pendingActionDao.insertPendingForSend(pendingSend)
    }

    private suspend fun saveMessageLocally(message: Message): Long {
        val currentTimeSeconds = ServerTime.currentTimeMillis() / 1000

        message.location = Constants.MessageLocationType.ALL_DRAFT.messageLocationTypeValue
        message.time = currentTimeSeconds
        message.isDownloaded = false

        return messageDetailsRepository.saveMessage(message)
    }

    data class SendMessageParameters(
        val message: Message,
        val newAttachmentIds: List<String>,
        val parentId: String?,
        val actionType: Constants.MessageActionType,
        val previousSenderAddressId: String,
        val securityOptions: MessageSecurityOptions
    )
}
