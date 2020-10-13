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

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.squareup.otto.Subscribe;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.user.MailSettingsEvent;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import timber.log.Timber;

import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_TO_INBOX;

@AndroidEntryPoint
public class MailboxLoginActivity extends BaseLoginActivity {

    public static final String EXTRA_KEY_SALT = "key_salt";

    @Inject
    ConnectivityBaseViewModel viewModel;

    @BindView(R.id.mailbox_password)
    EditText mPasswordEditText;
    @BindView(R.id.progress_container)
    View mProgressContainer;
    @BindView(R.id.forgot_mailbox_password)
    TextView mForgotPasswordView;
    @BindView(R.id.sign_in)
    Button mSignIn;

    private boolean mDisableBack = false;
    private boolean mIsUnRegistered;

    @Override
    protected boolean shouldCheckForAutoLogout() {
        return false;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_mailbox_login;
    }

    @Override
    protected void setFocuses() {
        mPasswordEditText.setOnFocusChangeListener(mFocusListener);
    }

    @Override
    protected boolean isPreventingScreenshots() {
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProgressBar.getIndeterminateDrawable().setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.MULTIPLY);
        mUserManager.setLoggedIn(false);
        mPasswordEditText.setFocusable(false);
        mPasswordEditText.setOnTouchListener(mTouchListener);
        viewModel.getHasConnectivity().observe(this, this::onConnectivityEvent);
    }

    @Override
    protected void onMailboxSuccess() {
        ProtonMailApplication.getApplication().getBus().unregister(this);
        mIsUnRegistered = true;
        mUserManager.setLoginState(LOGIN_STATE_TO_INBOX);
    }

    @Override
    protected void onMailboxNoNetwork() {
        resetState();
    }

    @Override
    protected void onMailboxUpdate() {
        resetState();
    }

    @Override
    protected void onMailboxInvalidCredential() {
        resetState();
    }

    @Override
    protected void onMailboxNotSignedUp() {
        resetState();
    }

    @Override
    protected void onMailboxFailed() {
        resetState();
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.checkConnectivity();
    }

    @Override
    protected void onPause() {
        networkSnackBarUtil.hideAllSnackBars();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (!mIsUnRegistered) {
            ProtonMailApplication.getApplication().getBus().unregister(this);
        }
    }

    @OnClick(R.id.forgot_mailbox_password)
    public void onForgotPassword() {
        TextExtensions.showToast(this, R.string.forgot_mailbox_password);
    }

    @OnClick(R.id.sign_in)
    public void onSignIn() {
        mDisableBack = true;
        mSignIn.setClickable(false);
        mPasswordEditText.setFocusable(false);
        mProgressContainer.setVisibility(View.VISIBLE);
        final String mailboxPassword = mPasswordEditText.getText().toString();
        UiUtil.hideKeyboard(this, mPasswordEditText);
        new Handler().postDelayed(() -> {
            mDisableBack = false;
            mUserManager.mailboxLogin(mUserManager.getUsername() /* TODO we should indicate in GUI, which user we log in */, mailboxPassword, getIntent().getStringExtra(EXTRA_KEY_SALT), false);
        }, 1500);
    }

    @Override
    public void onBackPressed() {
        if (!mDisableBack) {
            mProgressContainer.setVisibility(View.VISIBLE);
            mUserManager.removeAccount(mUserManager.getUsername(), null);
            super.onBackPressed();
        }
    }

    private void resetState() {
        mProgressContainer.setVisibility(View.GONE);
        mPasswordEditText.setFocusable(true);
        mPasswordEditText.setFocusableInTouchMode(true);
        mSignIn.setClickable(true);
    }

    private Function0<Unit> onConnectivityCheckRetry() {
        return () -> {
            networkSnackBarUtil.getCheckingConnectionSnackBar(mSnackLayout, null).show();
            viewModel.checkConnectivityDelayed();
            return null;
        };
    }

    private void onConnectivityEvent(boolean hasConnectivity) {
        Timber.v("onConnectivityEvent hasConnectivity:%s", hasConnectivity);
        if (!hasConnectivity) {
            networkSnackBarUtil.getNoConnectionSnackBar(
                    mSnackLayout,
                    mUserManager.getUser(),
                    this,
                    onConnectivityCheckRetry(),
                    null,
                    R.string.no_connectivity_detected_troubleshoot,
                    false
            ).show();
        } else {
            networkSnackBarUtil.hideAllSnackBars();
        }
    }

    @Subscribe
    public void  onMailSettingsEvent(MailSettingsEvent event) {
        loadMailSettings();
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {

        if (event.status == Status.NO_NETWORK) {
            mProgressContainer.setVisibility(View.GONE);
            TextExtensions.showToast(this, R.string.no_network, Toast.LENGTH_SHORT);
            return;
        }

        AppUtil.clearTasks(mJobManager);
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
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

    @Subscribe
    public void onLoginEvent(MailboxLoginEvent event) {
        super.onLoginEvent(event);
    }
}
