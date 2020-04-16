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

import android.util.Base64;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dino on 3/15/18.
 */

public class MessageSendKey implements Serializable {

    @SerializedName(Fields.Message.Send.KEY)
    private String key; // <base64_encoded_session_key>
    @SerializedName(Fields.Message.Send.ALGORITHM)
    private String algorithm; // algorithm corresponding to session key

    public MessageSendKey(String algorithm, String key) {
        this.algorithm = algorithm;
        this.key = key;
    }

    public MessageSendKey(String algorithm, byte[] key) {
        this(algorithm, Base64.encodeToString(key, Base64.NO_WRAP));
    }

    public String getKey() {
        return key;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
