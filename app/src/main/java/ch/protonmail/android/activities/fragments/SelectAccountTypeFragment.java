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
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.squareup.otto.Subscribe;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.AllCurrencyPlans;
import ch.protonmail.android.api.models.AvailablePlansResponse;
import ch.protonmail.android.api.models.Plan;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.AvailablePlansEvent;

/**
 * Created by dkadrikj on 7/1/16.
 */
public class SelectAccountTypeFragment extends CreateAccountBaseFragment implements AdapterView.OnItemSelectedListener {

    public static final String ARGUMENT_ALL_AVAILABLE_PLANS = "all_available_plans";

    @BindView(R.id.plan_plus_content)
    View planPlusContent;
    @BindView(R.id.contract_plus)
    View plusContract;
    @BindView(R.id.expand_plus)
    View plusExpand;
    @BindView(R.id.plan_plus_price)
    TextView plusPlanPriceHeaderTitleView;
    @BindView(R.id.plan_free_content)
    View planFreeContent;
    @BindView(R.id.contract_free)
    View freeContract;
    @BindView(R.id.expand_free)
    View freeExpand;
    @BindView(R.id.plan_visio_content)
    View planVisioContent;
    @BindView(R.id.contract_visio)
    View visioContract;
    @BindView(R.id.expand_visio)
    View visioExpand;
    @BindView(R.id.plan_visio_price)
    TextView visioPlanPriceHeaderTitleView;
    @BindView(R.id.plan_business_content)
    View planBusinessContent;
    @BindView(R.id.contract_business)
    View businessContract;
    @BindView(R.id.expand_business)
    View businessExpand;
//    @BindView(R.id.plan_business_price)
//    TextView businessPlanPrice;
    @BindView(R.id.scroll_view)
    ScrollView scrollView;

    // plus plan
    @BindView(R.id.plan_plus_storage)
    TextView planPlusStorage;
    @BindView(R.id.plan_visio_storage)
    TextView planVisioStorage;
    @BindView(R.id.plan_plus_domains)
    TextView planPlusDomains;
    @BindView(R.id.plan_visio_domains)
    TextView planVisioDomains;
    @BindView(R.id.plan_plus_addresses)
    TextView planPlusAddresses;
    @BindView(R.id.plan_visio_addresses)
    TextView planVisioAddresses;
    @BindView(R.id.currency_spinner)
    Spinner currencySpinner;
    @BindView(R.id.plus_billing_info)
    TextView planPlusBillingInfoTextView;
    @BindView(R.id.visio_billing_info)
    TextView planVisioBillingInfoTextView;
    @BindView(R.id.plus_currency_duration)
    TextView planPlusChangeDuration;
    @BindView(R.id.visio_currency_duration)
    TextView planVisioChangeDuration;
    @BindView(R.id.progress_container)
    View mProgressView;
    private TextView mAnnualDurationPrice;
    private TextView mMonthlyDurationPrice;

    private State planPlusState;
    private State planFreeState;
    private State planVisioState;
    private State planBusinessState;
    private double plusPlanBaseAnnualMonthlyPrice;
    private double visioPlanBaseAnnualMonthlyPrice;

    private double plusPlanAnnualPrice;
    private double visioPlanAnnualPrice;

    private List<String> currencies;
    private AllCurrencyPlans allCurrencyPlans;
    private Plan freePlan;
    private Plan plusPlan;
    private Plan visioPlan;
    private NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
    private Constants.CurrencyType selectedCurrency;
    private int selectedDurationId;
    private boolean selectedDurationYearly;

    public static SelectAccountTypeFragment newInstance(int windowHeight, AllCurrencyPlans allCurrencyPlans) {
        SelectAccountTypeFragment fragment = new SelectAccountTypeFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_WINDOW_HEIGHT, windowHeight);
        extras.putSerializable(ARGUMENT_ALL_AVAILABLE_PLANS, allCurrencyPlans);
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        windowHeight = getArguments().getInt(ARGUMENT_WINDOW_HEIGHT);
        allCurrencyPlans = (AllCurrencyPlans) getArguments().getSerializable(ARGUMENT_ALL_AVAILABLE_PLANS);
        selectedCurrency = Constants.CurrencyType.EUR;
        selectedDurationId = 0;
        selectedDurationYearly = true;

