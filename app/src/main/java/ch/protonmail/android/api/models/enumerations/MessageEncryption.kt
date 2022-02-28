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

enum class MessageEncryption(
    /**
     * Drafts are also seen as end-to-end encrypted as drafts are never leaked to the server.
     */
    val isEndToEndEncrypted: Boolean,
    val isStoredEncrypted: Boolean,
    val isInternalEncrypted: Boolean,
    val isPGPEncrypted: Boolean
) {

    INTERNAL(
        isEndToEndEncrypted = true,
        isStoredEncrypted = true,
        isInternalEncrypted = true,
        isPGPEncrypted = false
    ),
    EXTERNAL(
        isEndToEndEncrypted = false,
        isStoredEncrypted = true,
        isInternalEncrypted = false,
        isPGPEncrypted = false
    ),
    EXTERNAL_PGP(
        isEndToEndEncrypted = false,
        isStoredEncrypted = true,
        isInternalEncrypted = false,
        isPGPEncrypted = true
    ),
    MIME_PGP(
        isEndToEndEncrypted = true,
        isStoredEncrypted = true,
        isInternalEncrypted = false,
        isPGPEncrypted = true
    ),
    AUTO_RESPONSE(
        isEndToEndEncrypted = false,
        isStoredEncrypted = true,
        isInternalEncrypted = true,
        isPGPEncrypted = false
    ),
    SENT_TO_EXTERNAL(
        isEndToEndEncrypted = false,
        isStoredEncrypted = true,
        isInternalEncrypted = true,
        isPGPEncrypted = false
    ),
    UNKNOWN(
        isEndToEndEncrypted = false,
        isStoredEncrypted = false,
        isInternalEncrypted = false,
        isPGPEncrypted = false
    )

}
