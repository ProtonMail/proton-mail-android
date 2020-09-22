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

import androidx.annotation.NonNull;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import ch.protonmail.android.api.models.PaymentMethodsResponse;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.payment.GetPaymentMethodsEvent;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 7/10/16.
 */
public class GetPaymentMethodsJob extends ProtonMailBaseJob {

    public GetPaymentMethodsJob() {
        super(new Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_PAYMENT));
    }

    @Override
    public void onRun() throws Throwable {
        try {
            PaymentMethodsResponse paymentMethodsResponse = getApi().fetchPaymentMethods();
            if (paymentMethodsResponse.getCode() == Constants.RESPONSE_CODE_OK) {
                AppUtil.postEventOnUi(new GetPaymentMethodsEvent(Status.SUCCESS, paymentMethodsResponse.getPaymentMethods()));
            } else {
                AppUtil.postEventOnUi(new GetPaymentMethodsEvent(Status.FAILED));
            }
        } catch (Exception e) {
            AppUtil.postEventOnUi(new GetPaymentMethodsEvent(Status.FAILED));
        }
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return RetryConstraint.CANCEL;
    }
}
