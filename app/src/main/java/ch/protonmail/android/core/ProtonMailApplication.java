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
package ch.protonmail.android.core;

import static ch.protonmail.android.api.segments.event.EventManagerKt.PREF_LATEST_EVENT;
import static ch.protonmail.android.core.Constants.FCM_MIGRATION_VERSION;
import static ch.protonmail.android.core.Constants.Prefs.PREF_NEW_USER_ONBOARDING_SHOWN;
import static ch.protonmail.android.core.Constants.Prefs.PREF_SENT_TOKEN_TO_SERVER;
import static ch.protonmail.android.core.Constants.Prefs.PREF_TIME_AND_DATE_CHANGED;
import static ch.protonmail.android.core.Constants.PrefsType.BACKUP_PREFS_NAME;
import static ch.protonmail.android.core.UserManagerKt.PREF_SHOW_STORAGE_LIMIT_REACHED;
import static ch.protonmail.android.core.UserManagerKt.PREF_SHOW_STORAGE_LIMIT_WARNING;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.StrictMode;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.hilt.work.HiltWorkerFactory;
import androidx.startup.AppInitializer;
import androidx.work.WorkManager;

import com.birbit.android.jobqueue.JobManager;
import com.datatheorem.android.trustkit.TrustKit;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import ch.protonmail.android.BuildConfig;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseActivity;
import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.models.AllCurrencyPlans;
import ch.protonmail.android.api.models.Organization;
import ch.protonmail.android.api.models.OrganizationResponse;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.api.segments.event.EventManager;
import ch.protonmail.android.di.DefaultSharedPreferences;
import ch.protonmail.android.events.ApiOfflineEvent;
import ch.protonmail.android.events.DownloadedAttachmentEvent;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.events.RequestTimeoutEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.StorageLimitEvent;
import ch.protonmail.android.events.organizations.OrganizationEvent;
import ch.protonmail.android.exceptions.ErrorStateGeneratorsKt;
import ch.protonmail.android.feature.account.AccountManagerKt;
import ch.protonmail.android.feature.account.AccountStateHandlerInitializer;
import ch.protonmail.android.feature.account.CoreAccountManagerMigration;
import ch.protonmail.android.featureflags.FeatureFlagsInitializer;
import ch.protonmail.android.notifications.data.remote.fcm.MultiUserFcmTokenManager;
import ch.protonmail.android.notifications.presentation.utils.NotificationServer;
import ch.protonmail.android.onboarding.base.presentation.AddStartOnboardingObserverIfNeeded;
import ch.protonmail.android.prefs.SecureSharedPreferences;
import ch.protonmail.android.security.presentation.SecurityManagerInitializer;
import ch.protonmail.android.sentry.SentryInitializer;
import ch.protonmail.android.sentry.SentryTree;
import ch.protonmail.android.settings.domain.usecase.ApplyAppThemeFromSettings;
import ch.protonmail.android.utils.CoreTimberLogger;
import ch.protonmail.android.utils.CustomLocale;
import ch.protonmail.android.utils.DownloadUtils;
import ch.protonmail.android.utils.FileUtils;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.crypto.OpenPGP;
import ch.protonmail.android.worker.FetchContactsDataWorker;
import ch.protonmail.android.worker.FetchContactsEmailsWorker;
import dagger.hilt.android.HiltAndroidApp;
import me.proton.core.accountmanager.domain.AccountManager;
import me.proton.core.auth.presentation.MissingScopeInitializer;
import me.proton.core.crypto.validator.presentation.init.CryptoValidatorInitializer;
import me.proton.core.domain.entity.UserId;
import me.proton.core.humanverification.presentation.HumanVerificationInitializer;
import me.proton.core.network.presentation.init.UnAuthSessionFetcherInitializer;
import me.proton.core.plan.presentation.UnredeemedPurchaseInitializer;
import me.proton.core.util.kotlin.CoreLogger;
import studio.forface.viewstatestore.ViewStateStoreConfig;
import timber.log.Timber;

@HiltAndroidApp
public class ProtonMailApplication extends Application implements androidx.work.Configuration.Provider {

