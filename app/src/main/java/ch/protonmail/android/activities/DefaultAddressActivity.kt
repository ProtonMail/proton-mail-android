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
import android.widget.RadioButton
import android.widget.TextView
import butterknife.OnClick
import ch.protonmail.android.R
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.jobs.UpdateSettingsJob
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.extensions.showToast
import kotlinx.android.synthetic.main.activity_default_address.*
import java.util.Collections

/**
 * Created by dkadrikj on 10/23/16.
 */

class DefaultAddressActivity : BaseActivity() {

    private var addresses: List<Address>? = null
    private var mInflater: LayoutInflater? = null
    private lateinit var mAllRadioButtons: MutableList<RadioButton>
    private var mAvailableAddressesMap: MutableMap<Address, RadioButton>? = null
    private var mSelectedAddress: Address? = null
    private var mUser: User? = null
    private var mChooserExpanded: Boolean = false
    private var mCurrentSelectedRadioButton: RadioButton? = null

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
        mAvailableAddressesMap = HashMap()
        mAllRadioButtons = ArrayList()
        mUser = mUserManager.currentLegacyUser
        addresses = ArrayList(mUser!!.addresses)
        mInflater = LayoutInflater.from(this)
        renderAddresses()
        mChooserExpanded = false
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

    override fun getLayoutId(): Int {
        return R.layout.activity_default_address
    }

    @OnClick(R.id.defaultAddress, R.id.defaultAddressArrow)
    fun onDefaultAddressClicked() {
        mChooserExpanded = !mChooserExpanded
        addressChooser.visibility = if (mChooserExpanded) View.VISIBLE else View.GONE
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
                addressRadio = mInflater!!.inflate(R.layout.alias_list_item, availableAddresses, false) as RadioButton
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
                inactiveAddress = mInflater!!.inflate(R.layout.alias_list_item_inactive, inactiveAddresses, false) as TextView
                inactiveAddress.text = address.email
            }

            if (aliasAvailable) {
                val divider = mInflater!!.inflate(R.layout.horizontal_divider, availableAddresses, false)
                mNoAvailableAddresses = false
                availableAddresses.addView(addressRadio)
                availableAddresses.addView(divider)
            } else {
                val divider = mInflater!!.inflate(R.layout.horizontal_divider, inactiveAddresses, false)
                mNoInactiveAddresses = false
                inactiveAddresses!!.addView(inactiveAddress)
                inactiveAddresses!!.addView(divider)
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
        saveLastInteraction()
        finish()
    }
}
