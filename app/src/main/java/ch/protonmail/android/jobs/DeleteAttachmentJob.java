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
import ch.protonmail.android.api.models.room.messages.Attachment;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.core.Constants;

import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_ATTACHMENT_DELETE_ID_INVALID;

public class DeleteAttachmentJob extends ProtonMailBaseJob {

    private final String mAttachmentId;

    public DeleteAttachmentJob(String attachmentId) {
        super(new Params(Priority.MEDIUM).requireNetwork());
        this.mAttachmentId = attachmentId;
    }

    @Override
    public void onRun() throws Throwable {
        final MessagesDatabase messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(
                getApplicationContext()).getDatabase();
        ResponseBody response = mApi.deleteAttachment(mAttachmentId);
        final Attachment attachment = messagesDatabase.findAttachmentById(mAttachmentId);
        if (attachment == null) {
            return;
        }
        if (response.getCode() == Constants.RESPONSE_CODE_OK || response.getCode() == RESPONSE_CODE_ATTACHMENT_DELETE_ID_INVALID) {
            messagesDatabase.deleteAttachment(attachment);
        }
    }
}
