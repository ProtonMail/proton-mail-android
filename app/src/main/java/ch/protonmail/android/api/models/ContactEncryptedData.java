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

import java.io.Serializable;

import ch.protonmail.android.api.models.enumerations.ContactEncryption;
import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;

/**
 * Created by dkadrikj on 11/23/16.
 */

public class ContactEncryptedData implements Serializable {

    @SerializedName(Fields.Contact.TYPE)
    private int type;
    @SerializedName(Fields.Contact.DATA)
    private String data;
    @SerializedName(Fields.Contact.SIGNATURE)
    private String signature;

    public ContactEncryptedData(String data, String signature, Constants.VCardType type) {
        this.data = data;
        this.signature = signature;
        this.type = type.getVCardTypeValue();
    }

    public String getData() {
        return data;
    }

    public int getType() {
        return type;
    }

    public ContactEncryption getEncryptionType() {
        return ContactEncryption.fromInteger(type);
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }
}
