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
package ch.protonmail.android.api.models.address;

import androidx.annotation.Nullable;

/**
 * Created by dkadrikj on 12/4/16.
 */

public class AddressPrivateKey {

    private String AddressID;
    private String PrivateKey;
    @Nullable
    private String Token;
    @Nullable
    private String Signature;
    @Nullable
    private SignedKeyList SignedKeyList;

    public AddressPrivateKey(String addressID, String privateKey) {
        AddressID = addressID;
        PrivateKey = privateKey;
    }

    public void setToken(@Nullable String token) {
        Token = token;
    }

    public void setSignature(@Nullable String signature) {
        Signature = signature;
    }

    public void setSignedKeyList(@Nullable SignedKeyList signedKeyList) {
        SignedKeyList = signedKeyList;
    }
}
