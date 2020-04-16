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
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.squareup.otto.Subscribe;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.CheckUsernameEvent;
import ch.protonmail.android.jobs.CheckUsernameAvailableJob;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;

/**
 * Created by dkadrikj on 17.7.15.
 */
public class CreateAccountFragment extends CreateAccountBaseFragment implements AdapterView.OnItemSelectedListener {

    private static final String ARGUMENT_DOMAINS = "ch.protonmail.android.ARG_DOMAINS";
    private static final String ARGUMENT_NAME_CHOSEN = "ch.protonmail.android.ARG_NAME_CHOSEN";

    private static final String STATE_HEIGHT = "state_height";
    private static final String STATE_DOMAINS = "state_domains";

    @BindView(R.id.username)
    EditText mUsernameEditText;
    @BindView(R.id.username_text)
    TextView mUsernameTextView;
    @BindView(R.id.sign_up)
    Button mSignUp;
    @BindView(R.id.username_availability)
    TextView mUsernameAvailability;
    @BindView(R.id.username_availability_icon)
    ImageView mUsernameAvailabilityIcon;
    @BindView(R.id.progress_circular)
    ProgressBar mProgressBar;
    @BindView(R.id.terms)
    TextView mTerms;
    @BindView(R.id.domains_spinner)
    Spinner mDomainsSpinner;
    @BindView(R.id.check_availability_circular)
    ProgressBar mCheckAvailabilityCircular;
    @BindView(R.id.title)
    TextView mTitle;

    private ArrayList<String> domains;
    private String domain;
    private boolean usernameAvailable;
    private boolean availabilityCheckRunning;
    private boolean immediatelyRedirect;
    private final RedirectHandler mRedirectHandler = new RedirectHandler();
    private String mNameChosen;

    public static CreateAccountFragment newInstance(int windowHeight, List<String> domains, String nameChosen) {
        CreateAccountFragment fragment = new CreateAccountFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_WINDOW_HEIGHT, windowHeight);
        extras.putStringArrayList(ARGUMENT_DOMAINS, (ArrayList<String>) domains);
        extras.putString(ARGUMENT_NAME_CHOSEN, nameChosen);
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        usernameAvailable = false;
        availabilityCheckRunning = false;
        if (windowHeight == 0) {
            windowHeight = getArguments().getInt(ARGUMENT_WINDOW_HEIGHT);
        }
        if (domains == null || domains.size() == 0) {
            domains = new ArrayList<>();
            List<String> availableDomains = getArguments().getStringArrayList(ARGUMENT_DOMAINS);
            mNameChosen = getArguments().getString(ARGUMENT_NAME_CHOSEN);
            if (availableDomains != null && availableDomains.size() > 0) {
                for (String d : availableDomains) {
                    domains.add("@" + d);
                }
            } else if (mListener != null) {
                mListener.getAvailableDomains();
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_HEIGHT, windowHeight);
        outState.putStringArrayList(STATE_DOMAINS, domains);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            windowHeight = savedInstanceState.getInt(STATE_HEIGHT);
            domains = savedInstanceState.getStringArrayList(STATE_DOMAINS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        mTerms.setMovementMethod(LinkMovementMethod.getInstance());
        mRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),R.layout.simple_spinner_item, domains);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDomainsSpinner.setAdapter(adapter);
        mDomainsSpinner.setOnItemSelectedListener(this);
        setUsernameEditTextPadding();
        if (!TextUtils.isEmpty(mNameChosen)) {
            mUsernameTextView.setVisibility(View.VISIBLE);
            mUsernameEditText.setVisibility(View.GONE);
            mUsernameTextView.setText(mNameChosen);
            mTitle.setText(getString(R.string.setup_new_account));
        }

        mUsernameEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                usernameAvailable = false;
                hideUsernameAvailabilityMessage();
            }

            @Override
            public void afterTextChanged(Editable s) {
                usernameAvailable = false;
            }
        });
        return rootView;
    }

    private void setUsernameEditTextPadding() {
        mDomainsSpinner.measure(0, 0);
        int spinnerWidth = mDomainsSpinner.getMeasuredWidth();
        mUsernameEditText.measure(0, 0);
        mUsernameEditText.setPadding(mUsernameEditText.getPaddingLeft(), mUsernameEditText.getPaddingTop(), spinnerWidth, mUsernameEditText.getPaddingBottom());
    }

    public void setDomains(List<String> availableDomains) {
        domains = new ArrayList<>();
        if (availableDomains != null && availableDomains.size() > 0) {
            for (String d : availableDomains) {
                domains.add("@" + d);
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),R.layout.simple_spinner_item, domains);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mDomainsSpinner.setAdapter(adapter);
        setUsernameEditTextPadding();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_create_account;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.CreateAccountFragment";
    }

    @OnClick(R.id.sign_up)
    public void onSignUp() {
        UiUtil.hideKeyboard(getContext(), mUsernameEditText);
        mSignUp.setClickable(false);
        if (usernameAvailable) {
            showPasswordsScreen();
        } else {
            immediatelyRedirect = true;
            checkUsernameAvailability();
        }
    }

    private void showPasswordsScreen() {
        String username = mUsernameEditText.getText().toString();
        if (!TextUtils.isEmpty(mNameChosen)) {
            username = mUsernameTextView.getText().toString();
        }

        if (!isInputValid(username)) {
            mSignUp.setClickable(true);
            TextExtensions.showToast(getContext(), R.string.invalid_credentials);
            return;
        }
        mProgressBar.setVisibility(View.VISIBLE);
        isVisibleToUser = false;
        domain = domain.substring(1, domain.length());
        mListener.onUsernameSelected(username);
        CreatePasswordsFragment createPasswordsFragment = CreatePasswordsFragment.newInstance(windowHeight, username, domain);
        mListener.replaceFragment(createPasswordsFragment, createPasswordsFragment.getFragmentKey());
    }

    private boolean isInputValid(String username) {
        boolean isValid = false;

        if (!TextUtils.isEmpty(username)) {
            isValid = true;
        }

        return isValid;
    }

    @Subscribe
    public void onCheckUsernameEvent(CheckUsernameEvent event) {
        mCheckAvailabilityCircular.setVisibility(View.GONE);
        switch (event.getStatus()) {
            case SUCCESS:
                usernameAvailable = event.isAvailable();
                displayAvailabilityResponse();
                break;
            case NO_NETWORK:
                TextExtensions.showToast(getContext(), R.string.no_network);
                break;
        }
    }

    private void setUpErrorMessage() {
        mSignUp.setClickable(true);
        mUsernameAvailabilityIcon.setImageResource(R.drawable.ic_cancel_black);
        mUsernameAvailabilityIcon.setColorFilter(0xFFBF0000, PorterDuff.Mode.SRC_IN);
        mUsernameAvailability.setTextColor(getResources().getColor(R.color.red));
    }

    private void displayAvailabilityResponse() {
        availabilityCheckRunning = false;
        String username = mUsernameEditText.getText().toString();
        username += domain;
        mUsernameAvailabilityIcon.setVisibility(View.VISIBLE);
        mUsernameAvailability.setVisibility(View.VISIBLE);
        if (usernameAvailable) {
            mUsernameAvailabilityIcon.setImageResource(R.drawable.ic_check_circle_black);
            mUsernameAvailabilityIcon.setColorFilter(0xFFA2C276, PorterDuff.Mode.SRC_IN);
            mUsernameAvailability.setTextColor(getResources().getColor(R.color.green));
            mUsernameAvailability.setText(String.format(getString(R.string.username_is_available), username));
            if (immediatelyRedirect) {
                mRedirectHandler.postDelayed(new RedirectRunnable(this), 1200);
            }
        } else if (username.length() > Constants.MAX_USERNAME_LENGTH) {
            setUpErrorMessage();
            mUsernameAvailability.setText(String.format(getString(R.string.username_is_too_long), Constants.MAX_USERNAME_LENGTH));
        } else {
            setUpErrorMessage();
            mUsernameAvailability.setText(R.string.username_is_not_available);
        }
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
        checkUsernameAvailability();
    }

    private void hideUsernameAvailabilityMessage() {
        if (mUsernameAvailabilityIcon.getVisibility() == View.VISIBLE) {
            mUsernameAvailabilityIcon.setVisibility(View.INVISIBLE);
        }
        if (mUsernameAvailability.getVisibility() == View.VISIBLE) {
            mUsernameAvailability.setVisibility(View.INVISIBLE);
        }
    }

    private void checkUsernameAvailability() {
        if (isAdded()) {
            if (availabilityCheckRunning) {
                return;
            }
            if (mUsernameEditText == null) {
                return;
            }
            String wantedUsername = mUsernameEditText.getText().toString();
            if (TextUtils.isEmpty(wantedUsername)) {
                return;
            }
            availabilityCheckRunning = true;
            hideUsernameAvailabilityMessage();
            mCheckAvailabilityCircular.setVisibility(View.VISIBLE);
            // focus lost, check for username availability
            CheckUsernameAvailableJob job = new CheckUsernameAvailableJob(wantedUsername);
            mListener.checkUsername(job);
        }
    }

    @Override
    protected void setFocuses() {
        mUsernameEditText.setOnFocusChangeListener(mFocusListener);
    }

    @Override
    protected int getSpacing() {
        return getResources().getDimensionPixelSize(R.dimen.spacing) +
                getResources().getDimensionPixelSize(R.dimen.fields_default_space_medium);
    }

    @Override
    protected int getLogoId() {
        return R.id.logo;
    }

    @Override
    protected int getTitleId() {
        return R.id.title;
    }

    @Override
    protected int getInputLayoutId() {
        return R.id.input_layout;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        domain = domains.get(position);
        immediatelyRedirect = false;
        checkUsernameAvailability();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        domain = domains.get(0);
        immediatelyRedirect = false;
        checkUsernameAvailability();
    }

    private static class RedirectHandler extends Handler {
        // non leaky handler
    }

    private static class RedirectRunnable implements Runnable {
        // non leaky runnable
        private final WeakReference<CreateAccountFragment> createAccountFragmentWeakReference;

        public RedirectRunnable(CreateAccountFragment createAccountFragment) {
            createAccountFragmentWeakReference = new WeakReference<>(createAccountFragment);
        }
        @Override
        public void run() {
            CreateAccountFragment createAccountFragment = createAccountFragmentWeakReference.get();
            if (createAccountFragment != null && createAccountFragment.isAdded()) {
                createAccountFragment.showPasswordsScreen();
            }
        }
    }
}
