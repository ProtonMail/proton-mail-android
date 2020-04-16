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

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;

/**
 * Created by dkadrikj on 1/20/16.
 */
public class HumanVerificationFragment extends CreateAccountBaseFragment {

    private static final String ARGUMENT_METHODS = "ch.protonmail.android.ARG_METHODS";

    private static final String METHOD_CAPTCHA = "captcha";
    private static final String METHOD_EMAIL = "email";
    private static final String METHOD_SMS = "sms";

    private ArrayList<String> mMethods;
    @BindView(R.id.captcha)
    View mCaptcha;
    @BindView(R.id.email_verification)
    View mEmail;
    @BindView(R.id.phone_verification)
    View mPhone;
    @BindView(R.id.no_methods)
    View mNoMethods;

    public static HumanVerificationFragment newInstance(final int windowHeight, ArrayList<String> methods) {
        HumanVerificationFragment fragment = new HumanVerificationFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_WINDOW_HEIGHT, windowHeight);
        extras.putStringArrayList(ARGUMENT_METHODS, methods);
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        windowHeight = getArguments().getInt(ARGUMENT_WINDOW_HEIGHT);
        mMethods = getArguments().getStringArrayList(ARGUMENT_METHODS);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        if (mMethods == null || mMethods.size() == 0) {
            mNoMethods.setVisibility(View.VISIBLE);
            return rootView;
        }

        for (String method : mMethods) {
            if (method.equals(METHOD_CAPTCHA)) {
                mCaptcha.setVisibility(View.VISIBLE);
            } else if (method.equals(METHOD_EMAIL)) {
                mEmail.setVisibility(View.VISIBLE);
            } else if (method.equals(METHOD_SMS)) {
                mPhone.setVisibility(View.VISIBLE);
            }
        }

        return rootView;
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

    @OnClick(R.id.captcha)
    public void onCaptchaClicked() {
        HumanVerificationCaptchaFragment humanVerificationCaptchaFragment = HumanVerificationCaptchaFragment.newInstance(windowHeight);
        mListener.replaceFragment(humanVerificationCaptchaFragment, humanVerificationCaptchaFragment.getFragmentKey());
    }

    @OnClick(R.id.email_verification)
    public void onEmailVerificationClicked() {
        HumanVerificationEmailFragment humanVerificationEmailFragment = HumanVerificationEmailFragment.newInstance(windowHeight);
        mListener.replaceFragment(humanVerificationEmailFragment, humanVerificationEmailFragment.getFragmentKey());
    }

    @OnClick(R.id.phone_verification)
    public void onSMSVerificationClicked() {
        HumanVerificationSmsFragment humanVerificationSmsFragment = HumanVerificationSmsFragment.newInstance(windowHeight);
        mListener.replaceFragment(humanVerificationSmsFragment, humanVerificationSmsFragment.getFragmentKey());
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_human_verification;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.HumanVerificationFragment";
    }
}
