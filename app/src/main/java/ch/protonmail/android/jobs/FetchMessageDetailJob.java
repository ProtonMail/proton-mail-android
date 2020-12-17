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

import ch.protonmail.android.api.models.messages.receive.MessageResponse;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.FetchMessageDetailEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

public class FetchMessageDetailJob extends ProtonMailBaseJob {

    private static final String TAG_FETCH_MESSAGE_DETAIL_JOB = "FetchMessageDetailJob";

    private final String mMessageId;

    public FetchMessageDetailJob(final String messageId) {
        super(new Params(Priority.MEDIUM).requireNetwork().groupBy(Constants.JOB_GROUP_MESSAGE));
        mMessageId = messageId;
    }

    @Override
    public void onRun() throws Throwable {
        final MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(
                getApplicationContext()).getDatabase();
        if (!getQueueNetworkUtil().isConnected()) {
            Logger.doLog(TAG_FETCH_MESSAGE_DETAIL_JOB, "no network cannot fetch message detail");
            AppUtil.postEventOnUi(new FetchMessageDetailEvent(false, mMessageId));
            return;
        }

        try {
            final MessageResponse messageResponse = getApi().messageDetail(mMessageId);
            final Message message = messageResponse.getMessage();
            Message savedMessage = getMessageDetailsRepository().findMessageByIdBlocking(message.getMessageId());
            final FetchMessageDetailEvent event = new FetchMessageDetailEvent(true, mMessageId);
            if (savedMessage != null) {
                message.writeTo(savedMessage);
                getMessageDetailsRepository().saveMessageInDB(savedMessage);
                event.setMessage(savedMessage);
            } else {
                message.setToList(message.getToList());
                message.setCcList(message.getCcList());
                message.setBccList(message.getBccList());
                message.setReplyTos(message.getReplyTos());
                message.setSender(message.getSender());
                message.setLabelIDs(message.getEventLabelIDs());
                message.setHeader(message.getHeader());
                message.setParsedHeaders(message.getParsedHeaders());
                message.setDownloaded(message.isDownloaded());
                Constants.MessageLocationType location = Constants.MessageLocationType.INBOX;
                for (String labelId : message.getAllLabelIDs()) {
                    if (labelId.length() <= 2) {
                        location = Constants.MessageLocationType.Companion.fromInt(Integer.valueOf(labelId));
                        if (location != Constants.MessageLocationType.ALL_MAIL && location != Constants.MessageLocationType.STARRED) {
                            break;
                        }
                    }
                }
                message.setLocation(location.getMessageLocationTypeValue());
                message.setFolderLocation(messagesDatabase);
                getMessageDetailsRepository().saveMessageInDB(message);
                event.setMessage(message);
            }

            AppUtil.postEventOnUi(event);
        } catch (Exception error) {
            Logger.doLogException(TAG_FETCH_MESSAGE_DETAIL_JOB, "error while fetching message detail", error);
            AppUtil.postEventOnUi(new FetchMessageDetailEvent(false, mMessageId));
            throw error;
        }
    }

    @Override
    protected int getRetryLimit() {
        return 1;
    }
}