    private static ProtonMailApplication sInstance;

    @Inject
    UserManager userManager;
    @Inject
    AccountManager accountManager;
    @Inject
    EventManager eventManager;
    @Inject
    JobManager jobManager;
    @Inject
    QueueNetworkUtil mNetworkUtil;
    @Inject
    ProtonMailApiManager mApi;
    @Inject
    SecureSharedPreferences.Factory secureSharedPreferencesFactory;
    @Inject
    OpenPGP mOpenPGP;

    @Inject
    ApplyAppThemeFromSettings applyAppThemeFromSettings;

    @Inject
    DownloadUtils downloadUtils;

    @Inject
    MultiUserFcmTokenManager multiUserFcmTokenManager;

    @Inject
    ch.protonmail.android.api.AccountManager.UsernameToIdMigration accountManagerUserIdMigration;
    @Inject
    CoreAccountManagerMigration coreAccountManagerMigration;

    @Inject
    @DefaultSharedPreferences
    SharedPreferences defaultSharedPreferences;

    @Inject
    AddStartOnboardingObserverIfNeeded addStartOnboardingObserverIfNeeded;

    private Bus mBus;
    private boolean appInBackground;
    private Snackbar apiOfflineSnackBar;
    @Nullable
    private StorageLimitEvent mLastStorageLimitEvent;
    private WeakReference<Activity> mCurrentActivity;
    private boolean mUpdateOccurred;
    private AllCurrencyPlans mAllCurrencyPlans;
    private Organization mOrganization;
    private String mCurrentLocale;
    private AlertDialog forceUpgradeDialog;

    @NonNull
    @Deprecated // Using this is an ERROR!
    @kotlin.Deprecated(message = "Use a better dependency strategy: ideally inject the needed " +
            "dependency directly or, where not possible, inject the Application or the Context")
    public static ProtonMailApplication getApplication() {
        return sInstance;
    }

    @Inject
    HiltWorkerFactory workerFactory;

    @NonNull
    @Override
    public androidx.work.Configuration getWorkManagerConfiguration() {
        return new androidx.work.Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build();
    }

    @Override
    public void onCreate() {
        sInstance = this;
        appInBackground = true;
        mBus = new Bus();
        mBus.register(this);

        CoreLogger.INSTANCE.set(new CoreTimberLogger());

        AppInitializer appInitializer = AppInitializer.getInstance(this);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            if (BuildConfig.FLAVOR != "uiAutomation") enableStrictMode();
        } else {
            appInitializer.initializeComponent(SentryInitializer.class);
            Timber.plant(new SentryTree());
        }

        TrustKit.initializeWithNetworkSecurityConfiguration(this);

        ViewStateStoreConfig.INSTANCE
                .setErrorStateGenerator(ErrorStateGeneratorsKt.getErrorStateGenerator());

        FileUtils.createDownloadsDir(this);
        setupNotificationChannels();

        super.onCreate();

        // Try to upgrade TLS Provider if needed
        upgradeTlsProviderIfNeeded();

        applyAppThemeFromSettings.blocking();

        accountManagerUserIdMigration.blocking();
        coreAccountManagerMigration.migrateBlocking();

        // Start internal mandatory Account state handling (after coreAccountManagerMigration).
        appInitializer.initializeComponent(AccountStateHandlerInitializer.class);
        appInitializer.initializeComponent(CryptoValidatorInitializer.class);
        appInitializer.initializeComponent(SecurityManagerInitializer.class);
        appInitializer.initializeComponent(HumanVerificationInitializer.class);
        appInitializer.initializeComponent(MissingScopeInitializer.class);
        appInitializer.initializeComponent(UnredeemedPurchaseInitializer.class);
        appInitializer.initializeComponent(UnAuthSessionFetcherInitializer.class);
        appInitializer.initializeComponent(FeatureFlagsInitializer.class);

