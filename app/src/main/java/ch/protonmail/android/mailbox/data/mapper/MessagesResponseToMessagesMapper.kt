/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.mailbox.data.mapper

import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.data.ProtonStoreMapper
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import javax.inject.Inject

/**
 * [ProtonStoreMapper] that maps from [MessagesResponse] to [List] of [Message]
 */
class MessagesResponseToMessagesMapper @Inject constructor() :
    ProtonStoreMapper<GetAllMessagesParameters, MessagesResponse, List<Message>> {

    override fun MessagesResponse.toOut(key: GetAllMessagesParameters): List<Message> =
        messages
}
