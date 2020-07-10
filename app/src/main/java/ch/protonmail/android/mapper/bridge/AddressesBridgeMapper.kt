package ch.protonmail.android.mapper.bridge

import ch.protonmail.android.domain.entity.user.Addresses
import ch.protonmail.android.api.models.address.Address as OldAddress

/**
 * Transforms a collection of [ch.protonmail.android.api.models.address.Address] to
 * [ch.protonmail.android.domain.entity.user.Addresses]
 * Inherit from [BridgeMapper]
 */
class AddressesBridgeMapper : BridgeMapper<Collection<OldAddress>, Addresses> {

    override fun Collection<OldAddress>.toNewModel(): Addresses {
        TODO("Not yet implemented")
    }
}
