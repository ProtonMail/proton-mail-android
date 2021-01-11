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

import java.util.Map;

import ch.protonmail.android.api.models.DonateBody;
import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.utils.ParseUtils;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.DonateEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.extensions.CollectionExtensions;

public class DonateJob extends ProtonMailBaseJob {

    private final DonateBody donateBody;

    public DonateJob(String paymentToken, int amount, Constants.CurrencyType currency) {
        super(new Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_PAYMENT));
        donateBody = new DonateBody(paymentToken, amount, currency);
    }

    @Override
    public void onRun() throws Throwable {
        ResponseBody response = getApi().donate(donateBody);
        if (response.getCode() == Constants.RESPONSE_CODE_OK) {
            AppUtil.postEventOnUi(new DonateEvent(Status.SUCCESS));
        } else {
            Map<String, String> details = CollectionExtensions.filterValues(response.getDetails(), String.class);
            AppUtil.postEventOnUi(new DonateEvent(Status.FAILED, response.getError(), ParseUtils.INSTANCE.compileSingleErrorMessage(details)));
        }

    }
}
