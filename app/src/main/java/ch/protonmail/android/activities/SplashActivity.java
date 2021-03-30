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
package ch.protonmail.android.activities;

import static ch.protonmail.android.activities.NavigationActivityKt.EXTRA_FIRST_LOGIN;
import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_LOGIN_FINISHED;
import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_NOT_INITIALIZED;
import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_TO_INBOX;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;

import ch.protonmail.android.R;
import ch.protonmail.android.activities.guest.FirstActivity;
import ch.protonmail.android.activities.mailbox.MailboxActivity;
import ch.protonmail.android.api.AccountManager;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.api.services.LoginService;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.user.UserSettingsEvent;

public class SplashActivity extends BaseActivity {

    private static final int DELAY = 2000;
    private final NavigateHandler mNavigateHandler = new NavigateHandler();
    private AlarmReceiver alarmReceiver = new AlarmReceiver();
    private NavigateRunnable mNavigateRunnable;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected boolean shouldCheckForAutoLogout() {
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mApp.startJobManager();
        boolean notFromHistory = (getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0;
        if (notFromHistory && mUserManager.getCurrentUserLoginState() == LOGIN_STATE_TO_INBOX) {
            alarmReceiver.setAlarm(getApplicationContext(), true);
            Intent home = new Intent(this, MailboxActivity.class);
            home.putExtra(EXTRA_FIRST_LOGIN, ProtonMailApplication.getApplication().hasUpdateOccurred());
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRunning();
        mApp.getBus().register(this);
    }

    private void startRunning() {
        mNavigateRunnable = new NavigateRunnable(this);
        mNavigateHandler.postDelayed(mNavigateRunnable, DELAY);
    }

    private void checkUserDetailsAndGoHome() {
        if (mUserManager.accessTokenExists() && !mUserManager.getUser().getAddresses().isEmpty()) {
            mUserManager.setLoggedIn(true);
            fetchMailSettingsWorkerEnqueuer.enqueue();
            goHome();
            finish();
        } else {
            LoginService.fetchUserDetails();
        }
    }

    private void navigate() {
        int loginState = mUserManager.getCurrentUserLoginState();
        if (loginState == LOGIN_STATE_NOT_INITIALIZED) {
            startActivity(new Intent(this, FirstActivity.class));
            finish();
        } else if (loginState == LOGIN_STATE_LOGIN_FINISHED) {
            // login finished but mailbox login not
            mUserManager.logoutBlocking(mUserManager.requireCurrentUserId());
            if (AccountManager.Companion.getInstance(this).getLoggedInUsers().size() >= 1) {
                // There were multiple accounts logged in
                checkUserDetailsAndGoHome();
            } else {
                // There was only one account logged in
                /*
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                TODO("startLoginWorkflow")
                */
            }
        } else {
            checkUserDetailsAndGoHome();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        mNavigateHandler.removeCallbacks(mNavigateRunnable);
        ProtonMailApplication.getApplication().getBus().unregister(this);
    }

    @Subscribe
    public void onUserSettingsEvent(UserSettingsEvent event) {
        if (event != null && event.getUserSettings() != null) {
            mUserManager.setLoggedIn(true);
            goHome();
        }
    }

    private void goHome() {
        Intent home = new Intent(this, MailboxActivity.class);
        alarmReceiver.setAlarm(ProtonMailApplication.getApplication());
        home.putExtra(EXTRA_FIRST_LOGIN, ProtonMailApplication.getApplication().hasUpdateOccurred());
        home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        home.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(home);
    }

    private static class NavigateHandler extends Handler {
        // non leaky handler
    }

    private static class NavigateRunnable implements Runnable {
        // non leaky runnable
        private final WeakReference<SplashActivity> splashActivityWeakReference;

        NavigateRunnable(SplashActivity activity) {
            splashActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void run() {
            SplashActivity splashActivity = splashActivityWeakReference.get();
            if (splashActivity != null) {
                splashActivity.mNavigateHandler.removeCallbacks(this);
                splashActivity.navigate();
            }
        }
    }
}