        AvailablePlansResponse defaultYearlyPlan = allCurrencyPlans.getPlan(true, selectedCurrency);
        freePlan = defaultYearlyPlan.getPlanByPlanType(Constants.PlanType.FREE);
        plusPlan = defaultYearlyPlan.getPlanByPlanType(Constants.PlanType.PLUS);
        visioPlan = defaultYearlyPlan.getPlanByPlanType(Constants.PlanType.VISIONARY);
        plusPlanAnnualPrice = plusPlan.getAmount() / 100;
        plusPlanBaseAnnualMonthlyPrice = plusPlanAnnualPrice / 12;
        visioPlanAnnualPrice = visioPlan.getAmount() / 100;
        visioPlanBaseAnnualMonthlyPrice = visioPlanAnnualPrice / 12;

        currencies = new ArrayList<>();
        currencies.add(Constants.CurrencyType.EUR.name());
        currencies.add(Constants.CurrencyType.CHF.name());
        currencies.add(Constants.CurrencyType.USD.name());
    }

    @Override
    protected void onFocusCleared() {
        // noop
    }

    @Override
    protected void setFocuses() {
        // noop
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

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);
        planPlusState = State.CLOSED;
        planFreeState = State.CLOSED;
        planVisioState = State.CLOSED;

        format.setCurrency(Currency.getInstance(getString(R.string.currency_euro_sign)));
        String amountFormatAnnual = format.format(plusPlanBaseAnnualMonthlyPrice);
        String amountFormatMonthly = format.format(visioPlanBaseAnnualMonthlyPrice);

        String fullYearlyAmountPlus = format.format(plusPlanAnnualPrice);
        String fullYearlyAmountVisio = format.format(visioPlanAnnualPrice);

        plusPlanPriceHeaderTitleView.setText(String.format(getResources().getString(R.string.plan_plus_with_price), amountFormatAnnual));
        plusPlanPriceHeaderTitleView.setTag(plusPlanBaseAnnualMonthlyPrice);
        visioPlanPriceHeaderTitleView.setText(String.format(getResources().getString(R.string.plan_visionary_with_price), amountFormatMonthly));
        visioPlanPriceHeaderTitleView.setTag(visioPlanBaseAnnualMonthlyPrice);
