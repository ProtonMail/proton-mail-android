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

import android.text.TextUtils;

import com.birbit.android.jobqueue.Params;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.api.models.room.counters.CounterDao;
import ch.protonmail.android.api.models.room.counters.CounterDatabase;
import ch.protonmail.android.api.models.room.counters.UnreadLocationCounter;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.RefreshDrawerEvent;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dino on 2/24/17.
 */

public class MoveToFolderJob extends ProtonMailBaseJob {
    private List<String> mMessageIds;
    private String mLabelId;

    public MoveToFolderJob(List<String> messageIds, String labelId) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL));
        this.mMessageIds = messageIds;
        this.mLabelId = labelId;
    }

    @Override
    public void onAdded() {
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext())
                .getDao();
        int totalUnread = 0;
        for (String id : mMessageIds) {
            final Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                if (!TextUtils.isEmpty(mLabelId)) {
                    int location = message.getLocation();
                    message.addLabels(Arrays.asList(mLabelId));
                    message.removeLabels(Arrays.asList(String.valueOf(location)));
                }
                if (markMessageLocally(counterDao, message)) {
                    totalUnread++;
                }
                getMessageDetailsRepository().saveMessageInDB(message);
            }
        }

        UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(Constants.MessageLocationType.SPAM.getMessageLocationTypeValue());
        if (unreadLocationCounter == null) {
            return;
        }
        unreadLocationCounter.increment(totalUnread);
        counterDao.insertUnreadLocation(unreadLocationCounter);
        AppUtil.postEventOnUi(new RefreshDrawerEvent());
    }

    @Override
    public void onRun() throws Throwable {
        IDList body = new IDList(mLabelId, mMessageIds);
        getApi().labelMessages(body);
    }

    private boolean markMessageLocally(CounterDao counterDao, Message message) {
        boolean unreadIncrease = false;
        if (!message.isRead()) {
            UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(message.getLocation());
            if (unreadLocationCounter != null) {
                unreadLocationCounter.decrement();
                counterDao.insertUnreadLocation(unreadLocationCounter);
            }
            unreadIncrease = true;
        }
        if (Constants.MessageLocationType.Companion.fromInt(message.getLocation()) == Constants.MessageLocationType.SENT) {
            message.addLabels(Collections.singletonList(mLabelId));
        } else {
            message.setLocation(Constants.MessageLocationType.ALL_MAIL.getMessageLocationTypeValue());
        }
        MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        message.setFolderLocation(messagesDatabase);
        getMessageDetailsRepository().saveMessageInDB(message);
        return unreadIncrease;
    }
}
