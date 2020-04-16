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

import java.io.Serializable;
import java.util.Map;

import ch.protonmail.android.api.models.Auth;
import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dino on 3/15/18.
 */

public class MessageSendAddressBody implements Serializable {

    @SerializedName(Fields.Message.Send.TYPE)
    private int type; // ProtonMail internal = 1, 2 = EO, 4 = Cleartext inline, 8 = Inline PGP
    @SerializedName(Fields.Message.Send.BODY_KEY_PACKET)
    private String bodyKeyPacket; // <base64_encoded_key_packet>
    @SerializedName(Fields.Message.Send.ATTACHMENT_KEY_PACKETS)
    private Map<String, String> attachmentKeyPackets; // optional if no attachments, <base64_encoded_key_packet>
    @SerializedName(Fields.Message.Send.PASSWORD_HINT)
    private String passwordHint;
    @SerializedName(Fields.Message.Send.SIGNATURE)
    private int signature; // 1 = attachment signatures
    @SerializedName(Fields.Message.Send.TOKEN)
    private String token;
    @SerializedName(Fields.Message.Send.ENC_TOKEN)
    private String encToken;
    @SerializedName(Fields.Message.Send.AUTH)
    private Auth auth;

    public void setType(int type) {
        this.type = type;
    }

    public int getType() {
        return this.type;
    }

    public void setBodyKeyPacket(String bodyKeyPacket) {
        this.bodyKeyPacket = bodyKeyPacket;
    }

    public void setAttachmentKeyPackets(Map<String, String> attachmentKeyPackets) {
        this.attachmentKeyPackets = attachmentKeyPackets;
    }

    public void setPasswordHint(String hint) {
        passwordHint = hint;
    }

    public void setSignature(int signature) {
        this.signature = signature;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setEncToken(String encToken) {
        this.encToken = encToken;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }
}
