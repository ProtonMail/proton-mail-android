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
package ch.protonmail.android.events;

import java.io.Serializable;

import ch.protonmail.android.api.models.room.messages.Message;

/**
 * Created by sunny on 8/12/15.
 */
public class DraftCreatedEvent implements Serializable {
    private String messageId;
    private String oldMessageId;
    private Status status;
    private Message message;

    public DraftCreatedEvent(String messageId, String oldMessageId, Message message){
        this.messageId = messageId;
        this.oldMessageId = oldMessageId;
        this.message = message;
        this.status = Status.SUCCESS;
    }

    public DraftCreatedEvent(String messageId, String oldMessageId, Message message, Status status) {
        this(messageId, oldMessageId, message);
        this.status = status;
    }

    public String getMessageId(){
        return messageId;
    }

    public Status getStatus() {
        return status;
    }

    public String getOldMessageId() {
        return oldMessageId;
    }

    public Message getMessage() {
        return message;
    }
}
