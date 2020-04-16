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

import java.util.List;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 7/5/16.
 */
public class Subscription {
    @SerializedName(Fields.Subscription.ID)
    private String id;
    @SerializedName(Fields.Subscription.INVOICE_ID)
    private String invoiceId;
    @SerializedName(Fields.Subscription.CYCLE)
    private int cycle;
    @SerializedName(Fields.Subscription.PERIOD_START)
    private long periodStart;
    @SerializedName(Fields.Subscription.PERIOD_END)
    private long periodEnd;
    @SerializedName(Fields.Subscription.COUPON_CODE)
    private String counponCode;
    @SerializedName(Fields.Subscription.CURRENCY)
    private String currency;
    @SerializedName(Fields.Subscription.AMOUNT)
    private String amount;
    @SerializedName(Fields.Subscription.PLANS)
    private List<Plan> plans;

    public Subscription() {
    }

    public String getId() {
        return id;
    }

    public String getCurrency() {
        return currency;
    }

    public String getAmount() {
        return amount;
    }

    public List<Plan> getPlans() {
        return plans;
    }
}
