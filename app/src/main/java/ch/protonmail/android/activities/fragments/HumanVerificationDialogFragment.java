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

import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.utils.extensions.TextExtensions;

/**
 * Created by dino on 12/27/16.
 */

public abstract class HumanVerificationDialogFragment extends DialogFragment {

    protected static final String ARGUMENT_TOKEN = "token";
    protected IHumanVerificationListener mListener;
    protected String mHost;
    protected String token;
    protected boolean hasConnectivity;
    protected String mToken;
    @BindView(R.id.cont)
    Button mContinue;
    @BindView(R.id.progress_circular)
    ProgressBar mProgressBar;

    protected abstract int getLayoutResourceId();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToken = getArguments().getString(ARGUMENT_TOKEN);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(getLayoutResourceId(), container, false);
        ButterKnife.bind(this, rootView);
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (IHumanVerificationListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement IHumanVerificationListener");
        }
    }

    private void verify() {
        if (isValid()) {
            mProgressBar.setVisibility(View.VISIBLE);
            mListener.verify(Constants.TokenType.CAPTCHA, token);
        } else {
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

    @OnClick(R.id.cont)
    public void onContinueClicked() {
        mContinue.setClickable(false);
        verify();
    }

    public interface IHumanVerificationListener {
        boolean hasConnectivity();

        void verify(Constants.TokenType tokenType, String token);

        void viewLoaded();

        void verificationOptionChose(Constants.TokenType tokenType, String token);
    }
}
