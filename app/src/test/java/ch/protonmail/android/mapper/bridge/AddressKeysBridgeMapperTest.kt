package ch.protonmail.android.mapper.bridge

import assert4k.*
import me.proton.core.util.kotlin.invoke
import kotlin.test.Test
import ch.protonmail.android.api.models.Keys as OldKey

/**
 * Test suite for [AddressKeysBridgeMapper] and [AddressKeyBridgeMapper]
 */
internal class AddressKeysBridgeMapperTest {

    private val singleMapper = AddressKeyBridgeMapper()
    private val multiMapper = AddressKeysBridgeMapper(singleMapper)

    @Test
    fun `can map correctly single Key`() {
        val oldKey = OldKey(
            id = "id",
            flags = 3,
            privateKey = "-----BEGIN PGP PRIVATE_KEY_BLOCK-----private_key-----END PGP PRIVATE_KEY_BLOCK-----",
            token = "-----BEGIN PGP MESSAGE-----token-----END PGP MESSAGE-----",
            signature = "-----BEGIN PGP SIGNATURE_KEY_BLOCK-----signature-----END PGP SIGNATURE_KEY_BLOCK-----",
            activation = "-----BEGIN PGP MESSAGE-----activation-----END PGP MESSAGE-----"
        )

        val newKey = singleMapper { oldKey.toNewModel() }

        assert that newKey * {
            +id.s equals "id"
            +canEncrypt equals true
            +canVerifySignature equals true
            +privateKey.content.s equals "private_key"
            +token?.content?.s equals "token"
            +signature?.content?.s equals "signature"
            +activation?.content?.s equals "activation"
        }
    }

    @Test
    fun `can map correctly multiple keys`() {
        val oldKeys = (1..10).map { OldKey("$it", primary = it == 4) }

        val newKeys = multiMapper { oldKeys.toNewModel() }

        assert that newKeys * {
            +primaryKey?.id?.s equals "4"
            +keys.size() equals 10
        }
    }

    @Test
    fun `does pick first Key as primary, if none is defined`() {
        val oldKeys = (1..10).map { OldKey("$it") }

        val newKeys = multiMapper { oldKeys.toNewModel() }

        assert that newKeys * {
            +primaryKey?.id?.s equals "1"
            +keys.size() equals 10
        }
    }

    private fun OldKey(
        id: String = "none",
        primary: Boolean = false,
        flags: Int = 0,
        privateKey: String = "none",
        token: String = "none",
        signature: String = "none",
        activation: String = "none"
    ) = OldKey(id, privateKey, flags, if (primary) 1 else 0, token, signature, activation)
}
