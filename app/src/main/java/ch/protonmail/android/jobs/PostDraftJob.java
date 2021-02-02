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

import java.util.List;

import ch.protonmail.android.api.models.IDList;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.core.Constants;

public class PostDraftJob extends ProtonMailEndlessJob {

    private final List<String> mMessageIds;

    public PostDraftJob(final List<String> messageIds) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageIds = messageIds;
    }

    @Override
    public void onAdded() {
        //TODO make a bulk operation
        for (String id : mMessageIds) {
            final Message message = getMessageDetailsRepository().findMessageByIdBlocking(id);
            if (message != null) {
                message.setLocation(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue());
                getMessageDetailsRepository().saveMessageInDB(message);
            }
        }
    }

    @Override
    public void onRun() throws Throwable {
        getApi().labelMessages(new IDList(String.valueOf(Constants.MessageLocationType.DRAFT.getMessageLocationTypeValue()), mMessageIds));
    }
}
