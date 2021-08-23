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
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.data.local.CounterDao;
import ch.protonmail.android.data.local.CounterDatabase;
import ch.protonmail.android.data.local.MessageDao;
import ch.protonmail.android.data.local.MessageDatabase;
import ch.protonmail.android.data.local.model.LabelEntity;
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLocationCounter;
import timber.log.Timber;

public class PostInboxJob extends ProtonMailCounterJob {

    private final List<String> mMessageIds;
    private final List<String> mFolderIds;

    public PostInboxJob(final List<String> messageIds) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist());
        mMessageIds = messageIds;
        mFolderIds = null;
    }

    public PostInboxJob(final List<String> messageIds, List<String> folderIds) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist());
        mMessageIds = messageIds;
        mFolderIds = folderIds;
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
                Timber.d("Post to INBOX message: %s", message.getMessageId());
                if (!message.isRead()) {
                    UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(message.getLocation());
                    if (unreadLocationCounter != null) {
                        unreadLocationCounter.decrement();
                        counterDao.insertUnreadLocation(unreadLocationCounter);
                    }
                    totalUnread++;
                }
                message.addLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.INBOX.getMessageLocationTypeValue())));
                if (mFolderIds != null) {
                    for (String folderId : mFolderIds) {
                        if (!TextUtils.isEmpty(folderId)) {
                            message.removeLabels(Collections.singletonList(folderId));
                        }
                    }
                }
                removeOldFolderIds(message);
                getMessageDetailsRepository().saveMessageBlocking(message);
            }
        }

        UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(Constants.MessageLocationType.INBOX.getMessageLocationTypeValue());
        if (unreadLocationCounter == null) {
            return;
        }
        unreadLocationCounter.increment(totalUnread);
        counterDao.insertUnreadLocation(unreadLocationCounter);
    }

    private void removeOldFolderIds(Message message) {
        List<String> oldLabels = message.getAllLabelIDs();
        ArrayList<String> labelsToRemove = new ArrayList<>();

        MessageDao messageDao = MessageDatabase.Factory
                .getInstance(getApplicationContext(), getUserId())
                .getDao();

        for (String labelId : oldLabels) {
            LabelEntity label = messageDao.findLabelByIdBlocking(labelId);
            // find folders
            if (label != null &&
                    (label.getType() == Constants.LABEL_TYPE_MESSAGE_FOLDERS) &&
                    !label.getId().equals(String.valueOf(Constants.MessageLocationType.INBOX.getMessageLocationTypeValue()))
            ) {
                labelsToRemove.add(labelId);
            }
        }

        message.removeLabels(labelsToRemove);
    }

    @Override
    public void onRun() {
        getApi().labelMessages(new IDList(String.valueOf(Constants.MessageLocationType.INBOX.getMessageLocationTypeValue()), mMessageIds));
    }

    @Override
    protected List<String> getMessageIds() {
        return mMessageIds;
    }

    @Override
    protected Constants.MessageLocationType getMessageLocation() {
        return Constants.MessageLocationType.INBOX;
    }
}
