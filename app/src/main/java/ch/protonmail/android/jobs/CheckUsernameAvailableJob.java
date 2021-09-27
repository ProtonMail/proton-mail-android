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

import java.net.URLEncoder;

import ch.protonmail.android.api.models.ResponseBody;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.CheckUsernameEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;

/**
 * Created by dkadrikj on 17.7.15.
 */
public class CheckUsernameAvailableJob extends ProtonMailBaseJob {

    public static final String TAG_CHECK_USERNAME_AVAILABLE_JOB = "CheckUsernameAvailableJob";
    private final String wantedUsername;

    public CheckUsernameAvailableJob(String wantedUsername) {
        super(new Params(Priority.MEDIUM).requireNetwork());
        this.wantedUsername = wantedUsername;
    }

    @Override
    public void onRun() throws Throwable {
        if (!getQueueNetworkUtil().isConnected()) {
            Logger.doLog(TAG_CHECK_USERNAME_AVAILABLE_JOB, "no network cannot fetch updates");
            AppUtil.postEventOnUi(new CheckUsernameEvent(Status.NO_NETWORK, false));
            return;
        }
        ResponseBody response = getApi().isUsernameAvailable(URLEncoder.encode(wantedUsername, "utf-8"));
        AppUtil.postEventOnUi(new CheckUsernameEvent(Status.SUCCESS, response.getCode() == Constants.RESPONSE_CODE_OK));
    }
}
