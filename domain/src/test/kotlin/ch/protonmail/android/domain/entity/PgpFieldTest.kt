package ch.protonmail.android.domain.entity

import assert4k.*
import kotlin.test.Test

/**
 * Test suite for [PgpField]
 */
internal class PgpFieldTest {

    private val MSG_PREFIX = "-----BEGIN PGP MESSAGE-----"
    private val MSG_SUFFIX = "-----END PGP MESSAGE-----"

    @Test
    fun `PgpField is initialized correctly with not empty input WITHOUT prefix and suffix`() {
        val input = "hello"
        val pgp = PgpField.Message(NotBlankString(input))

        assert that pgp * {
            +content.s equals input
            +string equals "$MSG_PREFIX$input$MSG_SUFFIX"
        }
    }

    @Test
    fun `PgpField is initialized correctly with not empty input WITH prefix and suffix`() {
        val input = "${MSG_PREFIX}hello$MSG_SUFFIX"
        val pgp = PgpField.Message(NotBlankString(input))

        assert that pgp * {
            +content.s equals "hello"
            +string equals input
        }
    }

    @Test
    fun `PgpField fails with empty input WITHOUT prefix and suffix`() {
        val input = "   "
        assert that fails<ValidationException> { PgpField.Message(NotBlankString(input)) }
    }

    @Test
    fun `PgpField fails with empty input WITH prefix and suffix`() {
        val input = "$MSG_PREFIX   $MSG_SUFFIX"
        assert that fails<ValidationException> { PgpField.Message(NotBlankString(input)) }
    }

    @Test
    fun `PgpField equals works properly across input with and without prefix and suffix`() {
        val plain = "hello world"
        val full = "$MSG_PREFIX$plain$MSG_SUFFIX"
        val other = "other"

        val plainPgp = PgpField.Message(NotBlankString(plain))
        val fullPgp = PgpField.Message(NotBlankString(full))
        val otherPgp = PgpField.Message(NotBlankString(other))

        assert that plainPgp equals fullPgp
        assert that plainPgp `not equals` otherPgp
    }
}
