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

package ch.protonmail.android.details.data

import ch.protonmail.android.api.models.enumerations.MessageEncryption
import ch.protonmail.android.api.models.enumerations.MessageFlag
import me.proton.core.domain.arch.Mapper
import javax.inject.Inject

/**
 * Maps a Long number representing a bitmap of `MessageFlags` to the corresponding `MessageEncryption` value.
 * The input `messageFlags` long should be the value that API returns for the `Message.Flags` field.
 *
 * To better understand how the flags work, here's an example:
 * A message that is "Sent" and "E2EE" is represented with a "flags" value of "10",
 * which is the sum of the individual values for `MessageFlags.SENT` (2L) and `MessageFlags.E2E` (8L).
 *
 * The conversion is done performing a bitwise `AND` operation between the aggregated flags value and each of
 * the individual flags we want to check.
 * Using the above message as example the binary value of the aggregated flags is `1010`.
 * Given that the value for SENT flag is `0010` (decimal 2) and the value for E2EE flag is `1000` (decimal 8),
 * performing an `AND` operations between the aggregated flags and SENT / E2EE will in both cases return
 * a value equal to the right side operand (SENT / E2EE), which will let us know that flag is "true".
 *
 * Please refer to `MessageFlag` class for a complete list of all of the flags' values
 */
class MessageFlagsToEncryptionMapper @Inject constructor() : Mapper<Long, MessageEncryption> {

    fun flagsToMessageEncryption(messageFlags: Long): MessageEncryption {
        val internal = messageFlags and MessageFlag.INTERNAL.value == MessageFlag.INTERNAL.value
        val e2e = messageFlags and MessageFlag.E2E.value == MessageFlag.E2E.value
        val received = messageFlags and MessageFlag.RECEIVED.value == MessageFlag.RECEIVED.value
        val sent = messageFlags and MessageFlag.SENT.value == MessageFlag.SENT.value
        val auto = messageFlags and MessageFlag.AUTO.value == MessageFlag.AUTO.value

        return when {
            internal -> handleInternalMessage(e2e, received, sent, auto)
            received -> handleReceivedMessage(e2e)
            e2e -> MessageEncryption.MIME_PGP
            else -> MessageEncryption.EXTERNAL
        }

    }

    private fun handleReceivedMessage(
        e2e: Boolean
    ) = if (e2e) {
        MessageEncryption.EXTERNAL_PGP
    } else {
        MessageEncryption.EXTERNAL
    }

    private fun handleInternalMessage(
        e2e: Boolean,
        received: Boolean,
        sent: Boolean,
        auto: Boolean
    ): MessageEncryption {
        if (e2e) {

            if (received && sent) {
                return MessageEncryption.INTERNAL
            }
            if (received && auto) {
                return MessageEncryption.AUTO_RESPONSE
            }
            return MessageEncryption.INTERNAL
        }

        if (auto) {
            return MessageEncryption.AUTO_RESPONSE
        }

        if (sent) {
            return MessageEncryption.SENT_TO_EXTERNAL
        }

        return MessageEncryption.INTERNAL
    }

}
