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
package ch.protonmail.android.core;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.hilt.work.HiltWorkerFactory;
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

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import ch.protonmail.android.BuildConfig;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseActivity;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.api.AccountManager;
import ch.protonmail.android.api.NetworkConfigurator;
import ch.protonmail.android.api.NetworkSwitcher;
import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.TokenManager;
import ch.protonmail.android.api.models.AllCurrencyPlans;
import ch.protonmail.android.api.models.Organization;
import ch.protonmail.android.api.models.doh.Proxies;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.models.room.messages.MessagesDatabase;
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.api.segments.event.EventManager;
import ch.protonmail.android.api.services.MessagesService;
import ch.protonmail.android.domain.entity.Id;
import ch.protonmail.android.domain.entity.user.User;
import ch.protonmail.android.domain.entity.user.UserKey;
import ch.protonmail.android.events.ApiOfflineEvent;
import ch.protonmail.android.events.AuthStatus;
import ch.protonmail.android.events.DownloadedAttachmentEvent;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.events.InvalidAccessTokenEvent;
import ch.protonmail.android.events.Login2FAEvent;
import ch.protonmail.android.events.LoginEvent;
import ch.protonmail.android.events.LoginInfoEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.events.PasswordChangeEvent;
import ch.protonmail.android.events.RequestTimeoutEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.StorageLimitEvent;
import ch.protonmail.android.events.organizations.OrganizationEvent;
import ch.protonmail.android.exceptions.ErrorStateGeneratorsKt;
import ch.protonmail.android.fcm.FcmUtil;
import ch.protonmail.android.jobs.FetchLabelsJob;
import ch.protonmail.android.jobs.organizations.GetOrganizationJob;
import ch.protonmail.android.jobs.user.FetchUserSettingsJob;
import ch.protonmail.android.prefs.SecureSharedPreferences;
import ch.protonmail.android.servers.notification.NotificationServer;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.CustomLocale;
import ch.protonmail.android.utils.DownloadUtils;
import ch.protonmail.android.utils.FileUtils;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.crypto.OpenPGP;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.worker.FetchContactsDataWorker;
import ch.protonmail.android.worker.FetchContactsEmailsWorker;
import dagger.hilt.android.HiltAndroidApp;
import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;
import studio.forface.viewstatestore.ViewStateStoreConfig;
import timber.log.Timber;

