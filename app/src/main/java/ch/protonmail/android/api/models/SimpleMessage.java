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
package ch.protonmail.android.api.models;

import android.text.TextUtils;

import java.util.List;

import ch.protonmail.android.api.models.room.messages.Message;

public class SimpleMessage {

    private final Long messageDbId;
    private final String messageID;
    private final String messageTitle;
    private final String senderName;
    private final String recipientList;
    private final long time;
    private final long timeMs;
    private final int location;
    private final boolean isRead;
    private final boolean isEncrypted;
    private final boolean isStarred;
    private final boolean hasAttachment;
    private final long expirationTime;
    private final List<String> labelIDs;
    private boolean isReplied;
    private boolean isRepliedAll;
    private boolean isForwarded;

    public SimpleMessage(Message message) {
        messageID = message.getMessageId();
        messageTitle = message.getSubject();
        time = message.getTime();
        timeMs = message.getTimeMs();
        String sender = message.getSenderName();
        sender = TextUtils.isEmpty(sender) ? message.getSenderEmail() : sender;
        senderName = sender;
        recipientList = message.getToListString();
        isRead = message.isRead();
        isEncrypted = message.isEncrypted();
        final Boolean starred = message.isStarred();
        isStarred = starred != null && starred;
        hasAttachment = message.getNumAttachments() >= 1;
        isReplied = message.isReplied()!=null&& message.isReplied();
        isRepliedAll = message.isRepliedAll() != null &&message.isRepliedAll();
        isForwarded = message.isForwarded() != null &&message.isForwarded();
        location = message.getLocation();
        expirationTime = message.getExpirationTime();
        labelIDs = message.getAllLabelIDs();
        messageDbId = message.getDbId();
    }

    public String getMessageId() {
        return messageID;
    }

    public String getMessageTitle() {
        return messageTitle;
    }

    public long getTime() {
        return time;
    }

    public long getTimeMs() {
        return timeMs;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getRecipientList() {
        return recipientList;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean isEncrypted() {
        return isEncrypted;
    }

    public boolean isStarred() {
        return isStarred;
    }

    public boolean hasAttachment() {
        return hasAttachment;
    }

    public boolean isReplied() {
        return isReplied;
    }

    public boolean isRepliedAll() {
        return isRepliedAll;
    }

    public boolean isForwarded() {
        return isForwarded;
    }

    public int getLocation() {
        return location;
    }

    public long getExpirationTime() {
        return expirationTime;
    }

    public void setIsReplied(boolean isReplied) {
        this.isReplied = isReplied;
    }

    public void setIsRepliedAll(boolean isRepliedAll) {
        this.isRepliedAll = isRepliedAll;
    }

    public void setIsForwarded(boolean isForwarded) {
        this.isForwarded = isForwarded;
    }

    public List<String> getLabelIDs() {
        return labelIDs;
    }

    public Long getMessageDbId() {
        return messageDbId;
    }
}
