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

import androidx.annotation.NonNull;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.ConnectivityEvent;
import ch.protonmail.android.utils.AppUtil;

import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_API_OFFLINE;

/**
 * Created by dkadrikj on 10/27/15.
 */
public class PingJob extends ProtonMailBaseJob {

    public PingJob() {
        super(new Params(Priority.HIGH).overrideDeadlineToRunInMs(1000));
    }

    @Override
    public void onRun() throws Throwable {
        boolean hasInternet = isInternetAccessible();
        mQueueNetworkUtil.setCurrentlyHasConnectivity(hasInternet);
        AppUtil.postEventOnUi(new ConnectivityEvent(hasInternet));
    }

    private boolean isInternetAccessible() {
        try {
            ResponseBody ping = mApi.ping();
            if (ping.getCode() == Constants.RESPONSE_CODE_OK) {
                return true;
            } else if (ping.getCode() == RESPONSE_CODE_API_OFFLINE) {
                return true;
            }
        } catch (Exception e) {
            // NOOP
        }
        return false;
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return RetryConstraint.CANCEL;
    }

    @Override
    protected int getRetryLimit() {
        return 1;
    }
}