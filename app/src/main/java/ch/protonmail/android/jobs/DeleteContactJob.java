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
package ch.protonmail.android.jobs;

import com.birbit.android.jobqueue.Params;

import java.util.ArrayList;
import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.api.models.room.contacts.ContactData;
import ch.protonmail.android.api.models.room.contacts.ContactEmail;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.rx.ThreadSchedulers;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.ContactDeleteEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

public class DeleteContactJob extends ProtonMailBaseJob{

	private final List<String> mContactIds;

	public DeleteContactJob(List<String> contactIds){
		super(new Params(Priority.MEDIUM).requireNetwork().groupBy(Constants.JOB_GROUP_CONTACT));
		mContactIds=contactIds;
	}

	@Override
	public void onAdded(){
        ContactsDatabaseFactory contactsDatabaseFactory = ContactsDatabaseFactory.Companion.getInstance(
                getApplicationContext());

        ContactsDatabase contactsDatabase = contactsDatabaseFactory
                .getDatabase();
	    for(String contactId : mContactIds){
			final ContactData contactData=contactsDatabase.findContactDataById(contactId);
			if(contactData!=null){
				contactsDatabaseFactory.runInTransaction(()->{
                    List<ContactEmail> contactEmails = contactsDatabase.findContactEmailsByContactId(
                            contactData.getContactId());
                    contactsDatabase.deleteAllContactsEmails(contactEmails);
                    contactsDatabase.deleteContactData(contactData);
				});
			}
		}
	}

	@Override
	public void onRun() throws Throwable{
		List<String> contactIds=new ArrayList<>();
		contactIds.addAll(mContactIds);
		getApi().deleteContact(new IDList(contactIds)).observeOn(ThreadSchedulers.io()).subscribeOn(ThreadSchedulers.io()).subscribe();
		AppUtil.postEventOnUi(new ContactDeleteEvent(Status.SUCCESS));
	}
}
