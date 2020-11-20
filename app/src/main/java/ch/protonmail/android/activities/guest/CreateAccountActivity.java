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
package ch.protonmail.android.activities.guest;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.ViewModelProvider;

import com.squareup.otto.Subscribe;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseConnectivityActivity;
import ch.protonmail.android.activities.fragments.BillingFragment;
import ch.protonmail.android.activities.fragments.CreateAccountFragment;
import ch.protonmail.android.activities.fragments.CreatePasswordsFragment;
import ch.protonmail.android.activities.fragments.HumanVerificationCaptchaDialogFragment;
import ch.protonmail.android.activities.fragments.HumanVerificationCaptchaFragment;
import ch.protonmail.android.activities.fragments.HumanVerificationDialogFragment;
import ch.protonmail.android.activities.fragments.SelectAccountTypeFragment;
import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.models.AllCurrencyPlans;
import ch.protonmail.android.api.models.LoginInfoResponse;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.address.AddressSetupResponse;
import ch.protonmail.android.api.services.LoginService;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.AddressSetupEvent;
import ch.protonmail.android.events.AuthStatus;
import ch.protonmail.android.events.AvailablePlansEvent;
import ch.protonmail.android.events.GetDirectEnabledEvent;
import ch.protonmail.android.events.KeysSetupEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.general.AvailableDomainsEvent;
import ch.protonmail.android.jobs.CheckUsernameAvailableJob;
import ch.protonmail.android.jobs.GetCurrenciesPlansJob;
import ch.protonmail.android.jobs.GetDirectEnabledJob;
import ch.protonmail.android.jobs.SendVerificationCodeJob;
import ch.protonmail.android.jobs.general.GetAvailableDomainsJob;
import ch.protonmail.android.jobs.payments.VerifyPaymentJob;
import ch.protonmail.android.usecase.fetch.LaunchInitialDataFetch;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.viewmodel.ConnectivityBaseViewModel;
import dagger.hilt.android.AndroidEntryPoint;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import timber.log.Timber;

import static ch.protonmail.android.core.UserManagerKt.LOGIN_STATE_TO_INBOX;

