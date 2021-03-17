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

import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.FetchDraftDetailEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

public class FetchDraftDetailJob extends ProtonMailBaseJob {

    private static final String TAG_FETCH_DRAFT_DETAIL_JOB = "FetchDraftDetailJob";

    private final String mMessageId;

    public FetchDraftDetailJob(final String messageId) {
        super(new Params(Priority.LOW).requireNetwork().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageId = messageId;
    }

    @Override
    public void onRun() throws Throwable {
        if (!getQueueNetworkUtil().isConnected()) {
            Logger.doLog(TAG_FETCH_DRAFT_DETAIL_JOB, "no network - cannot fetch draft detail");
            AppUtil.postEventOnUi(new FetchDraftDetailEvent(false));
            return;
        }

        try {
            final Message message = getApi().fetchMessageDetailsBlocking(mMessageId).getMessage();
            Message savedMessage = getMessageDetailsRepository().findMessageByIdBlocking(message.getMessageId());
            if (savedMessage != null) {
                message.setInline(savedMessage.isInline());
            }
            message.setDownloaded(true);
            long messageDbId = getMessageDetailsRepository().saveMessageInDB(message);
            final FetchDraftDetailEvent event = new FetchDraftDetailEvent(true);
            // we need to re-query MessageRepository, because after saving, messageBody may be replaced with uri to file
            event.setMessage(getMessageDetailsRepository().findMessageByMessageDbIdBlocking(messageDbId));
            AppUtil.postEventOnUi(event);
        } catch (Exception e) {
            AppUtil.postEventOnUi(new FetchDraftDetailEvent(false));
            throw e;
        }
    }

    @Override
    protected int getRetryLimit() {
        return 0;
    }
}
