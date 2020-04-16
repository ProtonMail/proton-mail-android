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
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.squareup.otto.Subscribe;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.GenerateKeyPairEvent;

/**
 * Created by dkadrikj on 2/12/16.
 */
public class EncryptionSetupFragment extends CreateAccountBaseFragment {

    @BindView(R.id.high_standard_security)
    CheckBox mHighStandardCheckBox;
    @BindView(R.id.extreme_security)
    CheckBox mExtremeCheckBox;
    @BindView(R.id.progress_container)
    View mProgressContainer;

    public static EncryptionSetupFragment newInstance(final int windowHeight) {
        EncryptionSetupFragment fragment = new EncryptionSetupFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_WINDOW_HEIGHT, windowHeight);
        fragment.setArguments(extras);
        return fragment;
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
        mHighStandardCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mExtremeCheckBox.setChecked(!isChecked);
            }
        });

        mExtremeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mHighStandardCheckBox.setChecked(!isChecked);
            }
        });
        return view;
    }

    @OnClick(R.id.cont)
    void onContinueClicked() {
        int encryptionBits = Constants.HIGH_SECURITY_BITS;
        if (mExtremeCheckBox.isChecked()) {
            encryptionBits = Constants.EXTREME_SECURITY_BITS;
        }
        mProgressContainer.setVisibility(View.VISIBLE);
        mListener.preventBackPress();
        mListener.saveEncryptionBits(encryptionBits);
    }

    @Subscribe
    public void onGenerateKeyPairEvent(GenerateKeyPairEvent event) {
        if (event != null && event.isFinished()) {
            mListener.generateKeyPairDone();
            mListener.allowBackPress();
            CreateAccountBaseFragment nextFragment;
            if (mListener.getSelectedAccountType() == Constants.AccountType.FREE) {
                nextFragment = HumanVerificationFragment.newInstance(windowHeight, mListener.getMethods());
            } else {
                nextFragment = BillingFragment.newInstance(Constants.BillingType.CREATE, windowHeight,
                        mListener.getCurrency(), mListener.getAmount(), mListener.getSelectedPlanId(), mListener.getSelectedCycle());
            }
            mListener.replaceFragment(nextFragment, nextFragment.getFragmentKey());
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

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_encryption_setup;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.EncryptionSetupFragment";
    }
}
