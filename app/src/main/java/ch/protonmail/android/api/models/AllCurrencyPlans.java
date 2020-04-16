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

import java.io.Serializable;
import java.util.List;

import ch.protonmail.android.core.Constants;

/**
 * Created by dkadrikj on 7/6/16.
 */
public class AllCurrencyPlans implements Serializable {
    private List<AvailablePlansResponse> plans;
    private Constants.CurrencyType currency;

    public AllCurrencyPlans(List<AvailablePlansResponse> plans) {
        this.plans = plans;
    }

    public AllCurrencyPlans(List<AvailablePlansResponse> plans, Constants.CurrencyType currency) {
        this.plans = plans;
        this.currency = currency;
    }

    public void addPlans(List<AvailablePlansResponse> plans) {
        this.plans.addAll(plans);
    }

    public List<AvailablePlansResponse> getPlans() {
        return plans;
    }

    public Constants.CurrencyType getCurrency() {
        return currency;
    }

    public AvailablePlansResponse getPlan(boolean yearly, Constants.CurrencyType currency) {
        for (AvailablePlansResponse plan : plans) {
            if (currency.name().equals(plan.getCurrency()) && (yearly ? plan.getCycle() == 12 : plan.getCycle() == 1)) {
                return plan;
            }
        }

        return null;
    }
}
