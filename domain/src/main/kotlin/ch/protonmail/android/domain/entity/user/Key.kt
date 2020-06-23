package ch.protonmail.android.domain.entity.user

import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.Validable
import ch.protonmail.android.domain.entity.Validated
import ch.protonmail.android.domain.entity.Validator
import ch.protonmail.android.domain.entity.requireValid

/**
 * Representation of an user's Key
 * @author Davide Farella
 */
@Validated
data class Key(
    val id: Id,
    val version: Int,
    val privateKey: NotBlankString
)

/**
 * A set of [Key]s with a primary one
 * [Validable]: [keys] must contains [primaryKey]
 */
@Validated
data class Keys(
    val primaryKey: Key,
    val keys: Collection<Key> = listOf(primaryKey) // Verify whether is a possible scenario to have a single key
) : Validable by Validator<Keys>({ primaryKey in keys }) {
    init { requireValid() }
}
