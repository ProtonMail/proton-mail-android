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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.api.models.room.counters.CounterDao;
import ch.protonmail.android.api.models.room.counters.CounterDatabase;
import ch.protonmail.android.api.models.room.counters.UnreadLocationCounter;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.RefreshDrawerEvent;
import ch.protonmail.android.utils.AppUtil;

public class PostTrashJobV2 extends ProtonMailCounterJob {

    private final List<String> mMessageIds;
    private final List<String> mFolderIds;
    private final String mLabelId;

    public PostTrashJobV2(final List<String> messageIds, String labelId) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
        mFolderIds = null;
        mLabelId = labelId;
    }

    public PostTrashJobV2(final List<String> messageIds, List<String> folderIds, String labelId) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
        mFolderIds = folderIds;
        mLabelId = labelId;
    }

    @Override
    public void onAdded() {
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext())
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
                    if (!TextUtils.isEmpty(mLabelId)) {
                        message.removeLabels(Collections.singletonList(mLabelId));
                    }
                    message.setLocation(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue());

                }
                if (mFolderIds != null) {
                    for (String folderId : mFolderIds) {
                        if (!TextUtils.isEmpty(folderId)) {
                            message.removeLabels(Collections.singletonList(folderId));
                        }
                    }
                }
                getMessageDetailsRepository().saveMessageInDB(message);
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
        List<String> messageIds = new ArrayList<>(mMessageIds);
        getApi().labelMessages(new IDList(String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue()), messageIds));
    }

    @Override
    protected List<String> getMessageIds() {
        return new ArrayList<>(mMessageIds);
    }

    @Override
    protected Constants.MessageLocationType getMessageLocation() {
        return Constants.MessageLocationType.TRASH;
    }
}
