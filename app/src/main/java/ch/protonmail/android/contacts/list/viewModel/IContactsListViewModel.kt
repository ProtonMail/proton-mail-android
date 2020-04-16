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
package ch.protonmail.android.contacts.list.viewModel

import androidx.lifecycle.LiveData
import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.contacts.list.progress.ProgressState
import ch.protonmail.android.toasts.IToastViewModel
import ch.protonmail.android.utils.Event
import ch.protonmail.android.views.models.LocalContact

/**
 * Created by Kamil Rajtar on 23.08.18.
 */
interface IContactsListViewModel:IToastViewModel {
	val contactItems:LiveData<List<ContactItem>>
	val androidContacts:LiveData<List<ContactItem>>
	val uploadProgress:LiveData<ProgressState?>
	val contactToConvert:LiveData<Event<LocalContact>>
	fun setProgressMax(max:Int?)
	fun setProgress(progress:Int?)
	fun setHasContactsPermission(hasPermission:Boolean)
	fun startConvertDetails(contactId:String)
}