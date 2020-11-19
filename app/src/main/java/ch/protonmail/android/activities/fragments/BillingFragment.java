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
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import ch.protonmail.android.R;
import ch.protonmail.android.activities.PaymentTokenApprovalActivity;
import ch.protonmail.android.adapters.SimpleCountriesAdapter;
import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.models.CardDetails;
import ch.protonmail.android.api.models.Countries;
import ch.protonmail.android.api.models.Country;
import ch.protonmail.android.api.models.CreatePaymentTokenErrorResponse;
import ch.protonmail.android.api.models.CreatePaymentTokenNetworkErrorResponse;
import ch.protonmail.android.api.models.CreatePaymentTokenResponse;
import ch.protonmail.android.api.models.CreatePaymentTokenSuccessResponse;
import ch.protonmail.android.api.models.PaymentMethod;
import ch.protonmail.android.api.models.PaymentToken;
import ch.protonmail.android.api.models.PaymentType;
import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.api.utils.ParseUtils;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.ConnectivityEvent;
import ch.protonmail.android.events.CreateUserEvent;
import ch.protonmail.android.events.DonateEvent;
import ch.protonmail.android.events.KeysSetupEvent;
import ch.protonmail.android.events.LoginEvent;
import ch.protonmail.android.events.LoginInfoEvent;
import ch.protonmail.android.events.MailboxLoginEvent;
import ch.protonmail.android.events.PaymentMethodEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.organizations.OrganizationEvent;
import ch.protonmail.android.events.payment.VerifyPaymentEvent;
import ch.protonmail.android.utils.FileUtils;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.utils.extensions.CollectionExtensions;
import ch.protonmail.android.utils.extensions.TextExtensions;
import ch.protonmail.android.viewmodel.BillingViewModel;
import ch.protonmail.android.views.PMWebView;

import static ch.protonmail.android.activities.PaymentTokenApprovalActivityKt.EXTRA_RESULT_PAYMENT_TOKEN_STRING;
import static ch.protonmail.android.activities.PaymentTokenApprovalActivityKt.EXTRA_RESULT_STATUS_STRING;
import static ch.protonmail.android.activities.PaymentTokenApprovalActivityKt.RESULT_CODE_ERROR;
import static ch.protonmail.android.api.models.CreatePaymentTokenBodyKt.PAYMENT_TYPE_CARD;
import static ch.protonmail.android.api.models.CreatePaymentTokenBodyKt.PAYMENT_TYPE_PAYPAL;
import static ch.protonmail.android.api.models.CreatePaymentTokenSuccessResponseKt.FIELD_HUMAN_VERIFICATION_TOKEN;
import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_ERROR_VERIFICATION_NEEDED;

/**
 * Created by dkadrikj on 7/1/16.
 */
public class BillingFragment extends CreateAccountBaseFragment {

    private static final String ARGUMENT_AMOUNT = "ch.protonmail.android.ARG_AMOUNT";
    private static final String ARGUMENT_CURRENCY = "ch.protonmail.android.ARG_CURRENCY";
    private static final String ARGUMENT_SELECTED_PLAN_ID = "ch.protonmail.android.ARG_SELECTED_PLAN_ID";
    private static final String ARGUMENT_BILLING_TYPE = "ch.protonmail.android.ARG_BILLING_TYPE";
    private static final String ARGUMENT_CYCLE = "ch.protonmail.android.ARG_CYCLE";

    private static final int REQUEST_CODE_PAYMENT_APPROVAL = 2421;

