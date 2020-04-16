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
package ch.protonmail.android.events;

/**
 * Created by sunny on 9/9/15.
 */
public class ContactDetailsFetchedEvent {

    private final Status mStatus;
    private final String mDecryptedVCardType0;
    private final String mDecryptedVCardType2;
    private final String mDecryptedVCardType3;

    private final String mDecryptedVCardType2Signature;
    private final String mDecryptedVCardType3Signature;

    public ContactDetailsFetchedEvent(Status status, String decryptedVCardType0, String decryptedVCardType2, String decryptedVCardType3,
                                      String vCardType2Signature, String vCardType3Signature) {
        mStatus = status;
        mDecryptedVCardType0 = decryptedVCardType0;
        mDecryptedVCardType2 = decryptedVCardType2;
        mDecryptedVCardType3 = decryptedVCardType3;
        mDecryptedVCardType2Signature = vCardType2Signature;
        mDecryptedVCardType3Signature = vCardType3Signature;
    }

    public String getRawVCardType0() {
        return mDecryptedVCardType0;
    }

    public String getRawVCardType2() {
        return mDecryptedVCardType2;
    }

    public String getRawVCardType3() {
        return mDecryptedVCardType3;
    }

    public String getVCardType2Signature() {
        return mDecryptedVCardType2Signature;
    }

    public String getVCardType3Signature() {
        return mDecryptedVCardType3Signature;
    }

    public Status getStatus() {
        return mStatus;
    }
}
