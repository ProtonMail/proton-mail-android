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
package ch.protonmail.android.settings.presentation

import android.os.Bundle
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import butterknife.OnClick
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.api.models.Organization
import ch.protonmail.android.api.models.PaymentMethod
import ch.protonmail.android.core.Constants.PlanType
import ch.protonmail.android.core.Constants.PlanType.Companion.fromString
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.Status
import ch.protonmail.android.events.organizations.OrganizationEvent
import ch.protonmail.android.jobs.organizations.GetOrganizationJob
import ch.protonmail.android.usecase.model.FetchPaymentMethodsResult
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showInfoDialog
import ch.protonmail.android.viewmodel.AccountTypeViewModel
import com.squareup.otto.Subscribe
import timber.log.Timber

class AccountTypeActivity : BaseActivity() {

    private val upgrade by lazy { findViewById<Button>(R.id.upgrade) }
    private val accountTypeTitle by lazy { findViewById<TextView>(R.id.account_type_title) }
    private val prefix by lazy { findViewById<TextView>(R.id.prefix) }
    private val accountTypeProgress by lazy { findViewById<View>(R.id.account_type_progress) }
    private val paymentMethodsParent by lazy { findViewById<LinearLayout>(R.id.payment_methods_parent) }
    private val noPaymentMethodsView by lazy { findViewById<TextView>(R.id.no_payment_methods) }
    private val editPaymentMethods by lazy { findViewById<TextView>(R.id.edit_payment_methods) }

    private var viewModel: AccountTypeViewModel? = null

    override fun getLayoutId(): Int {
        return R.layout.activity_account_type
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        viewModel = ViewModelProvider(this).get(AccountTypeViewModel::class.java)
        val user = mUserManager.currentLegacyUser
        if (user != null) {
            if (!user.isPaidUser) {
                setAccountType(0)
            } else {
                setAndCheckOrganization()
            }
        }
        viewModel?.fetchPaymentMethodsResult?.observe(
            this, { result: FetchPaymentMethodsResult -> onPaymentMethods(result) })
        viewModel?.fetchPaymentMethods()
    }

    override fun onStart() {
        super.onStart()
        app.bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        app.bus.unregister(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            saveLastInteraction()
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setAndCheckOrganization() {
        val organization = ProtonMailApplication.getApplication().organization
        if (organization != null) {
            setOrganization(organization)
        } else {
            accountTypeProgress.visibility = View.VISIBLE
        }
        val getOrganizationJob = GetOrganizationJob()
        mJobManager.addJobInBackground(getOrganizationJob)
    }

    @Subscribe
    fun onOrganizationEvent(event: OrganizationEvent) {
        if (event.status == Status.SUCCESS) {
            val organization = event.response.organization
            organization?.let { setOrganization(it) }
        }
    }

    private fun onPaymentMethods(result: FetchPaymentMethodsResult) {
        Timber.v("Payment methods result:%s", result)
        if (result is FetchPaymentMethodsResult.Success) {
            val paymentMethods = result.paymentMethods
            showCardDetails(paymentMethods)
        }
    }

    private fun setOrganization(organization: Organization) {
        accountTypeProgress.visibility = View.GONE
        val planName = organization.planName
        val planType = fromString(planName)
        when {
            TextUtils.isEmpty(planName) -> {
                setAccountType(0)
            }
            planType === PlanType.PLUS -> {
                setAccountType(1)
            }
            planType === PlanType.VISIONARY -> {
                setAccountType(2)
            }
            planType === PlanType.PROFESSIONAL -> {
                setAccountType(3)
            }
        }
    }

    private fun setAccountType(type: Int) {
        if (type == 0) {
            upgrade.visibility = View.VISIBLE
            noPaymentMethodsView.visibility = View.VISIBLE
            styleEditPaymentMethod()
        } else {
            upgrade.visibility = View.GONE
            viewModel?.fetchPaymentMethods()
        }
        val accountTypeColor = resources.getIntArray(R.array.account_type_colors)[type]
        val accountName = resources.getStringArray(R.array.account_type_names)[type]
        val prefixValue = getString(R.string.protonmail)
        prefix.text = prefixValue
        accountTypeTitle.text = accountName
        accountTypeTitle.setTextColor(accountTypeColor)
    }

    private fun showCardDetails(paymentMethods: List<PaymentMethod>?) {
        if (paymentMethods != null) {
            accountTypeProgress.visibility = View.GONE
            if (paymentMethods.isEmpty()) {
                styleEditPaymentMethod()
                noPaymentMethodsView.visibility = View.VISIBLE
            } else {
                val inflater = LayoutInflater.from(this)
                paymentMethodsParent.removeAllViews()
                for (paymentMethod in paymentMethods) {
                    val cardDetails = paymentMethod.cardDetails
                    val cardTypeLayout = inflater.inflate(R.layout.layout_card, null)
                    val cardTitle = cardTypeLayout.findViewById<TextView>(R.id.card_title)
                    val cardSubtitle = cardTypeLayout.findViewById<TextView>(R.id.card_subtitle)
                    if (paymentMethod.cardDetails.billingAgreementId != null) {
                        cardTitle.text =
                            String.format(getString(R.string.payment_method_paypal_placeholder), cardDetails.payer)
                        cardSubtitle.text = ""
                    } else {
                        cardTitle.text =
                            String.format(getString(R.string.card_details_title), cardDetails.brand, cardDetails.last4)
                        cardSubtitle.text = String.format(
                            getString(R.string.card_details_subtitle),
                            cardDetails.expirationMonth + "/" + cardDetails.expirationYear
                        )
                    }
                    paymentMethodsParent.addView(cardTypeLayout)
                }
            }
        } else {
            accountTypeProgress.visibility = View.VISIBLE
        }
    }

    private fun styleEditPaymentMethod() {
        val editPaymentMethodsText = editPaymentMethods.text.toString()
        val editPaymentMethodsTextLowerCase = editPaymentMethodsText.toLowerCase()
        val startIndex = editPaymentMethodsTextLowerCase.indexOf("protonmail.com")
        val spannableString = SpannableString(editPaymentMethods.text)
        spannableString.setSpan(URLSpan("https://www.protonmail.com"), startIndex, startIndex + 14, 0)
        editPaymentMethods.movementMethod = LinkMovementMethod.getInstance()
        editPaymentMethods.text = spannableString
    }

    @OnClick(R.id.upgrade)
    fun onUpgrade() {
        showInfoDialog(
            this,
            "",
            getString(R.string.info_for_missing_functionality)
        ) { unit: Unit -> unit }
        /*
        Intent upgradeIntent = new Intent(this, UpsellingActivity.class);
        upgradeIntent.putExtra(UpsellingActivity.EXTRA_OPEN_UPGRADE_CONTAINER, true);
        startActivityForResult(AppUtil.decorInAppIntent(upgradeIntent), REQUEST_CODE_UPGRADE);
        TODO("startUpgradePlanWorkflow")
        */
    }
}
