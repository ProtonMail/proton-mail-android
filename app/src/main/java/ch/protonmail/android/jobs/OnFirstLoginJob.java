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

import ch.protonmail.android.api.services.MessagesService;
import ch.protonmail.android.core.Constants;

/**
 * Persistent job to process certain jobs after first login and which requires network
 */
public class OnFirstLoginJob extends ProtonMailBaseJob {

    private static final int FETCH_CONTACT_DELAY_MS = 2000;
    private final boolean refreshDetails;
    private final boolean refreshContacts;

    public OnFirstLoginJob(boolean refreshDetails, boolean refreshContacts) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist());
        this.refreshDetails = refreshDetails;
        this.refreshContacts = refreshContacts;
    }

    public OnFirstLoginJob(boolean refreshDetails) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist());
        this.refreshDetails = refreshDetails;
        this.refreshContacts = true;
    }

    @Override
    public void onRun() throws Throwable {
        fetchAllMailbox();

        // delay initial contact fetch
        if (refreshContacts) {
            mJobManager.addJob(new FetchContactsEmailsJob(FETCH_CONTACT_DELAY_MS));
            mJobManager.addJob(new FetchContactsDataJob());
        }
        mJobManager.addJob(new FetchMailSettingsJob());
    }

    private void fetchAllMailbox() {
        MessagesService.Companion.startFetchLabels();
        MessagesService.Companion.startFetchFirstPage(Constants.MessageLocationType.INBOX, refreshDetails, null, false);
    }
}
