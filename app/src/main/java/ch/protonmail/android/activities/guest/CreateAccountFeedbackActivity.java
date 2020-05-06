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

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.BaseConnectivityActivity;
import ch.protonmail.android.activities.fragments.CreateAccountFeedbackFragment;
import ch.protonmail.android.activities.fragments.CreateAccountFragment;
import ch.protonmail.android.activities.mailbox.MailboxActivity;
import ch.protonmail.android.api.models.LoginInfoResponse;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.jobs.CheckUsernameAvailableJob;
import ch.protonmail.android.jobs.GetCurrenciesPlansJob;
import ch.protonmail.android.jobs.UpdateNotificationEmailAndUpdatesJob;
import ch.protonmail.android.utils.AppUtil;

/**
 * Created by dkadrikj on 1/22/16.
 */
public class CreateAccountFeedbackActivity extends BaseConnectivityActivity implements
        CreateAccountFragment.ICreateAccountListener {

    public static final String EXTRA_WINDOW_SIZE = "window_size";
    public static final String EXTRA_SUCCESS = "success";
    public static final String EXTRA_PASSWORD = "password";

    @BindView(R.id.fragmentContainer)
    View fragmentContainer;

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
            if (extras != null) {
                int windowSize = extras.getInt(EXTRA_WINDOW_SIZE);
                boolean success = extras.getBoolean(EXTRA_SUCCESS);
                byte[] password = extras.getByteArray(EXTRA_PASSWORD);
                CreateAccountFeedbackFragment createAccountFragment = CreateAccountFeedbackFragment
                        .newInstance(windowSize, success, password);
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragmentContainer, createAccountFragment, createAccountFragment.getFragmentKey())
                        .commitAllowingStateLoss();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        ProtonMailApplication.getApplication().getBus().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        ProtonMailApplication.getApplication().getBus().unregister(this);
    }

    @Override
    public void onAccountSelected(Constants.AccountType selectedAccountType) {
        // noop
    }

    @Override
    public Constants.AccountType getSelectedAccountType() {
        return Constants.AccountType.FREE;
    }

    @Override
    public void checkUsername(CheckUsernameAvailableJob job) {
        // noop
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
        // noop
    }

    @Override
    public void createUser(Constants.TokenType tokenType, String token) {
        // noop
    }

    @Override
    public void saveEncryptionBits(int bits) {
        // noop
    }

    @Override
    public void saveUserData(String username, byte[] password, String domain, String notificationEmail, boolean updateMe) {
        // noop
    }

    @Override
    public void startLoginInfo() {
        // noop
    }

    @Override
    public void startLogin(final LoginInfoResponse infoResponse, final int fallbackAuthVersion) {
        // noop
    }

    @Override
    public void createUserCompleted(int windowSize, boolean success) {
        // noop
    }

    @Override
    public void generateKeyPairDone() {
        // noop
    }

    @Override
    public void showInbox(String email, byte[] password, String displayName, boolean updateMe) {
        if (!TextUtils.isEmpty(email) || updateMe || !TextUtils.isEmpty(displayName)) {
            mJobManager.addJobInBackground(new UpdateNotificationEmailAndUpdatesJob(email, password, displayName, updateMe));
        }
        Intent mailboxIntent = AppUtil.decorInAppIntent(new Intent(this, MailboxActivity.class));
        mailboxIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mailboxIntent);
        finish();
    }

    @Override
    public void preventBackPress() {
        // noop
    }

    @Override
    public void allowBackPress() {
        // noop
    }

    @Override
    public boolean hasConnectivity() {
        return mNetworkUtil.isConnected();
    }

    @Override
    public void createVerificationPaymentForPaymentToken(int amount, Constants.CurrencyType currency, String paymentToken) {
        // noop
    }

    @Override
    public void createSubscriptionForPaymentToken(String paymentToken, int amount, Constants.CurrencyType currency, String couponCode, List<String> planIds, int cycle) {
        // noop
    }

    @Override
    public void onPaymentOptionChosen(Constants.CurrencyType currency, int amount, String planId, int cycle) {
        // noop
    }

    @Override
    public void donateForPaymentToken(int amount, Constants.CurrencyType currency, String paymentToken) {
        // noop
    }

    @Override
    public Constants.CurrencyType getCurrency() {
        return null;
    }

    @Override
    public int getAmount() {
        return 0;
    }

    @Override
    public String getSelectedPlanId() {
        return null;
    }

    @Override
    public int getSelectedCycle() {
        return 0;
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
        // noop
    }

    @Override
    public void onBillingCompleted() {
        // noop
    }

    @Override
    public void onBillingError(String error, String errorDescription) {
        // noop
    }

    @Override
    public void replaceFragment(Fragment fragment, String backstackName) {
        // noop
    }

    @Override
    public void donateDone() {
        // noop
    }

    @Override
    public void getAvailableDomains() {
        // noop
    }

    @Override
    public void startAddressSetup() {

    }
}
