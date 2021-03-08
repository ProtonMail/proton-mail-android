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

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.text.method.SingleLineTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;

import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;

/**
 * Created by dkadrikj on 17.7.15.
 */
public class CreatePasswordsFragment extends CreateAccountBaseFragment {

    private static final String ARGUMENT_USERNAME = "ch.protonmail.android.ARG_USERNAME";
    private static final String ARGUMENT_DOMAIN = "ch.protonmail.android.ARG_DOMAIN";

    @BindView(R.id.generate_keypair)
    Button mGenerateKeypair;
    @BindView(R.id.password)
    EditText mPasswordEditText;
    @BindView(R.id.confirm_password)
    EditText mConfirmPasswordEditText;
    private String password;

    public static CreatePasswordsFragment newInstance(final int windowHeight, final String username, final String domain) {
        CreatePasswordsFragment fragment = new CreatePasswordsFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_WINDOW_HEIGHT, windowHeight);
        extras.putString(ARGUMENT_USERNAME, username);
        extras.putString(ARGUMENT_DOMAIN, domain);
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_create_passwords;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.CreateAccountMailboxFragment";
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        windowHeight = getArguments().getInt(ARGUMENT_WINDOW_HEIGHT);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);
        return view;
    }

    @OnClick(R.id.generate_keypair)
    public void onGenerateKeypair() {
        UiUtil.hideKeyboard(getActivity());
        mGenerateKeypair.setClickable(false);
        password = mPasswordEditText.getText().toString();
        final String confirmPassword = mConfirmPasswordEditText.getText().toString();
        if (!isPasswordInputValid(password, confirmPassword)) {
            mGenerateKeypair.setClickable(true);
            TextExtensions.showToast(getContext(), R.string.message_passwords_do_not_match);
            return;
        }

        mProgressBar.setVisibility(View.VISIBLE);
        Bundle args = getArguments();
        String username = args.getString(ARGUMENT_USERNAME);
        String domain = args.getString(ARGUMENT_DOMAIN);
        mListener.saveUserData(username, password.getBytes() /*TODO passphrase*/, domain, "", false);
        EncryptionSetupFragment encryptionSetupFragment = EncryptionSetupFragment.newInstance(windowHeight);
        mListener.replaceFragment(encryptionSetupFragment, encryptionSetupFragment.getFragmentKey());
    }

    private boolean isPasswordInputValid(String password, String confirmPassword) {
        boolean isValid = false;

        if (!TextUtils.isEmpty(password) && !TextUtils.isEmpty(confirmPassword) && confirmPassword.equals(password)) {
            isValid = true;
        }

        return isValid;
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

    /**
     * Hide soft keyboard when touch outside EditText
     */
    @OnClick({R.id.container})
    public void onOutsideClick() {
        UiUtil.hideKeyboard((Activity) getContext());
    }

    @Override
    protected void onFocusCleared() {

    }

    @Override
    protected void setFocuses() {
        mPasswordEditText.setOnFocusChangeListener(mFocusListener);
        mConfirmPasswordEditText.setOnFocusChangeListener(mFocusListener);
    }

    @Override
    protected int getSpacing() {
        return getResources().getDimensionPixelSize(R.dimen.spacing) +
                getResources().getDimensionPixelSize(R.dimen.fields_default_space_medium);
    }

    @Override
    protected int getLogoId() {
        return R.id.logo_mailbox;
    }

    @Override
    protected int getTitleId() {
        return R.id.title_mailbox;
    }

    @Override
    protected int getInputLayoutId() {
        return R.id.input_layout_mailbox;
    }
}