    @BindView(R.id.credit_card_name)
    EditText creditCardNameEditText;
    @BindView(R.id.card_number)
    EditText creditCardNumberEditText;
    @BindView(R.id.cvc)
    EditText cvcNumberEditText;
    @BindView(R.id.zip_code)
    EditText zipCodeEditText;
    @BindView(R.id.countries_spinner)
    Spinner mCountriesSpinner;
    @BindView(R.id.payment_methods_spinner)
    Spinner mPaymentMethodsSpinner;
    @BindView(R.id.months_spinner)
    Spinner mMonthsSpinner;
    @BindView(R.id.years_spinner)
    Spinner mYearsSpinner;
    @BindView(R.id.terms)
    TextView mTermsTextView;
    @BindView(R.id.progress_container)
    View mProgressContainer;
    @BindView(R.id.fingerprint)
    PMWebView mWebView;
    @BindView(R.id.input_layout)
    View mInputFormLayout;
    @BindView(R.id.payment_methods_layout)
    View mPaymentMethodsLayout;
    @BindView(R.id.paypal_layout)
    View mPaypalLayout;
    @BindView(R.id.success_page_layout)
    View mSuccessPageLayout;
    @BindView(R.id.payment_picker_layout)
    View mPickerLayout;
    @BindView(R.id.snackBarLayout)
    View snackBarLayout;
    @BindView(R.id.submit)
    Button submitButton;
    @BindView(R.id.submit_paypal)
    Button submitPaypalButton;
    @BindView(R.id.text_payment_success_title)
    TextView paymentSuccessTitle;
    @BindView(R.id.text_payment_success_text)
    TextView paymentSuccessText;

    /**
     * A boolean for keep track whether the connection has already been available
     * We use this for load the fingerprint verification only once
     */
    private boolean hadConnection = false;

    /** A {@link Snackbar} that shows when connectivity is not available */
    private Snackbar noConnectivitySnackBar;

    private SimpleCountriesAdapter mAdapter;

    private IBillingListener billingListener;

    private Constants.BillingType billingType;
    private int amount;
    private Constants.CurrencyType currency;
    private String couponCode;
    private List<String> planIds;
    private String cardNumber;
    private String cardName;
    private String cardCvc;
    private String zip;
    private String month;
    private String year;
    private String countryCode;
    private String mFingerprint;
    private int cycle;
    private List<PaymentMethod> mPaymentMethods;

    private BillingViewModel billingViewModel = null;
    private String paymentTokenForSubscription = null;

    private final int PAYMENT_SUCCESS_PAGE_TIMEOUT = 5000;

