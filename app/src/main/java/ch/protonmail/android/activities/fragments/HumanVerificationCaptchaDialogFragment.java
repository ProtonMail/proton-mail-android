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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import androidx.annotation.Nullable;

import java.net.MalformedURLException;
import java.net.URL;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.doh.Proxies;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.views.PMWebView;

/**
 * Created by dino on 12/25/16.
 */

public class HumanVerificationCaptchaDialogFragment extends HumanVerificationDialogFragment {

    @BindView(R.id.captcha)
    PMWebView mWebView;

    public static HumanVerificationCaptchaDialogFragment newInstance(String token) {
        HumanVerificationCaptchaDialogFragment fragment = new HumanVerificationCaptchaDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARGUMENT_TOKEN, token);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_captcha_dialog;
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
        // mHost = Constants.ENDPOINT_URI.substring(8);
        SharedPreferences prefs = ProtonMailApplication.getApplication().getDefaultSharedPreferences();
        String apiUrl = Proxies.Companion.getInstance(null, prefs).getCurrentWorkingProxyDomain();
        mHost = apiUrl.substring(8);
        int slashIndex = mHost.indexOf('/');
        if (slashIndex > 0) {
            mHost = mHost.substring(0, slashIndex);
        }
        try {
            // mHost = new URL(Constants.ENDPOINT_URI).getHost();
            mHost = new URL(apiUrl).getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (hasConnectivity) {
            mWebView.loadUrl("https://secure.protonmail.com/captcha/captcha.html?token=" + mToken + "&client=android&host=" + mHost);
        } else {
            TextExtensions.showToast(getContext(), R.string.no_connectivity_detected);
        }
        return rootView;
    }

    public void connectionArrived() {
        if (!hasConnectivity) {
            mWebView.loadUrl("https://secure.protonmail.com/captcha/captcha.html?token=" + mToken + "&client=android&host=" + mHost);
        }
    }

    private class CaptchaWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress == 100 && isAdded()) {
                mProgressBar.setVisibility(View.GONE);
                mContinue.setVisibility(View.VISIBLE);
                mListener.viewLoaded();
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
