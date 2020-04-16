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

import ch.protonmail.android.api.models.DirectEnabledResponse;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.GetDirectEnabledEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

/**
 * Created by dkadrikj on 17.7.15.
 */
public class GetDirectEnabledJob extends ProtonMailBaseJob {

    public static final String TAG_GET_DIRECT_ENABLED_JOB = "GetDirectEnabledJob";

    public GetDirectEnabledJob() {
        super(new Params(Priority.HIGH).requireNetwork());
    }

    @Override
    public void onRun() throws Throwable {
        if (!mQueueNetworkUtil.isConnected(ProtonMailApplication.getApplication())) {
            Logger.doLog(TAG_GET_DIRECT_ENABLED_JOB, "no network cannot fetch updates");
            AppUtil.postEventOnUi(new GetDirectEnabledEvent(Status.NO_NETWORK));
            return;
        }
        DirectEnabledResponse response = mApi.fetchDirectEnabled();
        AppUtil.postEventOnUi(new GetDirectEnabledEvent(response.getDirect(), response.getVerifyMethods()));
    }
}
