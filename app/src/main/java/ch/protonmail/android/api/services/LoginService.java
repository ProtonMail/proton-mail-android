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

import static java.nio.charset.StandardCharsets.UTF_8;
import static ch.protonmail.android.BuildConfig.SAFETY_NET_API_KEY;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.ProtonJobIntentService;

import com.birbit.android.jobqueue.JobManager;
import com.google.android.gms.safetynet.SafetyNet;
import com.proton.gopenpgp.crypto.ClearTextMessage;
import com.proton.gopenpgp.helper.Helper;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import ch.protonmail.android.api.AccountManager;
import ch.protonmail.android.api.ProtonMailApi;
import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.TokenManager;
import ch.protonmail.android.api.models.KeySalts;
import ch.protonmail.android.api.models.Keys;
import ch.protonmail.android.api.models.KeysSetupBody;
import ch.protonmail.android.api.models.LoginInfoResponse;
import ch.protonmail.android.api.models.LoginResponse;
import ch.protonmail.android.api.models.MailSettings;
import ch.protonmail.android.api.models.MailSettingsResponse;
import ch.protonmail.android.api.models.ModulusResponse;
import ch.protonmail.android.api.models.PasswordVerifier;
import ch.protonmail.android.api.models.TwoFABody;
import ch.protonmail.android.api.models.TwoFAResponse;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.UserInfo;
import ch.protonmail.android.api.models.UserSettings;
import ch.protonmail.android.api.models.UserSettingsResponse;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.address.AddressKeyActivationWorker;
import ch.protonmail.android.api.models.address.AddressPrivateKey;
import ch.protonmail.android.api.models.address.AddressSetupBody;
import ch.protonmail.android.api.models.address.AddressSetupResponse;
import ch.protonmail.android.api.models.address.AddressesResponse;
import ch.protonmail.android.api.models.address.SignedKeyList;
import ch.protonmail.android.api.models.requests.UpgradePasswordBody;
import ch.protonmail.android.api.segments.BaseApiKt;
import ch.protonmail.android.api.segments.event.AlarmReceiver;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.QueueNetworkUtil;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.domain.entity.Id;
import ch.protonmail.android.domain.entity.Name;
import ch.protonmail.android.domain.util.EitherKt;
import ch.protonmail.android.events.AddressSetupEvent;
import ch.protonmail.android.events.AuthStatus;
import ch.protonmail.android.events.ConnectAccountLoginEvent;
import ch.protonmail.android.events.ConnectAccountMailboxLoginEvent;
import ch.protonmail.android.events.CreateUserEvent;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.events.KeysSetupEvent;
import ch.protonmail.android.events.Login2FAEvent;
import ch.protonmail.android.events.LoginEvent;
import ch.protonmail.android.events.LoginInfoEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.events.user.UserSettingsEvent;
import ch.protonmail.android.usecase.FindUserIdForUsername;
import ch.protonmail.android.usecase.fetch.LaunchInitialDataFetch;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.ConstantTime;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.PasswordUtils;
import ch.protonmail.android.utils.SRPClient;
import ch.protonmail.android.utils.crypto.KeyType;
import ch.protonmail.android.utils.crypto.OpenPGP;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import kotlin.text.Charsets;
import timber.log.Timber;

@AndroidEntryPoint
public class LoginService extends ProtonJobIntentService {
    private static final String TAG_LOGIN_SERVICE = "LoginService";

    private static final String ACTION_CREATE_USER = "ACTION_CREATE_USER";
    private static final String ACTION_GENERATE_KEYS = "ACTION_GENERATE_KEYS";
    private static final String ACTION_LOGIN = "ACTION_LOGIN";
    private static final String ACTION_2FA = "ACTION_2FA";
    private static final String ACTION_LOGIN_INFO = "ACTION_LOGIN_INFO";
    private static final String ACTION_MAILBOX_LOGIN = "ACTION_MAILBOX_LOGIN";
    private static final String ACTION_SETUP_ADDRESS = "ACTION_SETUP_ADDRESS";
    private static final String ACTION_SETUP_KEYS = "ACTION_SETUP_KEYS";
    private static final String ACTION_CONNECT_ACCOUNT_LOGIN = "ACTION_CONNECT_ACCOUNT_LOGIN";
    private static final String ACTION_CONNECT_ACCOUNT_MAILBOX_LOGIN = "ACTION_CONNECT_ACCOUNT_MAILBOX_LOGIN";
    private static final String ACTION_FETCH_USER_DETAILS = "ACTION_FETCH_USER_DETAILS";

    private static final String EXTRA_KEY_SALT = "key_salt";
    private static final String EXTRA_CURRENT_PRIMARY_USER_ID = "current_primary_user_id";

    // Parameters for ACTION_LOGIN
    private static final String EXTRA_USERNAME = "username";
    private static final String EXTRA_USER_ID = "user_id";
    private static final String EXTRA_PASSWORD = "password";
    private static final String EXTRA_TWO_FACTOR = "two_factor";
    private static final String EXTRA_LOGIN_INFO_RESPONSE = "login_info_response";
    private static final String EXTRA_LOGIN_RESPONSE = "login_response";
    private static final String EXTRA_FALLBACK_AUTH_VERSION = "fallback_auth_version";
    private static final String EXTRA_BITS = "bits";
    private static final String EXTRA_SIGNUP = "signup";
    private static final String EXTRA_CONNECTING = "connecting";

    // Parameters for ACTION_MAILBOX_LOGIN
    private static final String EXTRA_MAILBOX_PASSWORD = "mailbox_password";

    // Parameters for ACTION_CREATE_USER
    private static final String EXTRA_UPDATE_ME = "update_me";
    private static final String EXTRA_DOMAIN = "domain";
    private static final String EXTRA_TOKEN = "token";
    private static final String EXTRA_TOKEN_TYPE = "token_type";

    // Parameters for ACTION_SETUP_ADDRESS
    private static final String EXTRA_ADDRESS_DOMAIN = "address_domain";

    // Parameters for ACTION_SETUP_KEYS
    private static final String EXTRA_ADDRESS_ID = "address_id";

    @Inject
    AccountManager accountManager;
    @Inject
    UserManager userManager;
    @Inject
    OpenPGP openPGP;
    @Inject
    ProtonMailApiManager api;
    @Inject
    JobManager jobManager;
    @Inject
    QueueNetworkUtil networkUtils;
    @Inject
    FindUserIdForUsername findUserIdForUsername;
    @Inject
    LaunchInitialDataFetch launchInitialDataFetch;

    private TokenManager tokenManager;

