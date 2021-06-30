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
import ch.protonmail.android.data.local.MessageDao
import javax.inject.Inject

class MessageLocationResolver @Inject constructor(
    // Unfortunately with the current "architecture" we cannot properly inject anything here and verify label type
    // correctly in resolveLabelType(), that should be updated with the networking module implementation
    // currently this is used in [MessageResponse] & [MessagesResponse] which should be simple data classes
    private val messageDao: MessageDao?
) {

    fun resolveLocationFromLabels(labelIds: List<String>): Constants.MessageLocationType {
        if (labelIds.isEmpty()) {
            return Constants.MessageLocationType.INBOX
        }

        if (
            labelIds.size == 1 &&
            labelIds[0] == Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue.toString()
        ) {
            return Constants.MessageLocationType.ALL_MAIL
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
                    return Constants.MessageLocationType.fromInt(locationInt)
                }
            } else {
                return resolveLabelType(item)
            }
        }

        throw IllegalArgumentException("No valid location found in IDs: $labelIds ")
    }

    private fun resolveLabelType(labelId: String): Constants.MessageLocationType {
        val label = messageDao?.findLabelById(labelId)
        return if (label != null && label.exclusive) {
            Constants.MessageLocationType.LABEL_FOLDER
        } else {
            Constants.MessageLocationType.LABEL
        }
    }

}
