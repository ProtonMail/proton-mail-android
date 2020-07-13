package ch.protonmail.android.mapper.bridge

import assert4k.*
import ch.protonmail.android.domain.entity.user.Address
import me.proton.core.util.kotlin.invoke
import kotlin.test.Test
import ch.protonmail.android.api.models.address.Address as OldAddress

/**
 * Test suite for [AddressesBridgeMapper] and [AddressBridgeMapper]
 */
internal class AddressesBridgeMapperTest {

    private val singleMapper = AddressBridgeMapper()
    private val multiMapper = AddressesBridgeMapper(singleMapper)

    @Test
    fun `can map correctly single Address`() {
        val oldAddress = OldAddress(
            id = "id",
            domainId = "domain_id",
            email = "davide@email.com",
            displayName = "Davide",
            enabled = true,
            type = 5,
            allowedToSend = true,
            allowedToReceive = true
        )

        val newAddress = singleMapper { oldAddress.toNewModel() }

        assert that newAddress * {
            +id.s equals "id"
            +domainId.s equals "domain_id"
            +email.s equals "davide@email.com"
            +displayName?.s equals "Davide"
            +enabled equals true
            +type equals Address.Type.EXTERNAL
            +allowedToSend equals true
            +allowedToReceive equals true
        }
    }

    @Test
    fun `can map correctly multiple Addresses`() {
        val oldAddresses = (1..10).map { OldAddress(it) }

        val newAddresses = multiMapper { oldAddresses.toNewModel() }

        assert that newAddresses * {
            +addresses.size() equals 10
        }
    }

    @Test
    fun `addresses are sorted properly`() {
        val oldAddresses = (20 downTo 11).map { OldAddress(order = it, id = "$it") }

        val newAddresses = multiMapper { oldAddresses.toNewModel() }

        assert that newAddresses * {
            +addresses.size() equals 10
            +primary?.id?.s equals "11"
            +sorted().map { it.id.s.toInt() } equals (11..20).toList()
        }
    }

    @Test
    fun `can map correctly multiple Addresses with undefined order`() {
        val oldAddresses = (1..10).map { OldAddress(id = "$it") }

        val newAddresses = multiMapper { oldAddresses.toNewModel() }

        assert that newAddresses * {
            +addresses.size() equals 10
            +sorted().map { it.id.s.toInt() } equals (1..10).toList()
        }
    }

    @Test
    fun `can fix Addresses with conflicting order`() {
        val order = (10..14).map { it to if (it == 12 || it == 13) 10 else it * 2 }.toMap()
        val oldAddresses = order.map { (id, order) -> OldAddress(order = order, id = "$id") }

        val newAddresses = multiMapper { oldAddresses.toNewModel() }

        assert that order equals mapOf(10 to 20, 11 to 22, 12 to 10, 13 to 10, 14 to 28)
        assert that newAddresses * {
            +addresses.size() equals 5
            +sorted().map { it.id.s.toInt() } equals listOf(12, 13, 10, 11, 14)
        }
    }

    private fun OldAddress(
        order: Int = 0,
        id: String = "none",
        domainId: String = "none",
        email: String = "none@email.com",
        displayName: String = "none",
        enabled: Boolean = false,
        type: Int = 1,
        allowedToSend: Boolean = false,
        allowedToReceive: Boolean = false
    ) = OldAddress(
        id,
        domainId,
        email,
        if (allowedToSend) 1 else 0,
        if (allowedToReceive) 1 else 0,
        if (enabled) 1 else 0,
        type,
        order,
        displayName,
        "none",
        -1,
        listOf()
    )
}
