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
package ch.protonmail.android.jobs.payments;

import com.birbit.android.jobqueue.Params;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import ch.protonmail.android.api.models.CreateSubscriptionBody;
import ch.protonmail.android.api.models.CreateUpdateSubscriptionResponse;
import ch.protonmail.android.api.models.GetSubscriptionResponse;
import ch.protonmail.android.api.models.PaymentBody;
import ch.protonmail.android.api.models.Plan;
import ch.protonmail.android.api.models.Subscription;
import ch.protonmail.android.api.models.TokenPaymentBody;
import ch.protonmail.android.api.utils.ParseUtils;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.PaymentMethodEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.jobs.user.FetchUserSettingsJob;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.extensions.CollectionExtensions;

@Deprecated
public class CreateSubscriptionJob extends ProtonMailBaseJob {

    private PaymentBody paymentMethodBody;
    private CreateSubscriptionBody subscriptionBody;
    private int amount;
    private Constants.CurrencyType currency;
    private String couponCode;
    private List<String> planIds;
    private int cycle;
    private String paymentToken;

    public CreateSubscriptionJob(int amount, Constants.CurrencyType currency, String couponCode, List<String> planIds, int cycle, String paymentToken) {
        super(new Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_PAYMENT));
        paymentMethodBody = new TokenPaymentBody(paymentToken);
        this.amount = amount;
        this.currency = currency;
        this.couponCode = couponCode;
        this.planIds = planIds;
        this.cycle = cycle;
        this.paymentToken = paymentToken;
    }

    @Override
    public void onRun() throws Throwable {
        GetSubscriptionResponse currentSubscriptions = getApi().fetchSubscriptionBlocking();

        Subscription subscription = currentSubscriptions.getSubscription();
        if (subscription != null) {
            List<Plan> subscriptionPlans = subscription.getPlans();
            if (subscriptionPlans != null && subscriptionPlans.size() > 0) {
                for (Plan plan : subscriptionPlans) {
                    Constants.VpnPlanType vpnPlanType = Constants.VpnPlanType.Companion.fromString(plan.getName());
                    if (vpnPlanType == Constants.VpnPlanType.BASIC || vpnPlanType == Constants.VpnPlanType.PLUS) {
                        planIds.add(plan.getId());
                    }
                }
            }
        }

        if (amount == 0) {
            // don't provide payment method and put "amount" as 0, because we are using stored credits
            subscriptionBody = new CreateSubscriptionBody(0, currency, (PaymentBody) null, couponCode, planIds, cycle);
        } else {
            // provide new payment method in body
            subscriptionBody = new CreateSubscriptionBody(amount, currency, paymentMethodBody, couponCode, planIds, cycle);
        }

        CreateUpdateSubscriptionResponse subscriptionResponse = getApi().createUpdateSubscriptionBlocking(subscriptionBody);

        if (subscriptionResponse.getCode() != Constants.RESPONSE_CODE_OK) {
            Map<String, String> details = CollectionExtensions.filterValues(subscriptionResponse.getDetails(), String.class);
            AppUtil.postEventOnUi(new PaymentMethodEvent(Status.FAILED, subscriptionResponse.getError(), ParseUtils.Companion.compileSingleErrorMessage(details)));
            return;
        }

        // store payment method if this was first payment using credits from "verification payment"
        if (amount == 0 && paymentToken != null) {
            try {
                getApi().createUpdatePaymentMethodBlocking(new TokenPaymentBody(paymentToken)).execute();
            } catch (IOException e) {
                Logger.doLogException(e);
            }
        }

        AppUtil.postEventOnUi(new PaymentMethodEvent(Status.SUCCESS, subscriptionResponse.getSubscription()));

        getJobManager().addJobInBackground(new FetchUserSettingsJob());

        GetPaymentMethodsJob paymentMethodsJob = new GetPaymentMethodsJob();
        getJobManager().addJobInBackground(paymentMethodsJob);
    }
}
