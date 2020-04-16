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

import android.content.SharedPreferences;

import com.birbit.android.jobqueue.Params;

import java.util.List;

import ch.protonmail.android.api.models.ContactsDataResponse;
import ch.protonmail.android.api.models.room.contacts.ContactData;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.ContactsFetchedEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

public class FetchContactsDataJob extends ProtonMailBaseJob {

    private int mPage;
    private final int mPageSize;

    public FetchContactsDataJob() {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_CONTACT));
		mPageSize = Constants.CONTACTS_PAGE_SIZE;
        mPage = 0;
    }

    @Override
    public void onAdded() {
        super.onAdded();
        final SharedPreferences prefs = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
        prefs.edit().putBoolean(Constants.Prefs.PREF_CONTACTS_LOADING, true).apply();
    }

    @Override
    protected int getRetryLimit() {
        return 3;
    }

    @Override
    public void onRun() throws Throwable {
        final ContactsDatabase contactsDatabase = ContactsDatabaseFactory.Companion.getInstance(
                getApplicationContext())
                .getDatabase();
        final SharedPreferences prefs = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
        ContactsDataResponse response = mApi.fetchContacts(mPage, mPageSize);
        List<ContactData> contacts = response.getContacts();
        Status status = Status.FAILED;
        if (contacts != null) {
            int total = response.getTotal();
            int fetched = contacts.size();
            while (total > fetched) {
                mPage++;
                response = mApi.fetchContacts(mPage, mPageSize);
                List<ContactData> contactDataList = response.getContacts();
                if (contactDataList.isEmpty()) {
                    break;
                }
                contacts.addAll(contactDataList);
                fetched = contacts.size();
            }
            try {
                contactsDatabase.saveAllContactsData(contacts);
                status = Status.SUCCESS;
            } finally {
                prefs.edit().putBoolean(Constants.Prefs.PREF_CONTACTS_LOADING, false).apply();
            }
        }
        AppUtil.postEventOnUi(new ContactsFetchedEvent(status));
    }
}
