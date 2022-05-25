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

package ch.protonmail.android.utils.base64

import android.util.Base64

class AndroidBase64Encoder : Base64Encoder {

    override fun decode(base64String: String): ByteArray =
        Base64.decode(base64String, Base64.DEFAULT)

    override fun encode(base64Bytes: ByteArray): String =
        Base64.encodeToString(base64Bytes, Base64.NO_WRAP)

}
