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

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 7/5/16.
 */
public class Plan implements Serializable {
    @SerializedName(Fields.Subscription.Plan.ID)
    private String id;
    @SerializedName(Fields.Subscription.Plan.TYPE)
    private int type;
    @SerializedName(Fields.Subscription.Plan.CYCLE)
    private int cycle;
    @SerializedName(Fields.Subscription.Plan.NAME)
    private String name;
    @SerializedName(Fields.Subscription.Plan.TITLE)
    private String title;
    @SerializedName(Fields.Subscription.Plan.CURRENCY)
    private String currency;
    @SerializedName(Fields.Subscription.Plan.AMOUNT)
    private int amount;
    @SerializedName(Fields.Subscription.Plan.MAX_DOMAINS)
    private int maxDomains;
    @SerializedName(Fields.Subscription.Plan.MAX_ADDRESSES)
    private int maxAddresses;
    @SerializedName(Fields.Subscription.Plan.MAX_SPACE)
    private long maxSpace;
    @SerializedName(Fields.Subscription.Plan.MAX_MEMBERS)
    private int maxMembers;
    @SerializedName(Fields.Subscription.Plan.TWO_FACTOR)
    private int twoFactor;
    @SerializedName(Fields.Subscription.Plan.QUANTITY)
    private int quantity;

    public Plan() {
    }

    public String getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public int getCycle() {
        return cycle;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title;
    }

    public String getCurrency() {
        return currency;
    }

    public int getAmount() {
        return amount;
    }

    public int getMaxDomains() {
        return maxDomains;
    }

    public int getMaxAddresses() {
        return maxAddresses;
    }

    public long getMaxSpace() {
        return maxSpace;
    }


}
