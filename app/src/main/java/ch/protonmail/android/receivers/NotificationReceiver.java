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
package ch.protonmail.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.JobManager;

import java.util.Arrays;

import javax.inject.Inject;

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.jobs.PostArchiveJob;
import ch.protonmail.android.jobs.PostTrashJobV2;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 29.9.15.
 */
public class NotificationReceiver extends BroadcastReceiver {

    public static final String EXTRA_NOTIFICATION_ARCHIVE_MESSAGE = "notification_archive_message";
    public static final String EXTRA_NOTIFICATION_DELETE_MESSAGE = "notification_delete_message";
    public static final String EXTRA_NOTIFICATION_TRASH_MESSAGE = "notification_trash_message";

    @Inject
    JobManager mJobManager;

    @Inject
    MessageDetailsRepository messageDetailsRepository;

    public NotificationReceiver() {
        super();
        ProtonMailApplication.getApplication().getAppComponent().inject(this);
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null && extras.containsKey(EXTRA_NOTIFICATION_ARCHIVE_MESSAGE)) {
            String messageId = extras.getString(EXTRA_NOTIFICATION_ARCHIVE_MESSAGE);
            Job job = new PostArchiveJob(Arrays.asList(messageId));
            mJobManager.addJobInBackground(job);
            AppUtil.clearNotifications(context);
        } else if (extras != null && extras.containsKey(EXTRA_NOTIFICATION_TRASH_MESSAGE)) {
            String messageId = extras.getString(EXTRA_NOTIFICATION_TRASH_MESSAGE);
            new TrashMessage(mJobManager, messageDetailsRepository, messageId).execute();
            AppUtil.clearNotifications(context);
        } else if (extras != null && extras.containsKey(EXTRA_NOTIFICATION_DELETE_MESSAGE)) {
            AppUtil.clearNotifications(ProtonMailApplication.getApplication());
        }
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.setAlarm(ProtonMailApplication.getApplication(), true);
    }

    private static class TrashMessage extends AsyncTask<Void,Void,Void> {

        private final JobManager jobManager;
        private final MessageDetailsRepository messageDetailsRepository;
        private final String messageId;

        TrashMessage(JobManager jobManager, MessageDetailsRepository messageDetailsRepository, String messageId) {
            this.jobManager = jobManager;
            this.messageDetailsRepository = messageDetailsRepository;
            this.messageId = messageId;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Message message = messageDetailsRepository.findMessageById(messageId);
            if (message != null) {
                Job job = new PostTrashJobV2(Arrays.asList(message.getMessageId()), null);
                jobManager.addJobInBackground(job);
            }
            return null;
        }
    }
}
