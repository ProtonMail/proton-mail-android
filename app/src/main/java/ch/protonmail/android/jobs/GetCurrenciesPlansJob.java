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
package ch.protonmail.android.jobs;

import com.birbit.android.jobqueue.Params;

import java.util.ArrayList;
import java.util.List;

import ch.protonmail.android.api.models.AllCurrencyPlans;
import ch.protonmail.android.api.models.AvailablePlansResponse;
import ch.protonmail.android.api.models.Plan;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.AvailablePlansEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 7/6/16.
 */
public class GetCurrenciesPlansJob extends ProtonMailBaseJob {

    private final List<Constants.CurrencyType> currencies;

    public GetCurrenciesPlansJob(List<Constants.CurrencyType> currencies) {
        super(new Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_PAYMENT));
        this.currencies = currencies;
    }

    @Override
    public void onRun() throws Throwable {
        List<AvailablePlansResponse> allPlans = new ArrayList<>();

        for (Constants.CurrencyType currency : currencies) {
            final AvailablePlansResponse monthlyPlans = getApi().fetchAvailablePlans(currency.name(), Constants.PaymentCycleType.MONTHLY.getPaymentCycleTypeValue());
            final AvailablePlansResponse yearlyPlans = getApi().fetchAvailablePlans(currency.name(), Constants.PaymentCycleType.YEARLY.getPaymentCycleTypeValue());
            allPlans.add(monthlyPlans);
            allPlans.add(yearlyPlans);
        }
        if (!validateResponses(allPlans)) {
            AppUtil.postEventOnUi(new AvailablePlansEvent(Status.FAILED));
        } else {
            AllCurrencyPlans allCurrencyPlans = null;
            if (currencies.size() == 1) {
                allCurrencyPlans = new AllCurrencyPlans(allPlans, currencies.get(0));
            } else {
                allCurrencyPlans = new AllCurrencyPlans(allPlans);
            }
            AppUtil.postEventOnUi(new AvailablePlansEvent(Status.SUCCESS, allCurrencyPlans));
        }
    }

    private AvailablePlansResponse filterPlans(AvailablePlansResponse response) {
        List<Plan> plans = new ArrayList<>();
        for (Plan plan : response.getPlans()) {
            if (plan.getType() == 1) {
                plans.add(plan);
            }
        }
        return new AvailablePlansResponse(plans);
    }

    private boolean validateResponses(List<AvailablePlansResponse> allPlans) {
        boolean allValid = true;
        for (AvailablePlansResponse response : allPlans) {
            if (response.getCode() != Constants.RESPONSE_CODE_OK) {
                allValid = false;
                break;
            }
        }

        return allValid;
    }
}
