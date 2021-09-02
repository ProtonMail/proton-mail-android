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
import ch.protonmail.android.data.local.model.Message;
import ch.protonmail.android.data.local.model.UnreadLocationCounter;
import ch.protonmail.android.labels.data.LabelRepository;
import ch.protonmail.android.labels.data.local.model.LabelEntity;
import ch.protonmail.android.labels.data.local.model.LabelId;
import ch.protonmail.android.labels.data.local.model.LabelType;
import timber.log.Timber;

public class MoveToFolderJob extends ProtonMailBaseJob {
    private List<String> mMessageIds;
    private String mLabelId;
    private final LabelRepository labelRepository;

    public MoveToFolderJob(List<String> messageIds, String labelId, LabelRepository labelRepository) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL));
        this.mMessageIds = messageIds;
        this.mLabelId = labelId;
        this.labelRepository = labelRepository;
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
                if (markMessageLocally(counterDao, message)) {
                    totalUnread++;
                }
            }
        }

        UnreadLocationCounter unreadLocationCounter = counterDao.findUnreadLocationById(Constants.MessageLocationType.SPAM.getMessageLocationTypeValue());
        if (unreadLocationCounter == null) {
            return;
        }
        unreadLocationCounter.increment(totalUnread);
        counterDao.insertUnreadLocation(unreadLocationCounter);
    }

    private boolean markMessageLocally(CounterDao counterDao, Message message) {
        boolean unreadIncrease = false;

        MessageDao messageDao = MessageDatabase.Factory
                .getInstance(getApplicationContext(), getUserId())
                .getDao();

        if (!TextUtils.isEmpty(mLabelId)) {
            message.addLabels(Collections.singletonList(mLabelId));
            removeOldFolderIds(message);
        }

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
            message.setLocation(Constants.MessageLocationType.LABEL_FOLDER.getMessageLocationTypeValue());
        }

        message.setFolderLocation(labelRepository);
        Timber.d("Move message id: %s, location: %s, labels: %s", message.getMessageId(), message.getLocation(), message.getAllLabelIDs());
        getMessageDetailsRepository().saveMessageBlocking(message);
        return unreadIncrease;
    }

    private void removeOldFolderIds(Message message) {
        List<String> oldLabels = message.getAllLabelIDs();
        ArrayList<String> labelsToRemove = new ArrayList<>();

        for (String labelId : oldLabels) {
            LabelEntity label = labelRepository.findLabelBlocking(new LabelId(labelId));
            // find folders
            if (label != null && (label.getType() == LabelType.FOLDER) && !label.getId().equals(mLabelId)) {
                labelsToRemove.add(labelId);
            }
        }

        message.removeLabels(labelsToRemove);
    }

    @Override
    public void onRun() throws Throwable {
        IDList body = new IDList(mLabelId, mMessageIds);
        getApi().labelMessages(body);
    }
}
