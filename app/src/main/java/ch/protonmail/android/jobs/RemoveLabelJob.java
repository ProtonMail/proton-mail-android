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
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.data.local.CounterDao;
import ch.protonmail.android.data.local.CounterDatabase;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLabelCounter;

@Deprecated // replaced with RemoveLabelWorker
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
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext(), getUserId())
                .getDao();
        int totalUnread = 0;
        for (String messageId : messageIds) {
            Message message = getMessageDetailsRepository().findMessageByIdBlocking(messageId);
            if (message == null) {
                continue;
            }
            if (message.isRead()) {
                totalUnread++;
            }
        }

        UnreadLabelCounter unreadLabelCounter = counterDao.findUnreadLabelById(labelId);
        if (unreadLabelCounter == null) {
            return;
        }
        unreadLabelCounter.increment(totalUnread);
        counterDao.insertUnreadLabel(unreadLabelCounter);
    }

    @Override
    public void onAdded() {
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext(), getUserId())
                .getDao();

        int totalUnread = 0;
        for (String messageId : messageIds) {
            Message message = getMessageDetailsRepository().findMessageByIdBlocking(messageId);
            if (message == null) {
                continue;
            }
            if (!message.isRead()) {
                totalUnread++;
            }
        }

        UnreadLabelCounter unreadLabelCounter = counterDao.findUnreadLabelById(labelId);
        if (unreadLabelCounter == null) {
            return;
        }
        unreadLabelCounter.decrement(totalUnread);
        counterDao.insertUnreadLabel(unreadLabelCounter);
    }

    @Override
    public void onRun() throws Throwable {
        getApi().unlabelMessages(new IDList(labelId, messageIds));
    }
}
