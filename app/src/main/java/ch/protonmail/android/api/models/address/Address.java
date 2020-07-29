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

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

import ch.protonmail.android.api.models.Keys;
import ch.protonmail.android.api.utils.Fields;

public class Address implements Parcelable {

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
    public String getID() {
        return ID;
    }

    public String getDomainId() {
        return DomainID;
    }

    public int getStatus() {
        return Status;
    }

    public int getOrder() {
        return Order;
    }

    public void setOrder(int order) {
        Order = order;
    }

    public void setEmail(String email) {
        this.Email = email;
    }

    public String getEmail() {
        return Email;
    }

    public List<Keys> getKeys() {
        return Keys;
    }

    public int getReceive() {
        return Receive;
    }

    public int getType() {
        return Type;
    }

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
}
