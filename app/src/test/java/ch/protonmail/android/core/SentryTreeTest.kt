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
package ch.protonmail.android.core

import assert4k.assert
import assert4k.equals
import assert4k.that
import kotlin.test.Test

internal class SentryTreeTest {

    @Test
    fun `obfuscateEmails obfuscate one email`() {
        val input = "Hello world! My email address is some.email@protonmail.ch. Have a nice day!!"
        val output = SentryTree().obfuscateEmails(input)
        assert that output equals
            "Hello world! My email address is *******ail@protonmail.ch. Have a nice day!!"
    }

    @Test
    fun `obfuscateEmails obfuscate many emails`() {
        val input = """
        Hello world! My email address is some.email@protonmail.ch. Have a nice day!!
        Oh! I forgot that I also have another email address which is another.email@protonmail.ch!!
        Have a nice day!
        """.trimIndent()

        val output = SentryTree().obfuscateEmails(input)
        assert that output equals """
            Hello world! My email address is *******ail@protonmail.ch. Have a nice day!!
            Oh! I forgot that I also have another email address which is **********ail@protonmail.ch!!
            Have a nice day!
            """.trimIndent()
    }
}
