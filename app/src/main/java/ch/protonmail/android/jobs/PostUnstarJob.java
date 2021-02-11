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

import java.util.ArrayList;
import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.api.models.room.counters.CounterDao;
import ch.protonmail.android.api.models.room.counters.CounterDatabase;
import ch.protonmail.android.api.models.room.counters.UnreadLocationCounter;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.RefreshDrawerEvent;
import ch.protonmail.android.utils.AppUtil;

public class PostUnstarJob extends ProtonMailEndlessJob {

    private final List<String> mMessageIds;

    public PostUnstarJob(final List<String> messageIds) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL));
        mMessageIds = messageIds;
    }

    @Override
    public void onAdded() {
        final CounterDao counterDao = CounterDatabase.Companion.getInstance(getApplicationContext()).getDao();

        for (String id : mMessageIds) {
            getMessageDetailsRepository().updateStarred(id, false);

            Constants.MessageLocationType messageLocation = Constants.MessageLocationType.INVALID;
            boolean isUnread = false;
            final Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                messageLocation = Constants.MessageLocationType.Companion.fromInt(message.getLocation());
                isUnread = !message.isRead();
            }

            if (messageLocation != Constants.MessageLocationType.INVALID && isUnread) {
                UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(messageLocation.getMessageLocationTypeValue());
                if (unreadLocationCounter == null) {
                    return;
                }
                unreadLocationCounter.increment(mMessageIds.size());
                List<UnreadLocationCounter> countersToUpdate = new ArrayList<>();
                countersToUpdate.add(unreadLocationCounter);

                    UnreadLocationCounter starredUnread = counterDao.findUnreadLocationById(Constants.MessageLocationType.STARRED.getMessageLocationTypeValue());
                    if (starredUnread != null) {
                        starredUnread.increment();
                        countersToUpdate.add(starredUnread);
                    }

                counterDao.insertAllUnreadLocations(countersToUpdate);
            }
            AppUtil.postEventOnUi(new RefreshDrawerEvent());
        }
    }

    @Override
    public void onRun() throws Throwable {
        List<String> messageIds = new ArrayList<>(mMessageIds);
        getApi().unlabelMessages(new IDList(String.valueOf(Constants.MessageLocationType.STARRED.getMessageLocationTypeValue()), messageIds));
    }
}
