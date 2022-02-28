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
package ch.protonmail.android.activities;

import static ch.protonmail.android.worker.FetchUserWorkerKt.FETCH_USER_INFO_WORKER_NAME;
import static ch.protonmail.android.worker.FetchUserWorkerKt.FETCH_USER_INFO_WORKER_RESULT;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.work.WorkManager;

import com.birbit.android.jobqueue.JobManager;
import com.google.android.material.snackbar.Snackbar;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.protonmail.android.R;
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
import ch.protonmail.android.utils.CustomLocale;
import ch.protonmail.android.utils.INetworkConfiguratorCallback;
import ch.protonmail.android.worker.FetchMailSettingsWorker;
import ch.protonmail.android.worker.FetchUserWorker;
import dagger.hilt.android.AndroidEntryPoint;
import me.proton.core.humanverification.presentation.HumanVerificationOrchestrator;
import timber.log.Timber;

@AndroidEntryPoint
public abstract class BaseActivity extends AppCompatActivity implements INetworkConfiguratorCallback {

    public static final String EXTRA_IN_APP = "extra_in_app";
    private static final int NO_LAYOUT_ID = -1;

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
    protected HumanVerificationOrchestrator humanVerificationOrchestrator;
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
    protected FetchUserWorker.Enqueuer fetchUserInfoWorkerEnqueuer;
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
    private String mCurrentLocale;
    protected Snackbar mRequestTimeoutSnack;

    private AlertDialog alertDelinquency;

    /**
     * Optional id for the layout
     *
     * @return the id of the layout to inflate, or {@link #NO_LAYOUT_ID} if
     * {@link #getRootView()} is used
     */
    @LayoutRes
    protected int getLayoutId() {
        return NO_LAYOUT_ID;
    }

    /**
     * Optional View to set as content
     *
     * @return the {@link View} to set as content or {@code null} if {@link #getLayoutId()} is used
     */
    @Nullable
    protected View getRootView() {
        return null;
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
        if (savedInstanceState != null) {
            mCurrentLocale = savedInstanceState.getString("curr_loc");
            if (mCurrentLocale != null && !mCurrentLocale.equals(getResources().getConfiguration().locale.toString())) {
            }
        }
        mCurrentLocale = app.getCurrentLocale();
        buildHtmlProcessor();

        int layoutId = getLayoutId();
        View rootView = getRootView();
        if (layoutId != NO_LAYOUT_ID) {
            setContentView(layoutId);
        } else if (rootView != null) {
            setContentView(rootView);
        }

        ButterKnife.bind(this);
        if (mToolbar != null) {
            setSupportActionBar(mToolbar);
        }

        humanVerificationOrchestrator.register(this, false);
        accountStateManager.setHumanVerificationOrchestrator(humanVerificationOrchestrator);
        accountStateManager.observeHVStateWithExternalLifecycle(getLifecycle());
    }

    @Override
    protected void onDestroy() {
        humanVerificationOrchestrator.unregister(this);
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    protected boolean isPreventingScreenshots() {
        return false;
    }

    @Override
    protected void onResume() {
        super.onResume();

        User user = mUserManager.getCurrentLegacyUser();

        app.setAppInBackground(false);
        networkConfigurator.setNetworkConfiguratorCallback(this);

        accountStateManager.setHumanVerificationOrchestrator(humanVerificationOrchestrator);
    }

    @Override
    protected void onPause() {
        app.setAppInBackground(true);
        networkConfigurator.removeNetworkConfiguratorCallback();
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

    @Override
    protected void onStart() {
        super.onStart();
        app.setAppInBackground(false);
        app.setCurrentActivity(this);
        getWindow().getDecorView().postDelayed(this::deactivateScreenProtector, 500);
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (!(this instanceof AddAttachmentsActivity)) {
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
                accountStateManager.signOutPrimary().invokeOnCompletion(throwable -> {
                    finish();
                    return null;
                });
            });
            btnClose.setOnClickListener(v -> finish());
            btnCheckAgain.setOnClickListener(v -> {
                // TODO: Remove fetchUserInfoWorkerEnqueuer, not adapted for this usage.
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
        tv.setTextColor(this.getColor(R.color.text_inverted));
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

    protected BroadcastReceiver setupLangReceiver() {
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
        super.attachBaseContext(CustomLocale.INSTANCE.apply(base));
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
    public void stopDohSignal() {
        isDohOngoing = false;
        Timber.d("BaseActivity: stopDohSignal");
    }

    @Override
    public void onDohFailed() {
        Timber.d("BaseActivity: Doh All alternative proxies failed");
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
