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
package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.core.Constants
import javax.inject.Inject

class MessageLocationResolver @Inject constructor() {

    fun resolveLocationFromLabels(labelIds: List<String>): Int {
        if (labelIds.isEmpty()) {
            return 0 // Inbox
        }

        val validLocations: List<Int> = listOf(
            Constants.MessageLocationType.INBOX.messageLocationTypeValue,
            Constants.MessageLocationType.TRASH.messageLocationTypeValue,
            Constants.MessageLocationType.SPAM.messageLocationTypeValue,
            Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue,
            Constants.MessageLocationType.SENT.messageLocationTypeValue,
            Constants.MessageLocationType.DRAFT.messageLocationTypeValue,
        )

        for (i in labelIds.indices) {
            val item = labelIds[i]
            if (item.length <= 2) {
                val locationInt = item.toInt()
                if (locationInt in validLocations) {
                    return locationInt
                }
            } else {
                return Constants.MessageLocationType.LABEL_FOLDER.messageLocationTypeValue
            }
        }

        throw IllegalArgumentException("No valid location found in IDs: $labelIds ")
    }

}
