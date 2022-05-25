/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.activities.messageDetails.details

import android.view.View
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.details.presentation.ui.MessageDetailsActivity
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.ui.RecipientDropDownClickListener

internal class RecipientContextMenuFactory(
    private val context: MessageDetailsActivity
) : Function1<String, View.OnClickListener> {

    override fun invoke(email: String): View.OnClickListener {
        val app = context.app
        val contactsDatabase = ContactDatabase.getInstance(app, app.userManager.requireCurrentUserId()).getDao()
        return RecipientDropDownClickListener(context, contactsDatabase, email)
    }
}
