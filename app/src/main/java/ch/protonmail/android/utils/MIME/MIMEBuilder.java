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

import android.util.Base64;

import org.apache.commons.lang3.StringEscapeUtils;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;

import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.crypto.AddressCrypto;
import ch.protonmail.android.crypto.CipherText;
import ch.protonmail.android.data.local.model.Attachment;
import ch.protonmail.android.data.local.model.AttachmentHeaders;
import ch.protonmail.android.utils.HTMLToMDConverter;
import ch.protonmail.android.utils.crypto.BinaryDecryptionResult;
import timber.log.Timber;

public class MIMEBuilder {

    private String html;
    private String plaintext;
    private List<Attachment> attachments;
    private ProtonMailApiManager api;
    private AddressCrypto crypto;


    public MIMEBuilder(ProtonMailApiManager api, AddressCrypto crypto) {
        this.loadPlaintext("")
            .loadAttachments(Collections.<Attachment>emptyList());
        this.api = api;
        this.crypto = crypto;
    }

    public MIMEBuilder loadPlaintext(String plaintext) {
        if (plaintext == null) {
            return this;
        }
        this.plaintext = plaintext;
        this.html = null;
        return this;
    }

    public MIMEBuilder loadHTML(String html) {
        if (html == null) {
            return this;
        }
        this.html = html;
        HTMLToMDConverter html2MD = new HTMLToMDConverter();
        this.plaintext = "";
        // FIXME: this is work around. the library Remark throws stack overflow exception for some emails.
        try {
            this.plaintext = html2MD.convert(html);
        } catch (Exception e) {
            // noop
        }
        return this;
    }

    public MIMEBuilder loadAttachments(List<Attachment> attachments) {
        this.attachments = attachments;
        return this;
    }

    public String buildString() {
        try {
            MIMEPart multipart = new MIMEPart("mixed");
            multipart.addBodyPart(buildBody());
            List<Attachment> unrelatedAttachments = html != null ? getAttachments(false) : attachments;
            for (Attachment attachment : unrelatedAttachments) {
                multipart.addBodyPart(buildAttachment(attachment));
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            multipart.writeHeaders(baos);
            multipart.writeTo(baos);
            baos.close();
            return baos.toString();
        } catch (Exception e) {
            Timber.e(e, "Build string error");
            return "";
        }
    }

    private BodyPart buildBody() throws Exception {
        if (html != null) {
            MIMEPart alternative = new MIMEPart("alternative");
            alternative.addBodyPart(buildPlainBody());
            alternative.addBodyPart(buildHtmlBody());
            return alternative.asBodyPart();
        }
        return buildPlainBody();
    }

    private BodyPart buildHtmlBody() throws Exception {
        MimeBodyPart htmlBody = new MimeBodyPart();
        htmlBody.setText(html, "utf-8", "html");
        htmlBody.setHeader("Content-Transfer-Encoding", "base64");

        List<Attachment> relatedAttachments = getAttachments(true);

        MIMEPart multipart = new MIMEPart("related");
        multipart.addBodyPart(htmlBody);
        for (Attachment attachment : relatedAttachments) {
            multipart.addBodyPart(buildAttachment(attachment));
        }
        return multipart.asBodyPart();
    }

    private List<Attachment> getAttachments(boolean related) {
        List<Attachment> output = new ArrayList<>();
        for (Attachment attachment : attachments) {
            if (related == attachment.getHeaders().getContentDisposition().contains("inline")) {
                output.add(attachment);
            }
        }
        return output;
    }

    private BodyPart buildAttachment(Attachment attachment) throws Exception {
        AttachmentHeaders attachmentHeaders = attachment.getHeaders();
        InternetHeaders headers = new InternetHeaders();
        String filename = StringEscapeUtils.escapeJson(attachment.getFileName());
        for (String value : attachmentHeaders.getContentDisposition()) {
            headers.addHeader("Content-Disposition", value + "; filename=\"" + filename + "\"");
        }
        if (attachmentHeaders.getContentDisposition().size() == 0) {
            headers.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        }
        String contentLocation = attachmentHeaders.getContentLocation();
        if (contentLocation != null && contentLocation.length() > 0) {
            headers.addHeader("Content-Location", contentLocation);
        }
        String contentType = attachment.getMimeType();
        if (contentType != null && contentType.length() > 0) {
            headers.addHeader("Content-Type", contentType + "; name=\"" + filename + "\"");
        }
        String contentId = attachmentHeaders.getContentId();
        if (contentId != null && contentId.length() > 0) {
            headers.addHeader("Content-Id", contentId);
        }
        byte[] data = attachment.getMimeData();
        if (data == null) {
            byte[] keyBytes = Base64.decode(attachment.getKeyPackets(), Base64.DEFAULT);
            byte[] dataBytes = api.downloadAttachmentBlocking(attachment.getAttachmentId());
            CipherText message = new CipherText(keyBytes, dataBytes);
            BinaryDecryptionResult result = crypto.decryptAttachment(message);
            data = result.getDecryptedData();
        }
        MimeBodyPart attachmentPart = new MimeBodyPart(headers, Base64.encode(data, Base64.NO_WRAP));
        attachmentPart.setHeader("Content-Transfer-Encoding", "base64");
        return attachmentPart;
    }

    private BodyPart buildPlainBody() throws MessagingException {
        MimeBodyPart plainBody = new MimeBodyPart();
        plainBody.setText(plaintext, "utf-8");
        plainBody.setHeader("Content-Transfer-Encoding", "quoted-printable");
        return plainBody;
    }

}
