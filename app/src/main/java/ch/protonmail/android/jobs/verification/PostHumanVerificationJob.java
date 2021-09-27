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
package ch.protonmail.android.jobs.verification;

import com.birbit.android.jobqueue.Params;

import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.models.requests.PostHumanVerificationBody;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.verification.PostHumanVerificationEvent;
import ch.protonmail.android.jobs.Priority;
import ch.protonmail.android.jobs.ProtonMailBaseJob;
import ch.protonmail.android.utils.AppUtil;

public class PostHumanVerificationJob extends ProtonMailBaseJob {

    private final Constants.TokenType mTokenType;
    private final String mToken;

    public PostHumanVerificationJob(Constants.TokenType tokenType, String token) {
        super(new Params(Priority.HIGH).requireNetwork().persist().groupBy(Constants.JOB_GROUP_MISC));

        mTokenType = tokenType;
        mToken = token;
    }

    @Override
    public void onRun() throws Throwable {
        if (!getQueueNetworkUtil().isConnected()) {
            AppUtil.postEventOnUi(new PostHumanVerificationEvent(Status.NO_NETWORK));
            return;
        }
        ResponseBody response = getApi().postHumanVerification(new PostHumanVerificationBody(mToken, mTokenType));
        AppUtil.postEventOnUi(new PostHumanVerificationEvent(response.getCode() == Constants.RESPONSE_CODE_OK ? Status.SUCCESS : Status.FAILED));
    }
}
