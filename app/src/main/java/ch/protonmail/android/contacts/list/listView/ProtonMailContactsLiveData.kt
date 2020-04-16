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
package ch.protonmail.android.contacts.list.listView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail

internal class ProtonMailContactsLiveData(contactsDataLiveData:LiveData<List<ContactData>>,
										  contactsEmailsLiveData:LiveData<List<ContactEmail>>):MediatorLiveData<List<ContactItem>>() {
	private var contactsData:List<ContactData>?=null
	private var contactsEmails:List<ContactEmail>?=null

	init {
		addSource(contactsDataLiveData){
			contactsData=it
			tryEmit()
		}
		addSource(contactsEmailsLiveData) {
			contactsEmails=it
			tryEmit()
		}
	}
	private fun tryEmit()
	{
		val contactsData=contactsData?:return
		val contactsEmails=contactsEmails?:return
		value=contactsData.getAdapterItems(contactsEmails)
	}

	private fun List<ContactData>.getAdapterItems(
			emails:List<ContactEmail>?):List<ContactItem> {
		val emailsMap=emails?.groupBy(ContactEmail::contactId)

		return map {
			val contactId=it.contactId
			val name=it.name
			var primaryEmail:String?=null
			var additionalEmailsCount=0

			emailsMap?.get(contactId)?.apply {
				if(!isEmpty()) {
					primaryEmail=get(0).email
				}
				if(size>1) {
					additionalEmailsCount=kotlin.math.max( size-1,0)
				}
			}

			ContactItem(true,
					contactId,
					name,
					primaryEmail,
					additionalEmailsCount)
		}
	}

}