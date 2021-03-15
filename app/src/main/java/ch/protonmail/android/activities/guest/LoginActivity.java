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

import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_LOGIN_FINISHED;
import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_TO_INBOX;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.text.util.Linkify;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.lifecycle.ViewModelProvider;

import com.squareup.otto.Subscribe;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.LoginInfoResponse;
import ch.protonmail.android.api.models.LoginResponse;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.domain.entity.Id;
import ch.protonmail.android.domain.entity.Name;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.events.Login2FAEvent;
import ch.protonmail.android.events.LoginEvent;
import ch.protonmail.android.events.LoginInfoEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.utils.ui.dialogs.DialogUtils;
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.text.Charsets;
import timber.log.Timber;

@AndroidEntryPoint
public class LoginActivity extends BaseLoginActivity {

    @Inject
    UserManager userManager;

    @Inject
    ProtonMailApplication app;

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

    private boolean mDisableBack = false;
    private AlertDialog m2faAlertDialog;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private ConnectivityBaseViewModel viewModel;

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
        viewModel = new ViewModelProvider(this).get(ConnectivityBaseViewModel.class);
        // clickable sign up link
        AppUtil.clearNotifications(this, 3); // TODO: check which notification Id is this one

        mUsernameEditText.setFocusable(false);
        mPasswordEditText.setFocusable(false);
        mUsernameEditText.setOnTouchListener(mTouchListener);
        mPasswordEditText.setOnTouchListener(mTouchListener);
        final ch.protonmail.android.domain.entity.user.User currentUser = userManager.getCurrentUserBlocking();
        if (currentUser != null) {
            mUsernameEditText.setText(currentUser.getName().getS());
            mPasswordEditText.requestFocus();
        }

        Intent intent = getIntent();
        if (intent.getBooleanExtra(EXTRA_API_OFFLINE, false)) {
            final SpannableString s = new SpannableString(getResources().getString(R.string.api_offline));
            Linkify.addLinks(s, Linkify.ALL);
            DialogUtils.Companion.showInfoDialog(this, getString(R.string.api_offline_title), s.toString(), null);
        } else if (intent.getBooleanExtra(EXTRA_FORCE_UPGRADE, false)) {
            DialogUtils.Companion.showInfoDialog(this, getString(R.string.update_app_title), getString(R.string.update_app), null);
        }
        mAppVersion.setText(String.format(getString(R.string.app_version_code_login), AppUtil.getAppVersionName(this), AppUtil.getAppVersionCode(this)));
        viewModel.getHasConnectivity().observe(this, this::onConnectivityEvent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        app.getBus().register(this);
        UiUtil.hideKeyboard(LoginActivity.this);
        app.resetLoginInfoEvent();
        viewModel.checkConnectivity();
    }

    @Override
    protected void onStop() {
        super.onStop();
        app.getBus().unregister(this);
    }

    @Override
    public void onBackPressed() {
        if (!mDisableBack) {
            if (!userManager.isEngagementShown()) {
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
            handler.postDelayed(() -> {
                mDisableBack = false;
                userManager.info(username, password.getBytes(Charsets.UTF_8) /*TODO passphrase*/);
            }, 1500);
        }
    }

    @NotNull
    private Function0<Unit> onConnectivityCheckRetry() {
        return () -> {
            networkSnackBarUtil.getCheckingConnectionSnackBar(mSnackLayout, null).show();
            viewModel.checkConnectivityDelayed();
            onSignIn();
            return null;
        };
    }

    private void onConnectivityEvent(Constants.ConnectionState connectivity) {
        Timber.v("onConnectivityEvent hasConnectivity:%s", connectivity.name());
        if (connectivity != Constants.ConnectionState.CONNECTED) {
            networkSnackBarUtil.getNoConnectionSnackBar(
                    mSnackLayout,
                    userManager.getUser(),
                    this,
                    null,
                    null,
                    connectivity == Constants.ConnectionState.NO_INTERNET
            ).show();
        } else {
            networkSnackBarUtil.hideAllSnackBars();
        }
    }

    @Subscribe
    public void onLogin2FAEvent(final Login2FAEvent event) {
        if (event == null || event.userId == null) {
            return;
        }
        app.resetLogin2FAEvent();
        hideProgress();
        enableInput();
        showTwoFactorDialog(
                event.userId,
                event.username,
                event.password,
                event.infoResponse,
                event.loginResponse,
                event.fallbackAuthVersion
        );
    }

    @Subscribe
    public void onLoginInfoEvent(final LoginInfoEvent event) {
        if (event == null) {
            return;
        }
        switch (event.status) {
            case SUCCESS: {
                app.resetLoginInfoEvent();
                userManager.login(event.username, event.password, event.response, event.fallbackAuthVersion, false);
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
        userManager.setCurrentUserLoginState(LOGIN_STATE_TO_INBOX);
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
        app.resetLoginEvent();
        switch (event.getStatus()) {
            case SUCCESS: {
                if (event.isRedirectToSetup()) {
                    User user = event.getUser();
                    redirectToSetup(event.getUsername(), event.getDomainName(), user.getAddresses());
                    return;
                }
                hideProgress();
                userManager.setCurrentUserLoginState(LOGIN_STATE_LOGIN_FINISHED);
                userManager.saveKeySaltBlocking(new Id(event.getUser().getId()), event.getKeySalt());
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
            }
            break;
            case INVALID_SERVER_PROOF: {
                hideProgress();
                enableInput();
                mSignIn.setClickable(true);
                TextExtensions.showToast(this, R.string.invalid_server_proof);
            }
            break;
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
        handler.postDelayed(() -> hideProgress(), 500);
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

    private void showTwoFactorDialog(
            final Id userId,
            final Name username,
            final byte[] password,
            final LoginInfoResponse infoResponse,
            final LoginResponse loginResponse,
            int fallbackAuthVersion
    ) {
        if (m2faAlertDialog != null && m2faAlertDialog.isShowing()) {
            return;
        }
        m2faAlertDialog = DialogUtils.Companion.show2FADialog(this,
                twoFactorString -> {
                    UiUtil.hideKeyboard(this);
                    mProgressContainer.setVisibility(View.VISIBLE);
                    twoFA(
                            userId,
                            username,
                            password,
                            twoFactorString,
                            infoResponse,
                            loginResponse,
                            fallbackAuthVersion
                    );
                    app.resetLoginInfoEvent();
                    return null;
                }, () -> {
                    userManager.logoutLastActiveAccountBlocking();
                    UiUtil.hideKeyboard(LoginActivity.this);
                    app.resetLoginInfoEvent();
                    return null;
                });
    }

    private void twoFA(
            Id userId,
            Name username,
            byte[] password,
            String twoFactor,
            LoginInfoResponse infoResponse,
            LoginResponse loginResponse, int fallbackAuthVersion
    ) {
        userManager.twoFA(
                userId,
                username,
                password,
                twoFactor,
                infoResponse,
                loginResponse,
                fallbackAuthVersion,
                false,
                false
        );
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
