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
package ch.protonmail.android.api.models.messages;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.utils.FileUtils;

/**
 * Created by kaylukas on 24/05/2018.
 */

public class ParsedHeaders implements Serializable {
    @SerializedName(Fields.Message.ParsedHeaders.RECIPIENT_ENCRYPTION)
    private String recipientEncryption;
    @SerializedName(Fields.Message.ParsedHeaders.RECIPIENT_AUTHENTICATION)
    private String recipientAuthentication;

    public ParsedHeaders() {
    }

    public ParsedHeaders(String recipientEncryption, String recipientAuthentication) {
        this.recipientEncryption = recipientEncryption;
        this.recipientAuthentication = recipientAuthentication;
    }

    public String serialize() {
        return FileUtils.toString(this);
    }

    @NonNull
    public Map<String, String> getRecipientEncryption() {
        return extractMap(recipientEncryption);
    }

    @NonNull
    public Map<String, String> getRecipientAuthentication() {
        return extractMap(recipientAuthentication);
    }
    @NonNull
    private static Map<String, String> extractMap(String encodedString) {
        if (encodedString == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new HashMap<>();
        String[] elements = encodedString.split(";");
        for (String element : elements) {
            String[] emailAndStatus = element.split("=");
            if(emailAndStatus.length != 2) {
                continue;
            }
            try {
                String email = URLDecoder.decode(emailAndStatus[0], "UTF-8").trim();
                String status = emailAndStatus[1];
                map.put(email, status);
            } catch (UnsupportedEncodingException e) {
                continue;
            }
        }
        return Collections.unmodifiableMap(map);
    }

    public static ParsedHeaders deserialize(String s) {
        return FileUtils.deserializeStringToObject(s);
    }
}
