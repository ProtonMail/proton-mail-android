/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.usecase.message

import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.repository.MessageRepository
import kotlinx.coroutines.flow.firstOrNull
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject

class GetDecryptedMessageById @Inject constructor(
    val accountManager: AccountManager,
    val userManager: UserManager,
    val messageRepository: MessageRepository
) {

    suspend fun orNull(messageId: String): Message? {
        return accountManager.getPrimaryUserId()
            .firstOrNull()
            ?.let { userId ->
                messageRepository.getMessage(userId, messageId)?.apply {
                    decrypt(userManager, userId)
                }
            }
    }
}
