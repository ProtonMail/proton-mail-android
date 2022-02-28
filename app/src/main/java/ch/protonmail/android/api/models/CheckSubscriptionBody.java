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

import com.google.gson.annotations.SerializedName;

import java.util.List;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;

/**
 * Created by dkadrikj on 7/10/16.
 */
public class CheckSubscriptionBody {
    @SerializedName(Fields.Payment.COUPON_CODE)
    private String coupon;
    @SerializedName(Fields.Payment.PLAN_IDS)
    private List<String> planIds;
    @SerializedName(Fields.Payment.CURRENCY)
    private String currency;
    @SerializedName(Fields.Subscription.CYCLE)
    private int cycle;

    public CheckSubscriptionBody(String coupon, List<String> planIds, Constants.CurrencyType currency, int cycle) {
        this.coupon = coupon;
        this.planIds = planIds;
        this.currency = currency.name();
        this.cycle = cycle;
    }
}
