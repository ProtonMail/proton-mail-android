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
package ch.protonmail.android.api.models.messages.send;

import com.google.gson.annotations.SerializedName;

import java.util.HashMap;
import java.util.Map;

import ch.protonmail.android.api.models.enumerations.PackageType;
import ch.protonmail.android.api.utils.Fields;

public class MessageSendPackage {
    @SerializedName(Fields.Message.Send.ADDRESSES)
    private Map<String, MessageSendAddressBody> addresses;
    @SerializedName(Fields.Message.Send.TYPE)
    private int type; // 8|4|2|1, all types sharing this package, a bitmask
    @SerializedName(Fields.Message.Send.BODY)
    private String Body; // <base64_encoded_openpgp_encrypted_data_packe>
    @SerializedName(Fields.Message.Send.MIME_TYPE)
    private String MIMEType;
    @SerializedName(Fields.Message.Send.BODY_KEY)
    private MessageSendKey bodyKey; // Include only if cleartext recipients
    @SerializedName(Fields.Message.Send.ATTACHMENT_KEYS)
    private Map<String, MessageSendKey> attachmentKeys;

    private transient Map<String, MessageSendKey> plainAttachmentKeys;
    private transient MessageSendKey plainBodyKey;

    public MessageSendPackage(String body, MessageSendKey bodyKey, Object mime, Map<String, MessageSendKey> attachmentKeys) {
        this.addresses = new HashMap<>();
        this.type = 0;
        this.Body = body;
        this.plainBodyKey = bodyKey;
        this.MIMEType = mime.toString();
        this.plainAttachmentKeys = attachmentKeys;
    }

    public void addAddress(String email, MessageSendAddressBody address) {
        this.addresses.put(email, address);
        this.type |= address.getType();
        if ((type & (PackageType.MIME.getValue() | PackageType.CLEAR.getValue())) != 0) {
            // expose plaintext keys
            this.attachmentKeys = this.plainAttachmentKeys;
            this.bodyKey = this.plainBodyKey;
        }
    }

    public Map<String, MessageSendKey> getAttachmentKeys() {
        return plainAttachmentKeys;
    }

    public MessageSendKey getBodyKey() {
        return plainBodyKey;
    }
}
