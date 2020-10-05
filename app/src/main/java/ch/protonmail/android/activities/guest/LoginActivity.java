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
package ch.protonmail.android.activities.guest;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.material.snackbar.Snackbar;
import com.squareup.otto.Subscribe;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.LoginInfoResponse;
import ch.protonmail.android.api.models.LoginResponse;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.ConnectivityEvent;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.events.Login2FAEvent;
import ch.protonmail.android.events.LoginEvent;
import ch.protonmail.android.events.LoginInfoEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;
import kotlin.text.Charsets;

import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_LOGIN_FINISHED;
import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_TO_INBOX;

public class LoginActivity extends BaseLoginActivity {

    @BindView(R.id.username)
    EditText mUsernameEditText;
    @BindView(R.id.password)
    EditText mPasswordEditText;
    @BindView(R.id.progress_container)
    View mProgressContainer;
    @BindView(R.id.sign_in)
    Button mSignIn;
    @BindView(R.id.app_version)
    TextView mAppVersion;
    private Snackbar mCheckForConnectivitySnack;
    private boolean mDisableBack = false;
    private AlertDialog m2faAlertDialog;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_login;
    }

    @Override
    protected boolean shouldCheckForAutoLogout() {
        return false;
    }

    @Override
    protected void setFocuses() {
        mUsernameEditText.setOnFocusChangeListener(mFocusListener);
        mUsernameEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                mPasswordEditText.setFocusable(true);
                mPasswordEditText.setFocusableInTouchMode(true);
                mPasswordEditText.requestFocus();
                return true;
            }
            return false;
        });
        mPasswordEditText.setOnFocusChangeListener(mFocusListener);
    }

    @Override
    protected boolean isPreventingScreenshots() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // clickable sign up link
        AppUtil.clearNotifications(this, 3); // TODO: check which notification Id is this one

        final String username = mUserManager.getUsername();
        mUsernameEditText.setText(username);
        mUsernameEditText.setFocusable(false);
        mPasswordEditText.setFocusable(false);
        mUsernameEditText.setOnTouchListener(mTouchListener);
        mPasswordEditText.setOnTouchListener(mTouchListener);
        if (!username.isEmpty()) {
            mPasswordEditText.requestFocus();
        }

        Intent intent = getIntent();
        if (intent.getBooleanExtra(EXTRA_API_OFFLINE, false)){
            final SpannableString s = new SpannableString(getResources().getString(R.string.api_offline));
            Linkify.addLinks(s, Linkify.ALL);
            DialogUtils.Companion.showInfoDialog(this, getString(R.string.api_offline_title), s.toString(), null);
        } else if (intent.getBooleanExtra(EXTRA_FORCE_UPGRADE, false)){
            DialogUtils.Companion.showInfoDialog(this, getString(R.string.update_app_title), getString(R.string.update_app), null);
        }
        mAppVersion.setText(String.format(getString(R.string.app_version_code_login), AppUtil.getAppVersionName(this), AppUtil.getAppVersionCode(this)));
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
        UiUtil.hideKeyboard(LoginActivity.this);
        ProtonMailApplication.getApplication().resetLoginInfoEvent();
    }

    @Override
    protected void onStop() {
        super.onStop();
        ProtonMailApplication.getApplication().getBus().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if (!mDisableBack) {
            if (mUserManager == null || !mUserManager.isEngagementShown()) {
                startActivity(AppUtil.decorInAppIntent(new Intent(this, FirstActivity.class)));
            }
            super.onBackPressed();
        }
    }

    @OnClick(R.id.forgot_password)
    public void onForgotPassword() {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
        browserIntent.setData(Uri.parse(getString(R.string.link_forgot_pass)));
        if (browserIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(browserIntent);
        } else {
            TextExtensions.showToast(this, R.string.no_browser_found);
        }
    }

    @OnClick(R.id.sign_in)
    public void onSignIn() {
        mUsernameEditText.setText(mUsernameEditText.getText().toString().replaceAll("\\s", ""));
        if (!mUsernameEditText.getText().toString().isEmpty() && !mPasswordEditText.getText().toString().isEmpty()) {
            UiUtil.hideKeyboard(this);
            mDisableBack = true;
            disableInput();
            mProgressContainer.setVisibility(View.VISIBLE);
            final String username = mUsernameEditText.getText().toString();
            final String password = mPasswordEditText.getText().toString();
            new Handler().postDelayed(() -> {
                mDisableBack = false;
                mUserManager.info(username, password.getBytes(Charsets.UTF_8) /*TODO passphrase*/);
            }, 1500);
        }
    }

    protected class ConnectivityRetryListener extends RetryListener {

        @Override
        public void onClick(View v) {
            mNetworkUtil.setCurrentlyHasConnectivity(true);
            mCheckForConnectivitySnack = networkSnackBarUtil.getCheckingConnectionSnackBar(
                    getMSnackLayout(), LoginActivity.this);
            mCheckForConnectivitySnack.show();
            onSignIn();
            super.onClick(v);
        }
    }

    private ConnectivityRetryListener connectivityRetryListener = new ConnectivityRetryListener();

    @Subscribe
    public void onConnectivityEvent(ConnectivityEvent event) {
        if (!event.hasConnection()) {
            showNoConnSnack(connectivityRetryListener, this);
        } else {
            mPingHasConnection = true;
            hideNoConnSnack();
        }
    }

    @Subscribe
    public void onLogin2FAEvent(final Login2FAEvent event) {
        if (event == null || TextUtils.isEmpty(event.username)) {
            return;
        }
        ProtonMailApplication.getApplication().resetLogin2FAEvent();
        hideProgress();
        enableInput();
        showTwoFactorDialog(event.username, event.password, event.infoResponse, event.loginResponse, event.fallbackAuthVersion);
    }

    @Subscribe
    public void onLoginInfoEvent(final LoginInfoEvent event) {
        if (event == null) {
            return;
        }
        switch (event.status) {
            case SUCCESS: {
                    ProtonMailApplication.getApplication().resetLoginInfoEvent();
                    mUserManager.login(event.username, event.password, event.response, event.fallbackAuthVersion,false);
            }
            break;
            case NO_NETWORK: {
                hideProgress();
                enableInput();
                mSignIn.setClickable(true);
                TextExtensions.showToast(this, R.string.no_network);
            }
            break;
            case UPDATE: {
                hideProgress();
                AppUtil.postEventOnUi(new ForceUpgradeEvent(event.response.getError()));
            }
            break;
            case FAILED:
            default: {
                hideProgress();
                enableInput();
                mSignIn.setClickable(true);
                TextExtensions.showToast(this, R.string.login_failure);
            }
        }
    }

    @Override
    protected void onMailboxSuccess() {
        mUserManager.setLoginState(LOGIN_STATE_TO_INBOX);
    }

    @Subscribe
    public void onLoginEvent(MailboxLoginEvent event) {
        super.onLoginEvent(event);
    }

    @Subscribe
    public void onLoginEvent(LoginEvent event) {
        if (event == null) {
            return;
        }
        ProtonMailApplication.getApplication().resetLoginEvent();
        switch (event.getStatus()) {
            case SUCCESS: {
                if (event.isRedirectToSetup()) {
                    User user = event.getUser();
                    redirectToSetup(event.getUsername(), event.getDomainName(), user.getAddresses());
                    return;
                }
                hideProgress();
                mUserManager.setLoginState(LOGIN_STATE_LOGIN_FINISHED);
                mUserManager.saveKeySalt(event.getKeySalt(), event.getUsername());
                Intent mailboxLoginIntent = new Intent(this, MailboxLoginActivity.class);
                mailboxLoginIntent.putExtra(MailboxLoginActivity.EXTRA_KEY_SALT, event.getKeySalt());
                startActivity(AppUtil.decorInAppIntent(mailboxLoginIntent));
                finish();
            }
            break;
            case NO_NETWORK: {
                hideProgress();
                enableInput();
                mSignIn.setClickable(true);
                TextExtensions.showToast(this, R.string.no_network);
            }
            break;
            case UPDATE: {
                hideProgress();
                AppUtil.postEventOnUi(new ForceUpgradeEvent(event.getError()));
            }
            break;
            case INVALID_CREDENTIAL: {
                hideProgress();
                enableInput();
                mSignIn.setClickable(true);
                TextExtensions.showToast(this, R.string.invalid_credentials);
            }
            break;
            case INVALID_SERVER_PROOF: {
                hideProgress();
                enableInput();
                mSignIn.setClickable(true);
                TextExtensions.showToast(this, R.string.invalid_server_proof);
            } break;
            case FAILED:
            default: {
                if (event.isRedirectToSetup()) {
                    redirectToSetup(event.getUsername(), event.getDomainName(), event.getAddresses());
                } else {
                    TextExtensions.showToast(this, R.string.login_failure);
                }
                hideProgress();
                enableInput();
                mSignIn.setClickable(true);
            }
        }
    }

    private void redirectToSetup(String username, String domainName, List<Address> addresses) {
        Address address = null;
        String domain = domainName;
        String finalUsername = username;
        boolean hasAddress = addresses != null && addresses.size() > 0;
        if (hasAddress) {
            address = addresses.get(0);
        }
        if (domain == null) {
            domain = Constants.MAIL_DOMAIN_COM;
        } else {
            if (username.contains("@")) {
                finalUsername = username.substring(0, username.indexOf("@"));
            }
        }
        showAccountCreation(address, finalUsername, domain);
        new Handler().postDelayed(() -> hideProgress(), 500);
        enableInput();
    }

    @OnClick(R.id.toggle_view_password)
    public void onShowPassword(ToggleButton showPassword) {
        if (showPassword.isChecked()) {
            mPasswordEditText.setTransformationMethod(SingleLineTransformationMethod.getInstance());
        } else {
            mPasswordEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }
        mPasswordEditText.setSelection(mPasswordEditText.getText().length());
    }

    @OnClick(R.id.create_account)
    public void onCreateAccountClicked(View view) {
        showAccountCreation(null, null, null);
    }

    private void hideProgress() {
        mProgressContainer.setVisibility(View.GONE);
    }

    private void showAccountCreation(Address address, String name, String domainName) {
        Intent intent = AppUtil.decorInAppIntent(new Intent(this, CreateAccountActivity.class));
        intent.putExtra(CreateAccountActivity.EXTRA_WINDOW_SIZE, getWindow().getDecorView().getHeight());
        intent.putExtra(CreateAccountActivity.EXTRA_ADDRESS_CHOSEN, address);
        intent.putExtra(CreateAccountActivity.EXTRA_NAME_CHOSEN, name);
        intent.putExtra(CreateAccountActivity.EXTRA_DOMAIN_NAME, domainName);
        startActivity(intent);
    }

    private void showTwoFactorDialog(final String username, final byte[] password, final LoginInfoResponse infoResponse,
                                     final LoginResponse loginResponse, int fallbackAuthVersion) {
        if (m2faAlertDialog != null && m2faAlertDialog.isShowing()) {
            return;
        }
        m2faAlertDialog = DialogUtils.Companion.show2FADialog(this,
            twoFactorString -> {
            UiUtil.hideKeyboard(this);
            mProgressContainer.setVisibility(View.VISIBLE);
            twoFA(username, password, twoFactorString, infoResponse, loginResponse, fallbackAuthVersion);
            ProtonMailApplication.getApplication().resetLoginInfoEvent();
            return null;
        }, () -> {
            mUserManager.logoutLastActiveAccount();
            UiUtil.hideKeyboard(LoginActivity.this);
            ProtonMailApplication.getApplication().resetLoginInfoEvent();
            return null;
        });
    }

    private void twoFA(String username, byte[] password, String twoFactor, LoginInfoResponse infoResponse,
                       LoginResponse loginResponse, int fallbackAuthVersion) {
        mUserManager.twoFA(username, password, twoFactor, infoResponse, loginResponse,  fallbackAuthVersion,false, false);
    }

    private void disableInput() {
        mSignIn.setClickable(false);
        mUsernameEditText.setFocusable(false);
        mPasswordEditText.setFocusable(false);
        mUsernameEditText.setFocusableInTouchMode(false);
        mPasswordEditText.setFocusableInTouchMode(false);
    }

    private void enableInput() {
        mSignIn.setClickable(true);
        mUsernameEditText.setFocusable(true);
        mPasswordEditText.setFocusable(true);
        mUsernameEditText.setFocusableInTouchMode(true);
        mPasswordEditText.setFocusableInTouchMode(true);
    }
}
