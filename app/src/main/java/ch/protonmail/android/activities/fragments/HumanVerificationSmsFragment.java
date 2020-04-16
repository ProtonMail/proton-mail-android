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

import android.graphics.PorterDuff;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.squareup.otto.Subscribe;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.adapters.CountriesAdapter;
import ch.protonmail.android.api.models.Countries;
import ch.protonmail.android.api.models.Country;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.CreateUserEvent;
import ch.protonmail.android.events.KeysSetupEvent;
import ch.protonmail.android.events.LoginEvent;
import ch.protonmail.android.events.LoginInfoEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.events.SendVerificationCodeEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.FileUtils;
import ch.protonmail.android.utils.extensions.TextExtensions;

/**
 * Created by dkadrikj on 1/20/16.
 */
public class HumanVerificationSmsFragment extends HumanVerificationBaseFragment {

    @BindView(R.id.phone_number)
    EditText mPhoneNumber;
    @BindView(R.id.send_verification_code)
    Button mSendCode;
    @BindView(R.id.verification_code)
    EditText mVerificationCode;
    @BindView(R.id.verify)
    Button mVerify;
    @BindView(R.id.countries_spinner)
    Spinner mCountriesSpinner;
    @BindView(R.id.phone_calling_code)
    TextView mCallingCode;

    @BindView(R.id.sending_sms_circular)
    ProgressBar mSendingSmsProgress;
    @BindView(R.id.sending_sms_icon)
    ImageView mSendingSmsIcon;
    @BindView(R.id.sending_sms_description)
    TextView mSendingSmsDescription;
    long mSendVerificationClickedTimestamp = 0;
    private String token;
    private CountriesAdapter mAdapter;

    public static HumanVerificationSmsFragment newInstance(final int windowHeight) {
        HumanVerificationSmsFragment fragment = new HumanVerificationSmsFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_WINDOW_HEIGHT, windowHeight);
        fragment.setArguments(extras);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        Countries countries = new Gson().fromJson(FileUtils.readRawTextFile(getContext(), R.raw.country_codes), Countries.class);
        Countries mostUsedCountries = new Gson().fromJson(FileUtils.readRawTextFile(getContext(), R.raw.country_codes_most_used), Countries.class);
        List<Country> all = mostUsedCountries.getCountries();
        List<Country> otherCountries = countries.getCountries();
        Collections.sort(otherCountries, new Comparator<Country>() {
            @Override
            public int compare(Country lhs, Country rhs) {
                return lhs.getName().compareTo(rhs.getName());
            }
        });
        all.addAll(otherCountries);
        mAdapter = new CountriesAdapter(getContext(), all);
        mCountriesSpinner.setAdapter(mAdapter);
        mCountriesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCallingCode.setText("+" + mAdapter.getItem(position).getCode());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // noop
            }
        });
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

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_sms_verification;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.HumanVerificationSmsFragment";
    }

    @Override
    public void enableSubmitButton() {

    }

    private boolean isValidCode() {
        String code = mVerificationCode.getText().toString();
        return !TextUtils.isEmpty(code);
    }

    private boolean isValidPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        } else {
            return Patterns.PHONE.matcher(phoneNumber).matches();
        }
    }

    private boolean isValidVerificationCode() {
        return !TextUtils.isEmpty(token);
    }

    @OnClick(R.id.send_verification_code)
    public void onSendVerificationCode() {
        long timestamp = System.currentTimeMillis();
        long diff = timestamp - mSendVerificationClickedTimestamp;
        if (diff < MIN_CLICK_DIFF) {
            TextExtensions.showToast(getContext(), String.format(getString(R.string.send_code_wait), ((int)
                    (MIN_CLICK_DIFF - diff) / 1000)));
            return;
        }
        mSendVerificationClickedTimestamp = timestamp;
        String prefix = mCallingCode.getText().toString();
        if (TextUtils.isEmpty(prefix)) {
            TextExtensions.showToast(getContext(), R.string.invalid_email);
            return;
        }
        String phoneNumber = prefix + mPhoneNumber.getText().toString();
        if (isValidPhoneNumber(phoneNumber)) {
            mSendCode.setClickable(false);
            mSendingSmsProgress.setVisibility(View.VISIBLE);
            mListener.sendVerificationCode(null, phoneNumber);
        } else {
            TextExtensions.showToast(getContext(), R.string.invalid_email);
        }
    }

    @OnClick(R.id.verify)
    public void onVerifyCode() {
        if (!isValidCode()) {
            TextExtensions.showToast(getContext(), R.string.invalid_code);
            return;
        }
        token = mVerificationCode.getText().toString();
        if (isValidVerificationCode()) {
            mProgressBar.setVisibility(View.VISIBLE);
            mListener.createUser(Constants.TokenType.SMS, token);
        }
    }

    @Subscribe
    public void onCreateUserEvent(CreateUserEvent event) {
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

    @Subscribe
    public void onSendVerificationCodeEvent(SendVerificationCodeEvent event) {
        mSendingSmsProgress.setVisibility(View.GONE);
        if (event.getStatus() != null && event.getStatus() == Status.SUCCESS) {
            mSendingSmsIcon.setImageResource(R.drawable.ic_check_circle_black);
            mSendingSmsIcon.setColorFilter(0xFFA2C276, PorterDuff.Mode.SRC_IN);
            mSendingSmsDescription.setTextColor(getResources().getColor(R.color.green));
            mSendingSmsDescription.setText(getString(R.string.sending_email_success));
        } else {
            mSendingSmsIcon.setImageResource(R.drawable.ic_cancel_black);
            mSendingSmsIcon.setColorFilter(0xFFBF0000, PorterDuff.Mode.SRC_IN);
            mSendingSmsDescription.setTextColor(getResources().getColor(R.color.red));
            String error = getString(R.string.sending_email_failed);
            if (!TextUtils.isEmpty(event.getReason())) {
                error = event.getReason();
            }
            mSendingSmsDescription.setText(error);
        }
        mSendCode.setClickable(true);
        mSendCode.setText(getString(R.string.resend_verification_code));
    }
}
