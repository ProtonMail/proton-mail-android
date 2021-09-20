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

/**
 * This enum represent some of the possible values that are contained in the
 * `Message.Flags` bitmap of message flags.
 * Here is a list of all the possible values such flags can assume.
 * Mapping between the Flag's bitmap and this enum is performed in `MessageFlagsToEncryptionMapper`.
 * <p>
 * Received = 1 (2^0)
 * Sent = 2 (2^1)
 * Internal = 4 (2^2)
 * E2E = 8 (2^3)
 * Auto = 16 (2^4)
 * Replied = 32 (2^5)
 * RepliedAll = 64 (2^6)
 * Forwarded = 128 (2^7)
 * Auto replied = 256 (2^8)
 * Imported = 512 (2^9)
 * Opened = 1024 (2^10)
 * Receipt Sent = 2048 (2^11)
 * Notified = 4096 (2^12)
 * Touched = 8192 (2^13)
 * Receipt = 16384 (2^14)
 * Proton = 32768 (2^15)
 * Receipt request = 65536 (2^16)
 * Public key = 131072 (2^17)
 * Sign = 262144 (2^18)
 * Unsubscribed = 524288 (2^19)
 * SPF fail = 16777216 (2^24)
 * DKIM fail = 33554432 (2^25)
 * DMARC fail = 67108864 (2^26)
 * Ham manual = 134217728 (2^27)
 * Spam auto = 268435456 (2^28)
 * Spam manual = 536870912 (2^29)
 * Phishing auto = 1073741824 (2^30)
 * Phishing manual = 2147483648 (2^31)
 */
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
