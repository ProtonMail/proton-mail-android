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
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import ch.protonmail.android.R;
import ch.protonmail.android.api.AccountManager;
import ch.protonmail.android.api.models.LoginInfoResponse;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.AuthStatus;
import ch.protonmail.android.events.CreateUserEvent;
import ch.protonmail.android.events.ForceUpgradeEvent;
import ch.protonmail.android.events.KeysSetupEvent;
import ch.protonmail.android.events.LoginEvent;
import ch.protonmail.android.events.LoginInfoEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.jobs.CheckUsernameAvailableJob;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;
import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public abstract class CreateAccountBaseFragment extends BaseFragment implements ViewTreeObserver.OnGlobalLayoutListener {

    @Inject
    AccountManager accountManager;

    public static final String ARGUMENT_WINDOW_HEIGHT = "ch.protonmail.android.ARG_WIN_HEIGHT";

    private static final int ANIMATION_START_DELAY = 0;
    private static final int ANIMATION_DURATION = 300;
    private final int mDefaultKeyboardDP = 100;
    private final int mEstimatedKeyboardDP = mDefaultKeyboardDP + (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ? 48 : 0);
    private int mVerticalAnimValue = 0;
    private int mHorizontalAnimValue = 0;
    private int mSpacing = 0;
    private int mLogoHeight = 0;
    private int mTitleHeight = 0;
    private int mInputLayoutVerticalValue = 0;
    private int mLogoVerticalValue = 0;
    private int mLogoHorizontalValue = 0;
    private int mTitleVerticalValue = 0;
    private int mTitleHorizontalValue = 0;
    private boolean mKeyboardShown = false;
    private int topOffset = -1;

    protected abstract void onFocusCleared();

    boolean isVisibleToUser;
    protected int windowHeight = 0;
    private boolean initialized;

    ImageView mLogo;
    TextView mTitle;
    LinearLayout mInputLayout;
    View.OnFocusChangeListener mFocusListener = (v, hasFocus) -> {
        if (hasFocus) {
            animate();
        } else {
            revertAnimation();
        }
    };
    View mRootLayout;

    protected abstract void setFocuses();

    protected abstract int getSpacing();

    protected abstract int getLogoId();

    protected abstract int getTitleId();

    protected abstract int getInputLayoutId();

    public void enableSubmitButton() {

    }

    protected ICreateAccountListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVerticalAnimValue = getResources().getDimensionPixelSize(R.dimen.activity_vertical_margin);
        mHorizontalAnimValue = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
        mSpacing = getSpacing();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootLayout = super.onCreateView(inflater, container, savedInstanceState);
        mLogo = mRootLayout.findViewById(getLogoId());
        mTitle = mRootLayout.findViewById(getTitleId());
        mInputLayout = mRootLayout.findViewById(getInputLayoutId());
        isVisibleToUser = true;
        return mRootLayout;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (ICreateAccountListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement ICreateAccountListener");
        }
    }

    public void init() {
        if (initialized) {
            return;
        }
        if (!isAdded()) {
            return;
        }
        if (mLogo == null || mTitle == null || mInputLayout == null) {
            return;
        }
        initialized = true;
        if (topOffset == -1) {
            topOffset = UiUtil.getStatusBarHeight(getActivity());
        }
        int[] locationLogo = new int[2];
        int[] locationTitle = new int[2];
        int[] locationInput = new int[2];
        mLogo.getLocationOnScreen(locationLogo);
        mTitle.getLocationOnScreen(locationTitle);
        mInputLayout.getLocationOnScreen(locationInput);
        mLogoHeight = mLogo.getHeight();
        mTitleHeight = mTitle.getHeight();
        mLogoHorizontalValue = locationLogo[0];
        mLogoVerticalValue = locationLogo[1] - topOffset;

        mTitleHorizontalValue = locationTitle[0];
        mTitleVerticalValue = locationTitle[1] - topOffset;

        setInputLayoutVerticalValue(locationInput);
//        setFocuses();
    }

    @Override
    public void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        ProtonMailApplication.getApplication().getBus().unregister(this);
    }

    private void animate() {

        if (mLogo == null) {
            mLogo = getView().findViewById(getLogoId());
        }
        if (mLogo != null) {
            mLogo.animate().x(mHorizontalAnimValue).y(mVerticalAnimValue).setDuration(ANIMATION_DURATION)
                    .setStartDelay(ANIMATION_START_DELAY).start();
        }
        if (mTitle == null) {
            mTitle = getView().findViewById(getTitleId());
        }
        if (mTitle != null) {
            mTitle.animate().x(mHorizontalAnimValue).y(mVerticalAnimValue + mLogoHeight - 10)
                    .setDuration(ANIMATION_DURATION).setStartDelay(ANIMATION_START_DELAY).start();
        }
        if (mInputLayout == null) {
            mInputLayout = getView().findViewById(getInputLayoutId());
        }
        if (mInputLayout != null) {
            mInputLayout.animate().y(mVerticalAnimValue + mSpacing + mTitleHeight)
                    .setDuration(ANIMATION_DURATION).setStartDelay(ANIMATION_START_DELAY).start();
        }
    }

    private void revertAnimation() {
        onFocusCleared();
        if (mLogo == null) {
            mLogo = getView().findViewById(getLogoId());
        }
        if (mLogo != null) {
            mLogo.animate().x(mLogoHorizontalValue).y(mLogoVerticalValue).setDuration(ANIMATION_DURATION)
                    .setStartDelay(ANIMATION_START_DELAY).start();
        }
        if (mTitle == null) {
            mTitle = getView().findViewById(getTitleId());
        }
        if (mTitle != null) {
            mTitle.animate().x(mTitleHorizontalValue).y(mTitleVerticalValue).setDuration(ANIMATION_DURATION)
                    .setStartDelay(ANIMATION_START_DELAY).start();
        }
        if (mInputLayout == null) {
            mInputLayout = getView().findViewById(getInputLayoutId());
        }
        if (mInputLayout != null) {
            mInputLayout.animate().y(mInputLayoutVerticalValue).setDuration(ANIMATION_DURATION)
                    .setStartDelay(ANIMATION_START_DELAY).start();
        }
    }

    void setInputLayoutVerticalValue(int[] locationInput) {
        mInputLayoutVerticalValue = locationInput[1] - topOffset;
        if (mInputLayoutVerticalValue <= 0) {
            mInputLayoutVerticalValue = mLogoVerticalValue + mTitleVerticalValue + topOffset;
        }
    }

    @Override
    public void onGlobalLayout() {
        if (mRootLayout == null) {
            return;
        }
        int estimatedKeyboardHeight = (int) TypedValue
                .applyDimension(TypedValue.COMPLEX_UNIT_DIP, mEstimatedKeyboardDP,
                        mRootLayout.getResources().getDisplayMetrics());
        final Rect r = new Rect();
        // Conclude whether the keyboard is shown or not.
        mRootLayout.getWindowVisibleDisplayFrame(r);
        int heightDiff = windowHeight - (r.bottom - r.top);
        boolean isKeyboardShown = heightDiff >= estimatedKeyboardHeight;
        if (!isKeyboardShown && mKeyboardShown) {
            revertAnimation();
            mKeyboardShown = false;
        } else if (isKeyboardShown) {
            animate();
            mKeyboardShown = true;
        }
        init();
    }

    public void onCreateUserEvent(CreateUserEvent event) {
        switch (event.status) {
            case SUCCESS: {
                mListener.startLoginInfo();
                break;
            }
            default:
                mListener.allowBackPress();
                handleLoginStatus(event.status, event.error);
                break;
        }
    }

    public void onLoginInfoEvent(final LoginInfoEvent event) {
        if (event == null) {
            return;
        }
        ProtonMailApplication.getApplication().resetLoginInfoEvent();
        switch (event.status) {
            case SUCCESS: {
                mListener.startLogin(event.response, event.fallbackAuthVersion);
            }
            break;
            default: {
                handleLoginStatus(event.status);
            }
            break;
        }
    }

    public void onLoginEvent(final LoginEvent event) {
        if (event == null) {
            return;
        }
        ProtonMailApplication.getApplication().resetLoginEvent();
        switch (event.getStatus()) {
            case SUCCESS: {
                mListener.startAddressSetup();
            }
            break;
            default:
                mListener.allowBackPress();
                handleLoginStatus(event.getStatus());
                break;
        }
    }

    public void onLoginEvent(MailboxLoginEvent event) {
        if (event == null) {
            return;
        }
        ProtonMailApplication.getApplication().resetMailboxLoginEvent();
        switch (event.status) {
            case SUCCESS: {
                // inform user creation completed
                mListener.startAddressSetup();
            }
            break;
            default:
                handleLoginStatus(event.status);
                break;
        }
    }

    public void onKeysSetupEvent(KeysSetupEvent event) {
        mProgressBar.setVisibility(View.GONE);
        switch (event.getStatus()) {
            case SUCCESS:
                mListener.createUserCompleted(windowHeight, true);
                break;
            default:
                handleLoginStatus(event.getStatus());
                break;
        }
    }

    void handleLoginStatus(AuthStatus status) {
        handleLoginStatus(status, null);
        accountManager.clearBlocking();
    }

    private void handleLoginStatus(AuthStatus status, String errorMessage) {
        if (mProgressBar != null) {
            mProgressBar.setVisibility(View.GONE);
        }
        switch (status) {
            case NO_NETWORK: {
                TextExtensions.showToast(getContext(), R.string.no_network);
                enableSubmitButton();
            }
            break;
            case UPDATE: {
                AppUtil.postEventOnUi(new ForceUpgradeEvent(errorMessage));
            }
            break;
            case INVALID_CREDENTIAL: {
                TextExtensions.showToast(getContext(), R.string.invalid_credentials);
                enableSubmitButton();
            }
            break;
            case INVALID_SERVER_PROOF: {
                TextExtensions.showToast(getContext(), R.string.invalid_server_proof);
                enableSubmitButton();
            }
            break;
            case FAILED:
            default: {
                String message = getString(R.string.login_failure);
                if (!TextUtils.isEmpty(errorMessage)) {
                    message = errorMessage;
                }
                TextExtensions.showToast(getContext(), message);
                enableSubmitButton();
            }
        }
    }

    public interface ICreateAccountListener {
        void onAccountSelected(Constants.AccountType selectedAccountType);

        Constants.AccountType getSelectedAccountType();

        void checkUsername(CheckUsernameAvailableJob job);

        void onUsernameSelected(String username);

        ArrayList<String> getMethods();

        void sendVerificationCode(String email, String phoneNumber);

        void createUser(Constants.TokenType tokenType, String token);

        void saveEncryptionBits(int bits);

        void saveUserData(String username, byte[] password, String domain, String notificationEmail, boolean updateMe);

        void startLoginInfo();

        void startLogin(LoginInfoResponse infoResponse, int fallbackAuthVersion);

        void createUserCompleted(int windowSize, boolean success);

        void generateKeyPairDone();

        void showInbox(String email, byte[] password, String displayName, boolean updateMe);

        void preventBackPress();

        void allowBackPress();

        boolean hasConnectivity();

        void createVerificationPaymentForPaymentToken(int amount, Constants.CurrencyType currency, String paymentToken);

        void onPaymentOptionChosen(Constants.CurrencyType currency, int amount, String planId, int cycle);

        void donateForPaymentToken(int amount, Constants.CurrencyType currency, String paymentToken);

        Constants.CurrencyType getCurrency();

        int getAmount();

        String getSelectedPlanId();

        int getSelectedCycle();

        void offerFreeAccount(int height);

        void fetchPlan(List<Constants.CurrencyType> currencies);

        void fetchOrganization();

        void onBillingCompleted();

        void onBillingError(String error, String errorDescription);

        /**
         * Adds a {@link Fragment} to this activity's layout.
         *
         * @param fragment The fragment to be added.
         */
        void replaceFragment(Fragment fragment, String backstackName);

        void donateDone();

        void getAvailableDomains();

        void startAddressSetup();
    }
}