        checkForUpdateAndClearCache();
    }

    private void upgradeTlsProviderIfNeeded() {
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (GooglePlayServicesRepairableException e) {
            if (!defaultSharedPreferences.getBoolean(Constants.Prefs.PREF_DONT_SHOW_PLAY_SERVICES, false)) {
                GoogleApiAvailability.getInstance().showErrorNotification(this, e.getConnectionStatusCode());
            }
        } catch (GooglePlayServicesNotAvailableException e) {
            // we already handle this by showing prompt about GCM notifications
        }
    }

    @NonNull
    @Deprecated
    @kotlin.Deprecated(message = "Use SecureSharedPreferences.Factory.appPreferences")
    public SharedPreferences getSecureSharedPreferences() {
        return secureSharedPreferencesFactory.appPreferences();
    }

    @NonNull
    public Bus getBus() {
        return mBus;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public void startJobManager() {
        if (jobManager != null) {
            jobManager.start();
        }
    }

    @Produce
    public StorageLimitEvent produceStorageLimitEvent() {
        final StorageLimitEvent latestEvent = mLastStorageLimitEvent;
        mLastStorageLimitEvent = null;
        return latestEvent;
    }

    @Subscribe
    public void onOrganizationEvent(OrganizationEvent event) {
        if (event.getStatus() == Status.SUCCESS) {
            OrganizationResponse response = event.getResponse().getValueOrNull();
            if (response != null) {
                mOrganization = response.getOrganization();
            }
        }
    }

    @Subscribe
    public void onRequestTimeoutEvent(RequestTimeoutEvent event) {
        if (mCurrentActivity != null) {
            Activity activity = mCurrentActivity.get();
            if (activity != null && activity instanceof BaseActivity) {
                ((BaseActivity) activity).showRequestTimeoutSnack();
            }
        }
    }

    @Subscribe
    public void onForceUpgradeEvent(ForceUpgradeEvent event) {
        if (mCurrentActivity != null) {
            final Activity activity = mCurrentActivity.get();
            AlarmReceiver alarmReceiver = new AlarmReceiver();
            alarmReceiver.cancelAlarm(this);
            if (!activity.isFinishing() && (forceUpgradeDialog == null || !forceUpgradeDialog.isShowing())) {
                forceUpgradeDialog = UiUtil.buildForceUpgradeDialog(activity, event.getMessage());
                forceUpgradeDialog.show();
            }
        }
    }

    @Subscribe
    public void onApiOfflineEvent(ApiOfflineEvent event) {
        if (mCurrentActivity != null) {
            final Activity activity = mCurrentActivity.get();
            if (activity != null && !activity.isFinishing()) {
                if (apiOfflineSnackBar == null || !apiOfflineSnackBar.isShown()) {
                    String message = event.getMessage();
                    if (TextUtils.isEmpty(message)) {
                        message = getResources().getString(R.string.api_offline);
                    }
                    final SpannableString s = new SpannableString(message);
                    Linkify.addLinks(s, Linkify.ALL);
                    apiOfflineSnackBar = Snackbar.make(
                            ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0),
                            message, Snackbar
                                    .LENGTH_INDEFINITE);
                    View view = apiOfflineSnackBar.getView();
                    TextView tv = view.findViewById(com.google.android.material.R.id.snackbar_text);
                    tv.setMaxLines(4);
                    tv.setTextColor(getResources().getColor(R.color.icon_purple));
                    apiOfflineSnackBar.setAction(getString(R.string.ok), v -> {
                        if (apiOfflineSnackBar != null) {
                            apiOfflineSnackBar.dismiss();
                        }
                    });
                    apiOfflineSnackBar.setActionTextColor(getResources().getColor(R.color.white));
                    apiOfflineSnackBar.show();
                }
            }
        }
    }

    @Subscribe
    public void onDownloadAttachmentEvent(DownloadedAttachmentEvent event) {
        final Status status = event.getStatus();
        if (status != Status.FAILED) {
            downloadUtils.viewAttachmentNotification(event.getFilename(), event.getAttachmentUri(), !event.isOfflineLoaded());
        }
    }

    public AllCurrencyPlans getAllCurrencyPlans() {
        return mAllCurrencyPlans;
    }

    public void setAllCurrencyPlans(AllCurrencyPlans allCurrencyPlans) {
        this.mAllCurrencyPlans = allCurrencyPlans;
    }

    public boolean hasUpdateOccurred() {
        return mUpdateOccurred;
    }

    public void updateDone() {
        mUpdateOccurred = false;
    }

    private void setupNotificationChannels() {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationServer notificationServer = new NotificationServer(this, notificationManager);
            notificationServer.createEmailsChannel();
            notificationServer.createAttachmentsChannel();
            notificationServer.createRetrievingNotificationsNotification();
            notificationServer.createAccountChannel();
    }

    private void checkForUpdateAndClearCache() {
        final SharedPreferences prefs = defaultSharedPreferences;
        mNetworkUtil.setCurrentlyHasConnectivity();
        //refresh local cache if new app version
        int previousVersion = prefs.getInt(Constants.Prefs.PREF_APP_VERSION, Integer.MIN_VALUE);
        if (previousVersion != BuildConfig.VERSION_CODE && previousVersion > 0) {
            prefs.edit().putInt(Constants.Prefs.PREF_PREVIOUS_APP_VERSION, previousVersion).apply();
            prefs.edit().putInt(Constants.Prefs.PREF_APP_VERSION, BuildConfig.VERSION_CODE).apply();
            mUpdateOccurred = true;

            if (BuildConfig.FETCH_FULL_CONTACTS) {
                new FetchContactsEmailsWorker.Enqueuer(WorkManager.getInstance(this)).enqueue(0);
                new FetchContactsDataWorker.Enqueuer(WorkManager.getInstance(this)).enqueue();
            }
            if (BuildConfig.REREGISTER_FOR_PUSH) {
                multiUserFcmTokenManager.setTokenUnsentForAllSavedUsersBlocking();
            }
            //new version will get set in RegisterGcmJob
            if (userManager != null) {
                // if this version requires the user to be logged out when updatingAttachmentMetadataDatabase
                // and if every single previous version should be force logged out
                // or any specific previous version should be logged out

                UserId currentUserId = userManager.getCurrentUserId();
                if (currentUserId != null) {
                    SharedPreferences secureSharedPreferences =
                            secureSharedPreferencesFactory.userPreferences(currentUserId);

                    if (defaultSharedPreferences.contains(PREF_SHOW_STORAGE_LIMIT_WARNING)) {
                        secureSharedPreferences.edit().putBoolean(
                                PREF_SHOW_STORAGE_LIMIT_WARNING,
                                defaultSharedPreferences.getBoolean(PREF_SHOW_STORAGE_LIMIT_WARNING, true)
                        ).apply();
                        defaultSharedPreferences.edit().remove(PREF_SHOW_STORAGE_LIMIT_WARNING).apply();
                    }
                    if (defaultSharedPreferences.contains(PREF_SHOW_STORAGE_LIMIT_REACHED)) {
                        secureSharedPreferences.edit().putBoolean(
                                PREF_SHOW_STORAGE_LIMIT_REACHED,
                                defaultSharedPreferences.getBoolean(PREF_SHOW_STORAGE_LIMIT_REACHED, true)
                        ).apply();
                        defaultSharedPreferences.edit().remove(PREF_SHOW_STORAGE_LIMIT_REACHED).apply();
                    }

                    addStartOnboardingObserverIfNeeded
                            .invoke(AppInitializer.getInstance(this), currentUserId);
                }

                Set<UserId> loggedInUsers = AccountManagerKt.allLoggedInBlocking(accountManager);
                Map<UserId, SharedPreferences> allLoggedInUserPreferences = new HashMap();
                for (UserId user : loggedInUsers) {
                    allLoggedInUserPreferences.put(
                            user,
                            secureSharedPreferencesFactory.userPreferences(user)
                    );
                }
                for (Map.Entry<UserId, SharedPreferences> entry : allLoggedInUserPreferences.entrySet()) {
                    SharedPreferences userPrefs = entry.getValue();
                    if (userPrefs.contains(PREF_LATEST_EVENT)) {
                        userPrefs.edit().remove(PREF_LATEST_EVENT).apply();
                    }
                }
                if (previousVersion < FCM_MIGRATION_VERSION) {
                    multiUserFcmTokenManager.setTokenUnsentForAllSavedUsersBlocking();
                }
                if (defaultSharedPreferences.contains(PREF_SENT_TOKEN_TO_SERVER)) {
                    for (Map.Entry<UserId, SharedPreferences> entry : allLoggedInUserPreferences.entrySet()) {
                        SharedPreferences userPrefs = entry.getValue();
                        userPrefs.edit().putBoolean(PREF_SENT_TOKEN_TO_SERVER,
                                defaultSharedPreferences.getBoolean(PREF_SENT_TOKEN_TO_SERVER, false)).apply();
                        defaultSharedPreferences.edit().remove(PREF_SENT_TOKEN_TO_SERVER).apply();
                    }
                    for (Map.Entry<UserId, SharedPreferences> entry : allLoggedInUserPreferences.entrySet()) {
                        SharedPreferences userPrefs = entry.getValue();
                        userPrefs.edit().putBoolean(PREF_SENT_TOKEN_TO_SERVER,
                                defaultSharedPreferences.getBoolean(PREF_SENT_TOKEN_TO_SERVER, false)).apply();
                        defaultSharedPreferences.edit().remove(PREF_SENT_TOKEN_TO_SERVER).apply();
                    }
                }
                if (userManager.getCurrentUserId() != null && !defaultSharedPreferences.contains(PREF_NEW_USER_ONBOARDING_SHOWN)) {
                    defaultSharedPreferences.edit().putBoolean(PREF_NEW_USER_ONBOARDING_SHOWN, true).apply();
                }
            }
        } else {
            mUpdateOccurred = false;
            if (previousVersion < 0) {
                prefs.edit().putInt(Constants.Prefs.PREF_APP_VERSION, BuildConfig.VERSION_CODE).apply();
            }
        }
    }

    @Deprecated
    @kotlin.Deprecated(message = "Please use injected UserManager instead")
    public UserManager getUserManager() {
        return userManager;
    }

    @Deprecated
    @kotlin.Deprecated(message = "Please use injected ProtonMailApiManager instead")
    public ProtonMailApiManager getApi() {
        return mApi;
    }

    @Deprecated
    @kotlin.Deprecated(message = "Please use injected OpenPGP instead")
    public OpenPGP getOpenPGP() {
        return mOpenPGP;
    }

    @Deprecated
    @kotlin.Deprecated(message = "Please use injected EventManager instead")
    public EventManager getEventManager() {
        return eventManager;
    }

    public void setCurrentActivity(BaseActivity activity) {
        if (apiOfflineSnackBar != null && apiOfflineSnackBar.isShown()) {
            apiOfflineSnackBar.dismiss();
        }
        apiOfflineSnackBar = null;
        mCurrentActivity = new WeakReference<>(activity);
    }

    public Activity getCurrentActivity() {
        if (mCurrentActivity != null) {
            return mCurrentActivity.get();
        }
        return null;
    }

    public boolean isAppInBackground() {
        return appInBackground;
    }

    public void setAppInBackground(boolean appInBackground) {
        this.appInBackground = appInBackground;
    }

    @Nullable
    public Organization getOrganization() {
        return mOrganization;
    }

    public void setOrganization(Organization organization) {
        mOrganization = organization;
    }

    public String getCurrentLocale() {
        mCurrentLocale = getResources().getConfiguration().locale.toString();
        return mCurrentLocale;
    }

    public void clearLocaleCache() {
        mCurrentLocale = null;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        CustomLocale.INSTANCE.apply(this);
    }

    public boolean isChangedSystemTimeDate() {
        final SharedPreferences pref = getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        return pref.getBoolean(PREF_TIME_AND_DATE_CHANGED, false);
    }

    public void setChangedSystemTimeDate(boolean changed) {
        final SharedPreferences pref = getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE);
        pref.edit().putBoolean(PREF_TIME_AND_DATE_CHANGED, changed).apply();
    }

    private void enableStrictMode() {
        StrictMode.ThreadPolicy.Builder threadPolicyBuilder = new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyFlashScreen()
                .penaltyLog();
        StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog();

        StrictMode.setThreadPolicy(threadPolicyBuilder.build());
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }

}
