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
package ch.protonmail.android.uiModel

import ch.protonmail.android.domain.entity.Id
import me.proton.core.util.kotlin.EMPTY_STRING

/**
 * Represent a base Navigation Drawer User data class.
 * @see ch.protonmail.android.adapters.AccountsAdapter
 *
 * TODO split to DrawerUserModel and AccountManagerUserModel
 */
internal sealed class DrawerUserModel {

    sealed class BaseUser : DrawerUserModel() {

        abstract val id: Id

        /**
         * User's user name. Default is empty
         */
        open val name: String = EMPTY_STRING

        /**
         * User's email. Default is empty.
         */
        open val emailAddress: String = EMPTY_STRING

        /**
         * Value of whether this user is currently logged in or it is logged out.
         */
        open val loggedIn: Boolean = false

        /**
         * Value of whether this user has snoozed his notifications at the moment.
         */
        open val notificationsSnoozed: Boolean = false

        data class AccountUser @JvmOverloads constructor(
            override val id: Id,
            override val name: String = EMPTY_STRING,
            override val emailAddress: String = EMPTY_STRING,
            override val loggedIn: Boolean = false,
            val primary: Boolean = false,
            val displayName: String = EMPTY_STRING
        ) : BaseUser() {

            @Deprecated(
                "Use constructor with id",
                ReplaceWith("AccountUser(id, name, emailAddress, loggedIn, primary, displayName"),
                DeprecationLevel.ERROR
            )
            constructor(
                name: String = EMPTY_STRING,
                emailAddress: String = EMPTY_STRING,
                loggedIn: Boolean = false,
                primary: Boolean = false,
                displayName: String = EMPTY_STRING
            ): this(TODO(), name, emailAddress, loggedIn, primary, displayName)
        }

        data class DrawerUser @JvmOverloads constructor(
            override val id: Id,
            override val name: String,
            override val emailAddress: String,
            override val loggedIn: Boolean,
            val notifications: Int,
            override val notificationsSnoozed: Boolean,
            val displayName: String
        ) : BaseUser()
    }

    /** Divider for Drawer Items */
    object Divider : DrawerUserModel()

    /** Footer for Nav Drawer Items */
    object Footer : DrawerUserModel()

    /** Footer for Account Manager Items */
    object AccFooter : DrawerUserModel()
}
