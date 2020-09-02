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

package ch.protonmail.android.credentials

import kotlinx.serialization.Serializable

// TODO subclasses should be holding associated credentials, now they don't as it will required some restructuring and
//  break the compatibility with current design
@Serializable
sealed class Credential {

    /**
     * No Credential found matching the constraint
     */
    @Serializable
    object NotFound : Credential()

    /**
     * User Id is available, but not associated credentials
     *
     * should be holding User Id
     */
    @Serializable
    object LoggedOut : Credential()

    /**
     * The user has inserted the password, but didn't insert the ( required ) Mailbox password.
     *
     * At this point, the caller should verify that the Mailbox password is still required, as it may has been disabled
     * meanwhile
     *
     * should be holding User Id and locked keys
     * Note: keys are currently tied to the User
     * @see ch.protonmail.android.domain.entity.user.User.keys
     * @see ch.protonmail.android.domain.entity.user.Address.keys
     * but in future, when the architecture will allow us, we should separate them, as the encryption/decryption should
     * be abstracted from our core business logic
     */
    @Serializable
    object MailboxPasswordRequired : Credential()

    /**
     * The user has fully completed the log-in process, thus it is allowed to use all the functionality.
     * It already completed 2FA, Mailbox Password and every possible step
     *
     * At this point, if Mailbox psw is null the caller should verify that it's still not required, because Settings
     * may have change meanwhile
     *
     * should be holding User Id, locked keys and Mailbox psw ( or null )
     * Note: keys are currently tied to the User
     * @see ch.protonmail.android.domain.entity.user.User.keys
     * @see ch.protonmail.android.domain.entity.user.Address.keys
     * but in future, when the architecture will allow us, we should separate them, as the encryption/decryption should
     * be abstracted from our core business logic
     */
    @Serializable
    object FullyLoggedIn : Credential()
}
