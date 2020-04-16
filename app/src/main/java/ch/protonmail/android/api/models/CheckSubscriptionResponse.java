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

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 7/10/16.
 */
public class CheckSubscriptionResponse extends ResponseBody {
    @SerializedName(Fields.Subscription.AMOUNT)
    private int amount;
    @SerializedName(Fields.Subscription.AMOUNT_DUE)
    private int amountDue;
    @SerializedName(Fields.Subscription.PRORATION)
    private int proration;
    @SerializedName(Fields.Subscription.COUPON_DISCOUNT)
    private int couponDiscount;
    @SerializedName(Fields.Subscription.CREDIT)
    private int credit;
    @SerializedName(Fields.Subscription.CURRENCY)
    private String currency;
    @SerializedName(Fields.Subscription.CYCLE)
    private int cycle;
    @SerializedName(Fields.Subscription.COUPON)
    private Coupon coupon;

    public int getAmount() {
        return amount;
    }

    public int getAmountDue() {
        return amountDue;
    }

    public String getCurrency() {
        return currency;
    }
}
