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

import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Params;

import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.api.models.room.counters.CountersDatabase;
import ch.protonmail.android.api.models.room.counters.CountersDatabaseFactory;
import ch.protonmail.android.api.models.room.counters.UnreadLabelCounter;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.RefreshDrawerEvent;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 17.7.15.
 */
public class RemoveLabelJob extends ProtonMailBaseJob {

    private List<String> messageIds;
    private String labelId;

    public RemoveLabelJob(List<String> messageIds, String labelId) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL));
        this.messageIds = messageIds;
        this.labelId = labelId;
    }

    @Override
    protected void onProtonCancel(int cancelReason, @Nullable Throwable throwable) {
        final CountersDatabase countersDatabase = CountersDatabaseFactory.Companion
                .getInstance(getApplicationContext())
                .getDatabase();
        int totalUnread = 0;
        for (String messageId : messageIds) {
            Message message = messageDetailsRepository.findMessageById(messageId);
            if (message == null) {
                continue;
            }
            if (message.isRead()) {
                totalUnread++;
            }
        }

        UnreadLabelCounter unreadLabelCounter = countersDatabase.findUnreadLabelById(labelId);
        if (unreadLabelCounter == null) {
            return;
        }
        unreadLabelCounter.increment(totalUnread);
        countersDatabase.insertUnreadLabel(unreadLabelCounter);
        AppUtil.postEventOnUi(new RefreshDrawerEvent());
    }

    @Override
    public void onAdded() {
        final CountersDatabase countersDatabase = CountersDatabaseFactory.Companion.getInstance(
                getApplicationContext()).getDatabase();

        int totalUnread = 0;
        for (String messageId : messageIds) {
            Message message = messageDetailsRepository.findMessageById(messageId);
            if (message == null) {
                continue;
            }
            if (!message.isRead()) {
                totalUnread++;
            }
        }

        UnreadLabelCounter unreadLabelCounter = countersDatabase.findUnreadLabelById(labelId);
        if (unreadLabelCounter == null) {
            return;
        }
        unreadLabelCounter.decrement(totalUnread);
        countersDatabase.insertUnreadLabel(unreadLabelCounter);
        AppUtil.postEventOnUi(new RefreshDrawerEvent());
    }

    @Override
    public void onRun() throws Throwable {
        mApi.unlabelMessages(new IDList(labelId, messageIds));
    }
}