    public LoginService() {
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        final String action = intent.getAction();
        Log.d("PMTAG", "LoginService action = " + action);
        if (ACTION_LOGIN.equals(action)) {
            final String username = intent.getStringExtra(EXTRA_USERNAME);
            final String password = intent.getStringExtra(EXTRA_PASSWORD);
            final LoginInfoResponse infoResponse = intent.getParcelableExtra(EXTRA_LOGIN_INFO_RESPONSE);
            final int fallbackAuthVersion = intent.getIntExtra(EXTRA_FALLBACK_AUTH_VERSION, 2);
            final boolean signUp = intent.getBooleanExtra(EXTRA_SIGNUP, false);
            handleLogin(username, password.getBytes(Charsets.UTF_8), infoResponse, fallbackAuthVersion, signUp);
        } else if (ACTION_2FA.equals(action)) {
            final Id userId = new Id(intent.getStringExtra(EXTRA_USER_ID));
            final Name username = new Name(intent.getStringExtra(EXTRA_USERNAME));
            final String password = intent.getStringExtra(EXTRA_PASSWORD);
            final String twoFactor = intent.getStringExtra(EXTRA_TWO_FACTOR);
            final LoginInfoResponse infoResponse = intent.getParcelableExtra(EXTRA_LOGIN_INFO_RESPONSE);
            final LoginResponse loginResponse = intent.getParcelableExtra(EXTRA_LOGIN_RESPONSE);
            final int fallbackAuthVersion = intent.getIntExtra(EXTRA_FALLBACK_AUTH_VERSION, 2);
            final boolean signUp = intent.getBooleanExtra(EXTRA_SIGNUP, false);
            final boolean isConnecting = intent.getBooleanExtra(EXTRA_CONNECTING, false);
            handle2FA(userId, username, password.getBytes(Charsets.UTF_8), twoFactor, infoResponse, loginResponse, fallbackAuthVersion, signUp, isConnecting);
        } else if (ACTION_LOGIN_INFO.equals(action)) {
            final String username = intent.getStringExtra(EXTRA_USERNAME);
            final String password = intent.getStringExtra(EXTRA_PASSWORD);
            final int fallbackAuthVersion = intent.getIntExtra(EXTRA_FALLBACK_AUTH_VERSION, 2);
            handleLoginInfo(username, password.getBytes(Charsets.UTF_8), fallbackAuthVersion);
        } else if (ACTION_MAILBOX_LOGIN.equals(action)) {
            final String mailboxPassword = intent.getStringExtra(EXTRA_MAILBOX_PASSWORD);
            final Id userId = new Id(intent.getStringExtra(EXTRA_USER_ID));
            final String keySalt = intent.getStringExtra(EXTRA_KEY_SALT);
            final boolean signUp = intent.getBooleanExtra(EXTRA_SIGNUP, false);
            handleMailboxLogin(userId, mailboxPassword.getBytes(Charsets.UTF_8), keySalt, signUp);
        } else if (ACTION_GENERATE_KEYS.equals(action)) {
            final String username = intent.getStringExtra(EXTRA_USERNAME);
            final int bits = intent.getIntExtra(EXTRA_BITS, Constants.HIGH_SECURITY_BITS);
            String domain = intent.getStringExtra(EXTRA_DOMAIN);
            final String password = intent.getStringExtra(EXTRA_PASSWORD);
            if (TextUtils.isEmpty(password)) {
                return;
            }
            //default domain is protonmail.com, later we may let the user pick .com/.ch or even a custom domain
            if (TextUtils.isEmpty(domain)) {
                domain = Constants.MAIL_DOMAIN_COM;
            }
            final String keySalt = openPGP.createNewKeySalt();
            final byte[] generatedMailboxPassword;
            try {
                generatedMailboxPassword = openPGP.generateMailboxPassword(keySalt, password.getBytes(Charsets.UTF_8));
                String privateKey = openPGP.generateKey(username, domain, generatedMailboxPassword, KeyType.RSA, bits);

                userManager.saveKeySaltBlocking(keySalt); // should save the keySalt with or without userId
                userManager.saveMailboxPasswordBlocking(generatedMailboxPassword); // should save the password with or without userId

                userManager.setPrivateKey(privateKey);
            } catch (Exception e) {
                Logger.doLogException(e);
            }

        } else if (ACTION_CREATE_USER.equals(action)) {
            final String username = intent.getStringExtra(EXTRA_USERNAME);
            final String password = intent.getStringExtra(EXTRA_PASSWORD);
            final String token = intent.getStringExtra(EXTRA_TOKEN);
            final Constants.TokenType tokenType = (Constants.TokenType) intent.getSerializableExtra(EXTRA_TOKEN_TYPE);
            final boolean updateMe = intent.getBooleanExtra(EXTRA_UPDATE_ME, false);

            handleCreateUser(username, password.getBytes(Charsets.UTF_8), updateMe, tokenType, token);
        } else if (ACTION_SETUP_ADDRESS.equals(action)) {
            String domain = intent.getStringExtra(EXTRA_ADDRESS_DOMAIN);
            //default domain is protonmail.com, later we may let the user pick .com/.ch or even a custom domain
            if (TextUtils.isEmpty(domain)) {
                domain = Constants.MAIL_DOMAIN_COM;
            }
            handleAddressSetup(domain);
        } else if (ACTION_SETUP_KEYS.equals(action)) {
            final String addressId = intent.getStringExtra(EXTRA_ADDRESS_ID);
            final String password = intent.getStringExtra(EXTRA_PASSWORD);

            handleKeysSetup(addressId, password.getBytes(Charsets.UTF_8));
        } else if (ACTION_CONNECT_ACCOUNT_LOGIN.equals(action)) {
            final String username = intent.getStringExtra(EXTRA_USERNAME);
            final String password = intent.getStringExtra(EXTRA_PASSWORD);
            final LoginInfoResponse infoResponse = intent.getParcelableExtra(EXTRA_LOGIN_INFO_RESPONSE);
            final int fallbackAuthVersion = intent.getIntExtra(EXTRA_FALLBACK_AUTH_VERSION, 2);
            connectAccountLogin(username, password.getBytes(Charsets.UTF_8), infoResponse, fallbackAuthVersion);
        } else if (ACTION_CONNECT_ACCOUNT_MAILBOX_LOGIN.equals(action)) {
            final String mailboxPassword = intent.getStringExtra(EXTRA_MAILBOX_PASSWORD);
            final Id userId = new Id(intent.getStringExtra(EXTRA_USER_ID));
            final String keySalt = intent.getStringExtra(EXTRA_KEY_SALT);
            final Id currentPrimaryUserId = new Id(intent.getStringExtra(EXTRA_CURRENT_PRIMARY_USER_ID));
            connectAccountMailboxLogin(userId, currentPrimaryUserId, mailboxPassword.getBytes(Charsets.UTF_8), keySalt);
        } else if (ACTION_FETCH_USER_DETAILS.equals(action)) {
            handleFetchUserDetails();
        }
    }

