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
package ch.protonmail.android.activities

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import butterknife.OnClick
import ch.protonmail.android.R
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.extensions.showToast

class DefaultAddressActivity : BaseActivity() {

    private var addresses: List<Address>? = null
    private var mInflater: LayoutInflater? = null
    private lateinit var mAllRadioButtons: MutableList<RadioButton>
    private var mAvailableAddressesMap: MutableMap<Address, RadioButton>? = null
    private var mSelectedAddress: Address? = null
    private var mUser: User? = null
    private var mCurrentSelectedRadioButton: RadioButton? = null

    private val addressChooser by lazy { findViewById<LinearLayout>(R.id.addressChooser) }
    private val defaultAddress by lazy { findViewById<TextView>(R.id.defaultAddress) }
    private val availableAddresses by lazy { findViewById<RadioGroup>(R.id.availableAddresses) }
    private val inactiveAddresses by lazy { findViewById<LinearLayout>(R.id.inactiveAddresses) }
    private val noAvailableAddresses by lazy { findViewById<TextView>(R.id.noAvailableAddresses) }
    private val noInactiveAddresses by lazy { findViewById<TextView>(R.id.noInactiveAddresses) }

    private val radioButtonClick = View.OnClickListener { v ->
        val selectedAddressRadioButton = v as RadioButton
        for ((key, radioButton) in mAvailableAddressesMap!!) {
            if (radioButton.id == selectedAddressRadioButton.id) {
                // this is selected
                if (MessageUtils.isPmMeAddress(key.email) && !mUser!!.isPaidUser) {
                    mCurrentSelectedRadioButton!!.isChecked = true
                    showToast(String.format(getString(R.string.pm_me_can_not_be_default), key.email))
                    return@OnClickListener
                }

                mSelectedAddress = key
                mCurrentSelectedRadioButton = selectedAddressRadioButton
                clearSelection()
                selectedAddressRadioButton.isChecked = true
                defaultAddress.text = mSelectedAddress!!.email

                val user = mUserManager.currentLegacyUser
                val selectedAddress = mSelectedAddress
                if (selectedAddress != null && user?.defaultAddressId != selectedAddress.id) {
                    // Add first the selected address.
                    val addressIds = mutableSetOf(selectedAddress.id)
                    // Add all other.
                    user?.addresses?.forEach { addressIds.add(it.id) }
                    //
                    val job = UpdateSettingsJob(addressIds = addressIds.toList())
                    mJobManager.addJobInBackground(job)
                }

                break
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        actionBar?.elevation = elevation

        mAvailableAddressesMap = HashMap()
        mAllRadioButtons = ArrayList()
        mUser = mUserManager.currentLegacyUser
        addresses = ArrayList(mUser!!.addresses)
        mInflater = LayoutInflater.from(this)
        renderAddresses()
    }

    override fun onStart() {
        super.onStart()
        ProtonMailApplication.getApplication().bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        ProtonMailApplication.getApplication().bus.unregister(this)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        if (itemId == android.R.id.home) {
            saveAndFinish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        saveAndFinish()
    }

    override fun getLayoutId() = R.layout.activity_default_address

    @OnClick(R.id.defaultAddress)
    fun onDefaultAddressClicked() {
        val icon = ResourcesCompat.getDrawable(
            resources, if (!addressChooser.isVisible) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up, null
        )?.mutate()

        defaultAddress.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, icon, null)
        addressChooser.isVisible = !addressChooser.isVisible
    }

    private fun clearSelection() {
        availableAddresses.clearCheck()

        for (radioButton in mAllRadioButtons) {
            radioButton.isChecked = false
        }
    }

    private fun renderAddresses() {
        val sortedAddresses = addresses.orEmpty()
            .sortedBy { it.order }
            .sortedBy { it.status == 1 && it.receive == 1 }

        mSelectedAddress = sortedAddresses[0]
        defaultAddress.text = mSelectedAddress!!.email
        var mNoAvailableAddresses = true
        var mNoInactiveAddresses = true

        sortedAddresses.forEachIndexed { index, address ->
            val aliasAvailable = address.status == 1 && address.receive == 1
            var addressRadio: RadioButton? = null
            var inactiveAddress: TextView? = null
            if (aliasAvailable) {
                addressRadio = mInflater!!.inflate(R.layout.radio_button_list_item, availableAddresses, false) as RadioButton
                addressRadio.text = address.email
                addressRadio.isChecked = index == 0
                if (index == 0) {
                    mCurrentSelectedRadioButton = addressRadio
                }
                mAllRadioButtons.add(addressRadio)
                addressRadio.setOnClickListener(radioButtonClick)
                addressRadio.id = View.generateViewId()

                mAvailableAddressesMap!![address] = addressRadio
            } else {
                inactiveAddress =
                    mInflater!!.inflate(R.layout.alias_list_item_inactive, inactiveAddresses, false) as TextView
                inactiveAddress.text = address.email
            }

            if (aliasAvailable) {
                mNoAvailableAddresses = false
                availableAddresses.addView(addressRadio)
            } else {
                mNoInactiveAddresses = false
                inactiveAddresses!!.addView(inactiveAddress)
            }
        }
        if (mNoAvailableAddresses) {
            noAvailableAddresses.visibility = View.VISIBLE
        }
        if (mNoInactiveAddresses) {
            noInactiveAddresses.visibility = View.VISIBLE
        }
    }

    private fun saveAndFinish() {
        setResult(Activity.RESULT_OK)
        finish()
    }
}
