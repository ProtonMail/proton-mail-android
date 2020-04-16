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
package ch.protonmail.android.utils.MIME;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import ch.protonmail.android.api.models.room.messages.Attachment;
import ch.protonmail.android.core.Constants;

/**
 * Created by kaylukas on 27/04/2018.
 */

public class MIMEParser {

    private String messageId;
    private List<Attachment> attachments;
    private String html;
    private String plaintext;

    public MIMEParser(String messageId) {
        this.messageId = messageId;
    }

    public MIMEParser() {
        this("");
    }


    public String getBody() {
        return html != null ? html : plaintext;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public String MIMEType() {
        return html != null ? Constants.MIME_TYPE_HTML : Constants.MIME_TYPE_PLAIN_TEXT;
    }

    public void parse(String mime) throws NoSuchAlgorithmException, MessagingException, IOException {
        InputStream dataInputStream = new ByteArrayInputStream(mime.getBytes());
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(System.getProperties()), dataInputStream);
        html = null;
        plaintext = null;
        ArrayList<Attachment> attachmentList = new ArrayList<>();
        parsePart(attachmentList, mimeMessage);
        attachments = Collections.unmodifiableList(attachmentList);
    }

    private void parseMultipart(Multipart content, List<Attachment> attachmentList) throws MessagingException, IOException, NoSuchAlgorithmException {
        for (int i = 0; i < content.getCount(); i++) {
            BodyPart part = content.getBodyPart(i);
            parsePart(attachmentList, part);
        }
    }

    private void parsePart(List<Attachment> attachmentList, Part part) throws MessagingException, IOException, NoSuchAlgorithmException {
        if (part.isMimeType(Constants.MIME_TYPE_HTML)) {
            html = (String) part.getContent();
            return;
        }
        if (part.isMimeType(Constants.MIME_TYPE_PLAIN_TEXT)) {
            plaintext = (String) part.getContent();
            return;
        }
        if (part.getContentType().contains("multipart")) {
            parseMultipart((Multipart) part.getContent(), attachmentList);
            return;
        }
        Attachment attachment = Attachment.Companion.fromMimeAttachment(part, messageId, attachmentList.size());
        if (attachment == null) {
            return;
        }
        attachmentList.add(attachment);
    }
}
