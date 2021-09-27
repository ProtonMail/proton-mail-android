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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;

public class CreateSubscriptionBody {
    @SerializedName(Fields.Payment.AMOUNT)
    private int amount;
    @SerializedName(Fields.Payment.CURRENCY)
    private String currency;
    @SerializedName(Fields.Payment.PAYMENT)
    private PaymentBody payment;
    @SerializedName(Fields.Payment.COUPON_CODE)
    private String couponCode;
    @SerializedName(Fields.Payment.PLAN_IDS)
    private Map<String, Integer> planIds;
    @SerializedName(Fields.Payment.CYCLE)
    private int cycle;

    @Deprecated
    public CreateSubscriptionBody(int amount, Constants.CurrencyType currency, PaymentBody payment, String couponCode, List<String> planIds, int cycle) {
        this.amount = amount;
        this.currency = currency.name();
        this.payment = payment;
        this.couponCode = couponCode;
        this.planIds = new HashMap<>();
        for (String plan: planIds) {
            this.planIds.put(plan, 1);
        }
        this.cycle = cycle;
    }

    public CreateSubscriptionBody(int amount, String currencyType, PaymentBody payment, String couponCode, List<String> planIds, int cycle) {
        this.amount = amount;
        this.currency = currencyType;
        this.payment = payment;
        this.couponCode = couponCode;
        this.planIds = new HashMap<>();
        for (String plan : planIds) {
            this.planIds.put(plan, 1);
        }
        this.cycle = cycle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        CreateSubscriptionBody that = (CreateSubscriptionBody) o;

        return new EqualsBuilder()
                .append(amount, that.amount)
                .append(cycle, that.cycle)
                .append(currency, that.currency)
                .append(payment, that.payment)
                .append(couponCode, that.couponCode)
                .append(planIds, that.planIds)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(amount)
                .append(currency)
                .append(payment)
                .append(couponCode)
                .append(planIds)
                .append(cycle)
                .toHashCode();
    }
}
