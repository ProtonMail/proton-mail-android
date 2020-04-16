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

import java.util.List;

import ch.protonmail.android.api.models.CheckSubscriptionBody;
import ch.protonmail.android.api.models.CheckSubscriptionResponse;
import ch.protonmail.android.api.models.GetSubscriptionResponse;
import ch.protonmail.android.api.models.Plan;
import ch.protonmail.android.api.models.Subscription;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.payment.CheckSubscriptionEvent;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 7/12/16.
 */
public class CheckSubscriptionJob extends ProtonMailBaseJob {

    private final String mCoupon;
    private final List<String> mPlanIds;
    private final Constants.CurrencyType mCurrency;
    private final int mCycle;

    public CheckSubscriptionJob(String coupon, List<String> planIds, Constants.CurrencyType currency, int cycle) {
        super(new Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_PAYMENT));
        mCoupon = coupon;
        mPlanIds = planIds;
        mCurrency = currency;
        mCycle = cycle;
    }

    @Override
    public void onRun() throws Throwable {
        GetSubscriptionResponse currentSubscriptions = null;
        try {
            currentSubscriptions = mApi.fetchSubscription();
        } catch (Exception error) {
            // noop
        }

        if (currentSubscriptions != null) {
            Subscription subscription = currentSubscriptions.getSubscription();
            if (subscription != null) {
                List<Plan> subscriptionPlans = subscription.getPlans();
                if (subscriptionPlans != null && subscriptionPlans.size() > 0) {
                    for (Plan plan : subscriptionPlans) {
                        Constants.VpnPlanType vpnPlanType = Constants.VpnPlanType.Companion.fromString(plan.getName());
                        if (vpnPlanType == Constants.VpnPlanType.BASIC || vpnPlanType == Constants.VpnPlanType.PLUS) {
                            mPlanIds.add(plan.getId());
                        }
                    }
                }
            }
        }
        CheckSubscriptionBody body = new CheckSubscriptionBody(mCoupon, mPlanIds, mCurrency, mCycle);
        CheckSubscriptionResponse response = mApi.checkSubscription(body);
        if (response.getCode() == Constants.RESPONSE_CODE_OK) {
            AppUtil.postEventOnUi(new CheckSubscriptionEvent(Status.SUCCESS, response));
        } else {
            AppUtil.postEventOnUi(new CheckSubscriptionEvent(Status.FAILED, response));
        }
    }
}
