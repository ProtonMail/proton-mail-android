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

import ch.protonmail.android.api.models.room.counters.CounterDao;
import ch.protonmail.android.api.models.room.counters.CounterDatabase;
import ch.protonmail.android.api.models.room.counters.UnreadLocationCounter;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.RefreshDrawerEvent;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 11/20/15.
 */
public abstract class ProtonMailCounterJob extends ProtonMailEndlessJob {

    protected ProtonMailCounterJob(Params params) {
        super(params);
    }

    protected abstract List<String> getMessageIds();

    protected abstract Constants.MessageLocationType getMessageLocation();

    @Override
    protected void onProtonCancel(int cancelReason, @Nullable Throwable throwable) {
        final CounterDao counterDao = CounterDatabase.Companion.getInstance(
                getApplicationContext()).getDao();

        int totalUnread = 0;
        List<String> messageIds = getMessageIds();
        for (String id : messageIds) {
            final Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                if ( !message.isRead() ) {
                    UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(message.getLocation());
                    if (unreadLocationCounter != null) {
                        unreadLocationCounter.increment();
                        counterDao.insertUnreadLocation(unreadLocationCounter);
                    }
                    totalUnread++;
                }
            }
        }
        UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(getMessageLocation().getMessageLocationTypeValue());

        if (unreadLocationCounter == null) {
            return;
        }
        unreadLocationCounter.decrement(totalUnread);
        counterDao.insertUnreadLocation(unreadLocationCounter);
        AppUtil.postEventOnUi(new RefreshDrawerEvent());
    }
}
