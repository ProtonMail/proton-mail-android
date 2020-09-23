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

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;

import com.squareup.otto.Subscribe;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.guest.LoginActivity;
import ch.protonmail.android.api.models.AllCurrencyPlans;
import ch.protonmail.android.api.models.AvailablePlansResponse;
import ch.protonmail.android.api.models.Organization;
import ch.protonmail.android.api.models.Plan;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.AvailablePlansEvent;
import ch.protonmail.android.events.LogoutEvent;
import ch.protonmail.android.events.PaymentMethodEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.payment.CheckSubscriptionEvent;
import ch.protonmail.android.events.payment.PaymentsStatusEvent;
import ch.protonmail.android.jobs.GetCurrenciesPlansJob;
import ch.protonmail.android.jobs.payments.CheckSubscriptionJob;
import ch.protonmail.android.jobs.payments.CreateSubscriptionJob;
import ch.protonmail.android.jobs.payments.GetPaymentMethodsJob;
import ch.protonmail.android.jobs.payments.GetPaymentsStatusJob;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.extensions.TextExtensions;

/**
 * Created by dkadrikj on 6/28/16.
 */
public class UpsellingActivity extends BaseActivity {

    public static final String EXTRA_OPEN_UPGRADE_CONTAINER = "EXTRA_OPEN_UPGRADE";
    private static final int REQUEST_CODE_DONATE = 1;
    private static final int REQUEST_CODE_UPGRADE = 2;

    @BindView(R.id.upgrade_container)
    View upgradeContainer;
    @BindView(R.id.donate_container)
    View donateContainer;
    @BindView(R.id.main_container)
    View mainContainer;
    @BindView(R.id.expand_upgrade)
    View upgradeExpand;
    @BindView(R.id.contract_upgrade)
    View upgradeContract;
    @BindView(R.id.expand_donate)
    View donateExpand;
    @BindView(R.id.contract_donate)
    View donateContract;
    @BindView(R.id.upgrade_header_title)
    TextView upgradeHeaderTitle;
    @BindView(R.id.amount_5)
    Button btnAmount5;
    @BindView(R.id.amount_15)
    Button btnAmount15;
    @BindView(R.id.amount_50)
    Button btnAmount50;
    @BindView(R.id.amount_100)
    Button btnAmount100;
    @BindView(R.id.upgrade_header)
    View upgradeHeader;
    @BindView(R.id.custom_amount)
    EditText donateCustomAmountEditText;
    @BindView(R.id.upselling_progress_container)
    View upsellingProgress;
    @BindView(R.id.billing_info)
    TextView mBillingInfo;
    @BindView(R.id.payment_options)
    TextView mPaymentOptions;
    @BindView(R.id.progress)
    View mProgress;
    private State upgradeContainerState;
    private State donateContainerState;

