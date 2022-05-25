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
package ch.protonmail.android.api.models.enumerations

/**
 * @see ~/Slim-API/mail/#operation/get_mail-v4-messages-{enc_id}
 */
enum class MessageFlag(val flagValue: Long) {

    RECEIVED(flagValue = 1),
    SENT(flagValue = 2),
    /**
     * Message is between ProtonMail recipients
     */
    INTERNAL(flagValue = 4),
    E2E(flagValue = 8),
    /**
     * Message is an auto-response
     */
    AUTO(flagValue = 16),
    REPLIED(flagValue = 32),
    REPLIED_ALL(flagValue = 64),
    FORWARDED(flagValue = 128),
    /**
     * Message has been responded to with an auto-response
     */
    AUTO_REPLIED(flagValue = 256),
    IMPORTED(flagValue = 512),
    OPENED(flagValue = 1_024),
    /**
     * Read receipt has been sent in response to the message
     */
    RECEIPT_SENT(flagValue = 2_048),
    /**
     * Request a read receipt for the message
     */
    RECEIPT_REQUEST(flagValue = 65_536),
    /**
     * Attach the public key
     */
    PUBLIC_KEY(flagValue = 131_072),
    SIGN(flagValue = 262_144),
    PHISHING_AUTO(flagValue = 1_073_741_824),
    PHISHING_MANUAL(flagValue = 2_147_483_648);
}

operator fun Long.contains(flag: MessageFlag) =
    this and flag.flagValue == flag.flagValue
