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
package ch.protonmail.android.activities.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Button;

import androidx.annotation.Nullable;

import com.squareup.otto.Subscribe;

import java.net.MalformedURLException;
import java.net.URL;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.doh.Proxies;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.AuthStatus;
import ch.protonmail.android.events.CreateUserEvent;
import ch.protonmail.android.events.KeysSetupEvent;
import ch.protonmail.android.events.LoginEvent;
import ch.protonmail.android.events.LoginInfoEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.views.PMWebView;

/**
 * Created by sunny on 11/3/15.
 */
public class HumanVerificationCaptchaFragment extends HumanVerificationBaseFragment {

    @BindView(R.id.webView)
    PMWebView mWebView;
    @BindView(R.id.continueButton)
    Button mContinue;

    private String host;
    private String token;
    private boolean hasConnectivity;

    public static HumanVerificationCaptchaFragment newInstance(final int windowHeight) {
        HumanVerificationCaptchaFragment fragment = new HumanVerificationCaptchaFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_WINDOW_HEIGHT, windowHeight);
        fragment.setArguments(extras);
        return fragment;
    }

    protected int getLayoutResourceId() {
        return R.layout.fragment_captcha;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.HumanVerificationCaptchaFragment";
    }

    @OnClick(R.id.continueButton)
    void onContinueClicked() {
        mContinue.setClickable(false);
        createUser();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");
        mWebView.setWebChromeClient(new CaptchaWebChromeClient());

        mProgressBar.setVisibility(View.VISIBLE);
        mContinue.setVisibility(View.GONE);
        hasConnectivity = mListener.hasConnectivity();
        // host = Constants.ENDPOINT_URI.substring(8);
        SharedPreferences prefs = ((ProtonMailApplication)(getContext().getApplicationContext())).getDefaultSharedPreferences();
        String apiUrl = Proxies.Companion.getInstance(null, prefs).getCurrentWorkingProxyDomain();
        host = apiUrl.substring(8);
        int slashIndex = host.indexOf('/');
        if (slashIndex > 0) {
            host = host.substring(0, slashIndex);
        }
        try {
            // host = new URL(Constants.ENDPOINT_URI).getHost();
            host = new URL(apiUrl).getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (hasConnectivity) {
            mWebView.loadUrl("https://secure.protonmail.com/captcha/captcha.html?token=signup&client=android&host=" + host);
        } else {
            mListener.allowBackPress();
            TextExtensions.showToast(getContext(), R.string.no_connectivity_detected);
        }
        return rootView;
    }

    public void connectionArrived() {
        if (!hasConnectivity) {
            mWebView.loadUrl("https://secure.protonmail.com/captcha/captcha.html?token=signup&client=android&host=" + host);
        }
    }

    @Override
    protected void onFocusCleared() {

    }

    @Override
    protected void setFocuses() {

    }

    @Override
    protected int getSpacing() {
        return 0;
    }

    @Override
    protected int getLogoId() {
        return 0;
    }

    @Override
    protected int getTitleId() {
        return 0;
    }

    @Override
    protected int getInputLayoutId() {
        return 0;
    }

    private void createUser() {
        if (isValid()) {
            mProgressBar.setVisibility(View.VISIBLE);
            mListener.preventBackPress();
            mListener.createUser(Constants.TokenType.CAPTCHA, token);
        } else {
            mListener.allowBackPress();
            mContinue.setClickable(true);
            TextExtensions.showToast(getActivity(), R.string.error_captcha_not_valid);
        }
    }

    private boolean isValid() {
        boolean isValid = false;
        if (!TextUtils.isEmpty(token)) {
            isValid = true;
        }
        return isValid;
    }

    @Override
    public void enableSubmitButton() {
        mContinue.setClickable(true);
    }

    @Subscribe
    public void onCreateUserEvent(CreateUserEvent event) {
        if (event.status != AuthStatus.SUCCESS) {
            mListener.allowBackPress();
        }
        super.onCreateUserEvent(event);
    }

    @Subscribe
    public void onLoginInfoEvent(final LoginInfoEvent event) {
        super.onLoginInfoEvent(event);
    }

    @Subscribe
    public void onLoginEvent(LoginEvent event) {
        super.onLoginEvent(event);
    }

    @Subscribe
    public void onLoginEvent(MailboxLoginEvent event) {
        super.onLoginEvent(event);
    }

    @Subscribe
    public void onKeysSetupEvent(KeysSetupEvent event) {
        super.onKeysSetupEvent(event);
    }

    private class CaptchaWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100 && isAdded()) {
                mProgressBar.setVisibility(View.GONE);
                mContinue.setVisibility(View.VISIBLE);
            }
        }
    }

    private class WebAppInterface {

        @JavascriptInterface
        public void receiveResponse(String message) {
            token = message;
        }
    }
}
