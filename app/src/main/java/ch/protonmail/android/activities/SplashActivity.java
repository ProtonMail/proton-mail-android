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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;

import ch.protonmail.android.R;
import ch.protonmail.android.activities.guest.FirstActivity;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.activities.guest.MailboxLoginActivity;
import ch.protonmail.android.activities.mailbox.MailboxActivity;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.api.services.LoginService;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.events.LoginInfoEvent;
import ch.protonmail.android.events.user.UserSettingsEvent;
import ch.protonmail.android.jobs.FetchMailSettingsJob;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.extensions.TextExtensions;

import static ch.protonmail.android.activities.NavigationActivityKt.EXTRA_FIRST_LOGIN;
import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_LOGIN_FINISHED;
import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_NOT_INITIALIZED;
import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_TO_INBOX;

public class SplashActivity extends BaseActivity {

    private static final String TAG_SPLASH_ACTIVITY = "SplashActivity";

    private static final int DELAY = 2000;
    private static final int RECHECK_DELAY = 500;
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
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0 &&
                mUserManager.loginState(mUserManager.getUsername()) == LOGIN_STATE_TO_INBOX) {
            alarmReceiver.setAlarm(mApp, true);
            Intent home = new Intent(this, MailboxActivity.class);
            home.putExtra(EXTRA_FIRST_LOGIN, mApp.hasUpdateOccurred());
            home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(home);
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startRunning();
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    private void startRunning() {
        mNavigateRunnable = new NavigateRunnable(this);
        mNavigateHandler.postDelayed(mNavigateRunnable, DELAY);
    }

    private void navigate() {
        String currentUser = mUserManager.getUsername();
        int loginState = mUserManager.loginState(currentUser);
        if (loginState == LOGIN_STATE_NOT_INITIALIZED) {
            if (mUserManager.isEngagementShown()) {
                startActivity(new Intent(this, LoginActivity.class));
            } else {
                startActivity(new Intent(this, FirstActivity.class));
            }
        } else if (loginState == LOGIN_STATE_LOGIN_FINISHED) {
            // login finished but mailbox login not
            Intent mailboxLoginIntent = new Intent(this, MailboxLoginActivity.class);
            String keySalt = mUserManager.getKeySalt();
            if (keySalt != null) {
                mailboxLoginIntent.putExtra(MailboxLoginActivity.EXTRA_KEY_SALT, keySalt);
                startActivity(AppUtil.decorInAppIntent(mailboxLoginIntent));
            } else {
                startActivity(new Intent(this, LoginActivity.class));
            }
        } else {
            if (mUserManager.accessTokenExists() && !mUserManager.getUser().getAddresses().isEmpty()) {
                mUserManager.setLoggedIn(currentUser, true);
                mJobManager.addJobInBackground(new FetchMailSettingsJob());
                goHome();
            } else {
                LoginService.fetchUserDetails();
                return;
            }
        }
        finish();
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
            mUserManager.setLoggedIn(event.getUserSettings().getUsername(), true);
            goHome();
        }
    }

    @Subscribe
    public void onLoginInfoEvent(final LoginInfoEvent event) {
        if (event == null) {
            return;
        }
        ProtonMailApplication.getApplication().resetLoginInfoEvent();
        switch (event.status) {
            case SUCCESS: {
                mUserManager.saveKeySalt(event.response.getSalt(), event.username);
                startActivity(new Intent(this, MailboxLoginActivity.class));
                finish();
            }
            break;
            case NO_NETWORK: {
                startActivity(new Intent(this, LoginActivity.class));
            }
            break;
            case UPDATE: {
                AppUtil.postEventOnUi(new ForceUpgradeEvent(event.getError()));
            }
            break;
            case FAILED:
            default: {
                TextExtensions.showToast(this, R.string.login_failure);
            }
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
                if (!ProtonMailApplication.getApplication().isInitialized()) {
                    splashActivity.mNavigateHandler.postDelayed(this, RECHECK_DELAY);
                    Logger.doLog(TAG_SPLASH_ACTIVITY, "app not initialized, delay navigate");
                } else {
                    splashActivity.mNavigateHandler.removeCallbacks(this);
                    splashActivity.navigate();
                }
            }
        }
    }
}
