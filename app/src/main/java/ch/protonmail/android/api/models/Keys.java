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

import android.os.Parcel;
import android.os.Parcelable;

/**
 * This model is called AddressKey on server, we can create hierarchy of Key -> UserKey/AddressKey for refactor.
 */
public class Keys extends ResponseBody implements Parcelable {

    String ID;
    String PrivateKey;
    int Flags;
    int Primary;
    String Token;
    String Signature;
    String Activation;

    public Keys(String ID, String privateKey, int flags, int primary, String token, String signature, String activation) {
        this.ID = ID;
        PrivateKey = privateKey;
        Flags = flags;
        Primary = primary;
        Token = token;
        Signature = signature;
        Activation = activation;
    }

    protected Keys(Parcel in) {
        ID = in.readString();
        PrivateKey = in.readString();
        Flags = in.readInt();
        Primary = in.readInt();
        Token = in.readString();
        Signature = in.readString();
        Activation = in.readString();
    }

    public static final Creator<Keys> CREATOR = new Creator<Keys>() {
        @Override
        public Keys createFromParcel(Parcel in) {
            return new Keys(in);
        }

        @Override
        public Keys[] newArray(int size) {
            return new Keys[size];
        }
    };

    public String getID() {
        return ID;
    }

    public int getFlags() {
        return Flags;
    }

    public boolean isPrimary() {
        return Primary == 1;
    }

    public String getPrivateKey() {
        return PrivateKey;
    }

    public String getToken() {
        return Token;
    }

    public String getSignature() {
        return Signature;
    }

    public String getActivation() {
        return Activation;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ID);
        dest.writeString(PrivateKey);
        dest.writeInt(Flags);
        dest.writeInt(Primary);
        dest.writeString(Token);
        dest.writeString(Signature);
        dest.writeString(Activation);
    }

}
