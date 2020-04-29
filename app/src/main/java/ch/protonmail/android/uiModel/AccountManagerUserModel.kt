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

import ch.protonmail.libs.core.utils.EMPTY_STRING

/**
 * Represent a base Navigation Drawer User data class.
 * @see ch.protonmail.android.adapters.AccountManagerAccountsAdapter
 *
 * @author Dino Kadrikj
 */
internal sealed class AccountManagerUserModel {

    // TODO: remove unused fields, if any
    data class User @JvmOverloads constructor(
        val name: String,
        val emailAddress: String = EMPTY_STRING,
        val loggedIn: Boolean = false,
        val primary: Boolean = false,
        val displayName: String = name
    ) : AccountManagerUserModel()


    /** Divider for Drawer Items */
    object Divider : AccountManagerUserModel()

    /** Footer for Account Manager Items */
    object AddAccount : AccountManagerUserModel()
}
