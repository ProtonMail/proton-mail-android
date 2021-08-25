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
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLocationCounter;
import ch.protonmail.android.labels.data.LabelRepository;
import ch.protonmail.android.labels.data.db.LabelEntity;
import ch.protonmail.android.labels.data.model.LabelId;
import ch.protonmail.android.labels.data.model.LabelType;
import timber.log.Timber;

public class PostTrashJobV2 extends ProtonMailCounterJob {

    private final List<String> mMessageIds;
    private final List<String> mFolderIds;
    private final String mLabelId;
    private final LabelRepository labelRepository;

    public PostTrashJobV2(final List<String> messageIds, String labelId, LabelRepository labelRepo) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
        labelRepository = labelRepo;
        mFolderIds = null;
        mLabelId = labelId;
    }

    public PostTrashJobV2(final List<String> messageIds, List<String> folderIds, String labelId, LabelRepository labelRepo) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
        mFolderIds = folderIds;
        mLabelId = labelId;
        labelRepository = labelRepo;
    }

    @Override
    public void onAdded() {
        Timber.v("Post to Trash ids: %s onAdded", mMessageIds);
        final CounterDao counterDao = CounterDatabase.Companion
                .getInstance(getApplicationContext(), getUserId())
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
                    message.addLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.ALL_SENT.getMessageLocationTypeValue())));
                    message.removeLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.SENT.getMessageLocationTypeValue())));
                } else {
                    message.setLocation(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue());
                    message.addLabels(Collections.singletonList(String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue())));
                    if (!TextUtils.isEmpty(mLabelId)) {
                        message.removeLabels(Collections.singletonList(mLabelId));
                    }
                    removeOldFolderIds(message);
                }
                if (mFolderIds != null) {
                    for (String folderId : mFolderIds) {
                        if (!TextUtils.isEmpty(folderId)) {
                            message.removeLabels(Collections.singletonList(folderId));
                        }
                    }
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

    }

    private void removeOldFolderIds(Message message) {
        List<String> oldLabels = message.getAllLabelIDs();
        ArrayList<String> labelsToRemove = new ArrayList<>();

        for (String labelId : oldLabels) {
            LabelEntity label = labelRepository.findLabelBlocking(new LabelId(labelId));
            // find folders
            if (label != null && (label.getType() == LabelType.FOLDER) && !label.getId().equals(String.valueOf(Constants.MessageLocationType.TRASH.getMessageLocationTypeValue()))) {
                labelsToRemove.add(labelId);
            }
        }

        message.removeLabels(labelsToRemove);
    }

    @Override
    public void onRun() {
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
