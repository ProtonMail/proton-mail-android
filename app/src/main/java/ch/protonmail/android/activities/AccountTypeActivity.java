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
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.lifecycle.ViewModelProvider;

import com.squareup.otto.Subscribe;

import java.util.List;

import butterknife.BindView;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.CardDetails;
import ch.protonmail.android.api.models.Organization;
import ch.protonmail.android.api.models.PaymentMethod;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.events.organizations.OrganizationEvent;
import ch.protonmail.android.jobs.organizations.GetOrganizationJob;
import ch.protonmail.android.usecase.model.FetchPaymentMethodsResult;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.viewmodel.AccountTypeViewModel;
import timber.log.Timber;

public class AccountTypeActivity extends BaseActivity {

    private static final int REQUEST_CODE_UPGRADE = 1;

    @BindView(R.id.upgrade)
    Button upgrade;
    @BindView(R.id.account_type_title)
    TextView accountTypeTitle;
    @BindView(R.id.prefix)
    TextView prefix;
    @BindView(R.id.account_type_progress)
    View accountTypeProgress;
    @BindView(R.id.payment_methods_parent)
    LinearLayout paymentMethodsParent;
    @BindView(R.id.no_payment_methods)
    TextView noPaymentMethodsView;
    @BindView(R.id.edit_payment_methods)
    TextView editPaymentMethods;

    private AccountTypeViewModel viewModel;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_account_type;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        viewModel = new ViewModelProvider(this).get(AccountTypeViewModel.class);

        final User user = mUserManager.getUser();
        if (user != null) {
            if (!user.isPaidUser()) {
                setAccountType(0);
            } else {
                setAndCheckOrganization();
            }
        }

        viewModel.getFetchPaymentMethodsResult().observe(
                this,
                this::onPaymentMethods
        );
        viewModel.fetchPaymentMethods();
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
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == android.R.id.home) {
            saveLastInteraction();
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setAndCheckOrganization() {
        Organization organization = ProtonMailApplication.getApplication().getOrganization();
        if (organization != null) {
            setOrganization(organization);
        } else {
            accountTypeProgress.setVisibility(View.VISIBLE);
        }
        GetOrganizationJob getOrganizationJob = new GetOrganizationJob();
        mJobManager.addJobInBackground(getOrganizationJob);
    }

    @Subscribe
    public void onOrganizationEvent(OrganizationEvent event) {
        if (event.getStatus() == Status.SUCCESS) {
            Organization organization = event.getResponse().getOrganization();
            if (organization != null) {
                setOrganization(organization);
            }
        }
    }

    private void onPaymentMethods(FetchPaymentMethodsResult result) {
        Timber.v("Payment methods result:%s", result);
        if (result instanceof FetchPaymentMethodsResult.Success) {
            List<PaymentMethod> paymentMethods =
                    ((FetchPaymentMethodsResult.Success) result).getPaymentMethods();
            showCardDetails(paymentMethods);
        }
    }

    private void setOrganization(Organization organization) {
        accountTypeProgress.setVisibility(View.GONE);
        String planName = organization.getPlanName();
        Constants.PlanType planType = Constants.PlanType.Companion.fromString(planName);
        if (TextUtils.isEmpty(planName)) {
            setAccountType(0);
        } else if (planType == Constants.PlanType.PLUS) {
            setAccountType(1);
        } else if (planType == Constants.PlanType.VISIONARY) {
            setAccountType(2);
        } else if (planType == Constants.PlanType.PROFESSIONAL) {
            setAccountType(3);
        }
    }

    private void setAccountType(int type) {
        if (type == 0) {
            upgrade.setVisibility(View.VISIBLE);
            noPaymentMethodsView.setVisibility(View.VISIBLE);
            styleEditPaymentMethod();
        } else {
            upgrade.setVisibility(View.GONE);
            viewModel.fetchPaymentMethods();
        }
        int accountTypeColor = getResources().getIntArray(R.array.account_type_colors)[type];
        String accountName = getResources().getStringArray(R.array.account_type_names)[type];
        String prefixValue = getString(R.string.protonmail);
        prefix.setText(prefixValue);
        accountTypeTitle.setText(accountName);
        accountTypeTitle.setTextColor(accountTypeColor);
    }

    private void showCardDetails(List<PaymentMethod> paymentMethods) {
        if (paymentMethods != null) {
            accountTypeProgress.setVisibility(View.GONE);
            if (paymentMethods.size() == 0) {
                styleEditPaymentMethod();
                noPaymentMethodsView.setVisibility(View.VISIBLE);
            } else {
                LayoutInflater inflater = LayoutInflater.from(this);
                paymentMethodsParent.removeAllViews();
                for (PaymentMethod paymentMethod : paymentMethods) {
                    CardDetails cardDetails = paymentMethod.getCardDetails();
                    View cardTypeLayout = inflater.inflate(R.layout.layout_card, null);
                    TextView cardTitle = cardTypeLayout.findViewById(R.id.card_title);
                    TextView cardSubtitle = cardTypeLayout.findViewById(R.id.card_subtitle);
                    if (paymentMethod.getCardDetails().getBillingAgreementId() != null) {
                        cardTitle.setText(String.format(getString(R.string.payment_method_paypal_placeholder), cardDetails.getPayer()));
                        cardSubtitle.setText("");
                    } else {
                        cardTitle.setText(String.format(getString(R.string.card_details_title), cardDetails.getBrand(), cardDetails.getLast4()));
                        cardSubtitle.setText(String.format(getString(R.string.card_details_subtitle), cardDetails.getExpirationMonth() + "/" + cardDetails.getExpirationYear()));
                    }
                    paymentMethodsParent.addView(cardTypeLayout);
                }
            }
        } else {
            accountTypeProgress.setVisibility(View.VISIBLE);
        }
    }

    private void styleEditPaymentMethod() {
        String editPaymentMethodsText = editPaymentMethods.getText().toString();
        String editPaymentMethodsTextLowerCase = editPaymentMethodsText.toLowerCase();
        int startIndex = editPaymentMethodsTextLowerCase.indexOf("protonmail.com");
        SpannableString spannableString = new SpannableString(editPaymentMethods.getText());
        spannableString.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.new_purple_dark)), startIndex, startIndex + 14, 0);
        spannableString.setSpan(new URLSpan("https://www.protonmail.com"), startIndex, startIndex + 14, 0);
        editPaymentMethods.setMovementMethod(LinkMovementMethod.getInstance());
        editPaymentMethods.setText(spannableString);
    }

    @OnClick(R.id.upgrade)
    public void onUpgrade() {
        /*
        Intent upgradeIntent = new Intent(this, UpsellingActivity.class);
        upgradeIntent.putExtra(UpsellingActivity.EXTRA_OPEN_UPGRADE_CONTAINER, true);
        startActivityForResult(AppUtil.decorInAppIntent(upgradeIntent), REQUEST_CODE_UPGRADE);
        TODO("startUpgradePlanWorkflow")
        */
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_UPGRADE) {
            Bundle extras = data.getExtras();
            if (extras != null) {
                boolean success = false; // TODO: extras.getBoolean(BillingActivity.EXTRA_SUCCESS);
                if (success) {
                    accountTypeProgress.setVisibility(View.VISIBLE);
                    final User user = mUserManager.getUser();
                    if (!user.isPaidUser()) {
                        setAccountType(0);
                    } else {
                        setAndCheckOrganization();
                    }
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
