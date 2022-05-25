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
package ch.protonmail.android.api.models.address;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import ch.protonmail.android.api.models.Keys;
import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.mapper.bridge.AddressBridgeMapper;

@Deprecated
@kotlin.Deprecated(
        message = "Replace with 'ch.protonmail.android.domain.entity.user.Address'.\n" +
                "Where possible to have a proper DI use 'AddressBridgeMapper', otherwise the " +
                "function 'toNewAddress'"
)
public class Address implements Parcelable {

    private final static String GENERIC_DEPRECATION_MESSAGE = "Use from new Address entity " +
            "'ch.protonmail.android.domain.entity.user.Address'\n" +
            "Use 'toNewAddress' or replace the User directly";

    @SerializedName(Fields.Auth.ID)
    private String ID;
    @SerializedName(Fields.Auth.DOMAIN_ID)
    private String DomainID;
    @SerializedName(Fields.Auth.EMAIL)
    private String Email;
    @SerializedName(Fields.Auth.SEND)
    private int Send;
    @SerializedName(Fields.Auth.RECEIVE)
    private int Receive;
    @SerializedName(Fields.Auth.STATUS)
    private int Status;
    @SerializedName(Fields.Auth.TYPE)
    private int Type;
    @SerializedName(Fields.Addresses.ORDER)
    private int Order;
    @SerializedName(Fields.Auth.DISPLAY_NAME)
    private String DisplayName;
    @SerializedName(Fields.Auth.SIGNATURE)
    private String Signature;
    @SerializedName(Fields.Auth.HAS_KEYS)
    private int HasKeys;
    @SerializedName(Fields.Auth.KEYS)
    private List<Keys> Keys;

    public Address() {
    }

    public Address(
            String ID,
            String domainID,
            String email,
            int send,
            int receive,
            int status,
            int type,
            int order,
            String displayName,
            String signature,
            int hasKeys,
            List<ch.protonmail.android.api.models.Keys> keys
    ) {
        this.ID = ID;
        DomainID = domainID;
        Email = email;
        Send = send;
        Receive = receive;
        Status = status;
        Type = type;
        Order = order;
        DisplayName = displayName;
        Signature = signature;
        HasKeys = hasKeys;
        Keys = keys;
    }

    protected Address(Parcel in) {
        ID = in.readString();
        Email = in.readString();
        Send = in.readInt();
        Order = in.readInt();
        Receive = in.readInt();
        Type = in.readInt();
        Status = in.readInt();
        Keys = Collections.unmodifiableList(in.createTypedArrayList(ch.protonmail.android.api.models.Keys.CREATOR));
        DisplayName = in.readString();
        Signature = in.readString();
        DomainID = in.readString();
        HasKeys = in.readInt();
    }

    public static final Creator<Address> CREATOR = new Creator<Address>() {
        @Override
        public Address createFromParcel(Parcel in) {
            return new Address(in);
        }

        @Override
        public Address[] newArray(int size) {
            return new Address[size];
        }
    };

    // region getters and setters
    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getID() {
        return ID;
    }

    @Nullable
    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getDomainId() {
        return DomainID;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public int getStatus() {
        return Status;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE +
            "\nThis must be get from parent Addresses entity")
    public int getOrder() {
        return Order;
    }

    public void setOrder(int order) {
        Order = order;
    }

    public void setEmail(String email) {
        this.Email = email;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getEmail() {
        return Email;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public List<Keys> getKeys() {
        return Keys;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE + "\nNew field name:  'allowedToReceive'")
    public int getReceive() {
        return Receive;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public int getType() {
        return Type;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE)
    public String getDisplayName() {
        return DisplayName;
    }

    public String getSignature() {
        return Signature == null ? "" : Signature;
    }

    public void setDisplayName(String displayName) {
        DisplayName = displayName;
    }

    public void setSignature(String signature) {
        Signature = signature;
    }

    @Deprecated
    @kotlin.Deprecated(message = GENERIC_DEPRECATION_MESSAGE + "\nNew field name:  'allowedToSend'")
    public int getSend() {
        return Send;
    }

    // endregion

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(ID);
        dest.writeString(Email);
        dest.writeInt(Send);
        dest.writeInt(Order);
        dest.writeInt(Receive);
        dest.writeInt(Type);
        dest.writeInt(Status);
        dest.writeTypedList(Keys);
        dest.writeString(DisplayName);
        dest.writeString(Signature);
        dest.writeString(DomainID);
        dest.writeInt(HasKeys);
    }

    /**
     * Convert this Address to new Address entity
     * @return {@link ch.protonmail.android.domain.entity.user.Address}
     */
    public ch.protonmail.android.domain.entity.user.Address toNewAddress() {
        return AddressBridgeMapper.buildDefault().toNewModel(this);
    }
}
