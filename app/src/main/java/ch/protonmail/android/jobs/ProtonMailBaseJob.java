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
import androidx.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.Params;
import com.birbit.android.jobqueue.RetryConstraint;

import javax.inject.Inject;

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository;
import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.QueueNetworkUtil;
import ch.protonmail.android.core.UserManager;
import timber.log.Timber;

public abstract class ProtonMailBaseJob extends Job {

    @Inject
    protected transient UserManager mUserManager;
    @Inject
    protected transient ProtonMailApiManager mApi;
    @Inject
    protected transient JobManager mJobManager;
    @Inject
    protected transient QueueNetworkUtil mQueueNetworkUtil;
    @Inject
    protected transient MessageDetailsRepository messageDetailsRepository;
    protected String username;

    protected void inject() {
        ProtonMailApplication.getApplication().getAppComponent().inject(this);
    }

    protected ProtonMailBaseJob(Params params, String username) {
        super(params);
        inject();
        this.username = (username == null) ? mUserManager.getUsername() : username;
    }

    protected ProtonMailBaseJob(Params params) {
        this(params, null);
        inject();
    }

    @Override
    public void onAdded() {
    }

    @Override
    protected int getRetryLimit() {
        return Constants.JOB_RETRY_LIMIT_DEFAULT;
    }

    /**
     * Use {@link #onProtonCancel} for custom cancellation logic.
     */
    @Override
    protected final void onCancel(int cancelReason, @Nullable Throwable throwable) {
        Timber.e(throwable, this.getClass().getName() + " cancelled, reason = " + cancelReason + ", retryLimit = " + this.getRetryLimit());

        try {
            onProtonCancel(cancelReason, throwable);
        } catch (Exception e) {
            Timber.e(e, this.getClass().getName() + " threw exception in onProtonCancel");
            // throw e; this exception is swallowed by JobQueue anyway so there's no need to rethrow
        }
    }

    /**
     * Use this method for custom logic when Jobs get cancelled.
     */
    protected void onProtonCancel(int cancelReason, @Nullable Throwable throwable) {}

    @Override
    protected RetryConstraint shouldReRunOnThrowable(@NonNull Throwable throwable, int runCount, int maxRunCount) {
        return null;
    }
}
