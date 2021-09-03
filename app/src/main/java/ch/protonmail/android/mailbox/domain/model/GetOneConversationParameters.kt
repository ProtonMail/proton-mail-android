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

package ch.protonmail.android.mailbox.domain.model

import me.proton.core.domain.entity.UserId

/**
 * Representation of Rest Query Parameters for 'mail/v4/conversations/{enc_id}' endpoint
 * Documentation at '*\/Slim-API/mail/#operation/get_mail-v4-conversations-{enc_id}'
 */
data class GetOneConversationParameters(
    val userId: UserId,
    val conversationId: String
)
