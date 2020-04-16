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

import ch.protonmail.android.api.models.LabelBody;
import ch.protonmail.android.api.models.messages.receive.LabelResponse;
import ch.protonmail.android.api.models.room.messages.Label;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.LabelAddedEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 17.7.15.
 */
public class PostLabelJob extends ProtonMailEndlessJob {

    private final String labelName;
    private final String color;
    private final int display;
    private final int exclusive;
    private final boolean update;
    private final String labelId;

    public PostLabelJob(String labelName, String color, int display, int exclusive, boolean update, String labelId) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_LABEL));
        this.labelName = labelName;
        this.color = color;
        this.display = display;
        this.exclusive = exclusive;
        this.update = update;
        this.labelId = labelId;
    }

    @Override
    public void onRun() throws Throwable {
        final MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        LabelResponse labelResponse;
        if (!update) {
            labelResponse = mApi.createLabel(new LabelBody(labelName, color, display, exclusive));
        } else {
            labelResponse = mApi.updateLabel(labelId, new LabelBody(labelName, color, display, exclusive));
        }
        if (labelResponse.hasError()) {
            final String errorText = labelResponse.getError();
            AppUtil.postEventOnUi(new LabelAddedEvent(Status.FAILED, errorText));
            return;
        }

        Label labelBody = labelResponse.getLabel();
        if (labelBody == null) {
            // we have no label response, checking for error
            AppUtil.postEventOnUi(new LabelAddedEvent(Status.FAILED, labelResponse.getError()));
            return;
        }
        String labelId = labelBody.getId();
        // update local label
        if (!labelId.equals("")) {
            messagesDatabase.saveLabel(labelBody);
            AppUtil.postEventOnUi(new LabelAddedEvent(Status.SUCCESS, null));
        }
    }
}
