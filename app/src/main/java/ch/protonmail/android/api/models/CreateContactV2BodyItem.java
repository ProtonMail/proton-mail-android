/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.api.models;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;

/**
 * Created by dkadrikj on 8/28/16.
 */
public class CreateContactV2BodyItem {

    @SerializedName(Fields.Contact.CARDS)
    private List<ContactEncryptedData> encryptedData;

    public CreateContactV2BodyItem(String signedData, String signedDataSignature, String encryptedData, String encryptedDataSignature) {
        List<ContactEncryptedData> encryptedDataList = new ArrayList<>();
        ContactEncryptedData contactSignedData = new ContactEncryptedData(signedData, signedDataSignature, Constants.VCardType.SIGNED);
        encryptedDataList.add(contactSignedData);
        if (encryptedData != null) {
            ContactEncryptedData contactEncryptedData = new ContactEncryptedData(encryptedData, encryptedDataSignature, Constants.VCardType.SIGNED_ENCRYPTED);
            encryptedDataList.add(contactEncryptedData);
        }
        this.encryptedData = encryptedDataList;
    }
}