    private int donationAmount;
    private int donationAmountPosition;
    View.OnTouchListener pressListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            resetAmountButtonsState();
            v.setPressed(!v.isPressed());
            donationAmount = getPresetChosenAmount();
            donateCustomAmountEditText.setText(String.valueOf(donationAmount));
            return true;
        }
    };
    private double selectedAnnualMonthlyPrice;
    private double selectedMonthlyPrice;
    private int fullAnnualMonthlyPriceCents;
    private int fullMonthlyPriceCents;
    private boolean currencyAndDurationSelected;
    private AllCurrencyPlans allCurrencyPlans;
    private boolean paidUser;
    private View mProgressView;
    private TextView mAnnualDurationPrice;
    private TextView mMonthlyDurationPrice;
    private Constants.CurrencyType mCurrency;
    private int mCycle;
    private String mSelectedPlanId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        boolean openUpgrade = getIntent().getBooleanExtra(EXTRA_OPEN_UPGRADE_CONTAINER, false);
        checkPaymentStatus();
        fetchPlan(Constants.CurrencyType.EUR);
        donateContainerState = State.CLOSED;
        btnAmount5.setOnTouchListener(pressListener);
        btnAmount15.setOnTouchListener(pressListener);
        btnAmount50.setOnTouchListener(pressListener);
        btnAmount100.setOnTouchListener(pressListener);
        btnAmount15.setPressed(true);
        donationAmountPosition = 1;
        donationAmount = getPresetChosenAmount();
        User user = mUserManager.getUser();
        Organization organization = ProtonMailApplication.getApplication().getOrganization();
        if (user != null && organization != null) {
            paidUser = user.isPaidUser() && !TextUtils.isEmpty(organization.getPlanName());
        }
        if (paidUser) {
            upgradeHeaderTitle.setText(getString(R.string.upgrade_paid_user));
            upgradeExpand.setVisibility(View.GONE);
            upgradeContract.setVisibility(View.GONE);
        } else {
            if (!openUpgrade) {
                upgradeContainerState = State.CLOSED;
            } else {
                upgradeContainerState = State.OPENED;
                openUpgradeContainer();
            }
            upgradeHeader.setOnClickListener(v -> onUpgradeHeaderClick());
        }

        GetPaymentMethodsJob paymentMethodsJob = new GetPaymentMethodsJob();
        mJobManager.addJobInBackground(paymentMethodsJob);
    }

    private void checkPaymentStatus() {
        GetPaymentsStatusJob job = new GetPaymentsStatusJob();
        mJobManager.addJobInBackground(job);
    }

    private void fetchPlan(Constants.CurrencyType currency) {
        boolean fetched = false;
        if (allCurrencyPlans != null) {
            for (AvailablePlansResponse plans : allCurrencyPlans.getPlans()) {
                if (plans.getCurrency().equals(currency.name())) {
                    fetched = true;
                    break;
                }
            }
        }
        if (!fetched) {
            if (mProgressView != null) {
                mProgressView.setVisibility(View.VISIBLE);
            }
            List<Constants.CurrencyType> currencies = new ArrayList<>();
            currencies.add(currency);
            GetCurrenciesPlansJob job = new GetCurrenciesPlansJob(currencies);
            mJobManager.addJobInBackground(job);
        } else {
            calculatePrices(currency);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (upsellingProgress != null) {
            upsellingProgress.setVisibility(View.GONE);
        }
        resetAmountButtonsState();
        setPressedByPosition();
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

    private void setPressedByPosition() {
        if (donationAmountPosition == 0) {
            btnAmount5.setPressed(true);
        } else if (donationAmountPosition == 1) {
            btnAmount15.setPressed(true);
        } else if (donationAmountPosition == 2) {
            btnAmount50.setPressed(true);
        } else if (donationAmountPosition == 3) {
            btnAmount100.setPressed(true);
        }
    }

    private void resetAmountButtonsState() {
        btnAmount5.setPressed(false);
        btnAmount15.setPressed(false);
        btnAmount50.setPressed(false);
        btnAmount100.setPressed(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_upselling;
    }

    public void onUpgradeHeaderClick() {
        if (upgradeContainerState == State.CLOSED) {
            upgradeContainerState = State.OPENED;
            donateContainerState = State.CLOSED;
            openUpgradeContainer();
        } else {
            upgradeContainerState = State.CLOSED;
            donateContainerState = State.CLOSED;
            closeUpgradeContainer();
        }
    }

    @OnClick(R.id.donate_paypal)
    public void onDonatePaypalClicked() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.donate_paypal)));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            TextExtensions.showToast(this, R.string.no_browser_found, Toast.LENGTH_SHORT);
        }
    }

    @OnClick(R.id.donate_header)
    public void onDonateHeaderClick() {
        if (donateContainerState == State.CLOSED) {
            upgradeContainerState = State.CLOSED;
            donateContainerState = State.OPENED;
            openDonateContainer();
        } else {
            upgradeContainerState = State.CLOSED;
            donateContainerState = State.CLOSED;
            closeDonateContainer();
        }
    }

    @OnClick(R.id.upgrade)
    public void onUpgradeClicked() {
        if (currencyAndDurationSelected) {
            fireCheckSubscription();
        } else {
            showCurrencyOptions(true);
        }
    }

    @OnClick(R.id.donate)
    public void onDonateClicked() {
        int amount;
        String customAmount = donateCustomAmountEditText.getText().toString();
        if (!TextUtils.isEmpty(customAmount)) {
            amount = Integer.valueOf(customAmount);
        } else {
            amount = donationAmount;
        }
        if (amount <= 0) {
            return;
        }
        Intent billingIntent = new Intent(this, BillingActivity.class);
        billingIntent.putExtra(BillingActivity.EXTRA_AMOUNT, amount * 100); // amount in cents
        billingIntent.putExtra(BillingActivity.EXTRA_BILLING_TYPE, Constants.BillingType.DONATE);
        startActivityForResult(AppUtil.decorInAppIntent(billingIntent), REQUEST_CODE_DONATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            if (requestCode == REQUEST_CODE_DONATE || requestCode == REQUEST_CODE_UPGRADE) {
                boolean success = extras.getBoolean(BillingActivity.EXTRA_SUCCESS);
                if (success) {
                    Intent intent = new Intent();
                    intent.putExtra(BillingActivity.EXTRA_SUCCESS, true);
                    setResult(RESULT_OK, intent);
                    saveLastInteraction();
                    finish();
                } else {
                    showUpgradeError(extras.getString(BillingActivity.EXTRA_ERROR), extras.getString(BillingActivity.EXTRA_ERROR_DESCRIPTION));
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void showUpgradeError(String error, String errorDescription) {
        if (isFinishing()) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView;
        dialogView = inflater.inflate(R.layout.layout_payment_error, null);
        builder.setView(dialogView);
        final TextView btnOk = dialogView.findViewById(R.id.button_ok);
        final TextView errorTitleText = dialogView.findViewById(R.id.error_title);
        final TextView errorDescriptionText = dialogView.findViewById(R.id.error_description);

        errorTitleText.setText(error);
        errorDescriptionText.setText(errorDescription);

        final AlertDialog alert = builder.create();

        btnOk.setOnClickListener(v -> alert.cancel());

        alert.show();
    }

    private int getPresetChosenAmount() {
        if (btnAmount5.isPressed()) {
            donationAmountPosition = 0;
            return 5;
        }
        if (btnAmount15.isPressed()) {
            donationAmountPosition = 1;
            return 15;
        }
        if (btnAmount50.isPressed()) {
            donationAmountPosition = 2;
            return 50;
        }
        if (btnAmount100.isPressed()) {
            donationAmountPosition = 3;
            return 100;
        }
        return 0;
    }

    private void openUpgradeContainer() {
        upgradeContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 6f));
        donateContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0f));
        mainContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0f));
        upgradeExpand.setVisibility(View.GONE);
        upgradeContract.setVisibility(View.VISIBLE);
        donateExpand.setVisibility(View.VISIBLE);
        donateContract.setVisibility(View.GONE);
    }

    private void openDonateContainer() {
        donateContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 6f));
        upgradeContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0f));
        mainContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0f));
        donateExpand.setVisibility(View.GONE);
        donateContract.setVisibility(View.VISIBLE);
        if (!paidUser) {
            upgradeExpand.setVisibility(View.VISIBLE);
            upgradeContract.setVisibility(View.GONE);
        }
    }

    private void closeUpgradeContainer() {
        upgradeContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0f));
        donateContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0f));
        mainContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 6f));
        upgradeExpand.setVisibility(View.VISIBLE);
        upgradeContract.setVisibility(View.GONE);
    }

    private void closeDonateContainer() {
        donateContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0f));
        upgradeContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 0f));
        mainContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 6f));
        donateExpand.setVisibility(View.VISIBLE);
        donateContract.setVisibility(View.GONE);
    }

    @Subscribe
    public void onPaymentsStatusEvent(PaymentsStatusEvent event) {
        if (event.getStatus() == Status.FAILED) {
            // TODO: show failed
        } else {
            if (!event.isStripeActive()) {
                // TODO: show stripe not active
            }
        }
    }

    @Subscribe
    public void onCheckSubscriptionEvent(CheckSubscriptionEvent event) {
        if (event.getStatus() == Status.SUCCESS) {
            if (event.getResponse().getAmountDue() == 0) {
                // prevent user from making illegal payment of value 0
                CreateSubscriptionJob job = new CreateSubscriptionJob(0, Constants.CurrencyType.valueOf(event.getResponse().getCurrency()), null, Collections.singletonList(mSelectedPlanId), mCycle, null);
                mJobManager.addJobInBackground(job);
            } else {
                Intent billingIntent = new Intent(this, BillingActivity.class);
                billingIntent.putExtra(BillingActivity.EXTRA_WINDOW_SIZE, getWindow().getDecorView().getHeight());
                billingIntent.putExtra(BillingActivity.EXTRA_AMOUNT, event.getResponse().getAmountDue());
                billingIntent.putExtra(BillingActivity.EXTRA_CURRENCY, event.getResponse().getCurrency());
                billingIntent.putExtra(BillingActivity.EXTRA_BILLING_TYPE, Constants.BillingType.UPGRADE);
                billingIntent.putExtra(BillingActivity.EXTRA_SELECTED_PLAN_ID, mSelectedPlanId);
                billingIntent.putExtra(BillingActivity.EXTRA_SELECTED_CYCLE, mCycle);
                startActivityForResult(AppUtil.decorInAppIntent(billingIntent), REQUEST_CODE_UPGRADE);
            }
        } else {
            TextExtensions.showToast(this, event.getResponse().getError());
            if (upsellingProgress != null) {
                upsellingProgress.setVisibility(View.GONE);
            }
        }
    }

    @Subscribe
    public void onPaymentMethodEvent(PaymentMethodEvent event) {

        if (mProgressView != null) {
            mProgressView.setVisibility(View.GONE);
        }

        switch (event.getStatus()) {
            case SUCCESS: {
                TextExtensions.showToast(this, R.string.upgrade_paid_user);
                finish();
                break;
            }
            default:
                TextExtensions.showToast(this, event.getError());
        }
    }

    @Subscribe
    public void onPlansEvent(AvailablePlansEvent event) {
        if (mProgressView != null) {
            mProgressView.setVisibility(View.GONE);
        }
        if (allCurrencyPlans != null) {
            allCurrencyPlans.addPlans(event.getAllPlansList());
        } else {
            allCurrencyPlans = event.getAllPlans();
        }
        calculatePrices(event.getAllPlans().getCurrency());
    }

    private void calculatePrices(Constants.CurrencyType currency) {
        fullAnnualMonthlyPriceCents = allCurrencyPlans.getPlan(true, currency).getPlanByPlanType(Constants.PlanType.PLUS).getAmount();
        fullMonthlyPriceCents = allCurrencyPlans.getPlan(false, currency).getPlanByPlanType(Constants.PlanType.PLUS).getAmount();
        selectedAnnualMonthlyPrice = fullAnnualMonthlyPriceCents / 12d / 100;
        selectedMonthlyPrice = fullMonthlyPriceCents / 100;
        NumberFormat format = null;
        if (currency == Constants.CurrencyType.USD) {
            format = NumberFormat.getCurrencyInstance(Locale.US);
        } else {
            format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        }
        format.setCurrency(Currency.getInstance(currency.name()));
        String amountFormatAnnual = format.format(selectedAnnualMonthlyPrice);
        String amountFormatMonthly = format.format(selectedMonthlyPrice);

        if (mAnnualDurationPrice != null) {
            mAnnualDurationPrice.setText(amountFormatAnnual);
        }
        if (mMonthlyDurationPrice != null) {
            mMonthlyDurationPrice.setText(amountFormatMonthly);
        }
        mBillingInfo.setText(String.format(getString(R.string.duration_annual_line3), format.format(fullAnnualMonthlyPriceCents / 100)));
        mPaymentOptions.setText(getString(R.string.switch_billing_monthly));
        mProgress.setVisibility(View.GONE);
    }

    @OnClick(R.id.payment_options)
    public void onPaymentOptionsClicked() {
        showCurrencyOptions(false);
    }

    private void showCurrencyOptions(final boolean proceed) {
        // default to show the € sign after the amount
        final NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        final Constants.PlanType selectedPlan = Constants.PlanType.PLUS;
        if (isFinishing()) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView;
        dialogView = inflater.inflate(R.layout.layout_payment_options, null);
        builder.setView(dialogView);
        mProgressView = dialogView.findViewById(R.id.progress_container);
        final View viewProgress = mProgressView;
        final TextView btnCancel = dialogView.findViewById(R.id.cancel);
        final Button btnSetContinue = dialogView.findViewById(R.id.set_continue);
        final RadioGroup rbgCurrency = dialogView.findViewById(R.id.currency_rbg);
        final RadioGroup rbgDuration = dialogView.findViewById(R.id.duration_rbg);
        mAnnualDurationPrice = dialogView.findViewById(R.id.duration_annual_title);
        mMonthlyDurationPrice = dialogView.findViewById(R.id.duration_monthly_title);
        final TextView annualDurationPrice = mAnnualDurationPrice;
        final TextView monthlyDurationPrice = mMonthlyDurationPrice;
        if (allCurrencyPlans != null) {
            fullAnnualMonthlyPriceCents = allCurrencyPlans.getPlan(true, Constants.CurrencyType.EUR).getPlanByPlanType(selectedPlan).getAmount();
            fullMonthlyPriceCents = allCurrencyPlans.getPlan(false, Constants.CurrencyType.EUR).getPlanByPlanType(selectedPlan).getAmount();
            selectedAnnualMonthlyPrice = fullAnnualMonthlyPriceCents / 12d / 100;
            selectedMonthlyPrice = fullMonthlyPriceCents / 100;
        } else {
            viewProgress.setVisibility(View.VISIBLE);
        }
        format.setCurrency(Currency.getInstance(getString(R.string.currency_euro_sign)));
        String amountFormatAnnual = format.format(selectedAnnualMonthlyPrice);
        String amountFormatMonthly = format.format(selectedMonthlyPrice);

        annualDurationPrice.setText(amountFormatAnnual);
        monthlyDurationPrice.setText(amountFormatMonthly);

        rbgCurrency.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                NumberFormat format = null;
                if (checkedId == R.id.currency_euro) {
                    format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
                    fullAnnualMonthlyPriceCents = allCurrencyPlans.getPlan(true, Constants.CurrencyType.EUR).getPlanByPlanType(selectedPlan).getAmount();
                    fullMonthlyPriceCents = allCurrencyPlans.getPlan(false, Constants.CurrencyType.EUR).getPlanByPlanType(selectedPlan).getAmount();
                    selectedAnnualMonthlyPrice = fullAnnualMonthlyPriceCents / 12d / 100;
                    selectedMonthlyPrice = fullMonthlyPriceCents / 100;
                    format.setCurrency(Currency.getInstance(getString(R.string.currency_euro_sign)));
                } else if (checkedId == R.id.currency_frank) {
                    format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
                    format.setCurrency(Currency.getInstance(getString(R.string.currency_frank_sign)));
                    fetchPlan(Constants.CurrencyType.CHF);
                } else if (checkedId == R.id.currency_dollar) {
                    format = NumberFormat.getCurrencyInstance(Locale.US);
                    format.setCurrency(Currency.getInstance(getString(R.string.currency_dollar_sign)));
                    fetchPlan(Constants.CurrencyType.USD);
                }
                String amountFormatAnnual = format.format(selectedAnnualMonthlyPrice);
                String amountFormatMonthly = format.format(selectedMonthlyPrice);
                annualDurationPrice.setText(amountFormatAnnual);
                monthlyDurationPrice.setText(amountFormatMonthly);
            }
        });

        final AlertDialog alert = builder.create();
        alert.show();
        btnCancel.setOnClickListener(v -> alert.cancel());
        btnSetContinue.setOnClickListener(v -> {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.GERMANY);
            int selectedCurrency = rbgCurrency.getCheckedRadioButtonId();
            int selectedDuration = rbgDuration.getCheckedRadioButtonId();
            AvailablePlansResponse selectedPlans = null;

            mCurrency = null;
            if (selectedCurrency == R.id.currency_euro) {
                mCurrency = Constants.CurrencyType.EUR;
            } else if (selectedCurrency == R.id.currency_frank) {
                mCurrency = Constants.CurrencyType.CHF;
            } else if (selectedCurrency == R.id.currency_dollar) {
                currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
                mCurrency = Constants.CurrencyType.USD;
            }
            currencyFormat.setCurrency(Currency.getInstance(mCurrency.name()));

            Plan plan = null;
            if (selectedDuration == R.id.duration_annual) {
                mCycle = 12;
                selectedPlans = allCurrencyPlans.getPlan(true, mCurrency);
                plan = selectedPlans.getPlanByPlanType(selectedPlan);
                mBillingInfo.setText(String.format(getString(R.string.duration_annual_line3), currencyFormat.format(plan.getAmount() / 100)));
                mPaymentOptions.setText(getString(R.string.switch_billing_monthly));
            } else if (selectedDuration == R.id.duration_monthly) {
                mCycle = 1;
                selectedPlans = allCurrencyPlans.getPlan(false, mCurrency);
                plan = selectedPlans.getPlanByPlanType(selectedPlan);
                mBillingInfo.setText(String.format(getString(R.string.duration_annual_line4), currencyFormat.format(plan.getAmount() / 100)));
                mPaymentOptions.setText(getString(R.string.switch_billing_yearly));
            }
            mSelectedPlanId = plan.getId();
            currencyAndDurationSelected = true;
            if (proceed) {
                fireCheckSubscription();
            }
            alert.cancel();
        });
    }

    private void fireCheckSubscription() {
        if (upsellingProgress != null) {
            upsellingProgress.setVisibility(View.VISIBLE);
        }
        List<String> planIds = new ArrayList<>();
        planIds.add(mSelectedPlanId);
        CheckSubscriptionJob job = new CheckSubscriptionJob(null, planIds, mCurrency, mCycle);
        mJobManager.addJobInBackground(job);
    }

    private enum State {
        CLOSED, OPENED
    }

    @Subscribe
    public void onLogoutEvent(LogoutEvent event) {
        startActivity(AppUtil.decorInAppIntent(new Intent(this, LoginActivity.class)));
        finish();
    }
}
