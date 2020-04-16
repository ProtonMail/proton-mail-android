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

/**
 * Created by sunny on 8/12/15.
 */
public class AttachmentFailedEvent {
    private final String messageId;
    private final String messageSubject;
    private final String attachmentName;

    public AttachmentFailedEvent(String messageId, String messageSubject, String attachmentName){
        this.messageId = messageId;
        this.attachmentName = attachmentName;
        this.messageSubject = messageSubject;
    }

    public String getMessageId(){
        return messageId;
    }

    public String getMessageSubject() {
        return messageSubject;
    }

    public String getAttachmentName() {
        return attachmentName;
    }
}