    public static BillingFragment newInstance(Constants.BillingType billingType, int windowHeight,
                                              Constants.CurrencyType currency, int amount, String selectedPlanId, int cycle) {
        BillingFragment fragment = new BillingFragment();
        Bundle extras = new Bundle();
        extras.putInt(ARGUMENT_WINDOW_HEIGHT, windowHeight);
        extras.putString(ARGUMENT_CURRENCY, currency.name());
        extras.putInt(ARGUMENT_AMOUNT, amount);
        extras.putSerializable(ARGUMENT_BILLING_TYPE, billingType);
        extras.putString(ARGUMENT_SELECTED_PLAN_ID, selectedPlanId);
        extras.putInt(ARGUMENT_CYCLE, cycle);
        fragment.setArguments(extras);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        windowHeight = getArguments().getInt(ARGUMENT_WINDOW_HEIGHT);
        currency = Constants.CurrencyType.valueOf(getArguments().getString(ARGUMENT_CURRENCY, Constants.CurrencyType.EUR.name()));
        billingType = (Constants.BillingType) getArguments().getSerializable(ARGUMENT_BILLING_TYPE);
        amount = getArguments().getInt(ARGUMENT_AMOUNT);
        String selectedPlanId = getArguments().getString(ARGUMENT_SELECTED_PLAN_ID);
        planIds = new ArrayList<>();
        planIds.add(selectedPlanId);
        cycle = getArguments().getInt(ARGUMENT_CYCLE);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        BillingViewModel.Factory billingViewModelFactory = new BillingViewModel.Factory(billingListener.getProtonMailApiManager());
        billingViewModel = new ViewModelProvider(this, billingViewModelFactory).get(BillingViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = super.onCreateView(inflater, container, savedInstanceState);

        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(new WebAppInterface(), "AndroidInterface");
        mTermsTextView.setMovementMethod(LinkMovementMethod.getInstance());
        mRootLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);
        Countries countries = new Gson().fromJson(FileUtils.readRawTextFile(getContext(), R.raw.country_codes), Countries.class);
        Countries mostUsedCountries = new Gson().fromJson(FileUtils.readRawTextFile(getContext(), R.raw.country_codes_most_used), Countries.class);
        List<Country> all = mostUsedCountries.getCountries();
        List<Country> otherCountries = countries.getCountries();
        Collections.sort(otherCountries, (lhs, rhs) -> lhs.getName().compareTo(rhs.getName()));
        all.addAll(otherCountries);
        Country selectCountry = new Country();
        selectCountry.setName(getString(R.string.country));
        all.add(0, selectCountry);
        mAdapter = new SimpleCountriesAdapter(getContext(), all);
        mCountriesSpinner.setAdapter(mAdapter);
        mCountriesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mAdapter.setSelectedPosition(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
        ArrayAdapter<CharSequence> monthsAdapter = ArrayAdapter.createFromResource(getContext(), R.array.months_array, R.layout.month_spinner_item);

        Calendar calendar = Calendar.getInstance();
        List<String> years = new ArrayList<>();
        int year = calendar.get(Calendar.YEAR);
        years.add("YY");
        for (int i = 0; i < 20; i++) {
            String y = String.valueOf(year + i);
            years.add(y);
        }
        ArrayAdapter<String> yearsAdapter = new ArrayAdapter<>(getContext(), R.layout.month_spinner_item, years);
        mMonthsSpinner.setAdapter(monthsAdapter);
        mYearsSpinner.setAdapter(yearsAdapter);

        // Set up no connectivity SnackBar
        noConnectivitySnackBar =
                Snackbar.make(snackBarLayout, R.string.no_connectivity_detected_troubleshoot, Snackbar.LENGTH_INDEFINITE);
        View snackBarView = noConnectivitySnackBar.getView();
        snackBarView.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.red));
        TextView snackBarTextView =
                snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
        snackBarTextView.setTextColor(Color.WHITE);

        showPaymentMethods();

        rootView.findViewById(R.id.submit_picker_paypal).setVisibility(Constants.FeatureFlags.PAYPAL_PAYMENT ? View.VISIBLE : View.GONE);

