package ch.protonmail.android.domain.entity.user

/**
 * A plan for [User]
 * Plan types are exclusive; for example if [Mail.Paid] is present, [Mail.Free] cannot
 *
 * Free plan is represented on BE as `Services`, paid as `Subscribed`
 * Combination of Mail + Vpn flag is 5.
 *
 * @author Davide Farella
 */
sealed class Plan {

    sealed class Mail : Plan() { // Flag is 1 on BE
        object Free : Mail()
        object Paid : Mail()
    }

    sealed class Vpn : Plan() { // Flag is 4 on BE
        object Free : Vpn()
        object Paid : Vpn()
    }
}
