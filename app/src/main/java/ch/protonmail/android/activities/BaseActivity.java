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

import static ch.protonmail.android.settings.pin.ValidatePinActivityKt.EXTRA_FRAGMENT_TITLE;
import static ch.protonmail.android.settings.pin.ValidatePinActivityKt.EXTRA_PIN_VALID;
import static ch.protonmail.android.worker.FetchUserInfoWorkerKt.FETCH_USER_INFO_WORKER_NAME;
import static ch.protonmail.android.worker.FetchUserInfoWorkerKt.FETCH_USER_INFO_WORKER_RESULT;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.ComponentActivity;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.work.WorkManager;

import com.birbit.android.jobqueue.JobManager;
import com.google.android.material.snackbar.Snackbar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.messageDetails.MessageDetailsActivity;
import ch.protonmail.android.adapters.swipe.SwipeProcessor;
import ch.protonmail.android.api.NetworkConfigurator;
import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.models.MailSettings;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.bl.Html5Handler;
import ch.protonmail.android.bl.HtmlDivHandler;
import ch.protonmail.android.bl.HtmlProcessor;
import ch.protonmail.android.bl.XHtmlHandler;
import ch.protonmail.android.core.BigContentHolder;
import ch.protonmail.android.core.NetworkResults;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.QueueNetworkUtil;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.feature.account.AccountStateManager;
import ch.protonmail.android.jobs.organizations.GetOrganizationJob;
import ch.protonmail.android.settings.pin.ValidatePinActivity;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.CustomLocale;
import ch.protonmail.android.utils.INetworkConfiguratorCallback;
import ch.protonmail.android.worker.FetchMailSettingsWorker;
import ch.protonmail.android.worker.FetchUserInfoWorker;
import dagger.hilt.android.AndroidEntryPoint;
import timber.log.Timber;

@AndroidEntryPoint
public abstract class BaseActivity extends AppCompatActivity implements INetworkConfiguratorCallback {

    public static final String EXTRA_IN_APP = "extra_in_app";
    public static final int REQUEST_CODE_VALIDATE_PIN = 998;

    private ProtonMailApplication app;

    @Deprecated // Doesn't make sense for this to be injected nor be used to sub-classes, as it can
    //              be retrieved directly from the application context
    @Inject
    protected ProtonMailApplication mApp;
    @Inject
    protected ProtonMailApiManager mApi;
    @Inject
    protected NetworkConfigurator networkConfigurator;
    @Inject
    @Deprecated // TODO this should not be used by sub-classes, they should get it injected
    //              directly, as are aiming to remove this base class
    protected UserManager mUserManager;
    @Inject
    protected AccountStateManager accountStateManager;
    @Inject
    protected JobManager mJobManager;
    @Inject
    protected QueueNetworkUtil mNetworkUtil;
    @Inject
    protected SwipeProcessor mSwipeProcessor;
    @Inject
    protected HtmlProcessor mHtmlProcessor;
    @Inject
    protected BigContentHolder mBigContentHolder;
    @Inject
    protected NetworkResults mNetworkResults;
    @Inject
    protected WorkManager workManager;
    @Inject
    protected FetchUserInfoWorker.Enqueuer fetchUserInfoWorkerEnqueuer;
    @Inject
    protected FetchMailSettingsWorker.Enqueuer fetchMailSettingsWorkerEnqueuer;

    @Nullable
    @BindView(R.id.toolbar)
    protected Toolbar mToolbar;
    @Nullable
    @BindView(R.id.layout_no_connectivity_info)
    protected View mConnectivitySnackLayout;
    @Nullable
    @BindView(R.id.screenProtectorView)
    protected View mScreenProtectorLayout;

    private BroadcastReceiver mLangReceiver = null;
    private boolean inApp = false;
    private boolean checkForPin = true;
    private String mCurrentLocale;
    protected Snackbar mRequestTimeoutSnack;

    private AlertDialog alertDelinquency;
    protected boolean mPinValid = false;
    private boolean shouldLock = false;

    protected abstract int getLayoutId();

    protected boolean shouldCheckForAutoLogout() {
        return true;
    }

    protected boolean isDohOngoing = false;
    protected boolean autoRetry = true;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        app = (ProtonMailApplication) getApplication();
        super.onCreate(savedInstanceState);
        app.setAppInBackground(false);
        inApp = getIntent().getBooleanExtra(EXTRA_IN_APP, false);
        if (savedInstanceState != null) {
            mCurrentLocale = savedInstanceState.getString("curr_loc");
            if (mCurrentLocale != null && !mCurrentLocale.equals(getResources().getConfiguration().locale.toString())) {
                inApp = false;
            }
        }
        mCurrentLocale = app.getCurrentLocale();
        accountStateManager.register(this);
        buildHtmlProcessor();