        return rootView;
    }

    private void showPaymentMethods() {

        mPaymentMethods = ProtonMailApplication.getApplication().getPaymentMethods();

        if (mPaymentMethods != null && mPaymentMethods.size() > 0) {
            List<String> paymentMethodsStrings = new ArrayList<>();
            boolean payPalMethodExists = false;
            for (PaymentMethod paymentMethod : mPaymentMethods) {
                CardDetails cardDetails = paymentMethod.getCardDetails();
                if (cardDetails.getBillingAgreementId() != null) {
                    payPalMethodExists = true;
                    paymentMethodsStrings.add(String.format(getString(R.string.payment_method_paypal_placeholder), cardDetails.getPayer()));
                } else {
                    paymentMethodsStrings.add(String.format(getString(R.string.payment_method_card_placeholder), cardDetails.getLast4(), cardDetails.getExpirationMonth(), cardDetails.getExpirationYear()));
                }
            }
            paymentMethodsStrings.add(getString(R.string.try_another_card));
            if (!payPalMethodExists && Constants.FeatureFlags.PAYPAL_PAYMENT) {
                paymentMethodsStrings.add(getString(R.string.try_another_paypal));
            }
            ArrayAdapter<String> paymentMethodsAdapter = new ArrayAdapter<>(getContext(), R.layout.month_spinner_item, paymentMethodsStrings);
            mPaymentMethodsSpinner.setAdapter(paymentMethodsAdapter);
        }

        if (mPaymentMethods != null && mPaymentMethods.size() > 0) {
            mInputFormLayout.setVisibility(View.GONE);
            mPaymentMethodsLayout.setVisibility(View.VISIBLE);
            mPaypalLayout.setVisibility(View.GONE);
            mPickerLayout.setVisibility(View.GONE);
            mSuccessPageLayout.setVisibility(View.GONE);
        } else {
            mInputFormLayout.setVisibility(View.GONE);
            mPaymentMethodsLayout.setVisibility(View.GONE);
            mPaypalLayout.setVisibility(View.GONE);
            mPickerLayout.setVisibility(View.VISIBLE);
            mSuccessPageLayout.setVisibility(View.GONE);
        }

    }

    private void showSuccessPage() {

        if (billingType == Constants.BillingType.DONATE) {
            paymentSuccessTitle.setText(R.string.donation_success_title);
            paymentSuccessText.setText(R.string.donation_success_text);
        } else {
            paymentSuccessTitle.setText(R.string.payment_success_title);
            paymentSuccessText.setText(R.string.payment_success_text);
        }

        mProgressContainer.setVisibility(View.GONE);
        mInputFormLayout.setVisibility(View.GONE);
        mPaymentMethodsLayout.setVisibility(View.GONE);
        mPaypalLayout.setVisibility(View.GONE);
        mPickerLayout.setVisibility(View.GONE);
        mSuccessPageLayout.setVisibility(View.VISIBLE);

    }

    private void onConnectivityChange(final boolean hasConnection) {
        if (hasConnection) {
            if (noConnectivitySnackBar.isShownOrQueued()) noConnectivitySnackBar.dismiss();
            submitButton.setEnabled(true);
            submitPaypalButton.setEnabled(true);
            if (!hadConnection) { // TODO
                mWebView.loadUrl("https://secure.protonmail.com/paymentwall.html");
                hadConnection = true;
            }

        } else {
            if (!noConnectivitySnackBar.isShownOrQueued()) noConnectivitySnackBar.show();
            submitButton.setEnabled(false);
            submitPaypalButton.setEnabled(false);
        }
    }

    public void setActivityListener(IBillingListener billingListener) {
        this.billingListener = billingListener;
    }

    public void retryPaymentAfterCaptchaValidation(String token, String tokenType) {
        mProgressContainer.setVisibility(View.VISIBLE);
        billingViewModel.retryCreatePaymentToken(token, tokenType);
    }

    @OnClick(R.id.submit_picker_card)
    public void onSubmitPickerCardClicked() {
        mInputFormLayout.setVisibility(View.VISIBLE);
        mPickerLayout.setVisibility(View.GONE);
    }

    @OnClick(R.id.submit_picker_paypal)
    public void onSubmitPickerPaypalClicked() {
        mPaypalLayout.setVisibility(View.VISIBLE);
        mPickerLayout.setVisibility(View.GONE);
    }

    @OnClick(R.id.countries_down_arrow)
    public void onCOuntriesSpinnerArrowClicked() {
        mCountriesSpinner.performClick();
    }

    @OnClick(R.id.months_down_arrow)
    public void onMonthsSpinnerArrowClicked() {
        mMonthsSpinner.performClick();
    }

    @OnClick(R.id.years_down_arrow)
    public void onYearsSpinnerArrowClicked() {
        mYearsSpinner.performClick();
    }

    @OnItemSelected(R.id.payment_methods_spinner)
    public void onPaymentSpinnerItemSelected() {
        int selectedPosition = mPaymentMethodsSpinner.getSelectedItemPosition();
        if (selectedPosition == mPaymentMethods.size()) { // second to last item in spinner clicked, show Credit Card input
            mInputFormLayout.setVisibility(View.VISIBLE);
            mPaymentMethodsLayout.setVisibility(View.GONE);
            mPaypalLayout.setVisibility(View.GONE);
            mPickerLayout.setVisibility(View.GONE);
        }
        if (selectedPosition == mPaymentMethods.size() + 1) { // last item in spinner clicked, show PayPal input
            mInputFormLayout.setVisibility(View.GONE);
            mPaymentMethodsLayout.setVisibility(View.GONE);
            mPaypalLayout.setVisibility(View.VISIBLE);
            mPickerLayout.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.payment_methods_down_arrow)
    public void onPaymentsSpinnerArrowClicked() {
        mPaymentMethodsSpinner.performClick();
    }

    @OnClick(R.id.submit_payment_method)
    public void onSubmitPaymentMethodClicked() { // on screen with saved payment methods spinner
        int selectedPosition = mPaymentMethodsSpinner.getSelectedItemPosition();
        PaymentMethod paymentMethod = mPaymentMethods.get(selectedPosition);
        if (billingType == Constants.BillingType.UPGRADE || billingType == Constants.BillingType.DONATE) {
            mProgressContainer.setVisibility(View.VISIBLE);
            billingViewModel.createPaymentTokenFromPaymentMethodId(amount, currency, paymentMethod.getId()).observe(this, createPaymentTokenResponse -> {
                handleCreatePaymentTokenResponse(createPaymentTokenResponse, paymentMethod.getCardDetails().getPayer() == null ? PAYMENT_TYPE_CARD : PAYMENT_TYPE_PAYPAL);
            });
        }
    }

    @OnClick(R.id.submit)
    public void onSubmitClicked() { // on screen with Credit Card details
        cardNumber = creditCardNumberEditText.getText().toString();
        cardName = creditCardNameEditText.getText().toString();
        int selectedCountryPosition = mCountriesSpinner.getSelectedItemPosition();
        month = (String) mMonthsSpinner.getSelectedItem();
        year = (String) mYearsSpinner.getSelectedItem();
        int monthInt;
        int yearInt;
        try {
            monthInt = Integer.valueOf(month);
            yearInt = Integer.valueOf(year);
        } catch (java.lang.NumberFormatException e) {
            monthInt = 0;
            yearInt = 0;
        }
        boolean inputValid = validateInput(cardNumber, cardName, selectedCountryPosition, monthInt, yearInt);
        if (inputValid) {
            mProgressContainer.setVisibility(View.VISIBLE);
            cardCvc = cvcNumberEditText.getText().toString();
            zip = zipCodeEditText.getText().toString();
            countryCode = mAdapter.getItem(selectedCountryPosition).getCountryCode();
        } else {
            return;
        }
        String fingerprint = mFingerprint;
        if ("0".equals(mFingerprint)) {
            fingerprint = "";
        }

        // we need to create PaymentToken first
        mProgressContainer.setVisibility(View.VISIBLE);

        Map<String, String> paymentDetails = new HashMap<>();
        paymentDetails.put(Fields.Payment.NUMBER, cardNumber);
        paymentDetails.put(Fields.Payment.EXPIRATION_MONTH, month);
        paymentDetails.put(Fields.Payment.EXPIRATION_YEAR, year);
        paymentDetails.put(Fields.Payment.CVC, cardCvc);
        paymentDetails.put(Fields.Payment.NAME, cardName);
        paymentDetails.put(Fields.Payment.COUNTRY, countryCode);
        paymentDetails.put(Fields.Payment.ZIP, zip);
        PaymentType cardPaymentType = new PaymentType.Card(paymentDetails);

        billingViewModel.createPaymentToken(amount, currency, cardPaymentType).observe(this, createPaymentTokenResponse -> {
            handleCreatePaymentTokenResponse(createPaymentTokenResponse, PAYMENT_TYPE_CARD);
        });
    }

    private void handleCreatePaymentTokenResponse(@Nullable CreatePaymentTokenResponse createPaymentTokenResponse, @NonNull String paymentType) {
        if (createPaymentTokenResponse != null) {
            if (createPaymentTokenResponse instanceof CreatePaymentTokenNetworkErrorResponse) {
                CreatePaymentTokenNetworkErrorResponse networkErrorResponse = (CreatePaymentTokenNetworkErrorResponse) createPaymentTokenResponse;
                if (!networkErrorResponse.getEventConsumed()) {
                    networkErrorResponse.setEventConsumed(true);
                    showPaymentError(null, getString(R.string.payment_approval_unknown_error_description));
                }
            } else if (createPaymentTokenResponse instanceof CreatePaymentTokenErrorResponse) {
                CreatePaymentTokenErrorResponse errorResponse = (CreatePaymentTokenErrorResponse) createPaymentTokenResponse;
                if (!errorResponse.getEventConsumed()) {
                    errorResponse.setEventConsumed(true);
                    if (errorResponse.getCode() == RESPONSE_CODE_ERROR_VERIFICATION_NEEDED) {
                        mProgressContainer.setVisibility(View.GONE);
                        billingListener.onRequestCaptchaVerification((String) errorResponse.getDetails().get(FIELD_HUMAN_VERIFICATION_TOKEN));
                    } else {
                        showPaymentError(null, errorResponse.getError());
                    }
                }
            } else if (createPaymentTokenResponse instanceof CreatePaymentTokenSuccessResponse) {
                CreatePaymentTokenSuccessResponse successResponse = ((CreatePaymentTokenSuccessResponse) createPaymentTokenResponse);
                if (!successResponse.getEventConsumed()) {
                    successResponse.setEventConsumed(true);
                    handlePaymentTokenStatusChange(successResponse.getStatus(), successResponse.getToken(), successResponse.getApprovalUrl(), paymentType, successResponse.getReturnHost());
                }
            }
        }
    }

    @OnClick(R.id.submit_paypal)
    public void onSubmitPaypalClicked() { // on screen with PayPal

        mProgressContainer.setVisibility(View.VISIBLE);

        billingViewModel.createPaymentToken(amount, currency, PaymentType.PayPal.INSTANCE).observe(this, createPaymentTokenResponse -> {
            handleCreatePaymentTokenResponse(createPaymentTokenResponse, PAYMENT_TYPE_PAYPAL);
        });
    }

    private void handlePaymentTokenStatusChange(@NonNull PaymentToken.Status status, @NonNull String token, @NonNull String approvalUrl, @NonNull String paymentType, @NonNull String returnHost) {

        switch (status) {
            case PENDING:
                startActivityForResult(PaymentTokenApprovalActivity.Companion.createApprovalIntent(getContext(), approvalUrl, token, paymentType, returnHost), REQUEST_CODE_PAYMENT_APPROVAL);
                break;
            case CHARGEABLE:

                mProgressContainer.setVisibility(View.VISIBLE);

                switch (billingType) {
                    case CREATE:
                        mListener.createVerificationPaymentForPaymentToken(amount, currency, token);
                        paymentTokenForSubscription = token;
                        break;
                    case UPGRADE:
                        mListener.createSubscriptionForPaymentToken(token, amount, currency, couponCode, planIds, cycle);
                        break;
                    case DONATE:
                        mListener.donateForPaymentToken(amount, currency, token);
                        mProgressContainer.setVisibility(View.VISIBLE);
                        break;
                }
                break;
            case FAILED:
            case NOT_SUPPORTED:
                showPaymentMethods();
                showPaymentError(null, getString(R.string.payment_approval_unknown_error_description));
                break;
            case CONSUMED:
                showPaymentMethods();
                showPaymentError(null, getString(R.string.payment_approval_consumed_error_description));
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_PAYMENT_APPROVAL) {
            if (resultCode == Activity.RESULT_CANCELED) {
                showPaymentMethods();
                showPaymentError(null, getString(R.string.payment_approval_cancel_error_description));
            } else if (resultCode == RESULT_CODE_ERROR) {
                showPaymentMethods();
                showPaymentError(null, getString(R.string.payment_approval_unknown_error_description));
            } else if (resultCode == Activity.RESULT_OK) {
                String tokenStatus = data.getStringExtra(EXTRA_RESULT_STATUS_STRING);
                String token = data.getStringExtra(EXTRA_RESULT_PAYMENT_TOKEN_STRING);

                handlePaymentTokenStatusChange(PaymentToken.Status.valueOf(tokenStatus), token, "", "", "");
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Subscribe
    public void onConnectivityEvent(ConnectivityEvent event) {
        onConnectivityChange(event.hasConnection());
    }

    @Subscribe
    public void onFetchOrganizationEvent(OrganizationEvent event) {
        mProgressContainer.setVisibility(View.GONE);
        if (event.getStatus() == Status.FAILED) {
            String error = event.getResponse().getError();
            Map<String, Object> detailsObjMap = event.getResponse().getDetails();
            Map<String, String> errorDetails =
                    CollectionExtensions.filterValues(detailsObjMap, String.class);
            mListener.onBillingError(error, ParseUtils.Companion.compileSingleErrorMessage(errorDetails));

        } else if (event.getStatus() == Status.SUCCESS) {
            mListener.onBillingCompleted();
        }
    }

    @Subscribe
    public void onVerificationEvent(VerifyPaymentEvent event) {
        if (event.getStatus() == Status.SUCCESS) {
            showSuccessPage();
            mListener.createUser(Constants.TokenType.PAYMENT, event.getVerificationCode());
        } else {
            mProgressContainer.setVisibility(View.GONE);
            showPaymentError(event.getError(), event.getErrorDescription());
        }
    }

    @Subscribe
    public void onLoginInfoEvent(LoginInfoEvent event) {
        super.onLoginInfoEvent(event);
    }

    @Subscribe
    public void onCreateUserEvent(CreateUserEvent event) {
        super.onCreateUserEvent(event);
    }

    @Subscribe
    public void onLoginEvent(LoginEvent event) {
        super.onLoginEvent(event);
    }

    @Subscribe
    public void onLoginEvent(MailboxLoginEvent event) {
        if (event == null) {
            return;
        }
        ProtonMailApplication.getApplication().resetMailboxLoginEvent();
        mProgressBar.setVisibility(View.GONE);
        switch (event.status) {
            case SUCCESS: {
                mListener.createSubscriptionForPaymentToken(paymentTokenForSubscription, 0, currency, couponCode, planIds, cycle);
            }
            break;
            default:
                handleLoginStatus(event.status);
                break;
        }
    }

    @Subscribe
    public void onKeysSetupEvent(KeysSetupEvent event) {
        super.onKeysSetupEvent(event);
    }

    @Subscribe
    public void onPaymentMethodEvent(PaymentMethodEvent event) {
        switch (event.getStatus()) {
            case SUCCESS: {

                showSuccessPage();

                new Handler().postDelayed(() -> {

                    if (billingType == Constants.BillingType.CREATE) {
                        // inform user creation completed
                        mListener.fetchOrganization();
                        mListener.startAddressSetup();
                    } else if (billingType == Constants.BillingType.UPGRADE) {
                        // create organization
                        mListener.fetchOrganization();
                    }

                }, PAYMENT_SUCCESS_PAGE_TIMEOUT);

                break;
            }
            default:
                mProgressContainer.setVisibility(View.GONE);
                showPaymentError(event.getError(), event.getErrorDescription());
                break;
        }
    }

    @Subscribe
    public void onDonateEvent(DonateEvent event) {
        if (event.getStatus() == Status.SUCCESS) {
            showSuccessPage();
            new Handler().postDelayed(() -> mListener.donateDone(), PAYMENT_SUCCESS_PAGE_TIMEOUT);
        } else {
            mProgressContainer.setVisibility(View.GONE);
            showPaymentError(event.getError(), event.getErrorDescription());
        }
    }

    private void clearFields() {
        creditCardNameEditText.setText("");
        creditCardNumberEditText.setText("");
        cvcNumberEditText.setText("");
        zipCodeEditText.setText("");
        mCountriesSpinner.setSelection(0);
        mMonthsSpinner.setSelection(0);
        mYearsSpinner.setSelection(0);
    }

    private void showPaymentError(String error, String errorDescription) {

        if (getActivity() != null) {
            UiUtil.hideKeyboard(getActivity());
        }
        mProgressContainer.setVisibility(View.GONE);

        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View dialogView;
        if (billingType == Constants.BillingType.CREATE) {
            dialogView = inflater.inflate(R.layout.layout_payment_error_create_account, null);
        } else if (billingType == Constants.BillingType.UPGRADE || billingType == Constants.BillingType.DONATE) {
            dialogView = inflater.inflate(R.layout.layout_payment_error_upgrade, null);
        } else {
            return;
        }
        builder.setView(dialogView);

        final TextView btnCancel = dialogView.findViewById(R.id.cancel);
        final TextView errorTitleText = dialogView.findViewById(R.id.stripe_error_title);
        final TextView errorDescriptionText = dialogView.findViewById(R.id.stripe_error_description);
        final View btnAnotherCard = dialogView.findViewById(R.id.another_card);
        errorTitleText.setText(error);
        errorDescriptionText.setText(errorDescription);
        TextView btnFreeAccount = null;

        if (billingType == Constants.BillingType.CREATE) {
            btnFreeAccount = dialogView.findViewById(R.id.create_free_account);
        }

        final AlertDialog alert = builder.create();

        btnAnotherCard.setOnClickListener(v -> {
            alert.cancel();
            clearFields();
            showPaymentMethods();
        });

        if (btnFreeAccount != null) {
            btnFreeAccount.setOnClickListener(v -> {
                alert.cancel();
                mListener.offerFreeAccount(windowHeight);
            });
        }
        btnCancel.setOnClickListener(v -> {
            alert.cancel();
            mListener.offerFreeAccount(windowHeight);
        });

        alert.show();
    }

    private boolean validateInput(String cardNumber, String name, int countryPosition, int month, int year) {
        String message = "";
        boolean isValid = true;
        if (TextUtils.isEmpty(cardNumber)) {
            message = getString(R.string.error_message_card_number_invalid);
            isValid = false;
        }
        int monthPosition = mMonthsSpinner.getSelectedItemPosition();
        int yearPosition = mYearsSpinner.getSelectedItemPosition();
        if (monthPosition == 0 || yearPosition == 0) {
            message = getString(R.string.error_message_exp_date_invalid);
            isValid = false;
        }
        Calendar calendar = new GregorianCalendar();
        if (year < calendar.get(Calendar.YEAR) || (year == calendar.get(Calendar.YEAR) && month < (calendar.get(Calendar.MONTH) + 1))) {
            message = getString(R.string.error_message_exp_date_past);
            isValid = false;
        }
        if (countryPosition == 0) {
            message = getString(R.string.error_message_select_country);
            isValid = false;
        }
        if (TextUtils.isEmpty(name)) {
            message = getString(R.string.error_message_card_name_invalid);
            isValid = false;
        }
        String cvc = cvcNumberEditText.getText().toString();
        if (TextUtils.isEmpty(cvc)) {
            message = getString(R.string.error_message_cvc_invalid);
            isValid = false;
        }
        String zip = zipCodeEditText.getText().toString();
        if (TextUtils.isEmpty(zip)) {
            message = getString(R.string.error_message_zip_code_invalid);
            isValid = false;
        }
        if (!isValid) {
            TextExtensions.showToast(getContext(), message);
        }
        return isValid;
    }

    @Override
    protected void onFocusCleared() {

    }

    @Override
    protected void setFocuses() {
        creditCardNameEditText.setOnFocusChangeListener(mFocusListener);
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
    protected int getLayoutResourceId() {
        return R.layout.fragment_billing;
    }

    @Override
    public String getFragmentKey() {
        return "ProtonMail.BillingFragment";
    }

    private class WebAppInterface {

        @JavascriptInterface
        public void getFingerprint(String message) {
            mFingerprint = message;
        }
    }

    public interface IBillingListener {
        ProtonMailApiManager getProtonMailApiManager();
        void onRequestCaptchaVerification(String token);
    }
}
