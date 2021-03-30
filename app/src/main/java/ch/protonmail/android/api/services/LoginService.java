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
package ch.protonmail.android.api.services;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.ProtonJobIntentService;

import com.birbit.android.jobqueue.JobManager;

import javax.inject.Inject;

import ch.protonmail.android.api.AccountManager;
import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.TokenManager;
import ch.protonmail.android.api.models.MailSettings;
import ch.protonmail.android.api.models.MailSettingsResponse;
import ch.protonmail.android.api.models.UserInfo;
import ch.protonmail.android.api.models.UserSettings;
import ch.protonmail.android.api.models.UserSettingsResponse;
import ch.protonmail.android.api.models.address.AddressKeyActivationWorker;
import ch.protonmail.android.api.models.address.AddressesResponse;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.QueueNetworkUtil;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.domain.entity.Id;
import ch.protonmail.android.events.user.UserSettingsEvent;
import ch.protonmail.android.usecase.FindUserIdForUsername;
import ch.protonmail.android.usecase.LoadUser;
import ch.protonmail.android.usecase.fetch.LaunchInitialDataFetch;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.crypto.OpenPGP;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class LoginService extends ProtonJobIntentService {

    private static final String ACTION_FETCH_USER_DETAILS = "ACTION_FETCH_USER_DETAILS";

    @Inject
    UserManager userManager;
    @Inject
    ProtonMailApiManager api;
    @Inject
    JobManager jobManager;
    @Inject
    LaunchInitialDataFetch launchInitialDataFetch;

    public LoginService() {
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getAction();
        Log.d("PMTAG", "LoginService action = " + action);
        if (ACTION_FETCH_USER_DETAILS.equals(action)) {
            handleFetchCurrentUserDetails();
        }
    }

    private void handleFetchCurrentUserDetails() {
        try {
            Id userId = userManager.getCurrentUserId();
            UserInfo userInfo = api.fetchUserInfoBlocking();
            UserSettingsResponse userSettingsResponse = api.fetchUserSettings();
            MailSettingsResponse mailSettingsResponse = api.fetchMailSettingsBlocking();
            MailSettings mailSettings = mailSettingsResponse.getMailSettings();
            UserSettings userSettings = userSettingsResponse.getUserSettings();
            AddressesResponse addressesResponse = api.fetchAddressesBlocking();
            userManager.setUserDetailsBlocking(userInfo.getUser(), addressesResponse.getAddresses(), mailSettings, userSettings);
            AddressKeyActivationWorker.Companion.activateAddressKeysIfNeeded(getApplicationContext(), addressesResponse.getAddresses(), userId);
            AppUtil.postEventOnUi(new UserSettingsEvent(userSettingsResponse.getUserSettings()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void fetchUserDetails() {
        final Context context = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(context, LoginService.class);
        intent.setAction(ACTION_FETCH_USER_DETAILS);
        JobIntentService.enqueueWork(context, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    private void onFirstAccountReady() {
        AlarmReceiver alarmReceiver = new AlarmReceiver();
        alarmReceiver.setAlarm(ProtonMailApplication.getApplication());
        jobManager.start();
    }

    private void onLastAccountNotReady() {
        AppUtil.clearTasks(jobManager);
    }

    private void onNewAccountReady() {
        launchInitialDataFetch.invoke(true, true);
    }

    /*
    private void ssfsadfdsf() {
        userManager.setLoggedIn(true);
        userManager.saveMailboxPasswordBlocking(userId, generatedMailboxPassword);
        userManager.setUserDetailsBlocking(userInfo.getUser(), addresses.getAddresses(), mailSettings.getMailSettings(), userSettings.getUserSettings());

        userManager.setCurrentUserBlocking(userId); // all calls after this do not have to rely on username

        tokenManager.handleLogin(loginResponse);
        tokenManager.setScope(response.getScope());
    }
   */
}
