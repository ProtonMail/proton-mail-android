/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.jobs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import java.io.IOException;

/**
 * Created by dkadrikj on 11/2/15.
 */
public abstract class ProtonMailEndlessJob extends ProtonMailBaseJob {

    protected ProtonMailEndlessJob(Params params) {
        super(params);
    }

    private boolean shouldReschedule;

    @Override
    protected void onProtonCancel(int cancelReason, @Nullable Throwable throwable) {
        if (shouldReschedule) {
            // we reschedule the job only if we previously got network IO exception
            getJobManager().addJobInBackground(this);
        }
    }

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        if (throwable instanceof Exception) {
            if (throwable.getCause() instanceof IOException) {
                shouldReschedule = true;
                getQueueNetworkUtil().retryPingAsPreviousRequestWasInconclusive();
            }
        }
        return RetryConstraint.RETRY;
    }
}
