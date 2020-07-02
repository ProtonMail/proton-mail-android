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