    public static void fetchUserDetails() {
        final Context context = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(context, LoginService.class);
        intent.setAction(ACTION_FETCH_USER_DETAILS);
        JobIntentService.enqueueWork(context, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void startGenerateKeys(final String username, final String domain, final byte[] password, final int bits) {
        final Context context = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(context, LoginService.class);
        intent.setAction(ACTION_GENERATE_KEYS);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_BITS, bits);
        intent.putExtra(EXTRA_DOMAIN, domain);
        intent.putExtra(EXTRA_PASSWORD, new String(password));
        JobIntentService.enqueueWork(context, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void startCreateUser(final String username, final byte[] password, final boolean updateMe, final Constants.TokenType tokenType, final String token) {
        final Context context = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(context, LoginService.class);
        intent.setAction(ACTION_CREATE_USER);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_PASSWORD, new String(password));
        intent.putExtra(EXTRA_UPDATE_ME, updateMe);
        intent.putExtra(EXTRA_TOKEN, token);
        intent.putExtra(EXTRA_TOKEN_TYPE, tokenType);
        JobIntentService.enqueueWork(context, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void startInfo(
            final String username,
            final byte[] password,
            final int fallbackAuthVersion
    ) {
        final Context context = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(context, LoginService.class)
                .setAction(ACTION_LOGIN_INFO)
                .putExtra(EXTRA_USERNAME, username)
                .putExtra(EXTRA_PASSWORD, new String(password))
                .putExtra(EXTRA_FALLBACK_AUTH_VERSION, fallbackAuthVersion);
        JobIntentService.enqueueWork(context, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void start2FA(
            Id userId,
            Name username,
            byte[] password,
            String twoFactor,
            final LoginInfoResponse infoResponse,
            final LoginResponse loginResponse,
            final int fallbackAuthVersion,
            final boolean signUp,
            final boolean isConnecting
    ) {
        final Context context = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(context, LoginService.class);
        intent.setAction(ACTION_2FA);
        intent.putExtra(EXTRA_USER_ID, userId.getS());
        intent.putExtra(EXTRA_USERNAME, username.getS());
        intent.putExtra(EXTRA_PASSWORD, new String(password));
        intent.putExtra(EXTRA_TWO_FACTOR, twoFactor);
        intent.putExtra(EXTRA_LOGIN_INFO_RESPONSE, infoResponse);
        intent.putExtra(EXTRA_LOGIN_RESPONSE, loginResponse);
        intent.putExtra(EXTRA_FALLBACK_AUTH_VERSION, fallbackAuthVersion);
        intent.putExtra(EXTRA_SIGNUP, signUp);
        intent.putExtra(EXTRA_CONNECTING, isConnecting);
        JobIntentService.enqueueWork(context, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void startLogin(String username, byte[] password, final LoginInfoResponse infoResponse, final int fallbackAuthVersion, final boolean signUp) {
        final Context context = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(context, LoginService.class);
        intent.setAction(ACTION_LOGIN);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_PASSWORD, new String(password));
        intent.putExtra(EXTRA_LOGIN_INFO_RESPONSE, infoResponse);
        intent.putExtra(EXTRA_FALLBACK_AUTH_VERSION, fallbackAuthVersion);
        intent.putExtra(EXTRA_SIGNUP, signUp);
        JobIntentService.enqueueWork(context, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void startMailboxLogin(Id userId, String mailboxPassword, String keySalt, boolean signUp) {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(app, LoginService.class);
        intent.setAction(ACTION_MAILBOX_LOGIN);
        intent.putExtra(EXTRA_USER_ID, userId.getS());
        intent.putExtra(EXTRA_MAILBOX_PASSWORD, mailboxPassword);
        intent.putExtra(EXTRA_KEY_SALT, keySalt);
        intent.putExtra(EXTRA_SIGNUP, signUp);
        JobIntentService.enqueueWork(app, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void startSetupAddress(final String domain) {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(app, LoginService.class);
        intent.setAction(ACTION_SETUP_ADDRESS);
        intent.putExtra(EXTRA_ADDRESS_DOMAIN, domain);
        JobIntentService.enqueueWork(app, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void startSetupKeys(final String addressId, final byte[] password) {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(app, LoginService.class);
        intent.setAction(ACTION_SETUP_KEYS);
        intent.putExtra(EXTRA_ADDRESS_ID, addressId);
        intent.putExtra(EXTRA_PASSWORD, new String(password));
        JobIntentService.enqueueWork(app, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void startConnectAccount(String username, byte[] password, String twoFactor, final LoginInfoResponse infoResponse, final int fallbackAuthVersion) {
        final Context context = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(context, LoginService.class);
        intent.setAction(ACTION_CONNECT_ACCOUNT_LOGIN);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_PASSWORD, new String(password));
        intent.putExtra(EXTRA_TWO_FACTOR, twoFactor);
        intent.putExtra(EXTRA_LOGIN_INFO_RESPONSE, infoResponse);
        intent.putExtra(EXTRA_FALLBACK_AUTH_VERSION, fallbackAuthVersion);
        JobIntentService.enqueueWork(context, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    public static void startConnectAccountMailboxLogin(Id userId, Id currentPrimaryUserId, String mailboxPassword, String keySalt) {
        final ProtonMailApplication app = ProtonMailApplication.getApplication();
        final Intent intent = new Intent(app, LoginService.class);
        intent.setAction(ACTION_CONNECT_ACCOUNT_MAILBOX_LOGIN);
        intent.putExtra(EXTRA_USER_ID, userId.getS());
        intent.putExtra(EXTRA_MAILBOX_PASSWORD, mailboxPassword);
        intent.putExtra(EXTRA_KEY_SALT, keySalt);
        intent.putExtra(EXTRA_CURRENT_PRIMARY_USER_ID, currentPrimaryUserId.getS());
        JobIntentService.enqueueWork(app, LoginService.class, Constants.JOB_INTENT_SERVICE_ID_LOGIN, intent);
    }

    private void handleFetchUserDetails() {
        try {
            UserInfo userInfo = api.fetchUserInfoBlocking();
            UserSettingsResponse userSettingsResponse = api.fetchUserSettings();
            MailSettingsResponse mailSettingsResponse = api.fetchMailSettingsBlocking();
            AddressesResponse addressesResponse = api.fetchAddressesBlocking();
            MailSettings mailSettings = mailSettingsResponse.getMailSettings();
            UserSettings userSettings = userSettingsResponse.getUserSettings();
            userManager.setUserDetailsBlocking(userInfo.getUser(), addressesResponse.getAddresses(), mailSettings, userSettings);
            AddressKeyActivationWorker.Companion.activateAddressKeysIfNeeded(getApplicationContext(), addressesResponse.getAddresses(), userManager.getCurrentUserId());
            AppUtil.postEventOnUi(new UserSettingsEvent(userSettingsResponse.getUserSettings()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleCreateUser(final String username, final byte[] password, final boolean updateMe, final Constants.TokenType tokenType, final String token) {
        AuthStatus status = AuthStatus.FAILED;
        if (networkUtils.isConnected()) {
            try {
                final ModulusResponse modulus = api.randomModulus();
                sendSafetyNetRequest(username, password, modulus, updateMe, tokenType.getTokenTypeValue(), token);
            } catch (Exception e) {
                e.printStackTrace();
                AppUtil.postEventOnUi(new CreateUserEvent(status, null));
            }
        } else {
            status = AuthStatus.NO_NETWORK;
            AppUtil.postEventOnUi(new CreateUserEvent(status, null));
        }
    }

    private void handleLoginInfo(
            final String username,
            final byte[] password,
            final int fallbackAuthVersion
    ) {
        AuthStatus status = AuthStatus.FAILED;
        LoginInfoResponse infoResponse = null;
        try {
            if (networkUtils.isConnected()) {
                infoResponse = api.loginInfoForAuthentication(username);
                boolean foundErrorCode = AppUtil.checkForErrorCodes(infoResponse.getCode(), infoResponse.getError());
                if (foundErrorCode) {
                    status = AuthStatus.UPDATE;
                } else {
                    status = AuthStatus.SUCCESS;
                }
            } else {
                status = AuthStatus.NO_NETWORK;
            }
        } catch (Exception e) {
            Timber.i(e, "Login failure");
        }
        AppUtil.postEventOnUi(new LoginInfoEvent(status, infoResponse, username, password, fallbackAuthVersion));
    }

    public static SRPClient.Proofs srpProofsForInfo(final String username, final byte[] password, final LoginInfoResponse infoResponse, final int fallbackAuthVersion) throws NoSuchAlgorithmException {
        int authVersion = infoResponse.getAuthVersion();
        if (authVersion == 0) {
            authVersion = fallbackAuthVersion;
        }

        if (authVersion <= 2) {
            return null;
        }

        final byte[] modulus;
        try {
            modulus = Base64.decode(new ClearTextMessage(infoResponse.getModulus()).getData(), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        final byte[] hashedPassword = PasswordUtils.hashPassword(authVersion, password, username, Base64.decode(infoResponse.getSalt(), Base64.DEFAULT), modulus);

        return SRPClient.generateProofs(2048, modulus, Base64.decode(infoResponse.getServerEphemeral(), Base64.DEFAULT), hashedPassword);
    }

    private void handleLogin(final String username, final byte[] password, final LoginInfoResponse infoResponse, final int fallbackAuthVersion, final boolean signUp) {
        LoginHelperData loginHelperData = new LoginHelperData();
        try {
            if (networkUtils.isConnected()) {
                final SRPClient.Proofs proofs = srpProofsForInfo(username, password, infoResponse, fallbackAuthVersion);
                if (proofs != null) {
                    LoginResponse loginResponse = api.login(username, infoResponse.getSRPSession(), proofs.clientEphemeral, proofs.clientProof);
                    boolean foundErrorCode = AppUtil.checkForErrorCodes(loginResponse.getCode(), loginResponse.getError());
                    if (!foundErrorCode && loginResponse.isValid() && ConstantTime.isEqual(proofs.expectedServerProof, Base64.decode(loginResponse.getServerProof(), Base64.DEFAULT))) {
                        // handling valid login response
                        Id userId = new Id(loginResponse.getUserID());
                        loginHelperData.userId = userId;
                        userManager.setCurrentUserBlocking(userId); // all calls after this do not have to rely on username
                        tokenManager = TokenManager.Companion.getInstance(getApplicationContext(), userId);
                        tokenManager.handleLogin(loginResponse);
                        if (loginResponse.getTwoFA().getEnabled() == 0) {
                            loginHelperData = handleSuccessLogin(loginResponse, userId, password, infoResponse, signUp, false, null);
                        } else {
                            AppUtil.postEventOnUi(new Login2FAEvent(loginHelperData.status, infoResponse, userId, new Name(username), password, loginResponse, fallbackAuthVersion));
                            return;
                        }
                    } else if (foundErrorCode || !loginResponse.isValid()) {
                        loginHelperData.status = AuthStatus.INVALID_CREDENTIAL;
                    } else if (!ConstantTime.isEqual(proofs.expectedServerProof, Base64.decode(loginResponse.getServerProof(), Base64.DEFAULT))) {
                        loginHelperData.status = AuthStatus.INVALID_SERVER_PROOF;
                    }
                }
            } else {
                loginHelperData.status = AuthStatus.NO_NETWORK;
            }
        } catch (Exception e) {
            Logger.doLogException(TAG_LOGIN_SERVICE, "error while login", e);
            loginHelperData.status = AuthStatus.FAILED;
        }

        handleAfterLogin(infoResponse, loginHelperData, username, password, fallbackAuthVersion);
    }

    private void handle2FA(
            final Id userId,
            final Name username,
            final byte[] password,
            final String twoFactor,
            final LoginInfoResponse infoResponse,
            final LoginResponse loginResponse,
            final int fallbackAuthVersion,
            final boolean signUp,
            final boolean isConnecting
    ) {
        TwoFAResponse response = api.twoFactor(new TwoFABody(twoFactor));
        if (response.getCode() == 1000) {
            userManager.setCurrentUserBlocking(userId);
            tokenManager = userManager.getTokenManagerBlocking(userId);
            tokenManager.handleLogin(loginResponse);
            tokenManager.setScope(response.getScope());

            if (isConnecting) {
                LoginHelperData loginHelperData = handleSuccessLogin(
                        loginResponse,
                        userId,
                        password,
                        infoResponse,
                        false,
                        true,
                        null
                );
                handleAfterConnect(infoResponse, loginHelperData, userId, username, password, 4);
            } else {
                LoginHelperData loginHelperData = handleSuccessLogin(
                        loginResponse,
                        userId,
                        password,
                        infoResponse,
                        signUp,
                        false,
                        null
                );
                handleAfterLogin(infoResponse, loginHelperData, username.getS(), password, 4);
            }
        } else {
            Login2FAEvent event = new Login2FAEvent(
                    AuthStatus.FAILED,
                    infoResponse,
                    userId,
                    username,
                    password,
                    loginResponse,
                    fallbackAuthVersion
            );
            AppUtil.postEventOnUi(event);
        }
    }

    private void handleMailboxLogin(final Id userId, final byte[] mailboxPassword, final String keySalt, final boolean signUp) {
        byte[] generatedMailboxPassword = null;
        try {
            generatedMailboxPassword = getGeneratedMailboxPassword(mailboxPassword, keySalt);
        } catch (UnsupportedEncodingException e) {
            Logger.doLogException(TAG_LOGIN_SERVICE, e);
            AppUtil.postEventOnUi(new MailboxLoginEvent(AuthStatus.INVALID_CREDENTIAL));
        }
        try {
            if (networkUtils.isConnected()) {
                tokenManager = userManager.getTokenManagerBlocking(userId);

                boolean checkPassphrase = openPGP.checkPassphrase(tokenManager.getEncPrivateKey(), generatedMailboxPassword);
                if (!checkPassphrase) {
                    AppUtil.postEventOnUi(new MailboxLoginEvent(AuthStatus.INVALID_CREDENTIAL));
                } else {
                    UserInfo userInfo = api.fetchUserInfoBlocking();
                    UserSettingsResponse userSettings = api.fetchUserSettings();
                    MailSettingsResponse mailSettings = api.fetchMailSettingsBlocking();
                    AddressesResponse addresses = api.fetchAddressesBlocking();

                    setAccountMigrationStatus(addresses, userInfo);

                    String message = userInfo.getError();
                    boolean foundErrorCode = AppUtil.checkForErrorCodes(userInfo.getCode(), message);
                    if (!foundErrorCode) {
                        userManager.setLoggedIn(true);
                        userManager.saveMailboxPasswordBlocking(userId, generatedMailboxPassword);
                        userManager.setUserDetailsBlocking(userInfo.getUser(), addresses.getAddresses(), mailSettings.getMailSettings(), userSettings.getUserSettings());
                        AddressKeyActivationWorker.Companion.activateAddressKeysIfNeeded(getApplicationContext(), addresses.getAddresses(), userId);
                        AppUtil.postEventOnUi(new MailboxLoginEvent(AuthStatus.SUCCESS));
                        if (!signUp) {
                            if (networkUtils.isConnected() && userManager.isLoggedIn() && userManager.accessTokenExists()) {
                                AlarmReceiver alarmReceiver = new AlarmReceiver();
                                alarmReceiver.setAlarm(ProtonMailApplication.getApplication());
                            }
                            if (userManager.isFirstLogin()) {
                                jobManager.start();
                                launchInitialDataFetch.invoke(userId, true, true);
                                userManager.firstLoginDone();
                            }
                        }
                    }
                }
            } else {
                doOfflineMailboxLogin(generatedMailboxPassword, userId);
            }
        } catch (Exception error) {
            Logger.doLogException(TAG_LOGIN_SERVICE, error);
            AppUtil.postEventOnUi(new MailboxLoginEvent(AuthStatus.NOT_SIGNED_UP));
            doOfflineMailboxLogin(generatedMailboxPassword, userId);
        }
    }

    private void connectAccountLogin(final String username, final byte[] password, final LoginInfoResponse infoResponse, final int fallbackAuthVersion) {
        // TODO: abstract the same code from login method
        LoginHelperData loginHelperData = new LoginHelperData();

        try {
            if (networkUtils.isConnected()) {
                final SRPClient.Proofs proofs = srpProofsForInfo(username, password, infoResponse, fallbackAuthVersion);
                if (proofs != null) {
                    Id currentPrimaryUserId = userManager.getCurrentUserId();
                    LoginResponse loginResponse = api.login(username, infoResponse.getSRPSession(), proofs.clientEphemeral, proofs.clientProof); //, twoFactor);
                    boolean foundErrorCode = AppUtil.checkForErrorCodes(loginResponse.getCode(), loginResponse.getError());
                    if (!foundErrorCode && loginResponse.isValid() && ConstantTime.isEqual(proofs.expectedServerProof, Base64.decode(loginResponse.getServerProof(), Base64.DEFAULT))) {
                        Id userId = new Id(loginResponse.getUserID());
                        loginHelperData.userId = userId;
                        // handling valid login response
                        userManager.setCurrentUserBlocking(userId);
                        tokenManager = userManager.getTokenManagerBlocking(userId);
                        tokenManager.handleLogin(loginResponse);
                        if (loginResponse.getTwoFA().getEnabled() == 0) {
                            loginHelperData = handleSuccessLogin(loginResponse, userId, password, infoResponse, false, true, currentPrimaryUserId);
                        } else {
                            AppUtil.postEventOnUi(new Login2FAEvent(loginHelperData.status, infoResponse, userId, new Name(username), password, loginResponse, fallbackAuthVersion));
                            return;
                        }
                    } else if (foundErrorCode || !loginResponse.isValid()) {
                        loginHelperData.status = AuthStatus.INVALID_CREDENTIAL;
                    } else if (!ConstantTime.isEqual(proofs.expectedServerProof, Base64.decode(loginResponse.getServerProof(), Base64.DEFAULT))) {
                        loginHelperData.status = AuthStatus.INVALID_SERVER_PROOF;
                    }
                }
            } else {
                loginHelperData.status = AuthStatus.NO_NETWORK;
            }
        } catch (Exception e) {
            Timber.e("Error while logging in", e);
            loginHelperData.status = AuthStatus.FAILED;
        }

        Id userId = EitherKt.orThrow(findUserIdForUsername.blocking(new Name(username)));
        loginHelperData.userId = userId;
        handleAfterConnect(infoResponse, loginHelperData, userId, new Name(username), password, fallbackAuthVersion);
    }

    private void connectAccountMailboxLogin(final Id userId, final Id currentPrimaryUserId, final byte[] mailboxPassword, final String keySalt) {
        // TODO: abstract the same code from mailbox login method
        byte[] generatedMailboxPassword = null;
        try {
            generatedMailboxPassword = getGeneratedMailboxPassword(mailboxPassword, keySalt);
        } catch (UnsupportedEncodingException e) {
            Logger.doLogException(TAG_LOGIN_SERVICE, e);
            AppUtil.postEventOnUi(new ConnectAccountMailboxLoginEvent(AuthStatus.INVALID_CREDENTIAL));
        }

        try {
            AppUtil.clearTasks(jobManager);
            if (networkUtils.isConnected()) {
                tokenManager = userManager.getTokenManagerBlocking(userId);
                boolean checkPassphrase = openPGP.checkPassphrase(tokenManager.getEncPrivateKey(), generatedMailboxPassword);
                if (!checkPassphrase) {
                    AppUtil.postEventOnUi(new ConnectAccountMailboxLoginEvent(AuthStatus.INVALID_CREDENTIAL));
                } else {
                    userManager.setCurrentUserBlocking(userId);
                    UserInfo userInfo = api.fetchUserInfoBlocking();
                    if (!userManager.canConnectAnotherAccountBlocking() && !userInfo.getUser().isPaidUser()) {
                        userManager.logoutBlocking(userId);
                        if (currentPrimaryUserId != null) {
                            userManager.setCurrentUserBlocking(currentPrimaryUserId);
                        } else {
                            userManager.setCurrentUserBlocking(userManager.getCurrentUserId());
                        }
                        AppUtil.postEventOnUi(new ConnectAccountMailboxLoginEvent(AuthStatus.CANT_CONNECT));
                        return;
                    }

                    UserSettingsResponse userSettings = api.fetchUserSettings();
                    MailSettingsResponse mailSettings = api.fetchMailSettingsBlocking();
                    AddressesResponse addresses = api.fetchAddressesBlocking();
                    String message = userInfo.getError();
                    boolean foundErrorCode = AppUtil.checkForErrorCodes(userInfo.getCode(), message);
                    if (!foundErrorCode) {
                        accountManager.setLoggedInBlocking(userId);
                        userManager.saveMailboxPasswordBlocking(userId, generatedMailboxPassword);
                        userManager.setUserDetailsBlocking(userInfo.getUser(), addresses.getAddresses(), mailSettings.getMailSettings(), userSettings.getUserSettings());
                        AddressKeyActivationWorker.Companion.activateAddressKeysIfNeeded(getApplicationContext(), addresses.getAddresses(), userId);
                        AppUtil.postEventOnUi(new ConnectAccountMailboxLoginEvent(AuthStatus.SUCCESS));
                        jobManager.start();
                        launchInitialDataFetch.invoke(userId, true, true);
                        userManager.firstLoginDone();
                    }
                }
            } else {
                doOfflineMailboxLogin(generatedMailboxPassword, userId);
            }
        } catch (Exception error) {
            Logger.doLogException(TAG_LOGIN_SERVICE, error);
            AppUtil.postEventOnUi(new MailboxLoginEvent(AuthStatus.NOT_SIGNED_UP));
            doOfflineMailboxLogin(generatedMailboxPassword, userId);
        }
    }

    private void handleAddressSetup(String domain) {
        AuthStatus status = AuthStatus.FAILED;
        AddressSetupResponse response = null;
        try {
            if (networkUtils.isConnected()) {
                response = api.setupAddress(new AddressSetupBody(domain));
                boolean foundErrorCode = AppUtil.checkForErrorCodes(response.getCode(), response.getError());
                if (foundErrorCode) {
                    status = AuthStatus.UPDATE;
                } else {
                    status = AuthStatus.SUCCESS;
                }
            } else {
                status = AuthStatus.NO_NETWORK;
            }
        } catch (Exception e) {
            Logger.doLogException(e);
        }
        AppUtil.postEventOnUi(new AddressSetupEvent(status, response));
    }

    // region support functions
    private void handleAfterConnect(
            LoginInfoResponse infoResponse,
            LoginHelperData loginHelperData,
            Id userId,
            Name username,
            byte[] password,
            int fallbackAuthVersion
    ) {
        if (infoResponse == null || loginHelperData.status == AuthStatus.FAILED) {
            userManager.logoutBlocking(userId);
            AppUtil.postEventOnUi(new ConnectAccountLoginEvent(AuthStatus.FAILED, null, loginHelperData.redirectToSetup, loginHelperData.user, null));
            return;
        }
        // username necessary for subsequent calls
        if (infoResponse.getAuthVersion() == 0 && loginHelperData.status.equals(AuthStatus.INVALID_CREDENTIAL) && fallbackAuthVersion != 0) {
            final int newFallback;
            String usernameString = username.getS();
            if (fallbackAuthVersion == 2 && !PasswordUtils.cleanUserName(usernameString).equals(usernameString.toLowerCase())) {
                newFallback = 1;
            } else {
                newFallback = 0;
            }

            startInfo(usernameString, password, newFallback);
        } else {
            if (loginHelperData.postLoginEvent) {
                AppUtil.postEventOnUi(new ConnectAccountLoginEvent(loginHelperData.status, loginHelperData.keySalt, loginHelperData.redirectToSetup, loginHelperData.user, loginHelperData.domainName));
            }
        }
    }

    private void handleAfterLogin(LoginInfoResponse infoResponse, LoginHelperData loginHelperData, String username, byte[] password, int fallbackAuthVersion) {
        if (infoResponse == null || (loginHelperData.status == AuthStatus.FAILED && loginHelperData.postLoginEvent)) {
            if (loginHelperData.redirectToSetup) {
                try {
                    AddressesResponse addressResponse = api.fetchAddressesBlocking();
                    AppUtil.postEventOnUi(
                            new LoginEvent(
                                    AuthStatus.FAILED,
                                    loginHelperData.keySalt,
                                    loginHelperData.redirectToSetup,
                                    loginHelperData.userId,
                                    loginHelperData.user,
                                    username,
                                    loginHelperData.domainName,
                                    addressResponse.getAddresses()
                            )
                    );
                } catch (Exception e) {
                    Timber.e(e);
                }
            } else {
                AppUtil.postEventOnUi(
                        new LoginEvent(
                                AuthStatus.FAILED,
                                null,
                                false,
                                loginHelperData.userId,
                                null,
                                username,
                                null,
                                null
                        )
                );
                Id userId = EitherKt.orThrow(findUserIdForUsername.blocking(new Name(username)));
                userManager.logoutBlocking(userId);
            }
            return;
        }
        if (infoResponse.getAuthVersion() == 0 && loginHelperData.status.equals(AuthStatus.INVALID_CREDENTIAL) && fallbackAuthVersion != 0) {
            final int newFallback;
            if (fallbackAuthVersion == 2 && !PasswordUtils.cleanUserName(username).equals(username.toLowerCase())) {
                newFallback = 1;
            } else {
                newFallback = 0;
            }

            startInfo(username, password, newFallback);
        } else if (loginHelperData.postLoginEvent) {
            AppUtil.postEventOnUi(
                    new LoginEvent(
                            loginHelperData.status,
                            loginHelperData.keySalt,
                            loginHelperData.redirectToSetup,
                            loginHelperData.userId,
                            loginHelperData.user,
                            username,
                            loginHelperData.domainName,
                            null
                    )
            );
        }
    }

    private LoginHelperData handleSuccessLogin(
            LoginResponse loginResponse,
            final Id userId,
            final byte[] password,
            final LoginInfoResponse infoResponse,
            final boolean signUp, final boolean isConnecting, final Id currentPrimaryUserId
    ) {
        AuthStatus status;
        boolean redirectToSetup = false;
        User user = null;
        String domainName = null;
        String keySalt = null;

        try {
            status = AuthStatus.SUCCESS;
            keySalt = loginResponse.getKeySalt();

            if (keySalt == null) { // new response doesn't contain salt
                UserInfo userInfo = api.fetchUserInfoBlocking();
                KeySalts keySalts = api.fetchKeySalts();

                String primaryKeyId = null;
                for (Keys key : userInfo.getUser().getKeys()) {
                    if (key.isPrimary()) {
                        primaryKeyId = key.getID();
                        tokenManager.setEncPrivateKey(key.getPrivateKey()); // it's needed for verification later
                        break;
                    }
                }

                if (primaryKeyId != null) {
                    for (KeySalts.KeySalt ks : keySalts.keySalts) {
                        if (primaryKeyId.equals(ks.keyId)) {
                            keySalt = ks.keySalt;
                            break;
                        }
                    }
                }

                if (userInfo.getUser().getKeys() == null || userInfo.getUser().getKeys().size() == 0) {
                    redirectToSetup = true;
                }
            } else { // old response with key and salt
                if (TextUtils.isEmpty(loginResponse.getPrivateKey())) {
                    redirectToSetup = true;
                }
            }

            if (infoResponse.getAuthVersion() < PasswordUtils.CURRENT_AUTH_VERSION) {
                final ModulusResponse modulus = api.randomModulus();
                String generatedMailboxPassword = null;
                try {
                    generatedMailboxPassword = new String(getGeneratedMailboxPassword(password, keySalt));
                } catch (UnsupportedEncodingException e) {
                    Logger.doLogException(TAG_LOGIN_SERVICE, e);
                }
                api.upgradeLoginPassword(new UpgradePasswordBody(PasswordVerifier.calculate(password, modulus)));
                tokenManager.clearAccessToken();
            }

            if (redirectToSetup && !signUp) {
                status = AuthStatus.FAILED;
                UserInfo userInfo = api.fetchUserInfoBlocking();
                UserSettingsResponse userSettingsResp = api.fetchUserSettings();
                MailSettingsResponse mailSettingsResp = api.fetchMailSettingsBlocking();
                AddressesResponse addressesResponse = api.fetchAddressesBlocking();
                user = userInfo.getUser();
                userManager.setUserDetailsBlocking(user, addressesResponse.getAddresses(), mailSettingsResp.getMailSettings(), userSettingsResp.getUserSettings());
                domainName = getDomainName(addressesResponse.getAddresses());
            }

            if (isConnecting) {
                if (loginResponse.getPasswordMode() == Constants.PasswordMode.SINGLE && !TextUtils.isEmpty(keySalt)) {
                    connectAccountMailboxLogin(userId, currentPrimaryUserId, password, keySalt);
                    return new LoginHelperData(userId, status, keySalt, redirectToSetup, user, domainName, false);
                }
            } else {
                if (signUp || (loginResponse.getPasswordMode() == Constants.PasswordMode.SINGLE && !TextUtils.isEmpty(keySalt))) {
                    if (signUp) {
                        if (TextUtils.isEmpty(tokenManager.getEncPrivateKey())) {
                            tokenManager.setEncPrivateKey(userManager.getPrivateKey());
                        }
                        handleMailboxLogin(userId, password, keySalt != null ? keySalt : userManager.getCurrentUserKeySalt(), true);
                    } else {
                        handleMailboxLogin(userId, password, keySalt, false);
                    }
                    return new LoginHelperData(userId, status, keySalt, redirectToSetup, user, domainName, false);
                }
            }

            // endregion
        } catch (Exception e) {
            Timber.e("Error wile logging in", e);
            status = AuthStatus.FAILED;
        }

        return new LoginHelperData(userId, status, keySalt, redirectToSetup, user, domainName);
    }

    private void handleKeysSetup(String addressId, byte[] password) {
        AuthStatus status = AuthStatus.FAILED;
        try {
            final ModulusResponse newModulus = api.randomModulus();
            final PasswordVerifier verifier = PasswordVerifier.calculate(password, newModulus);
            final String keySalt = userManager.getCurrentUserKeySalt();
            final String privateKey = userManager.getPrivateKey();

            AddressPrivateKey addressPrivateKey = new AddressPrivateKey(addressId, privateKey);

            // TODO: uncomment when the API is updated (i.e. when new accounts are migrated by default)
            // TokenAndSignature tokenAndSignature = generateTokenAndSignature(privateKey);
            // addressPrivateKey.setToken(tokenAndSignature.token);
            // addressPrivateKey.setSignature(tokenAndSignature.signature);
            addressPrivateKey.setSignedKeyList(generateSignedKeyList(privateKey));
            List<AddressPrivateKey> addressPrivateKeys = new ArrayList<>();
            addressPrivateKeys.add(addressPrivateKey);
            KeysSetupBody keysSetupBody = new KeysSetupBody(privateKey, keySalt, addressPrivateKeys, verifier.AuthVersion, verifier.ModulusID, verifier.Salt, verifier.SRPVerifier);
            UserInfo response = api.setupKeys(keysSetupBody);
            boolean foundErrorCode = AppUtil.checkForErrorCodes(response.getCode(), response.getError()) || response.getCode() != Constants.RESPONSE_CODE_OK;
            if (foundErrorCode) {
                status = AuthStatus.FAILED;
            } else {
                status = AuthStatus.SUCCESS;
                UserInfo userInfo = api.fetchUserInfoBlocking();
                UserSettingsResponse userSettings = api.fetchUserSettings();
                AddressesResponse addressesResponse = api.fetchAddressesBlocking();
                User user = userInfo.getUser();
                userManager.setUserSettings(userSettings.getUserSettings());
                user.setAddresses(addressesResponse.getAddresses());
                user.setUsername(user.getName());
                userManager.setUser(user);
            }
            AppUtil.postEventOnUi(new KeysSetupEvent(status, response));
            return;
        } catch (Exception e) {
            Logger.doLogException(e);
        }
        AppUtil.postEventOnUi(new KeysSetupEvent(status, null));
    }

    private SignedKeyList generateSignedKeyList(String key) throws Exception {
        String keyFingerprint = openPGP.getFingerprint(key);
        String keyList = "[{\"Fingerprint\": \"" + keyFingerprint + "\", " +
                            "\"SHA256Fingerprints\": " + new String(Helper.getJsonSHA256Fingerprints(key)) + ", " +
                            "\"Primary\": 1, \"Flags\": 3}]"; // one-element JSON list
        String signedKeyList = openPGP.signTextDetached(keyList, key, userManager.getCurrentUserMailboxPassword());
        return new SignedKeyList(keyList, signedKeyList);
    }

    /**
     * @param keySalt if empty, we don't salt MailboxPassword and return it as is
     */
    private byte[] getGeneratedMailboxPassword(byte[] mailboxPassword, String keySalt) throws UnsupportedEncodingException {
        if (!TextUtils.isEmpty(keySalt)) {
            return openPGP.generateMailboxPassword(keySalt, mailboxPassword);
        } else {
            return mailboxPassword;
        }
    }

    private void doOfflineMailboxLogin(byte[] mailboxPassword, Id userId) {
        User user = userManager.getCurrentLegacyUserBlocking();
        String addressId = null;
        if (user != null) {
            addressId = user.getAddressId();
        }
        if (TextUtils.isEmpty(addressId) || !userManager.isLoggedIn()) {
            AppUtil.postEventOnUi(new MailboxLoginEvent(AuthStatus.NO_NETWORK));
        } else {
            final boolean isPwdOk = openPGP.checkPassphrase(tokenManager.getEncPrivateKey(), mailboxPassword);
            if (!isPwdOk) {
                AppUtil.postEventOnUi(new MailboxLoginEvent(AuthStatus.INVALID_CREDENTIAL));
            } else {
                userManager.setCurrentUserBlocking(userId);
                AppUtil.postEventOnUi(new MailboxLoginEvent(AuthStatus.SUCCESS));
            }
        }
    }

    private String getDomainName(List<Address> addressList) {
        if (addressList != null && addressList.size() > 0) {
            Address address = addressList.get(0);
            if (address != null) {
                String email = address.getEmail();
                return email.substring(email.indexOf("@") + 1);
            }
        }
        return null;
    }
// endregion

    static class LoginHelperData {
        Id userId;
        AuthStatus status;
        String keySalt;
        boolean redirectToSetup;
        User user;
        String domainName;
        boolean postLoginEvent;

        LoginHelperData() {
            this(null);
        }

        LoginHelperData(Id userId) {
            this(userId, AuthStatus.FAILED, null, false, null, null);
        }

        LoginHelperData(
                Id userId,
                AuthStatus status,
                String keySalt,
                boolean redirectToSetup,
                User user,
                String domainName
        ) {
            this(userId, status, keySalt, redirectToSetup, user, domainName, true);
        }

        LoginHelperData(
                Id userId,
                AuthStatus status,
                String keySalt,
                boolean redirectToSetup,
                User user,
                String domainName,
                boolean postLoginEvent
        ) {
            this.userId = userId;
            this.status = status;
            this.keySalt = keySalt;
            this.redirectToSetup = redirectToSetup;
            this.user = user;
            this.domainName = domainName;
            this.postLoginEvent = postLoginEvent;
        }
    }

    private void sendSafetyNetRequest(String username, byte[] password, ModulusResponse modulus, Boolean updateMe, String tokenType, String token) {

        String timestamp = System.currentTimeMillis() + "";
        MessageDigest digester = null;
        try {
            digester = MessageDigest.getInstance("SHA-256");
            digester.update((username + timestamp).getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            Timber.e(e);
        }
        Activity activity = ProtonMailApplication.getApplication().getCurrentActivity();

        byte[] nonce = digester != null ? digester.digest() : new byte[0];

        // TODO: api key should be retrieved from server for future versions
        SafetyNet.getClient(this).attest(nonce, new String(Base64.decode(SAFETY_NET_API_KEY, Base64.NO_WRAP), UTF_8))
                .addOnSuccessListener(activity, response -> {
                    // Indicates communication with the service was successful.
                    String jwsResult = response.getJwsResult();

                    new CheckDeviceVerification(api.getApi(), username, password, modulus, updateMe, tokenType, token, timestamp, jwsResult).execute();
                })
                .addOnFailureListener(activity, e -> {
                    // An error occurred while communicating with the service.
                    new CheckDeviceVerification(api.getApi(), username, password, modulus, updateMe, tokenType, token, timestamp, "").execute();

                    // An error with the Google Play services API contains some
                    // or
                    // A different, unknown type of error occurred.

                });
    }


    private static class CheckDeviceVerification extends AsyncTask<Unit, Unit, UserInfo> {
        private final ProtonMailApi api;
        private final String username;
        private final byte[] password;
        private final ModulusResponse modulus;
        private final Boolean updateMe;
        private final String tokenType;
        private final String token;
        private final String timestamp;
        private final String jwsResult;

        CheckDeviceVerification(ProtonMailApi api, String username, byte[] password, ModulusResponse modulus, Boolean updateMe, String tokenType, String token, String timestamp, String jwsResult) {
            this.api = api;
            this.username = username;
            this.password = password;
            this.modulus = modulus;
            this.updateMe = updateMe;
            this.tokenType = tokenType;
            this.token = token;
            this.timestamp = timestamp;
            this.jwsResult = jwsResult;
        }

        @Override
        protected UserInfo doInBackground(Unit... units) {
            UserInfo userInfo = new UserInfo();
            try {
                userInfo = api.createUser(username, PasswordVerifier.calculate(password, modulus), updateMe, tokenType, token, timestamp, jwsResult);
            } catch (Exception e) {
                e.printStackTrace();
                AppUtil.postEventOnUi(new CreateUserEvent(AuthStatus.FAILED, null));
            }
            return userInfo;
        }

        @Override
        protected void onPostExecute(UserInfo userInfo) {
            AuthStatus status = AuthStatus.FAILED;
            String error = null;
            error = userInfo.getError();
            if (userInfo.getCode() == BaseApiKt.RESPONSE_CODE_INVALID_APP_CODE || userInfo.getCode() == BaseApiKt.RESPONSE_CODE_FORCE_UPGRADE) {
                AppUtil.postEventOnUi(new ForceUpgradeEvent(userInfo.getError()));
            } else if (userInfo.getCode() == Constants.RESPONSE_CODE_OK && userInfo.getUser() != null) {
                status = AuthStatus.SUCCESS;
            }
            AppUtil.postEventOnUi(new CreateUserEvent(status, error));
        }
    }

    private void setAccountMigrationStatus(AddressesResponse addresses, UserInfo userInfo) {
        // check for user account type if it's legacy or migrated and persist the info
        List<Address> addressList = addresses.getAddresses();
        if(!addressList.isEmpty()) {
            List<Keys> keys = addressList.get(0).getKeys();
            if (!keys.isEmpty()) {
                Keys key = keys.get(0);
                if (key.toAddressKey().getSignature() == null && key.toAddressKey().getToken() == null) {
                    userInfo.getUser().setLegacyAccount(true);
                } else if (key.toAddressKey().getSignature() != null && key.toAddressKey().getToken() != null) {
                    userInfo.getUser().setLegacyAccount(false);
                }
            }
        }
    }
}
