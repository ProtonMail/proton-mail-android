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
package ch.protonmail.android.api.models.enumerations;

public enum MessageFlag {
    RECEIVED(1L), // whether a message is received
    SENT(2L), // whether a message is sent
    INTERNAL(4L), // whether the message is between ProtonMail recipients
    E2E(8L), // whether the message is end-to-end encrypted
    AUTO(16L), // whether the message is an autoresponse
    REPLIED(32L), // whether the message is replied to
    REPLIED_ALL(64L), // whether the message is replied all to
    FORWARDED(128L), // whether the message is forwarded
    AUTO_REPLIED(256L), // whether the message has been responded to with an autoresponse
    IMPORTED(512L), // whether the message is an import
    OPENED(1024L), // whether the message has ever been opened by the user
    RECEIPT_SENT(2048L), // whether a read receipt has been sent in response to the message
    RECEIPT_REQUEST(65536L), // whether to request a read receipt for the message
    PUBLIC_KEY(131072L), // whether to attach the public key
    SIGN(262144L); // whether to sign the message

    private final Long value;

    MessageFlag(Long value) {
        this.value = value;
    }

    public Long getValue() {
        return value;
    }
}
