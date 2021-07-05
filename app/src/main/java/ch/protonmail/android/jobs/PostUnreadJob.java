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
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.data.local.CounterDao;
import ch.protonmail.android.data.local.CounterDatabase;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLocationCounter;
import timber.log.Timber;

public class PostUnreadJob extends ProtonMailEndlessJob {

    private final List<String> mMessageIds;

    public PostUnreadJob(final List<String> messageIds) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
    }

    @Override
    public void onAdded() {
        Timber.d("markUnRead %s", mMessageIds);
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext(), getUserId())
                .getDao();
        Constants.MessageLocationType messageLocation = Constants.MessageLocationType.INVALID;
        boolean starred = false;
        for (String id : mMessageIds) {
            final Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                starred = message.isStarred() != null && message.isStarred();
                messageLocation = Constants.MessageLocationType.Companion.fromInt(message.getLocation());
                message.setIsRead(false);
                getMessageDetailsRepository().saveMessageBlocking(message);
            }
        }

        if (messageLocation != Constants.MessageLocationType.INVALID) {
            UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(messageLocation.getMessageLocationTypeValue());
            if (unreadLocationCounter == null) {
                return;
            }
            unreadLocationCounter.increment(mMessageIds.size());
            List<UnreadLocationCounter> countersToUpdate = new ArrayList<>();
            countersToUpdate.add(unreadLocationCounter);
            if (starred) {
                UnreadLocationCounter starredUnread = counterDao.findUnreadLocationById(Constants.MessageLocationType.STARRED.getMessageLocationTypeValue());
                if (starredUnread != null) {
                    starredUnread.increment();
                    countersToUpdate.add(starredUnread);
                }
            }
            counterDao.insertAllUnreadLocations(countersToUpdate);
        }
    }

    @Override
    public void onRun() throws Throwable {
        List<String> messageIds = new ArrayList<>(mMessageIds);
        getApi().markMessageAsUnRead(new IDList(messageIds));
    }
}
