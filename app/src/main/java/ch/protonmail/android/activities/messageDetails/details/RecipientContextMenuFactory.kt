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
package ch.protonmail.android.activities.messageDetails.details

import android.view.View
import ch.protonmail.android.activities.messageDetails.MessageDetailsActivity
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.utils.ui.RecipientDropDownClickListener

/**
 * Created by Kamil Rajtar on 14.08.18.
 */
internal class RecipientContextMenuFactory(private val context:MessageDetailsActivity):Function1<String,View.OnClickListener> {

	override fun invoke(email: String): View.OnClickListener {
		val contactsDatabase = ContactDatabase.getInstance(ProtonMailApplication.getApplication()).getDao()
		return RecipientDropDownClickListener(context, contactsDatabase, email)
	}
}
