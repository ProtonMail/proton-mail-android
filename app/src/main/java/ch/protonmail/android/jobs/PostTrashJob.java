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

import java.util.Collections;
import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.data.local.CounterDao;
import ch.protonmail.android.data.local.CounterDatabase;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLocationCounter;
import ch.protonmail.android.events.RefreshDrawerEvent;
import ch.protonmail.android.utils.AppUtil;

public class PostTrashJob extends ProtonMailCounterJob {

    private final String mLabelId;
    private final List<String> mMessageIds;

    public PostTrashJob(final List<String> messageIds, String labelId) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
        mLabelId = labelId;
    }

    @Override
    public void onAdded() {
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext(), userId)
                .getDao();

        int totalUnread = 0;
        for (String id : mMessageIds) {
            Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                if (!message.isRead()) {
                    UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(message.getLocation());
                    if (unreadLocationCounter != null) {
                        unreadLocationCounter.decrement();
                        counterDao.insertUnreadLocation(unreadLocationCounter);
                    }
                    totalUnread++;
                }
                if (Constants.MessageLocationType.Companion.fromInt(message.getLocation()) == Constants.MessageLocationType.SENT) {
                    message.setLocation(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue());
                    message.removeLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.SENT.getMessageLocationTypeValue())));
                    message.addLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.ALL_SENT.getMessageLocationTypeValue())));
                } else {
                    message.addLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue())));
                    if (!TextUtils.isEmpty(mLabelId)) {
                        message.removeLabels(Collections.singletonList(mLabelId));
                    }
                    message.setLocation(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue());
                }
                getMessageDetailsRepository().saveMessageBlocking(message);

            }
        }
        UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue());
        if (unreadLocationCounter == null) {
            return;
        }
        unreadLocationCounter.increment(totalUnread);
        counterDao.insertUnreadLocation(unreadLocationCounter);
        AppUtil.postEventOnUi(new RefreshDrawerEvent());
    }

    @Override
    public void onRun() throws Throwable {
        getApi().labelMessages(new IDList(String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue()), mMessageIds));
    }

    @Override
    protected List<String> getMessageIds() {
        return mMessageIds;
    }

    @Override
    protected Constants.MessageLocationType getMessageLocation() {
        return Constants.MessageLocationType.TRASH;
    }
}
