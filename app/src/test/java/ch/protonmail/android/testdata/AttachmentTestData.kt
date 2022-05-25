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

import ch.protonmail.android.data.local.model.Attachment

object AttachmentTestData {
    const val ID = "attachmentId"
    const val KEY_PACKETS = "keyPackets"
    val WITH_MIME_DATA = Attachment(attachmentId = ID, mimeData = ByteArray(10), keyPackets = KEY_PACKETS)
    val WITHOUT_MIME_DATA = Attachment(attachmentId = ID, mimeData = null, keyPackets = KEY_PACKETS)
}
