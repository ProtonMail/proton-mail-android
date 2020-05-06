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

import android.text.TextUtils;

import com.birbit.android.jobqueue.Params;

import ch.protonmail.android.R;
import ch.protonmail.android.api.models.Destination;
import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.api.models.VerificationCodeBody;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.SendVerificationCodeEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

import static ch.protonmail.android.jobs.CheckUsernameAvailableJob.TAG_CHECK_USERNAME_AVAILABLE_JOB;

/**
 * Created by dkadrikj on 17.7.15.
 */
public class SendVerificationCodeJob extends ProtonMailBaseJob {

    private final String username;
    private final String emailAddress;
    private final String phoneNumber;

    public SendVerificationCodeJob(String username, String emailAddress, String phoneNumber) {
        super(new Params(Priority.MEDIUM).requireNetwork());
        this.username = username;
        this.emailAddress = emailAddress;
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void onRun() throws Throwable {
        if (!mQueueNetworkUtil.isConnected()) {
            Logger.doLog(TAG_CHECK_USERNAME_AVAILABLE_JOB, "no network");
            AppUtil.postEventOnUi(new SendVerificationCodeEvent(Status.NO_NETWORK, ProtonMailApplication.getApplication().getString(R.string.no_network)));
            return;
        }
        Destination destination = null;
        String type = null;
        if (!TextUtils.isEmpty(emailAddress)) {
            type = "email";
            destination = new Destination(emailAddress, null);
        } else if (!TextUtils.isEmpty(phoneNumber)) {
            type = "sms";
            destination = new Destination(null, phoneNumber);
        }
        VerificationCodeBody verificationCodeBody = new VerificationCodeBody(username, type, destination);
        ResponseBody response = mApi.sendVerificationCode(verificationCodeBody);
        if (response.getCode() == Constants.RESPONSE_CODE_OK) {
            AppUtil.postEventOnUi(new SendVerificationCodeEvent(Status.SUCCESS, null));
        } else {
            AppUtil.postEventOnUi(new SendVerificationCodeEvent(Status.FAILED, response.getError()));
        }
    }
}
