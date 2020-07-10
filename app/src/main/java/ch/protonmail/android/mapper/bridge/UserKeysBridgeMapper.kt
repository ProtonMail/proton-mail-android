package ch.protonmail.android.mapper.bridge

import ch.protonmail.android.domain.entity.user.UserKeys
import ch.protonmail.android.api.models.Keys as OldKeys

/**
 * Transforms a collection of [ch.protonmail.android.api.models.Keys] to
 * [ch.protonmail.android.domain.entity.user.UserKeys]
 * Inherit from [BridgeMapper]
 */
class UserKeysBridgeMapper : BridgeMapper<Collection<OldKeys>, UserKeys> {

    override fun Collection<OldKeys>.toNewModel(): UserKeys {
        TODO("Not yet implemented")
    }
}
