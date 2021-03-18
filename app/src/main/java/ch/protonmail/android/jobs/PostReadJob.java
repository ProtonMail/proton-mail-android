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
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLocationCounter;
import ch.protonmail.android.events.RefreshDrawerEvent;
import ch.protonmail.android.utils.AppUtil;

public class PostReadJob extends ProtonMailEndlessJob {

    private final List<String> mMessageIds;

    public PostReadJob(final List<String> messageIds) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
    }

    @Override
    public void onAdded() {
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext())
                .getDao();
        Constants.MessageLocationType messageLocation = Constants.MessageLocationType.INVALID;
        boolean starred = false;
        for (String id : mMessageIds) {
            final Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                starred = message.isStarred() != null && message.isStarred();
                messageLocation = Constants.MessageLocationType.Companion.fromInt(message.getLocation());
                message.setIsRead(true);
                getMessageDetailsRepository().saveMessageInDB(message);
            }
        }

        if (messageLocation != Constants.MessageLocationType.INVALID) {
            UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(messageLocation.getMessageLocationTypeValue());
            if (unreadLocationCounter == null) {
                return;
            }
            unreadLocationCounter.decrement(mMessageIds.size());
            List<UnreadLocationCounter> countersToUpdate = new ArrayList<>();
            countersToUpdate.add(unreadLocationCounter);
            if (starred) {
                UnreadLocationCounter starredUnread = counterDao.findUnreadLocationById(Constants.MessageLocationType.STARRED.getMessageLocationTypeValue());
                if (starredUnread != null) {
                    starredUnread.decrement();
                    countersToUpdate.add(starredUnread);
                }
            }
            counterDao.insertAllUnreadLocations(countersToUpdate);
        }
        AppUtil.postEventOnUi(new RefreshDrawerEvent());
    }

    @Override
    public void onRun() throws Throwable {
        try {
            List<String> messageIds = new ArrayList<>(mMessageIds);
            getApi().markMessageAsRead(new IDList(messageIds));
        } catch (Exception e) {
            // noop
        }
    }
}