import static ch.protonmail.android.api.segments.event.EventManagerKt.PREF_LATEST_EVENT;
import static ch.protonmail.android.core.Constants.FCM_MIGRATION_VERSION;
import static ch.protonmail.android.core.Constants.Prefs.PREF_SENT_TOKEN_TO_SERVER;
import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_TO_INBOX;
import static ch.protonmail.android.core.UserManagerKt.PREF_LOGIN_STATE;
import static ch.protonmail.android.core.UserManagerKt.PREF_SHOW_STORAGE_LIMIT_REACHED;
import static ch.protonmail.android.core.UserManagerKt.PREF_SHOW_STORAGE_LIMIT_WARNING;

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
    OpenPGP mOpenPGP;

    @Inject
    NetworkConfigurator networkConfigurator;
    @Inject
    NetworkSwitcher networkSwitcher;
    @Inject
    DownloadUtils downloadUtils;

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

    private ContactsDatabase contactsDatabase;
    private MessagesDatabase messagesDatabase;

    @NonNull
    @Deprecated // Using this is an ERROR!
    @kotlin.Deprecated(message = "Use a better dependency strategy: ideally inject the needed " +
            "dependency directly or, where not possible, inject the Application or the Context")
    public static ProtonMailApplication getApplication() {
        return sInstance;
    }

    @Inject
    HiltWorkerFactory workerFactory;

    @NotNull
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

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        } else {
            Sentry.init(
                    String.format(
                            getString(R.string.sentry_url),
                            BuildConfig.SENTRY_DNS_1,
                            BuildConfig.SENTRY_DNS_2
                    ), new AndroidSentryClientFactory(this));
            Timber.plant(new SentryTree());
        }

        // Try to upgrade TLS Provider if needed
        if (Constants.FeatureFlags.TLS_12_UPGRADE) {
            upgradeTlsProviderIfNeeded();
        }

        // Initialize TrustKit for TLS Certificate Pinning
        TrustKit.initializeWithNetworkSecurityConfiguration(this);

        ViewStateStoreConfig.INSTANCE
                .setErrorStateGenerator(ErrorStateGeneratorsKt.getErrorStateGenerator());

        contactsDatabase = ContactsDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        messagesDatabase = MessagesDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();

        FileUtils.createDownloadsDir(this);
        setupNotificationChannels();

        super.onCreate();

        WorkManager.initialize(this, getWorkManagerConfiguration());

        checkForUpdateAndClearCache();
    }

    private void upgradeTlsProviderIfNeeded() {
        try {
            ProviderInstaller.installIfNeeded(this);
        } catch (GooglePlayServicesRepairableException e) {
            final SharedPreferences prefs = getDefaultSharedPreferences();
            if (!prefs.getBoolean(Constants.Prefs.PREF_DONT_SHOW_PLAY_SERVICES, false)) {
                GoogleApiAvailability.getInstance().showErrorNotification(this, e.getConnectionStatusCode());
            }
        } catch (GooglePlayServicesNotAvailableException e) {
            // we already handle this by showing prompt about GCM notifications
        }
    }

    @NonNull
    public SharedPreferences getDefaultSharedPreferences() {
        return PreferenceManager.getDefaultSharedPreferences(this);
    }

    @NonNull
    public SharedPreferences getSecureSharedPreferences() {
        return SecureSharedPreferences.Companion.getPrefs(this, "ProtonMailSSP", Context.MODE_PRIVATE);
    }

    @NonNull
    @Deprecated // Using it is an ERROR!
    @kotlin.Deprecated(message = "Use with user Id with SecureSharedPreferences#getPrefsForUser(context: Context, userId: Id)")
    public SharedPreferences getSecureSharedPreferences(String username) {
        throw new UnsupportedOperationException("Use with user Id with SecureSharedPreferences.forUser");
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
            mOrganization = event.getResponse().getOrganization();
        }
    }

    @Subscribe
    public void onInvalidAccessTokenEvent(InvalidAccessTokenEvent event) {
        final Intent intent = AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class));
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = mCurrentActivity != null ? mCurrentActivity.get() : null;
        if (activity != null) {
            activity.startActivity(intent);
            activity.finish();
        } else {
            startActivity(intent);
        }
        Id currentUserId = userManager.getCurrentUserId();
        if (currentUserId != null) {
            userManager.logoutOfflineBlocking(currentUserId);
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
                    apiOfflineSnackBar.setAction(getString(R.string.okay), v -> {
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
    public void onPasswordChangeEvent(PasswordChangeEvent event) {
        if (mCurrentActivity != null) {
            final Activity activity = mCurrentActivity.get();
            if (event.getStatus() == AuthStatus.SUCCESS) {
                if (event.getPasswordType() == Constants.PASSWORD_TYPE_LOGIN) {
                    TextExtensions.showToast(activity.getApplicationContext(), R.string.new_login_password_saved);
                } else if (event.getPasswordType() == Constants.PASSWORD_TYPE_MAILBOX) {
                    TextExtensions.showToast(activity.getApplicationContext(), R.string.new_mailbox_password_saved);
                }
            } else {
                String message = event.getStatusMessage();
                if (!mNetworkUtil.isConnected()) {
                    message = getString(R.string.no_connectivity_detected);
                } else if (message == null || message.isEmpty()) {
                    message = getString(R.string.default_error_message);
                }
                TextExtensions.showToast(activity.getApplicationContext(), message);
            }
        }
    }

    @Subscribe
    public void onDownloadAttachmentEvent(DownloadedAttachmentEvent event) {
        final Status status = event.getStatus();
        if (status != Status.FAILED) {
            downloadUtils.viewAttachmentNotification(this, event.getFilename(), event.getAttachmentUri(), !event.isOfflineLoaded());
        }
    }

    private LoginInfoEvent loginInfoEvent;
    private Login2FAEvent login2FAEvent;
    private MailboxLoginEvent mailboxLoginEvent;
    private LoginEvent loginEvent;

    // region login info event
    @Subscribe
    public void onLoginInfoEvent(LoginInfoEvent loginInfoEvent) {
        this.loginInfoEvent = loginInfoEvent;
    }

    @Produce
    public LoginInfoEvent produceLoginInfoEvent() {
        return loginInfoEvent;
    }

    public void resetLoginInfoEvent() {
        loginInfoEvent = null;
    }
    // endregion

    // region login 2fa event
    @Subscribe
    public void onLogin2FAEvent(Login2FAEvent login2FAEvent) {
        this.login2FAEvent = login2FAEvent;
    }

    @Produce
    public Login2FAEvent produceLogin2FAEvent() {
        return login2FAEvent;
    }

    public void resetLogin2FAEvent() {
        login2FAEvent = null;
    }
    // endregion

    // region mailbox event
    @Subscribe
    public void onLoginEvent(MailboxLoginEvent event) {
        mailboxLoginEvent = event;
    }

    @Produce
    public MailboxLoginEvent produceMailboxLoginEvent() {
        return mailboxLoginEvent;
    }

    public void resetMailboxLoginEvent() {
        mailboxLoginEvent = null;
    }
    // endregion

    // region login event
    @Subscribe
    public void onLoginEvent(LoginEvent event) {
        loginEvent = event;
    }

    @Produce
    public LoginEvent produceLoginEvent() {
        return loginEvent;
    }

    public void resetLoginEvent() {
        loginEvent = null;
    }
    // endregion

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

    private static class RefreshMessagesAndAttachments extends AsyncTask<Void, Void, Void> {

        private final MessagesDatabase messagesDatabase;

        private RefreshMessagesAndAttachments(MessagesDatabase messagesDatabase) {
            this.messagesDatabase = messagesDatabase;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            messagesDatabase.clearAttachmentsCache();
            messagesDatabase.clearMessagesCache();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            MessagesService.Companion.startFetchFirstPage(Constants.MessageLocationType.INBOX, false, null, false);
        }
    }

    private void setupNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationServer notificationServer = new NotificationServer(this, notificationManager);
            notificationServer.createEmailsChannel();
            notificationServer.createAttachmentsChannel();
            notificationServer.createRetrievingNotificationsNotification();
            notificationServer.createAccountChannel();
        }
    }

    private void checkForUpdateAndClearCache() {
        final SharedPreferences prefs = getDefaultSharedPreferences();
        mNetworkUtil.setCurrentlyHasConnectivity();
        //refresh local cache if new app version
        int previousVersion = prefs.getInt(Constants.Prefs.PREF_APP_VERSION, Integer.MIN_VALUE);
        if (previousVersion != BuildConfig.VERSION_CODE && previousVersion > 0) {
            prefs.edit().putInt(Constants.Prefs.PREF_PREVIOUS_APP_VERSION, previousVersion).apply();
            prefs.edit().putInt(Constants.Prefs.PREF_APP_VERSION, BuildConfig.VERSION_CODE).apply();
            mUpdateOccurred = true;

            if (userManager.isLoggedIn()) {
                userManager.setCurrentUserLoginState(LOGIN_STATE_TO_INBOX);
            }

            if (BuildConfig.DEBUG) {
                new RefreshMessagesAndAttachments(messagesDatabase).execute();
            }
            if (BuildConfig.FETCH_FULL_CONTACTS && userManager.isLoggedIn()) {
                new FetchContactsEmailsWorker.Enqueuer(WorkManager.getInstance(this)).enqueue(0);
                new FetchContactsDataWorker.Enqueuer(WorkManager.getInstance(this)).enqueue();
            }
            if (BuildConfig.REREGISTER_FOR_PUSH) {
                FcmUtil.setTokenSent(false);
            }
            jobManager.addJobInBackground(new FetchLabelsJob());
            //new version will get set in RegisterGcmJob
            if (userManager != null) {
                // if this version requires the user to be logged out when updatingAttachmentMetadataDatabase
                // and if every single previous version should be force logged out
                // or any specific previous version should be logged out

                Id currentUserId = userManager.getCurrentUserId();
                if (BuildConfig.DEBUG) {
                    AlarmReceiver alarmReceiver = new AlarmReceiver();
                    alarmReceiver.cancelAlarm(this);
                    startJobManager();
                    Set<Id> loggedInUsers = accountManager.allLoggedInBlocking();
                    jobManager.addJobInBackground(new FetchUserSettingsJob(currentUserId));
                    for (Id loggedInUser : loggedInUsers) {
                        if (!loggedInUser.equals(currentUserId)) {
                            jobManager.addJobInBackground(new FetchUserSettingsJob(loggedInUser));
                        }
                    }
                    eventManager.clearState();
                    alarmReceiver.setAlarm(this);
                }
                TokenManager tokenManager = userManager.getTokenManager();
                if (tokenManager != null && TextUtils.isEmpty(tokenManager.getEncPrivateKey())) {
                    User user = userManager.getCurrentUserBlocking();
                    if (user != null) {
                        UserKey primaryKey = user.getKeys().getPrimaryKey();
                        if (primaryKey != null) {
                            tokenManager.setEncPrivateKey(primaryKey.getPrivateKey().getString()); // it's needed for verification later
                        }
                    }
                }

                SharedPreferences defaultSharedPreferences = getDefaultSharedPreferences();
                if (currentUserId != null) {
                    SharedPreferences secureSharedPreferences =
                            SecureSharedPreferences.Companion.getPrefsForUser(
                                    getApplicationContext(),
                                    currentUserId
                            );

                    if (defaultSharedPreferences.contains(PREF_SHOW_STORAGE_LIMIT_WARNING)) {
                        secureSharedPreferences.edit().putBoolean(PREF_SHOW_STORAGE_LIMIT_WARNING,
                                defaultSharedPreferences.getBoolean(PREF_SHOW_STORAGE_LIMIT_WARNING, true)).apply();
                        defaultSharedPreferences.edit().remove(PREF_SHOW_STORAGE_LIMIT_WARNING).apply();
                    }
                    if (defaultSharedPreferences.contains(PREF_SHOW_STORAGE_LIMIT_REACHED)) {
                        secureSharedPreferences.edit().putBoolean(PREF_SHOW_STORAGE_LIMIT_REACHED,
                                defaultSharedPreferences.getBoolean(PREF_SHOW_STORAGE_LIMIT_REACHED, true)).apply();
                        defaultSharedPreferences.edit().remove(PREF_SHOW_STORAGE_LIMIT_REACHED).apply();
                    }
                }

                Set<Id> loggedInUsers = accountManager.allLoggedInBlocking();
                if (defaultSharedPreferences.contains(PREF_LOGIN_STATE)) {
                    for (Id user : loggedInUsers) {
                        SharedPreferences secureSharedPreferencesForUser = SecureSharedPreferences.Companion.getPrefsForUser(getApplicationContext(),
                                user);
                        if (userManager.getMailboxPassword(user) == null) {
                            userManager.logoutBlocking(user);
                        } else {
                            secureSharedPreferencesForUser.edit().putInt(PREF_LOGIN_STATE, LOGIN_STATE_TO_INBOX).apply();
                        }
                    }
                    defaultSharedPreferences.edit().remove(PREF_LOGIN_STATE).apply();
                }
                for (Id user : loggedInUsers) {
                    SharedPreferences secureSharedPreferencesForUser =
                            SecureSharedPreferences.Companion.getPrefsForUser(
                                    getApplicationContext(),
                                    user
                            );
                    if (secureSharedPreferencesForUser.contains(PREF_LATEST_EVENT)) {
                        secureSharedPreferencesForUser.edit().remove(PREF_LATEST_EVENT).apply();
                    }
                }
                if (previousVersion < FCM_MIGRATION_VERSION) {
                    FcmUtil.setTokenSent(false);
                }
                if (defaultSharedPreferences.contains(PREF_SENT_TOKEN_TO_SERVER)) {
                    for (Id user : loggedInUsers) {
                        SharedPreferences secureSharedPreferencesForUser =
                                SecureSharedPreferences.Companion.getPrefsForUser(
                                        getApplicationContext(),
                                        user
                                );
                        secureSharedPreferencesForUser.edit().putBoolean(PREF_SENT_TOKEN_TO_SERVER,
                                defaultSharedPreferences.getBoolean(PREF_SENT_TOKEN_TO_SERVER, false)).apply();
                        defaultSharedPreferences.edit().remove(PREF_SENT_TOKEN_TO_SERVER).apply();
                    }
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

    public void fetchOrganization() {
        GetOrganizationJob getOrganizationJob = new GetOrganizationJob();
        jobManager.addJobInBackground(getOrganizationJob);
    }

    public void notifyLoggedOut(String username) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        INotificationServer notificationServer = new NotificationServer(this, notificationManager);
        if (userManager != null && userManager.isLoggedIn()) {
            notificationServer.notifyUserLoggedOut(userManager.getUser(username));
        }
    }

    public String getCurrentLocale() {
        mCurrentLocale = getResources().getConfiguration().locale.toString();
        return mCurrentLocale;
    }

    public void clearLocaleCache() {
        mCurrentLocale = null;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(CustomLocale.Companion.apply(base));
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        CustomLocale.Companion.apply(this);
    }

    public void changeApiProviders() {
        final SharedPreferences prefs = getDefaultSharedPreferences();
        networkConfigurator.networkSwitcher.reconfigureProxy(Proxies.Companion.getInstance(null, prefs));
    }
}
