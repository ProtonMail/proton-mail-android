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
import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.data.local.CounterDao;
import ch.protonmail.android.data.local.CounterDatabase;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLocationCounter;
import ch.protonmail.android.events.RefreshDrawerEvent;
import ch.protonmail.android.utils.AppUtil;

public class PostSpamJob extends ProtonMailCounterJob {

    private final List<String> mMessageIds;
    private final String mFolderId;

    public PostSpamJob(final List<String> messageIds) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
        mFolderId = null;
    }

    public PostSpamJob(final List<String> messageIds, String folderId) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
        mFolderId = folderId;
    }

    @Override
    public void onAdded() {
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext(), getUserId())
                .getDao();
        int totalUnread = 0;
        for (String id : mMessageIds) {
            final Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                if (markMessageLocally(counterDao,message)) {
                    totalUnread++;
                }
                if (!TextUtils.isEmpty(mFolderId)) {
                    message.removeLabels(Arrays.asList(mFolderId));
                }
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
        message.setLocation(Constants.MessageLocationType.SPAM.getMessageLocationTypeValue());
        message.setLabelIDs(Arrays.asList(String.valueOf(Constants.MessageLocationType.SPAM.getMessageLocationTypeValue()), String.valueOf(Constants.MessageLocationType.ALL_MAIL.getMessageLocationTypeValue())));
        getMessageDetailsRepository().saveMessageBlocking(message);
        return unreadIncrease;
    }

    @Override
    public void onRun() throws Throwable {
        getApi().labelMessages(new IDList(String.valueOf(Constants.MessageLocationType.SPAM.getMessageLocationTypeValue()), mMessageIds));
    }

    @Override
    protected List<String> getMessageIds() {
        return mMessageIds;
    }

    @Override
    protected Constants.MessageLocationType getMessageLocation() {
        return Constants.MessageLocationType.SPAM;
    }
}
