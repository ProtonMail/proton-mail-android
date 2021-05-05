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

import androidx.annotation.NonNull;

import com.birbit.android.jobqueue.Params;

import java.util.List;

import ch.protonmail.android.api.models.messages.receive.MessagesResponse;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.events.NoResultsEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

public class SearchMessagesJob extends ProtonMailBaseJob {

    private static final String TAG_SEARCH_MESSAGES_JOB = "SearchMessagesJob";

    private final String queryString;
    private final int page;

    public SearchMessagesJob(@NonNull String queryString, int page) {
        super(new Params(Priority.MEDIUM));
        this.queryString = queryString;
        this.page = page;
    }

    @Override
    public void onRun() throws Throwable {
        if (queryString.trim().length() > 0) {
            boolean hasResults;
            if (!getQueueNetworkUtil().isConnected()) {
                hasResults = doLocalSearch();
            } else {
                hasResults = doRemoteSearch();
            }
            if (!hasResults) {
                AppUtil.postEventOnUi(new NoResultsEvent(page));
            }
        }
    }

    private boolean doLocalSearch() {
        final List<Message> messages = getMessageDetailsRepository().searchMessages(queryString, queryString, queryString);
        return messages.size() > 0;
    }

    private boolean doRemoteSearch() {
        try {
            MessagesResponse response = getApi().search(queryString, page);
            List<Message> resultsList = response.getMessages();
            return resultsList.size() > 0;
        } catch (Exception error) {
            Logger.doLogException(TAG_SEARCH_MESSAGES_JOB, "Error searching messages", error);
        }
        return false;
    }
}