@AndroidEntryPoint
public class CreateAccountActivity extends BaseConnectivityActivity implements
        CreateAccountFragment.ICreateAccountListener,
        BillingFragment.IBillingListener,
        HumanVerificationDialogFragment.IHumanVerificationListener {

    public static final String EXTRA_WINDOW_SIZE = "window_size";
    public static final String EXTRA_ADDRESS_CHOSEN = "address_chosen";
    public static final String EXTRA_NAME_CHOSEN = "name_chosen";
    public static final String EXTRA_DOMAIN_NAME = "domain_name";

    private static final String STATE_USERNAME = "username";
    private static final String STATE_DOMAIN = "domain";
    private static final String STATE_ADDRESS = "address";

    private ConnectivityBaseViewModel viewModel;

    @Inject
    protected LaunchInitialDataFetch launchInitialDataFetch;

    @BindView(R.id.fragmentContainer)
    View fragmentContainer;
    @BindView(R.id.progress_container)
    View mProgressContainer;
    @BindView(R.id.progress_circular)
    ProgressBar mProgressBar;

    private SelectAccountTypeFragment selectAccountFragment;
    private CreateAccountFragment createUsernameFragment;
    private CreatePasswordsFragment createPasswordsFragment;
    private HumanVerificationCaptchaFragment captchaFragment;
    private BillingFragment billingFragment;
    private HumanVerificationCaptchaDialogFragment humanVerificationCaptchaDialogFragment;

    private String mUserName;
    private byte[] mPassword;
    private String mDomain;
    private String mNotificationEmail;
    private boolean mUpdateMe;
    private int mBits;
    private boolean mBackPressAllowed = true;
    private String mAddressId;

    private boolean mDirect = false;
    private List<String> mVerifyMethods;
    private Constants.AccountType selectedAccountType;
    private String wantedUsername;
    private AllCurrencyPlans mAllCurrencyPlans;
    private List<String> availableDomains;

    private Constants.CurrencyType currency;
    private int amount;
    private String selectedPlanId;
    private int selectedCycle;
    // for uncompleted account setup
    private Address addressChosen;
    private String nameChosen;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_fragment_container;
    }

    @Override
    protected boolean shouldCheckForAutoLogout() {
        return false;
    }

    @Override
    protected boolean isPreventingScreenshots() {
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this).get(ConnectivityBaseViewModel.class);
        if (fragmentContainer != null) {
            if (savedInstanceState != null) {
                addressChosen = savedInstanceState.getParcelable(STATE_ADDRESS);
                nameChosen = savedInstanceState.getString(STATE_USERNAME);
                mDomain = savedInstanceState.getString(STATE_DOMAIN);
            }
            mProgressBar.getIndeterminateDrawable().setColorFilter(0xFFFFFFFF, android.graphics.PorterDuff.Mode.MULTIPLY);
            mProgressContainer.setVisibility(View.VISIBLE);
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                addressChosen = extras.getParcelable(EXTRA_ADDRESS_CHOSEN);
                nameChosen = extras.getString(EXTRA_NAME_CHOSEN);
                mDomain = extras.getString(EXTRA_DOMAIN_NAME);
            }
            if (ProtonMailApplication.getApplication().getAllCurrencyPlans() == null) {
                List<Constants.CurrencyType> currencies = new ArrayList<>();
                currencies.add(Constants.CurrencyType.EUR);
                GetCurrenciesPlansJob job = new GetCurrenciesPlansJob(currencies);
                mJobManager.start();
                mJobManager.addJobInBackground(job);
            } else {
                attachAccountTypeSelection(ProtonMailApplication.getApplication().getAllCurrencyPlans());
            }

            checkDirectEnabled();
        }
        viewModel.getHasConnectivity().observe(this, this::onConnectivityEvent);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.checkConnectivity();
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        if (fragment instanceof BillingFragment) {
            billingFragment = (BillingFragment) fragment;
            billingFragment.setActivityListener(this);
        }
    }

    @Subscribe
    public void onPlansEvent(AvailablePlansEvent event) {
        attachAccountTypeSelection(event.getAllPlans());
    }

    @Override
    protected void onStop() {
        super.onStop();
        ProtonMailApplication.getApplication().getBus().unregister(this);
    }

    private void attachAccountTypeSelection(AllCurrencyPlans allCurrencyPlans) {
        if (selectAccountFragment != null) {
            return;
        }
        mAllCurrencyPlans = allCurrencyPlans;
        ProtonMailApplication.getApplication().setAllCurrencyPlans(mAllCurrencyPlans);
        if (availableDomains != null && availableDomains.size() > 0) {
            showAccountTypeSelection();
        } else {
            GetAvailableDomainsJob availableDomainsJob = new GetAvailableDomainsJob(false);
            mJobManager.addJobInBackground(availableDomainsJob);
        }
    }

    // region BillingFragment.IBillingListener
    @Override
    public ProtonMailApiManager getProtonMailApiManager() {
        return mApi;
    }

    @Override
    public void onRequestCaptchaVerification(String token) {
        humanVerificationCaptchaDialogFragment = HumanVerificationCaptchaDialogFragment.newInstance(token);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragmentContainer, humanVerificationCaptchaDialogFragment)
                .commitAllowingStateLoss();
    }
    // endregion

    // region HumanVerificationDialogFragment.IHumanVerificationListener
    @Override
    public void verify(Constants.TokenType tokenType, String token) {
        if (humanVerificationCaptchaDialogFragment != null && humanVerificationCaptchaDialogFragment.isAdded()) {
            humanVerificationCaptchaDialogFragment.dismiss();
        }
        billingFragment.retryPaymentAfterCaptchaValidation(token, tokenType.getTokenTypeValue());
    }

    @Override
    public void viewLoaded() {
        UiUtil.hideKeyboard(this);
    }

    @Override
    public void verificationOptionChose(Constants.TokenType tokenType, String token) {
        // noop
    }
    // endregion

    @Override
    public void onAccountSelected(Constants.AccountType selectedAccountType) {
        this.selectedAccountType = selectedAccountType;
        createUsernameFragment = CreateAccountFragment.newInstance(getIntent().getExtras().getInt(EXTRA_WINDOW_SIZE), availableDomains, null);
        replaceFragment(createUsernameFragment, createUsernameFragment.getFragmentKey());
    }

    @Override
    public Constants.AccountType getSelectedAccountType() {
        return selectedAccountType;
    }

    @Override
    public void checkUsername(CheckUsernameAvailableJob job) {
        mJobManager.addJobInBackground(job);
    }

    @Override
    public void onUsernameSelected(String username) {
        wantedUsername = username;
        mUserManager.setPrivateKey(null);
    }

    @Override
    public ArrayList<String> getMethods() {
        if (mVerifyMethods == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(mVerifyMethods);
    }

    @Override
    public void sendVerificationCode(String email, String phoneNumber) {
        SendVerificationCodeJob job = new SendVerificationCodeJob(mUserName, email, phoneNumber);
        mJobManager.addJobInBackground(job);
    }

    @Override
    public void createUser(Constants.TokenType tokenType, String token) {
        if (addressChosen == null && TextUtils.isEmpty(nameChosen)) {
            mUserManager.createUser(mUserName, mPassword, mUpdateMe, tokenType, token);
        } else if (addressChosen == null && !TextUtils.isEmpty(nameChosen)) {
            startLoginInfo();
        } else if (addressChosen != null && !TextUtils.isEmpty(nameChosen)) {
            startKeysSetup();
        }
    }

    @Override
    public void saveEncryptionBits(int bits) {
        mBits = bits;
        mUserManager.setPrivateKey(null);
        mUserManager.generateKeyPair(mUserName, mDomain, mPassword, mBits);
    }

    @Override
    public void saveUserData(String username, byte[] password, String domain, String notificationEmail, boolean updateMe) {
        mUserName = username;
        mPassword = password;
        mDomain = domain;
        mNotificationEmail = notificationEmail;
        mUpdateMe = updateMe;
    }

    @Override
    public void startLoginInfo() {
        mUserManager.info(mUserName, mPassword);
    }

    @Override
    public void startLogin(final LoginInfoResponse infoResponse, final int fallbackAuthVersion) {
        mUserManager.login(mUserName, mPassword, infoResponse, fallbackAuthVersion, true);
    }

    @Override
    public void createUserCompleted(int windowSize, boolean success) {
        mUserManager.setLoginState(LOGIN_STATE_TO_INBOX);
        mUserManager.setLoggedIn(true);
        mUserManager.engagementShowNextTime();
        Intent feedbackIntent = AppUtil.decorInAppIntent(new Intent(this, CreateAccountFeedbackActivity.class));
        feedbackIntent.putExtra(CreateAccountFeedbackActivity.EXTRA_WINDOW_SIZE, windowSize);
        feedbackIntent.putExtra(CreateAccountFeedbackActivity.EXTRA_SUCCESS, success);
        feedbackIntent.putExtra(CreateAccountFeedbackActivity.EXTRA_PASSWORD, mPassword);
        feedbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(feedbackIntent);
        finish();
    }

    @Override
    public void generateKeyPairDone() {
        mUserManager.resetGenerateKeyPairEvent();
    }

    @Override
    public void showInbox(String email, byte[] password, String displayName, boolean updateMe) {
        // noop
    }

    @Override
    public void preventBackPress() {
        mBackPressAllowed = false;
    }

    @Override
    public void allowBackPress() {
        mBackPressAllowed = true;
    }

    @Override
    public boolean hasConnectivity() {
        return mNetworkUtil.isConnected();
    }

    @Override
    public void createVerificationPaymentForPaymentToken(int amount, Constants.CurrencyType currency, String paymentToken) {
        VerifyPaymentJob job = new VerifyPaymentJob(amount, currency, paymentToken);
        mJobManager.addJobInBackground(job);
    }

    @Override
    public void onPaymentOptionChosen(Constants.CurrencyType currency, int amount, String planId, int cycle) {
        this.currency = currency;
        this.selectedPlanId = planId;
        this.amount = amount;
        this.selectedCycle = cycle;
    }

    @Override
    public void donateForPaymentToken(int amount, Constants.CurrencyType currency, String paymentToken) {
        // noop
    }

    @Override
    public Constants.CurrencyType getCurrency() {
        return currency;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public String getSelectedPlanId() {
        return selectedPlanId;
    }

    @Override
    public int getSelectedCycle() {
        return selectedCycle;
    }

    @Override
    public void offerFreeAccount(int windowHeight) {
        // noop
    }

    @Override
    public void fetchPlan(List<Constants.CurrencyType> currencies) {
        GetCurrenciesPlansJob job = new GetCurrenciesPlansJob(currencies);
        mJobManager.addJobInBackground(job);
    }

    @Override
    public void fetchOrganization() {
        ProtonMailApplication.getApplication().fetchOrganization();
    }

    @Override
    public void onBillingCompleted() {
        // noop
    }

    @Override
    public void onBillingError(String error, String errorDescription) {
        // noop
    }

    public void showDirectDisabled() {
        if (!isFinishing()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutInflater inflater = this.getLayoutInflater();
            View dialogView = inflater.inflate(R.layout.layout_message_dialog, null);
            TextView subtitle = dialogView.findViewById(R.id.subtitle);
            subtitle.setMovementMethod(LinkMovementMethod.getInstance());
            builder.setView(dialogView);
            builder.setPositiveButton(R.string.okay, (dialog, which) -> finish());
            final AlertDialog alert = builder.create();
            alert.setCanceledOnTouchOutside(false);
            alert.setCancelable(false);
            alert.show();
        }
    }

    public void directEnabled(List<String> methods) {
        mVerifyMethods = methods;
        mDirect = true;
    }

    @Override
    public void onBackPressed() {
        saveLastInteraction();
        if (mBackPressAllowed) {
            super.onBackPressed();
        }
    }

    /**
     * Adds a {@link Fragment} to this activity's layout.
     *
     * @param fragment The fragment to be added.
     */
    @Override
    public void replaceFragment(Fragment fragment, String backstackName) {
        if (fragment instanceof CreatePasswordsFragment) {
            createPasswordsFragment = (CreatePasswordsFragment) fragment;
        }
        if (fragment instanceof HumanVerificationCaptchaFragment) {
            captchaFragment = (HumanVerificationCaptchaFragment) fragment;
        }
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment);
        fragmentTransaction.addToBackStack(backstackName);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void donateDone() {
        // noop
    }

    @Override
    public void getAvailableDomains() {
        GetAvailableDomainsJob availableDomainsJob = new GetAvailableDomainsJob(true);
        mJobManager.addJobInBackground(availableDomainsJob);
    }

    @Override
    public void startAddressSetup() {
        if (addressChosen == null) {
            mUserManager.setupAddress(mDomain);
        } else {
            startKeysSetup();
        }
    }

    public void startKeysSetup() {
        mUserManager.setupKeys(mAddressId, mPassword);
    }

    @Subscribe
    public void onSetupAddressEvent(AddressSetupEvent event) {
        if (event.getStatus() == AuthStatus.SUCCESS) {
            AddressSetupResponse response = event.getResponse();
            mAddressId = response.getAddress().getID();
            User user = mUserManager.getUser();
            user.setAddresses(Collections.singletonList(response.getAddress()));
            user.save();
            startKeysSetup();
        }
    }

    @Subscribe
    public void onKeysSetupEvent(KeysSetupEvent event) {
        if (event.getStatus() == AuthStatus.SUCCESS) {
            if (mUserManager.isFirstLogin()) {
                LoginService.fetchUserDetails();
                mJobManager.start();
                launchInitialDataFetch.invoke(true, true);
                mUserManager.firstLoginDone();
            }
        }
    }

    @Subscribe
    public void onAvailableDomainsEvent(AvailableDomainsEvent event) {
        if (event.getStatus() == Status.SUCCESS) {
            new Handler().postDelayed(() -> mProgressContainer.setVisibility(View.GONE), 250);
            availableDomains = event.getDomains();
            if (!event.isRetryOnError() && mAllCurrencyPlans != null) {
                showAccountTypeSelection();
            } else {
                if (createUsernameFragment != null) {
                    createUsernameFragment.setDomains(availableDomains);
                }
            }
        } else {
            // show error
        }
    }

    private void onConnectivityEvent(boolean hasConnectivity) {
        Timber.v("onConnectivityEvent hasConnectivity:%s", hasConnectivity);
        if (!hasConnectivity) {
            networkSnackBarUtil.getNoConnectionSnackBar(
                    mSnackLayout,
                    mUserManager.getUser(),
                    this,
                    onConnectivityCheckRetry(),
                    null
            ).show();
        } else {
            if (captchaFragment != null && captchaFragment.isAdded()) {
                captchaFragment.connectionArrived();
            }
            networkSnackBarUtil.hideAllSnackBars();
        }
    }

    @NotNull
    private Function0<Unit> onConnectivityCheckRetry() {
        return () -> {
            networkSnackBarUtil.getCheckingConnectionSnackBar(mSnackLayout, null).show();
            viewModel.checkConnectivityDelayed();
            checkDirectEnabled();
            return null;
        };
    }

    private void checkDirectEnabled() {
        GetDirectEnabledJob job = new GetDirectEnabledJob();
        mJobManager.addJobInBackground(job);
        mJobManager.start();
    }

    private void showAccountTypeSelection() {
        int windowHeight = getIntent().getExtras().getInt(EXTRA_WINDOW_SIZE);
        if (addressChosen == null && TextUtils.isEmpty(nameChosen)) {
            selectAccountFragment = SelectAccountTypeFragment.newInstance(windowHeight, mAllCurrencyPlans);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, selectAccountFragment, selectAccountFragment.getFragmentKey())
                    .commitAllowingStateLoss();
        } else if (addressChosen == null && !TextUtils.isEmpty(nameChosen) && TextUtils.isEmpty(mDomain)) {
            createUsernameFragment = CreateAccountFragment.newInstance(windowHeight, availableDomains, nameChosen);
            replaceFragment(createUsernameFragment, createUsernameFragment.getFragmentKey());
        } else if (addressChosen == null && !TextUtils.isEmpty(nameChosen) && !TextUtils.isEmpty(mDomain)) {
            mUserName = nameChosen;
            CreatePasswordsFragment createPasswordsFragment = CreatePasswordsFragment.newInstance(windowHeight, mUserName, mDomain);
            replaceFragment(createPasswordsFragment, createPasswordsFragment.getFragmentKey());
        } else if (addressChosen != null && !TextUtils.isEmpty(nameChosen)) {
            mAddressId = addressChosen.getID();
            mUserName = nameChosen;
            CreatePasswordsFragment createPasswordsFragment = CreatePasswordsFragment.newInstance(windowHeight, mUserName, mDomain);
            replaceFragment(createPasswordsFragment, createPasswordsFragment.getFragmentKey());
        }
    }

    @Subscribe
    public void onGetDirectEnabledEvent(GetDirectEnabledEvent event) {
        switch (event.getStatus()) {
            case SUCCESS:
                if (event.getDirect() == 0) {
                    showDirectDisabled();
                } else {
                    directEnabled(event.getVerifyMethods());
                }
                break;
            case NO_NETWORK:
                checkDirectEnabled();
                onConnectivityEvent(false);
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_USERNAME, mUserName);
        outState.putString(STATE_DOMAIN, mDomain);
        outState.putParcelable(STATE_ADDRESS, addressChosen);
    }
}
