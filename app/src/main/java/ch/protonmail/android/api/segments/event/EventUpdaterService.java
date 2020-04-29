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

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.ProtonJobIntentService;

import com.birbit.android.jobqueue.JobManager;

import javax.inject.Inject;

import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.QueueNetworkUtil;
import ch.protonmail.android.core.UserManager;

/**
 * Created by dkadrikj on 11.9.15.
 */
public class EventUpdaterService extends ProtonJobIntentService {

    @Inject
    EventManager eventManager;
    @Inject
    UserManager mUserManager;
    @Inject
    JobManager mJobManager;
    @Inject
    QueueNetworkUtil mNetworkUtils;
    AlarmReceiver alarmReceiver = new AlarmReceiver();

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, EventUpdaterService.class, Constants.JOB_INTENT_SERVICE_ID_EVENT_UPDATER, work);
    }

    public EventUpdaterService() {
        super();
        ProtonMailApplication.getApplication().getAppComponent().inject(this);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        if (mUserManager.isLoggedIn(mUserManager.getUsername())) {
            if (mUserManager.isBackgroundSyncEnabled()) {
                if (mNetworkUtils.isConnected() && mUserManager.accessTokenExists()) {
                    startService();
                } else {
                    startService();
                }
            }
        }
    }

    private void startService() {
        mJobManager.addJob(new FetchUpdatesJob(eventManager));
        alarmReceiver.setAlarm(getApplicationContext());
    }
}