        setContentView(getLayoutId());
        handlePin();
        ButterKnife.bind(this);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        inApp = intent.getBooleanExtra(EXTRA_IN_APP, false);
    }

    @Override
    public void onBackPressed() {
        if (!(this instanceof SplashActivity)) {
            saveLastInteraction();
        }
        finish();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        validationCanceled = true;
        if (checkForPin) {
            handlePin();
        }
    }

    protected boolean isPreventingScreenshots() {
        return false;
    }

    protected boolean secureContent() {
        return false;
    }

    protected void enableScreenshotProtector() { }

    protected void disableScreenshotProtector() { }

    @Override
    protected void onResume() {
        super.onResume();

        User user = mUserManager.getCurrentLegacyUser();

        // Enable secure mode if screenshots are disabled, else disable it
        if (isPreventingScreenshots() || user != null && user.isPreventTakingScreenshots()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
        if (shouldLock && (secureContent() || user != null && user.isPreventTakingScreenshots())) {
            enableScreenshotProtector();
        }
        app.setAppInBackground(false);
        NetworkConfigurator.Companion.setNetworkConfiguratorCallback(this);
    }

    @Override
    protected void onPause() {
        app.setAppInBackground(true);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putString("curr_loc", mCurrentLocale);
        super.onSaveInstanceState(outState);
    }

    protected boolean isAutoShowRemoteImages() {
        MailSettings mailSettings = mUserManager.getCurrentUserMailSettingsBlocking();
        if (mailSettings != null)
            return mailSettings.getShowImagesFrom().includesRemote();
        else
            return false;
    }

    protected boolean isAutoShowEmbeddedImages() {
        MailSettings mailSettings = mUserManager.getCurrentUserMailSettingsBlocking();
        if (mailSettings != null)
            return mailSettings.getShowImagesFrom().includesEmbedded();
        else
            return false;
    }

    private void shouldLock() {
        User user = mUserManager.getCurrentLegacyUser();
        if (user == null)
            return;

        long diff = user.getLastInteractionDiff();

        if (user.isUsePin()) {
            if (diff >= 0) {
                shouldLock = user.shouldPINLockTheApp(diff);
            } else {
                shouldLock = true;
            }
        }
    }

    private void handlePin() {
        app.setAppInBackground(false);
        app.setCurrentActivity(this);
        shouldLock();
        if (!shouldCheckForAutoLogout()) {
            return;
        }

        checkPinLock(shouldLock);
    }

    protected void checkPinLock(boolean shouldLock) {
        if (inApp || mPinValid) {
            this.shouldLock = false;
            mPinValid = false;
            return;
        }
        if (shouldLock) {
            Intent validatePinIntent = new Intent(this, ValidatePinActivity.class);
            if (this instanceof MessageDetailsActivity) {
                validatePinIntent.putExtra(EXTRA_FRAGMENT_TITLE, R.string.enter_pin_message_details);
            }
            Intent pinIntent = AppUtil.decorInAppIntent(validatePinIntent);
            startActivityForResult(pinIntent, REQUEST_CODE_VALIDATE_PIN);
        } else {
            this.shouldLock = false;
        }
    }

    private boolean validationCanceled = true;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((resultCode == RESULT_OK || resultCode == RESULT_CANCELED) && requestCode == REQUEST_CODE_VALIDATE_PIN) {
            if (data == null) {
                return;
            }
            if (resultCode == RESULT_CANCELED) {
                checkForPin = false;
            }
            boolean isValid = data.getBooleanExtra(EXTRA_PIN_VALID, false);
            if (!isValid) {
                validationCanceled = true;
                finish();
            } else {
                if (this instanceof ValidatePinActivity) {
                    validationCanceled = false;
                }
            }
            shouldLock = false;
            disableScreenshotProtector();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        app.setAppInBackground(false);
        getWindow().getDecorView().postDelayed(this::deactivateScreenProtector, 500);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Enable secure mode for hide content from recent if pin is enabled, else disable it so
        // content will be visible in recent
        User currentUser = mUserManager.getCurrentLegacyUser();
        if (currentUser != null && currentUser.isUsePin())
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);

        if (!validationCanceled && !(this instanceof ValidatePinActivity)) {
            saveLastInteraction();
        }
        if (!(this instanceof AddAttachmentsActivity)) {
            inApp = false;
            activateScreenProtector();
        }
    }

    private void deactivateScreenProtector() {
        if (mScreenProtectorLayout != null) {
            mScreenProtectorLayout.setVisibility(View.GONE);
        }
    }

    private void activateScreenProtector() {
        User currentUser = mUserManager.getCurrentLegacyUser();
        if (currentUser != null && currentUser.isUsePin()) {
            if (mScreenProtectorLayout != null) {
                mScreenProtectorLayout.setVisibility(View.VISIBLE);
            }
        }
    }

    protected void saveLastInteraction() {
        User user = mUserManager.getCurrentLegacyUser();
        if (user != null) user.setLastInteraction(SystemClock.elapsedRealtime());
    }

    protected void checkDelinquency() {
        ch.protonmail.android.domain.entity.user.User user = mUserManager.getCurrentUser();
        if (user == null) return;
        boolean areMailRoutesAccessible = user.getDelinquent().getMailRoutesAccessible();
        if (!areMailRoutesAccessible && (alertDelinquency == null || !alertDelinquency.isShowing())) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.layout_delinquency_dialog, null, false);
            TextView subtitle = dialogView.findViewById(R.id.subtitle);
            subtitle.setMovementMethod(LinkMovementMethod.getInstance());
            Button btnClose = dialogView.findViewById(R.id.close_app);
            Button btnCheckAgain = dialogView.findViewById(R.id.recheck);
            Button btnLogout = dialogView.findViewById(R.id.logout);

            btnLogout.setOnClickListener(v -> {
                accountStateManager.logoutPrimary().invokeOnCompletion(throwable -> {
                    finish();
                    return null;
                });
            });
            btnClose.setOnClickListener(v -> finish());
            btnCheckAgain.setOnClickListener(v -> {
                fetchUserInfoWorkerEnqueuer.invoke(user.getId());
                workManager.getWorkInfosForUniqueWorkLiveData(FETCH_USER_INFO_WORKER_NAME)
                        .observe(this, workInfo -> {
                            boolean isDelinquent = workInfo.get(0).getOutputData().getBoolean(FETCH_USER_INFO_WORKER_RESULT, true);
                            if (!isDelinquent && alertDelinquency != null && alertDelinquency.isShowing()) {
                                alertDelinquency.dismiss();
                            }
                        });
            });
            builder.setView(dialogView);
            alertDelinquency = builder.create();
            alertDelinquency.setCanceledOnTouchOutside(false);
            alertDelinquency.setCancelable(false);
            if (!isFinishing()) {
                alertDelinquency.show();
            }
        }
    }

    public void showRequestTimeoutSnack() {
        mRequestTimeoutSnack = Snackbar.make(mConnectivitySnackLayout, getString(R.string.request_timeout), Snackbar.LENGTH_LONG);
        View view = mRequestTimeoutSnack.getView();
        TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
        tv.setTextColor(Color.WHITE);
        mRequestTimeoutSnack.show();
    }

    protected void fetchOrganizationData() {
        User user = mUserManager.getCurrentLegacyUser();
        if (user != null && user.isPaidUser()) {
            GetOrganizationJob getOrganizationJob = new GetOrganizationJob();
            mJobManager.addJobInBackground(getOrganizationJob);
        } else {
            app.setOrganization(null);
        }
    }

    protected BroadcastReceiver setupLangReceiver(){
        if (mLangReceiver == null) {
            mLangReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    // noop
                }
            };

            IntentFilter filter = new IntentFilter(Intent.ACTION_LOCALE_CHANGED);
            registerReceiver(mLangReceiver, filter);
        }

        return mLangReceiver;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(CustomLocale.Companion.apply(base));
    }

    private void buildHtmlProcessor() {
        mHtmlProcessor.addHandler(new HtmlDivHandler());
        mHtmlProcessor.addHandler(new Html5Handler());
        mHtmlProcessor.addHandler(new XHtmlHandler());
    }

    @Override
    public void onApiProvidersChanged() {
        networkConfigurator.refreshDomainsAsync();
    }

    @Override
    public void startDohSignal() {
        isDohOngoing = true;
        Timber.d("BaseActivity: startDohSignal");
    }

    @Override
    public void stopDohSignal () {
        isDohOngoing = false;
        Timber.d("BaseActivity: stopDohSignal");
    }

    @Override
    public void startAutoRetry() {
        autoRetry = true;
    }

    @Override
    public void stopAutoRetry() {
        autoRetry = false;
    }
}
