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

import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.PostPhishingReportEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

public class ReportPhishingJob extends ProtonMailEndlessJob {

    private final String messageId;
    private final String body;
    private final String mimeType;

    public ReportPhishingJob(final Message message) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist());
        this.messageId = message.getMessageId();
        this.body = message.getDecryptedBody();
        this.mimeType = message.getMimeType();
    }

    @Override
    public void onAdded() {
        super.onAdded();
        if (!getQueueNetworkUtil().isConnected()) {
            AppUtil.postEventOnUi(new PostPhishingReportEvent(Status.NO_NETWORK));
        }
    }

    @Override
    public void onRun() throws Throwable {
        ResponseBody response = getApi().postPhishingReport(messageId, body, mimeType);
        if (response != null && response.getCode() == Constants.RESPONSE_CODE_OK) {
            AppUtil.postEventOnUi(new PostPhishingReportEvent(Status.SUCCESS));
        } else {
            AppUtil.postEventOnUi(new PostPhishingReportEvent(Status.FAILED));
        }
    }
}
