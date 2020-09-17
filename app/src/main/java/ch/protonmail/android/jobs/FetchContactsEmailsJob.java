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

import javax.inject.Inject;

import ch.protonmail.android.api.segments.contact.ContactEmailsManager;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.ContactsFetchedEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

public class FetchContactsEmailsJob extends ProtonMailBaseJob {

    @Inject
    transient ContactEmailsManager contactEmailsManager;

    public FetchContactsEmailsJob(long delayMs) {
        super(new Params(Priority.HIGH).delayInMs(delayMs).requireNetwork().groupBy(Constants.JOB_GROUP_CONTACT));
    }

    @Override
    public void onRun() throws Throwable {
        try {
            contactEmailsManager.refresh();
            AppUtil.postEventOnUi(new ContactsFetchedEvent(Status.SUCCESS));
        } catch (Exception e) {
            AppUtil.postEventOnUi(new ContactsFetchedEvent(Status.FAILED));
        }
    }
}
