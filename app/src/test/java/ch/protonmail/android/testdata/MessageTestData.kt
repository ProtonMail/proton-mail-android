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

package ch.protonmail.android.testdata

import ch.protonmail.android.data.local.model.Message
import io.mockk.every
import io.mockk.just
import io.mockk.runs
import io.mockk.spyk

object MessageTestData {
    const val MESSAGE_ID_RAW = "message_id"
    const val MESSAGE_SUBJECT = "A fancy subject"
    const val MESSAGE_BODY = "<span>I just call, to say, hello world.</span>"
    const val MESSAGE_BODY_FORMATTED = "<span>I just call, to say, hello world. But now I am formatted.</span>"
    const val MESSAGE_DATABASE_ID = 42L

    fun messageSpy(messageId: String? = MESSAGE_ID_RAW) = Message(messageId).toSpy()

    fun Message.toSpy(): Message {
        return spyk(this).apply {
            every { decrypt(any(), any(), any()) } just runs
        }
    }
}
