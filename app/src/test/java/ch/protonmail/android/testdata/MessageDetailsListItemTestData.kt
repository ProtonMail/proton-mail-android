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

package ch.protonmail.android.testdata

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.model.MessageDetailsListItem

object MessageDetailsListItemTestData {

    fun withoutLoadedBodyFrom(message: Message) = MessageDetailsListItem.Body(
        message = message,
        messageFormattedHtml = null,
        messageFormattedHtmlWithQuotedHistory = null,
        showOpenInProtonCalendar = false,
        showLoadEmbeddedImagesButton = false,
        showDecryptionError = false,
        showScheduledInfo = false
    )

    fun withLoadedBodyFrom(message: Message) = MessageDetailsListItem.Body(
        message = message,
        messageFormattedHtml = MessageTestData.MESSAGE_BODY_FORMATTED,
        messageFormattedHtmlWithQuotedHistory = MessageTestData.MESSAGE_BODY_FORMATTED,
        showOpenInProtonCalendar = false,
        showLoadEmbeddedImagesButton = false,
        showDecryptionError = false,
        showScheduledInfo = false
    )
}
