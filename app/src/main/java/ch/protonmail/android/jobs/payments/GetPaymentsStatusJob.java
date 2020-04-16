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

import ch.protonmail.android.api.models.PaymentsStatusResponse;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.payment.PaymentsStatusEvent;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 7/11/16.
 */
public class GetPaymentsStatusJob extends ProtonMailBaseJob {

    public GetPaymentsStatusJob() {
        super(new Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_PAYMENT));
    }

    @Override
    public void onRun() throws Throwable {
        PaymentsStatusResponse response = mApi.fetchPaymentsStatus();
        if (response.getCode() == Constants.RESPONSE_CODE_OK) {
            AppUtil.postEventOnUi(new PaymentsStatusEvent(Status.SUCCESS, response.isStripeActive(), response.isPaypalActive()));
        } else {
            AppUtil.postEventOnUi(new PaymentsStatusEvent(Status.FAILED));
        }
    }
}
