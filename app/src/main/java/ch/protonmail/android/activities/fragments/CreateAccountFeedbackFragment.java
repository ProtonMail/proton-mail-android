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
import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;

import androidx.annotation.Nullable;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.CommonExtensionsKt;
import ch.protonmail.android.utils.extensions.TextExtensions;

/**
 * Created by dkadrikj on 12/4/15.
 */
public class CreateAccountFeedbackFragment extends CreateAccountBaseFragment {

    private static final String ARGUMENT_SUCCESS = "ch.protonmail.android.ARG_SUCCESS";
    private static final String ARGUMENT_PASSWORD = "ch.protonmail.android.ARG_PASSWORD";

    @BindView(R.id.notification_email)
    EditText mNotificationEmailEditText;
    @BindView(R.id.update_for_new_features)
    CheckBox mUpdateMeCheckBox;
    @BindView(R.id.display_name)
    EditText mDisplayNameEditText;
    private byte[] password;

    public static CreateAccountFeedbackFragment newInstance(final int windowHeight, final boolean
            success, final byte[] password) {
        CreateAccountFeedbackFragment fragment = new CreateAccountFeedbackFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_WINDOW_HEIGHT, windowHeight);
        extras.putBoolean(ARGUMENT_SUCCESS, success);
        extras.putByteArray(ARGUMENT_PASSWORD, password);
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        windowHeight = getArguments().getInt(ARGUMENT_WINDOW_HEIGHT);
        password = getArguments().getByteArray(ARGUMENT_PASSWORD);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        mRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);
        return view;
    }

    @Override
    protected void onFocusCleared() {

    }

    /**
     * Hide soft keyboard when touch outside EditText
     */
    @OnClick({R.id.container})
    void onOutsideClick() {
        UiUtil.hideKeyboard((Activity) getContext());
    }

    @OnClick(R.id.go_to_inbox)
    void onGoToInboxClicked() {
        proceedToInbox(false);
    }

    @Override
    protected void setFocuses() {
        mNotificationEmailEditText.setOnFocusChangeListener(mFocusListener);
    }

    private void proceedToInbox(boolean proceedIfEmptyEmail) {
        String email = mNotificationEmailEditText.getText().toString();
        String displayName = mDisplayNameEditText.getText().toString();
        if (isValidEmailAddress(email, proceedIfEmptyEmail)) {
            boolean updateMe = mUpdateMeCheckBox.isChecked();
            mListener.showInbox(email, password /*TODO passphrase*/, displayName, updateMe);
        } else {
            if (TextUtils.isEmpty(email)) {
                askForEmptyRecoveryEmail();
            } else {
                TextExtensions.showToast(getContext(), R.string.invalid_email);
            }
        }
    }

    private boolean isValidEmailAddress(String email, boolean validIfEmpty) {
        if (TextUtils.isEmpty(email) && validIfEmpty) {
            return true;
        } else {
            return CommonExtensionsKt.isValidEmail(email);
        }
    }

    @Override
    protected int getSpacing() {
        return getResources().getDimensionPixelSize(R.dimen.spacing) +
                getResources().getDimensionPixelSize(R.dimen.fields_default_space_medium);
    }

    @Override
    protected int getLogoId() {
        return R.id.logo_feedback;
    }

    @Override
    protected int getTitleId() {
        return R.id.title_feedback;
    }

    @Override
    protected int getInputLayoutId() {
        return R.id.input_layout_feedback;
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_create_account_feedback;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.CreateAccountFeedbackFragment";
    }

    private void askForEmptyRecoveryEmail() {
        if (!((Activity) getContext()).isFinishing()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle(R.string.warning)
                    .setMessage(R.string.empty_recovery_email_question)
                    .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                    .setPositiveButton(R.string.yes, (dialog, which) -> proceedToInbox(true))
                    .create()
                    .show();
        }
    }
}
