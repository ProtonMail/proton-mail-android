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
package ch.protonmail.android.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.fragments.BillingFragment;
import ch.protonmail.android.activities.fragments.CreateAccountBaseFragment;
import ch.protonmail.android.activities.fragments.HumanVerificationCaptchaDialogFragment;
import ch.protonmail.android.activities.fragments.HumanVerificationDialogFragment;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.models.LoginInfoResponse;
import ch.protonmail.android.api.models.PaymentMethod;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.jobs.CheckUsernameAvailableJob;
import ch.protonmail.android.jobs.DonateJob;
import ch.protonmail.android.jobs.GetCurrenciesPlansJob;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.UiUtil;

/**
 * Created by dkadrikj on 6/30/16.
 */
public class BillingActivity extends BaseActivity implements
        CreateAccountBaseFragment.ICreateAccountListener,
        BillingFragment.IBillingListener,
        HumanVerificationDialogFragment.IHumanVerificationListener {

    public static final String EXTRA_WINDOW_SIZE = "window_size";
    public static final String EXTRA_AMOUNT = "billing_extra_amount";
    public static final String EXTRA_CURRENCY = "billing_extra_currency";
    public static final String EXTRA_BILLING_TYPE = "billing_extra_type";
    public static final String EXTRA_PAYMENT_METHODS = "payment_methods";
    public static final String EXTRA_SELECTED_PLAN_ID = "selected_plan_id";
    public static final String EXTRA_SELECTED_CYCLE = "selected_cycle";

    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_ERROR = "error";
    public static final String EXTRA_ERROR_DESCRIPTION = "error_description";

    @BindView(R.id.fragmentContainer)
    View fragmentContainer;

    private BillingFragment billingFragment;
    private HumanVerificationCaptchaDialogFragment humanVerificationCaptchaDialogFragment;
    private Constants.CurrencyType currency;
    private int amount;
    private String selectedPlanId;
    private Constants.BillingType billingType;
    private int mSelectedCycle;
    private List<PaymentMethod> paymentMethods;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_fragment_container;
    }

    @Override
    protected boolean isPreventingScreenshots() {
        return true;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (fragmentContainer != null) {
            if (savedInstanceState != null) {
                return;
            }
            Bundle extras = getIntent().getExtras();
            amount = extras.getInt(EXTRA_AMOUNT);
            currency = Constants.CurrencyType.valueOf((extras.getString(EXTRA_CURRENCY, Constants.CurrencyType.EUR.name())));
            int windowSize = extras.getInt(EXTRA_WINDOW_SIZE);
            billingType = (Constants.BillingType) extras.getSerializable(EXTRA_BILLING_TYPE);
            selectedPlanId = extras.getString(EXTRA_SELECTED_PLAN_ID);
            mSelectedCycle = extras.getInt(EXTRA_SELECTED_CYCLE);
            paymentMethods = (List<PaymentMethod>) extras.getSerializable(EXTRA_PAYMENT_METHODS);
            billingFragment = BillingFragment.newInstance(billingType, windowSize, currency, amount, selectedPlanId, mSelectedCycle);
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentContainer, billingFragment, billingFragment.getFragmentKey())
                    .commitAllowingStateLoss();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        if (fragment instanceof BillingFragment) {
            BillingFragment billingFragment = (BillingFragment) fragment;
            billingFragment.setActivityListener(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        ProtonMailApplication.getApplication().getBus().unregister(this);
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

    }

    @Override
    public Constants.AccountType getSelectedAccountType() {
        return Constants.AccountType.FREE;
    }

    @Override
    public void checkUsername(CheckUsernameAvailableJob job) {

    }

    @Override
    public void onUsernameSelected(String username) {
        // noop
    }

    @Override
    public ArrayList<String> getMethods() {
        return null;
    }

    @Override
    public void sendVerificationCode(String email, String phoneNumber) {

    }

    @Override
    public void createUser(Constants.TokenType tokenType, String token) {

    }

    @Override
    public void saveEncryptionBits(int bits) {

    }

    @Override
    public void saveUserData(String username, byte[] password, String domain, String notificationEmail, boolean updateMe) {

    }

    @Override
    public void startLoginInfo() {

    }

    @Override
    public void startLogin(final LoginInfoResponse infoResponse, final int fallbackAuthVersion) {

    }

    @Override
    public void createUserCompleted(int windowSize, boolean success) {

    }

    @Override
    public void generateKeyPairDone() {

    }

    @Override
    public void showInbox(String email, byte[] password, String displayName, boolean updateMe) {

    }

    @Override
    public void preventBackPress() {

    }

    @Override
    public void allowBackPress() {

    }

    @Override
    public boolean hasConnectivity() {
        return mNetworkUtil.isConnected();
    }

    @Override
    public void createVerificationPaymentForPaymentToken(int amount, Constants.CurrencyType currency, String paymentToken) {

    }

    @Override
    public void onPaymentOptionChosen(Constants.CurrencyType currency, int amount, String planId, int cycle) {
        // TODO: payment
    }

    @Override
    public void donateForPaymentToken(int amount, Constants.CurrencyType currency, String paymentToken) {
        DonateJob job = new DonateJob(paymentToken, amount, currency);
        mJobManager.addJobInBackground(job);
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
        return mSelectedCycle;
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
        fetchOrganizationData();
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SUCCESS, true);
        setResult(RESULT_OK, intent);
        saveLastInteraction();
        finish();
    }

    @Override
    public void onBillingError(String error, String errorDescription) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SUCCESS, false);
        intent.putExtra(EXTRA_ERROR, error);
        intent.putExtra(EXTRA_ERROR_DESCRIPTION, errorDescription);
        setResult(RESULT_OK, intent);
        saveLastInteraction();
        finish();
    }

    @Override
    public void replaceFragment(Fragment fragment, String backstackName) {
        // noop
    }

    @Override
    public void donateDone() {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_SUCCESS, true);
        setResult(RESULT_OK, intent);
        saveLastInteraction();
        finish();
    }

    @Override
    public void getAvailableDomains() {
        // noop
    }

    @Override
    public void startAddressSetup() {

    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
    }
}
