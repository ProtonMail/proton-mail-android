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

package ch.protonmail.android.usecase.compose

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.di.CurrentUsername
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.worker.CreateDraftWorker
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class SaveDraft @Inject constructor(
    private val addressCryptoFactory: AddressCrypto.Factory,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val dispatchers: DispatcherProvider,
    private val pendingActionsDao: PendingActionsDao,
    private val createDraftWorker: CreateDraftWorker.Enqueuer,
    @CurrentUsername private val username: String
) {

    suspend operator fun invoke(
        params: SaveDraftParameters
    ): Result = withContext(dispatchers.Io) {
        val message = params.message
        val messageId = requireNotNull(message.messageId)
        val addressId = requireNotNull(message.addressID)

        val addressCrypto = addressCryptoFactory.create(Id(addressId), Name(username))
        val encryptedBody = addressCrypto.encrypt(message.decryptedBody ?: "", true).armored

        message.messageBody = encryptedBody
        message.setLabelIDs(
            listOf(
                ALL_DRAFT.messageLocationTypeValue.toString(),
                ALL_MAIL.messageLocationTypeValue.toString(),
                DRAFT.messageLocationTypeValue.toString()
            )
        )

        val messageDbId = messageDetailsRepository.saveMessageLocally(message)
        messageDetailsRepository.insertPendingDraft(messageDbId)

        if (params.newAttachmentIds.isNotEmpty()) {
            pendingActionsDao.insertPendingForUpload(PendingUpload(messageId))
        }

        pendingActionsDao.findPendingSendByDbId(messageDbId)?.let {
            // TODO allow draft to be saved in this case when starting to use SaveDraft use case in PostMessageJob too
            return@withContext Result.SendingInProgressError
        }

        createDraftWorker.enqueue(message, params.parentId)

        return@withContext Result.Success
    }

    sealed class Result {
        object SendingInProgressError : Result()
        object Success : Result()
    }

    data class SaveDraftParameters(
        val message: Message,
        val newAttachmentIds: List<String>,
        val parentId: String?
    )
}
