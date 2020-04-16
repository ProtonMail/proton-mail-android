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

import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.models.room.contacts.ContactLabel;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.LabelDeletedEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 28.8.15.
 */
public class DeleteLabelJob extends ProtonMailEndlessJob {

    private final String mLabelId;

    public DeleteLabelJob(String labelId) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL));
        mLabelId = labelId;
    }

    @Override
    public void onAdded() {
        // TODO: 8/22/18 kadrikj make it work with contact groups also!
        // TODO: 9/1/18 kadrikj onAdded is executed on main thread !
        ContactsDatabase contactsDatabase = ContactsDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        final ContactLabel contactLabel = contactsDatabase.findContactGroupById(mLabelId);
        if (contactLabel != null) {
            contactsDatabase.deleteContactGroup(contactLabel);
        }
        final MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(
                getApplicationContext()).getDatabase();
            messagesDatabase.deleteLabelById(mLabelId);
    }

    @Override
    public void onRun() throws Throwable {
        ResponseBody responseBody = mApi.deleteLabel(mLabelId).blockingGet();

        if (responseBody.getCode() == Constants.RESPONSE_CODE_OK) {
            AppUtil.postEventOnUi(new LabelDeletedEvent(Status.SUCCESS));
        } else {
            AppUtil.postEventOnUi(new LabelDeletedEvent(Status.FAILED));
        }
    }
}
