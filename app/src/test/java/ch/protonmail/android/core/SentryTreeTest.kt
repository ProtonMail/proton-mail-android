package ch.protonmail.android.core

import assert4k.*
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
