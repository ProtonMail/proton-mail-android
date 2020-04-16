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

import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.BugReportEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

public class ReportBugsJob extends ProtonMailEndlessJob {

    private final String mOSName;
    private final String mAppVersion;
    private final String mClient;
    private final String mClientVersion;
    private final String mTitle;
    private final String mDescription;
    private final String mUserName;
    private final String mEmail;

    public ReportBugsJob(@NonNull String OSName, @NonNull String appVersion, @NonNull String client, @NonNull String clientVersion, @NonNull String title, @NonNull String description, @NonNull String username, @NonNull String email) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_BUGS));
        mOSName = OSName;
        mAppVersion = appVersion;
        mClient = client;
        mClientVersion = clientVersion;
        mTitle = title;
        mDescription = description;
        mUserName = username;
        mEmail = email;
    }

    @Override
    public void onAdded() {
        super.onAdded();
        if (!mQueueNetworkUtil.isConnected(ProtonMailApplication.getApplication())) {
            AppUtil.postEventOnUi(new BugReportEvent(Status.NO_NETWORK));
        }
    }

    @Override
    public void onRun() throws Throwable {
        ResponseBody response = mApi.reportBug(mOSName, mAppVersion, mClient, mClientVersion, mTitle, mDescription, mUserName, mEmail);
        if (response.getCode() == Constants.RESPONSE_CODE_OK) {
            AppUtil.postEventOnUi(new BugReportEvent(Status.SUCCESS));
        } else {
            AppUtil.postEventOnUi(new BugReportEvent(Status.FAILED));
        }
    }
}
