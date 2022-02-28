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

import android.os.Parcel;
import android.os.Parcelable;

import ch.protonmail.android.domain.entity.user.AddressKey;
import ch.protonmail.android.domain.entity.user.UserKey;
import ch.protonmail.android.mapper.bridge.AddressKeyBridgeMapper;
import ch.protonmail.android.mapper.bridge.UserKeyBridgeMapper;

@Deprecated
@kotlin.Deprecated(
        message = "Replace with 'ch.protonmail.android.domain.entity.user.UserKey' or" +
                "'ch.protonmail.android.domain.entity.user.UserKey'.\n" +
                "Where possible to have a proper DI use 'UserKeyBridgeMapper' or " +
                "'AddressKeyBridgeMapper', otherwise the functions 'toAddressKey' or 'toUserKey'"
)
public class Keys extends ResponseBody implements Parcelable {

    private final static String GENERIC_DEPRECATION_MESSAGE = "Use from new AddressKey or UserKey " +
            "entities from  'ch.protonmail.android.domain.entity.user' package.\n" +
            "Use  'toUserKey' or  'toAddressKey' or replace the User or Address directly";

    String ID;
    String PrivateKey;
    int Flags;
    int Primary;
    String Token;
    String Signature;
    String Activation;
    int Active;

    public Keys(String ID, String privateKey, int flags, int primary, String token, String signature, String activation, int active) {
        this.ID = ID;
        PrivateKey = privateKey;
        Flags = flags;
        Primary = primary;
        Token = token;
        Signature = signature;
        Activation = activation;
        Active = active;
    }

    protected Keys(Parcel in) {
        ID = in.readString();
        PrivateKey = in.readString();
        Flags = in.readInt();
        Primary = in.readInt();
        Token = in.readString();
        Signature = in.readString();
        Activation = in.readString();
        Active = in.readInt();
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

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getID() {
        return ID;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nNew fields names:  'canEncrypt' /  'canVerifySignature'")
    public int getFlags() {
        return Flags;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nThis must be get from parent AddressKeys or UserKeys entity")
    public boolean isPrimary() {
        return Primary == 1;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getPrivateKey() {
        return PrivateKey;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getToken() {
        return Token;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getSignature() {
        return Signature;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getActivation() {
        return Activation;
    }

    public int getActive() {
        return Active;
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
        dest.writeInt(Active);
    }

    /**
     * Convert this Key to new Address Key entity
     * @return {@link ch.protonmail.android.domain.entity.user.AddressKey}
     */
    public AddressKey toAddressKey() {
        return new AddressKeyBridgeMapper().toNewModel(this);
    }

    /**
     * Convert this Key to new User Key entity
     * @return {@link ch.protonmail.android.domain.entity.user.UserKey}
     */
    public UserKey toUserKey() {
        return new UserKeyBridgeMapper().toNewModel(this);
    }
}
