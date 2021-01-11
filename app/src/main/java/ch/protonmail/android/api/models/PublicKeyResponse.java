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

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;

public class PublicKeyResponse extends ResponseBody {
    @SerializedName(Fields.Keys.RECIPIENT_TYPE)
    private int recipientTypeInt;
    @SerializedName(Fields.Keys.MIME_TYPE)
    private String mimeType;
    @SerializedName(Fields.Keys.KEYS)
    private PublicKeyBody[] keys;

    public PublicKeyResponse(int recipientType, String mimeType, PublicKeyBody[] keys) {
        this.recipientTypeInt = recipientType;
        this.mimeType = mimeType;
        this.keys = keys;
    }

    public boolean hasError() {
        return getCode() != Constants.RESPONSE_CODE_OK;
    }

    public RecipientType getRecipientType() {
        return RecipientType.FromInt(recipientTypeInt);
    }

    public String getMIMEType() {
        return mimeType;
    }

    public PublicKeyBody[] getKeys() {
        return keys;
    }

    public enum RecipientType {
        INTERNAL(1), EXTERNAL(2);

        private int value;
        RecipientType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public static RecipientType FromInt(int value) {
            RecipientType[] types = RecipientType.values();
            if (value > 0 && value <= types.length) {
                return types[value - 1];
            }
            return null;
        }
    }
}
