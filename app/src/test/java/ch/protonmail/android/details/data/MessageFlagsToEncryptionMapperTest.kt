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
import kotlin.test.Test
import kotlin.test.assertEquals

private const val RECEIVED_MESSAGE_FLAG_VALUE = 1L
private const val SENT_MESSAGE_FLAG_VALUE = 2L
private const val INTERNAL_MESSAGE_FLAG_VALUE = 4L
private const val E2E_MESSAGE_FLAG_VALUE = 8L
private const val AUTO_MESSAGE_FLAG_VALUE = 16L
private const val INVALID_FLAG_VALUE = 0L

class MessageFlagsToEncryptionMapperTest {

    private val mapper = MessageFlagsToEncryptionMapper()

    @Test
    fun messageFlagsIsMappedToSentToExternalMessageEncryptionTypeWhenFlagsIsInternalAndSentButNotE2ee() {
        val internalSentFlag = INTERNAL_MESSAGE_FLAG_VALUE + SENT_MESSAGE_FLAG_VALUE

        val actual = mapper.flagsToMessageEncryption(internalSentFlag)

        assertEquals(MessageEncryption.SENT_TO_EXTERNAL, actual)
    }

    @Test
    fun messageFlagsIsMappedToInternalMessageEncryptionTypeWhenFlagsIsInternalAndE2ee() {
        val internalE2eeFlag = INTERNAL_MESSAGE_FLAG_VALUE + E2E_MESSAGE_FLAG_VALUE

        val actual = mapper.flagsToMessageEncryption(internalE2eeFlag)

        assertEquals(MessageEncryption.INTERNAL, actual)
    }

    @Test
    fun messageFlagsIsMappedToInternalMessageEncryptionTypeWhenFlagsIsInternalAndE2EeAndReceivedAndSent() {
        val internalE2eeFlag = INTERNAL_MESSAGE_FLAG_VALUE +
            E2E_MESSAGE_FLAG_VALUE +
            RECEIVED_MESSAGE_FLAG_VALUE +
            SENT_MESSAGE_FLAG_VALUE

        val actual = mapper.flagsToMessageEncryption(internalE2eeFlag)

        assertEquals(MessageEncryption.INTERNAL, actual)
    }

    @Test
    fun messageFlagsIsMappedToAutoResponseMessageEncryptionTypeWhenFlagsIsInternalAndE2eeAndReceivedAndAuto() {
        val internalE2eeReceivedAutoFlag = INTERNAL_MESSAGE_FLAG_VALUE +
            E2E_MESSAGE_FLAG_VALUE +
            RECEIVED_MESSAGE_FLAG_VALUE +
            AUTO_MESSAGE_FLAG_VALUE

        val actual = mapper.flagsToMessageEncryption(internalE2eeReceivedAutoFlag)

        assertEquals(MessageEncryption.AUTO_RESPONSE, actual)
    }

    @Test
    fun messageFlagsIsMappedToAutoResponseMessageEncryptionTypeWhenFlagsIsInternalAndAuto() {
        val internalAutoFlag = INTERNAL_MESSAGE_FLAG_VALUE +
            AUTO_MESSAGE_FLAG_VALUE

        val actual = mapper.flagsToMessageEncryption(internalAutoFlag)

        assertEquals(MessageEncryption.AUTO_RESPONSE, actual)
    }

    @Test
    fun messageFlagsIsMappedToExternalPgpMessageEncryptionTypeWhenFlagsIsReceivedAndE2eeButNotInternal() {
        val receivedAndE2eeFlags = RECEIVED_MESSAGE_FLAG_VALUE + E2E_MESSAGE_FLAG_VALUE

        val actual = mapper.flagsToMessageEncryption(receivedAndE2eeFlags)

        assertEquals(MessageEncryption.EXTERNAL_PGP, actual)
    }

    @Test
    fun messageFlagsIsMappedToExternalMessageEncryptionTypeWhenFlagsIsReceivedButNotInternalOrE2ee() {
        val receivedMessageFlag = RECEIVED_MESSAGE_FLAG_VALUE

        val actual = mapper.flagsToMessageEncryption(receivedMessageFlag)

        assertEquals(MessageEncryption.EXTERNAL, actual)
    }

    @Test
    fun messageFlagsIsMappedToMimePgpMessageEncryptionTypeWhenFlagsIsE2eeButNotInternalOrReceived() {
        val receivedMessageFlag = E2E_MESSAGE_FLAG_VALUE

        val actual = mapper.flagsToMessageEncryption(receivedMessageFlag)

        assertEquals(MessageEncryption.MIME_PGP, actual)
    }

    @Test
    fun messageFlagsIsMappedToExternalMessageEncryptionTypeAsAFallback() {
        val receivedMessageFlag = INVALID_FLAG_VALUE

        val actual = mapper.flagsToMessageEncryption(receivedMessageFlag)

        assertEquals(MessageEncryption.EXTERNAL, actual)
    }

}
