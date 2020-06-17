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
package ch.protonmail.android.api.segments.event;

import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Params;

import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.protonmail.android.api.AccountManager;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.ConnectivityEvent;
import ch.protonmail.android.events.FetchUpdatesEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

public class FetchUpdatesJob extends ProtonMailBaseJob {

    private static final String TAG_FETCH_UPDATES_JOB = "FetchUpdatesJob";
    private EventManager eventManager;

    FetchUpdatesJob(EventManager eventManager) {
        super(new Params(Priority.HIGH).requireNetwork());
        this.eventManager = eventManager;
	}

	public FetchUpdatesJob() {
        this(ProtonMailApplication.getApplication().getEventManager());
    }

    @Override
    public void onRun() throws Throwable {
        MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        if (!mQueueNetworkUtil.isConnected()) {
            Logger.doLog(TAG_FETCH_UPDATES_JOB, "no network cannot fetch updates");
            AppUtil.postEventOnUi(new FetchUpdatesEvent(Status.NO_NETWORK));
            return;
        }

        //check for expired messages in the cache and delete them
        long currentTime = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis());
        messagesDatabase.deleteExpiredMessages(currentTime);
        try {
            List<String> loggedInUsers = AccountManager.Companion.getInstance(ProtonMailApplication.getApplication()).getLoggedInUsers();
            eventManager.start(loggedInUsers);
            AppUtil.postEventOnUi(new FetchUpdatesEvent(Status.SUCCESS));
        } catch (Exception e) {
            if (e.getCause() instanceof ConnectException) {
                AppUtil.postEventOnUi(new ConnectivityEvent(false));
            }
            AppUtil.postEventOnUi(new FetchUpdatesEvent(Status.FAILED));
        }
    }

    @Override
    protected void onProtonCancel(int cancelReason, @Nullable Throwable throwable) {
        AppUtil.postEventOnUi(new FetchUpdatesEvent(Status.FAILED));
    }
}
