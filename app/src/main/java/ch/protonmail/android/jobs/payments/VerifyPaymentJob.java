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

import android.content.SharedPreferences;

import com.birbit.android.jobqueue.Params;

import java.util.Map;

import ch.protonmail.android.api.models.VerifyBody;
import ch.protonmail.android.api.models.VerifyResponse;
import ch.protonmail.android.api.utils.ParseUtils;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.payment.VerifyPaymentEvent;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.extensions.CollectionExtensions;

public class VerifyPaymentJob extends ProtonMailBaseJob {

    private final VerifyBody verifyBody;

    public VerifyPaymentJob(int amount, Constants.CurrencyType currency, String paymentToken) {
        super(new Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_PAYMENT));
            verifyBody = new VerifyBody(amount, currency, paymentToken);
    }

    @Override
    public void onRun() throws Throwable {
        VerifyResponse response = mApi.verifyPayment(verifyBody);
        if (response.getCode() == Constants.RESPONSE_CODE_OK) {
            String verifyCode = response.getVerifyCode();
            SharedPreferences prefs = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
            prefs.edit().putString(Constants.Prefs.PREF_VERIFY_CODE, verifyCode).apply();
            AppUtil.postEventOnUi(new VerifyPaymentEvent(Status.SUCCESS, verifyCode));
        } else {
            Map<String, String> details = CollectionExtensions.filterValues(response.getDetails(), String.class);
            AppUtil.postEventOnUi(new VerifyPaymentEvent(Status.FAILED, response.getError(), ParseUtils.Companion.compileSingleErrorMessage(details)));
        }
    }
}