//        businessPlanPrice.setText(String.format(getResources().getString(R.string.plan_business_with_price), businessPlanBaseAnnualMonthlyPrice));

        planPlusBillingInfoTextView.setText(String.format(getResources().getString(R.string.duration_annual_line3), fullYearlyAmountPlus));
        planVisioBillingInfoTextView.setText(String.format(getResources().getString(R.string.duration_annual_line3), fullYearlyAmountVisio));

        onPlusPlanClicked();

        double gbStorage = calcStorageInGB(plusPlan.getMaxSpace());
        double mbStorage = 0;
        if (gbStorage < 1) {
            mbStorage = gbStorage * 1000;
        }
        planPlusStorage.setText(String.format(getString(mbStorage > 0 ? R.string.storage_mb : R.string.storage_gb), (int)((mbStorage > 0 ? mbStorage : gbStorage))));
        mbStorage = 0;
        gbStorage = calcStorageInGB(visioPlan.getMaxSpace());
        if (gbStorage < 1) {
            mbStorage = gbStorage * 1000;
        }
        planVisioStorage.setText(String.format(getString(mbStorage > 0 ? R.string.storage_mb : R.string.storage_gb), (int) ((mbStorage > 0 ? mbStorage : gbStorage))));

        int plusMaxDomains = plusPlan.getMaxDomains();
        int visioMaxDomains = visioPlan.getMaxDomains();
        planPlusDomains.setText(getResources().getQuantityString(R.plurals.custom_domains, plusMaxDomains, plusMaxDomains));
        planVisioDomains.setText(getResources().getQuantityString(R.plurals.custom_domains, visioMaxDomains, visioMaxDomains));

        int plusMaxAddresses = plusPlan.getMaxAddresses();
        int visioMaxAddresses = visioPlan.getMaxAddresses();
        planPlusAddresses.setText(String.format(getString(R.string.max_addresses), plusMaxAddresses));
        planVisioAddresses.setText(String.format(getString(R.string.max_addresses), visioMaxAddresses));

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(),R.layout.currency_spinner_item, currencies);
        adapter.setDropDownViewResource(R.layout.currency_spinner_drop_down_item);
        currencySpinner.setAdapter(adapter);
        currencySpinner.setOnItemSelectedListener(this);

        return rootView;
    }

    private double calcStorageInGB(long space) {
        return (space / Math.pow(2, 30));
    }

    @OnClick(R.id.plan_free_header)
    public void onFreePlanClicked() {
        if (planFreeState == State.CLOSED) {
            planFreeState = State.OPENED;
            planFreeContent.setVisibility(View.VISIBLE);
            freeExpand.setVisibility(View.GONE);
            freeContract.setVisibility(View.VISIBLE);
        } else {
            planFreeState = State.CLOSED;
            planFreeContent.setVisibility(View.GONE);
            freeExpand.setVisibility(View.VISIBLE);
            freeContract.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.plan_plus_header)
    public void onPlusPlanClicked() {
        if (planPlusState == State.CLOSED) {
            planPlusState = State.OPENED;
            planPlusContent.setVisibility(View.VISIBLE);
            plusExpand.setVisibility(View.GONE);
            plusContract.setVisibility(View.VISIBLE);
            planPlusContent.requestFocus();
            scrollView.requestChildFocus(planPlusContent, planPlusContent);
        } else {
            planPlusState = State.CLOSED;
            planPlusContent.setVisibility(View.GONE);
            plusExpand.setVisibility(View.VISIBLE);
            plusContract.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.plan_visio_header)
    public void onVisioPlanClicked() {
        if (planVisioState == State.CLOSED) {
            planVisioState = State.OPENED;
            planVisioContent.setVisibility(View.VISIBLE);
            visioExpand.setVisibility(View.GONE);
            visioContract.setVisibility(View.VISIBLE);
            planVisioContent.requestFocus();
            scrollView.requestChildFocus(planVisioContent, planVisioContent);
        } else {
            planVisioState = State.CLOSED;
            planVisioContent.setVisibility(View.GONE);
            visioExpand.setVisibility(View.VISIBLE);
            visioContract.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.plan_business_header)
    public void onBusinessPlanClicked() {
        if (planBusinessState == State.CLOSED) {
            planBusinessState = State.OPENED;
            planBusinessContent.setVisibility(View.VISIBLE);
            businessExpand.setVisibility(View.GONE);
            businessContract.setVisibility(View.VISIBLE);
            planBusinessContent.requestFocus();
            scrollView.requestChildFocus(planBusinessContent, planBusinessContent);
        } else {
            planBusinessState = State.CLOSED;
            planBusinessContent.setVisibility(View.GONE);
            businessExpand.setVisibility(View.VISIBLE);
            businessContract.setVisibility(View.GONE);
        }
    }

    @Override
    protected int getLayoutResourceId() {
        return R.layout.fragment_select_account_type;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.SelectAccountTypeFragment";
    }

    @OnClick(R.id.free_continue)
    public void onFreeSelected() {
        showCreateUsernameFragment(Constants.AccountType.FREE);
    }

    @OnClick(R.id.plus_continue)
    public void onPlusSelected() {
        mListener.onPaymentOptionChosen(selectedCurrency, plusPlan.getAmount(), plusPlan.getId(), plusPlan.getCycle());
        showCreateUsernameFragment(Constants.AccountType.PLUS);
    }

    @OnClick(R.id.visio_continue)
    public void onVisionarySelected() {
        mListener.onPaymentOptionChosen(selectedCurrency, visioPlan.getAmount(), visioPlan.getId(), visioPlan.getCycle());
        showCreateUsernameFragment(Constants.AccountType.VISIONARY);
    }

    @OnClick(R.id.business_continue)
    public void onBusinessSelected() {
        showCreateUsernameFragment(Constants.AccountType.BUSINESS);
    }

    @OnClick(R.id.plus_currency_duration)
    public void onPlusCurrencyDurationClicked() {
        showCurrencyDurationDialog(Constants.PlanType.PLUS, Constants.AccountType.PLUS);
    }

    @OnClick(R.id.visio_currency_duration)
    public void onVisioCurrencyDurationClicked() {
        showCurrencyDurationDialog(Constants.PlanType.VISIONARY, Constants.AccountType.VISIONARY);
    }

    @OnClick(R.id.business_currency_duration)
    public void onBusinessCurrencyDurationClicked() {
        showCurrencyDurationDialog(Constants.PlanType.BUSINESS, Constants.AccountType.BUSINESS);
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

    private void showCreateUsernameFragment(Constants.AccountType selectedAccountType) {
        mListener.onAccountSelected(selectedAccountType);
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
            mListener.fetchPlan(currencies);
        } else {
            calculatePrices(currency);
        }
    }

    private void calculatePrices(Constants.CurrencyType currency) {
        format.setCurrency(Currency.getInstance(selectedCurrency.name()));

        String planPlusAmountMonthly = format.format((double) plusPlanPriceHeaderTitleView.getTag());
        String planVisioAmountMonthly = format.format((double) visioPlanPriceHeaderTitleView.getTag());
        plusPlanPriceHeaderTitleView.setText(String.format(getResources().getString(R.string.plan_plus_with_price), planPlusAmountMonthly));
        visioPlanPriceHeaderTitleView.setText(String.format(getResources().getString(R.string.plan_visionary_with_price), planVisioAmountMonthly));

        if (selectedDurationYearly) {
            plusPlan = allCurrencyPlans.getPlan(true, currency).getPlanByPlanType(Constants.PlanType.PLUS);
            visioPlan = allCurrencyPlans.getPlan(true, currency).getPlanByPlanType(Constants.PlanType.VISIONARY);
            String fullYearlyAmountPlus = format.format(plusPlan.getAmount() / 100);
            String fullYearlyAmountVisio = format.format(visioPlan.getAmount() / 100);
            planPlusBillingInfoTextView.setText(String.format(getResources().getString(R.string.duration_annual_line3), fullYearlyAmountPlus));
            planVisioBillingInfoTextView.setText(String.format(getResources().getString(R.string.duration_annual_line3), fullYearlyAmountVisio));
        } else {
            plusPlan = allCurrencyPlans.getPlan(false, currency).getPlanByPlanType(Constants.PlanType.PLUS);
            visioPlan = allCurrencyPlans.getPlan(false, currency).getPlanByPlanType(Constants.PlanType.VISIONARY);
            planPlusBillingInfoTextView.setText(getResources().getString(R.string.duration_monthly_line3));
            planVisioBillingInfoTextView.setText(getResources().getString(R.string.duration_monthly_line3));
        }
    }

    private void showCurrencyDurationDialog(final Constants.PlanType selectedPlan, final Constants.AccountType selectedAccountType) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        if (selectedCurrency.equals(Constants.CurrencyType.USD)) {
            format = NumberFormat.getCurrencyInstance(Locale.US);
        }
        if (((Activity) getContext()).isFinishing()) {
            return;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView;
        dialogView = inflater.inflate(R.layout.layout_duration_options, null);
        builder.setView(dialogView);
        final TextView btnCancel = dialogView.findViewById(R.id.cancel);
        final Button btnSetContinue = dialogView.findViewById(R.id.set_continue);
        final RadioGroup rbgDuration = dialogView.findViewById(R.id.duration_rbg);
        mAnnualDurationPrice = dialogView.findViewById(R.id.duration_annual_title);
        mMonthlyDurationPrice = dialogView.findViewById(R.id.duration_monthly_title);
        final TextView annualDurationPrice = mAnnualDurationPrice;
        final TextView monthlyDurationPrice = mMonthlyDurationPrice;

        if (selectedDurationId != 0) {
            rbgDuration.check(selectedDurationId);
        }

        int fullAnnualMonthlyPriceCents = allCurrencyPlans.getPlan(true, selectedCurrency).getPlanByPlanType(selectedPlan).getAmount();
        int fullMonthlyPriceCents = allCurrencyPlans.getPlan(false, selectedCurrency).getPlanByPlanType(selectedPlan).getAmount();
        double selectedAnnualMonthlyPrice = fullAnnualMonthlyPriceCents / 12d / 100;
        double selectedMonthlyPrice = fullMonthlyPriceCents / 100;

        format.setCurrency(Currency.getInstance(selectedCurrency.name()));
        final String amountFormatAnnual = format.format(selectedAnnualMonthlyPrice);
        final String amountFormatMonthly = format.format(selectedMonthlyPrice);

        annualDurationPrice.setText(amountFormatAnnual);
        monthlyDurationPrice.setText(amountFormatMonthly);

        final AlertDialog alert = builder.create();
        alert.show();
        btnCancel.setOnClickListener(v -> alert.cancel());
        btnSetContinue.setOnClickListener(v -> {
            selectedDurationId = rbgDuration.getCheckedRadioButtonId();
            NumberFormat format1 = NumberFormat.getCurrencyInstance(Locale.GERMANY);
            if (selectedCurrency.equals(Constants.CurrencyType.USD)) {
                format1 = NumberFormat.getCurrencyInstance(Locale.US);
            }
            if (selectedDurationId == R.id.duration_annual) {
                selectedDurationYearly = true;

                AvailablePlansResponse currentYearlyPlan = allCurrencyPlans.getPlan(true, selectedCurrency);
                freePlan = currentYearlyPlan.getPlanByPlanType(Constants.PlanType.FREE);
                plusPlan = currentYearlyPlan.getPlanByPlanType(Constants.PlanType.PLUS);
                visioPlan = currentYearlyPlan.getPlanByPlanType(Constants.PlanType.VISIONARY);

                String fullYearlyAmountPlus = format1.format(plusPlan.getAmount() / 100);
                String fullYearlyAmountVisio = format1.format(visioPlan.getAmount() / 100);
                planPlusBillingInfoTextView.setText(String.format(getResources().getString(R.string.duration_annual_line3), fullYearlyAmountPlus));
                planVisioBillingInfoTextView.setText(String.format(getResources().getString(R.string.duration_annual_line3), fullYearlyAmountVisio));

                planPlusChangeDuration.setText(getString(R.string.switch_billing_monthly));
                planVisioChangeDuration.setText(getString(R.string.switch_billing_monthly));

                String amountFormatAnnual1 = format1.format(plusPlan.getAmount() / 100 / 12);
                String amountFormatMonthly1 = format1.format(visioPlan.getAmount() / 100 / 12);

                plusPlanPriceHeaderTitleView.setText(String.format(getResources().getString(R.string.plan_plus_with_price), amountFormatAnnual1));
                plusPlanPriceHeaderTitleView.setTag(Double.valueOf(plusPlan.getAmount() / 100 / 12));
                visioPlanPriceHeaderTitleView.setText(String.format(getResources().getString(R.string.plan_visionary_with_price), amountFormatMonthly1));
                visioPlanPriceHeaderTitleView.setTag(Double.valueOf(visioPlan.getAmount() / 100 / 12));
            } else if (selectedDurationId == R.id.duration_monthly) {
                selectedDurationYearly = false;
                planPlusBillingInfoTextView.setText(getResources().getString(R.string.duration_monthly_line3));
                planVisioBillingInfoTextView.setText(getResources().getString(R.string.duration_monthly_line3));

                planPlusChangeDuration.setText(getString(R.string.switch_billing_yearly));
                planVisioChangeDuration.setText(getString(R.string.switch_billing_yearly));

                AvailablePlansResponse currentMonthlyPlan = allCurrencyPlans.getPlan(false, selectedCurrency);
                freePlan = currentMonthlyPlan.getPlanByPlanType(Constants.PlanType.FREE);
                plusPlan = currentMonthlyPlan.getPlanByPlanType(Constants.PlanType.PLUS);
                visioPlan = currentMonthlyPlan.getPlanByPlanType(Constants.PlanType.VISIONARY);

                String amountFormatAnnual1 = format1.format(plusPlan.getAmount() / 100);
                String amountFormatMonthly1 = format1.format(visioPlan.getAmount() / 100);

                plusPlanPriceHeaderTitleView.setText(String.format(getResources().getString(R.string.plan_plus_with_price), amountFormatAnnual1));
                plusPlanPriceHeaderTitleView.setTag(Double.valueOf(plusPlan.getAmount() / 100));
                visioPlanPriceHeaderTitleView.setText(String.format(getResources().getString(R.string.plan_visionary_with_price), amountFormatMonthly1));
                visioPlanPriceHeaderTitleView.setTag(Double.valueOf(visioPlan.getAmount() / 100));
            }
            alert.cancel();
        });
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (position == 0) {
            selectedCurrency = Constants.CurrencyType.EUR;
            format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
            fetchPlan(Constants.CurrencyType.EUR);
        } else if (position == 1) {
            selectedCurrency = Constants.CurrencyType.CHF;
            format = NumberFormat.getCurrencyInstance(Locale.GERMANY);
            fetchPlan(Constants.CurrencyType.CHF);
        } else if (position == 2) {
            selectedCurrency = Constants.CurrencyType.USD;
            format = NumberFormat.getCurrencyInstance(Locale.US);
            fetchPlan(Constants.CurrencyType.USD);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private enum State {
        CLOSED, OPENED
    }
}
