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
package ch.protonmail.android.settings.pin

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.biometric.BiometricManager
import butterknife.OnClick
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.views.SettingsDefaultItemView
import kotlinx.android.synthetic.main.activity_pin_settings.*

// region constants
private const val REQUEST_CODE_SETUP_PIN = 9
private const val REQUEST_CODE_CHANGE_PIN = 14
// endregion

@SuppressLint("ServiceCast")
class PinSettingsActivity : BaseActivity() {

    private val autoLockContainer by lazy { findViewById<SettingsDefaultItemView>(R.id.autoLockContainer) }
    private val autoLockContainerToggle by lazy { autoLockContainer.getToggle() }
    private val autoLockTimer by lazy { findViewById<SettingsDefaultItemView>(R.id.autoLockTimer) }
    private val autoLockTimerSpinner by lazy { autoLockTimer.getSpinner() as Spinner }
    private val useFingerprint by lazy { findViewById<SettingsDefaultItemView>(R.id.useFingerprint) }
    private val useFingerprintToggle by lazy { useFingerprint.getToggle() }
    private val autoLockOtherSettingsContainer by lazy {
        findViewById<LinearLayout>(
            R.id.autoLockOtherSettingsContainer
        )
    }
    private var mPinTimeoutValue: Int = 0
    private val user by lazy { mUserManager.requireCurrentLegacyUser() }

    private val mBiometricManager by lazy {
        BiometricManager.from(this@PinSettingsActivity)
    }
    private val useFingerprintCheckListener: (CompoundButton, Boolean) -> Unit = { _, _ -> saveCurrentSettings(null) }
    override fun getLayoutId(): Int = R.layout.activity_pin_settings
    private val usePinCheckListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        if (isChecked) {
            val oldPin = mUserManager.getMailboxPin()
            if (TextUtils.isEmpty(oldPin)) {
                autoLockContainerToggle.setOnCheckedChangeListener(null)
                val pinIntent =
                    AppUtil.decorInAppIntent(Intent(this@PinSettingsActivity, CreatePinActivity::class.java))
                startActivityForResult(pinIntent, REQUEST_CODE_SETUP_PIN)
                autoLockContainerToggle.isChecked = false
            } else {
                changeItemsEnabledState(true)
                mPinTimeoutValue = user.autoLockPINPeriod
                autoLockTimerSpinner.setSelection(mPinTimeoutValue)
                saveCurrentSettings(oldPin)
            }
        } else {
            handlePinSwitchOff()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val elevation = resources.getDimension(R.dimen.action_bar_elevation)
        supportActionBar?.elevation = elevation

        val timeoutAdapter = ArrayAdapter(
            this, R.layout.timeout_spinner_item, resources.getStringArray(R.array.auto_logout_options_array)
        )
        timeoutAdapter.setDropDownViewResource(R.layout.timeout_spinner_item_dropdown)
        autoLockTimerSpinner.adapter = timeoutAdapter
        isBiometricHardwareDetected()
        autoLockContainerToggle.isChecked = user.isUsePin && !TextUtils.isEmpty(mUserManager.getMailboxPin())
        autoLockContainerToggle.setOnCheckedChangeListener(usePinCheckListener)
        useFingerprintToggle.isChecked = user.isUseFingerprint
        useFingerprintToggle.setOnCheckedChangeListener(useFingerprintCheckListener)
        if (user.isUsePin && !TextUtils.isEmpty(mUserManager.getMailboxPin())) {
            autoLockTimerSpinner.setSelection(user.autoLockPINPeriod)
            changeItemsEnabledState(true)
        } else {
            changeItemsEnabledState(false)
        }
        autoLockTimerSpinner.onItemSelectedListener = autoLogoutListener
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

    @OnClick(R.id.changePinCode)
    fun onChangePinClicked() {
        if (autoLockContainerToggle.isChecked) {
            autoLockContainerToggle.setOnCheckedChangeListener(null)
            val changePinIntent =
                AppUtil.decorInAppIntent(Intent(this@PinSettingsActivity, ChangePinActivity::class.java))
            startActivityForResult(changePinIntent, REQUEST_CODE_CHANGE_PIN)
        } else {
            showToast(R.string.pin_not_activated, Toast.LENGTH_SHORT)
        }
    }

    @OnClick(R.id.autoLockTimer)
    fun onChangePinTimeoutClicked() {
        if (autoLockContainerToggle.isChecked) {
            changeItemsEnabledState(true)
            autoLockTimerSpinner.performClick()
        } else {
            showToast(R.string.pin_not_activated, Toast.LENGTH_SHORT)
        }
    }

    private fun handlePinSwitchOff() {
        if (user.isUsePin) {
            changeItemsEnabledState(false)
            autoLockContainerToggle.isChecked = false
            saveCurrentSettings(null)
        }
    }

    private val autoLogoutListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
            mPinTimeoutValue = position
            autoLockTimerSpinner.setSelection(position)
            saveCurrentSettings(null)
        }

        override fun onNothingSelected(parent: AdapterView<*>) {
            // noop
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_CODE_SETUP_PIN) {
                val pinSet = data?.getBooleanExtra(EXTRA_PIN_SET, false) ?: false
                val newPin = data?.getStringExtra(EXTRA_PIN)
                if (pinSet) {
                    autoLockContainerToggle.isChecked = true
                    changeItemsEnabledState(true)
                    mPinTimeoutValue = 2
                    autoLockTimerSpinner.setSelection(mPinTimeoutValue)
                    saveCurrentSettings(newPin)
                    isBiometricHardwareDetected()
                }
                autoLockContainerToggle.setOnCheckedChangeListener(usePinCheckListener)
            } else if (requestCode == REQUEST_CODE_CHANGE_PIN) {
                val pinSet = data?.getBooleanExtra(EXTRA_NEW_PIN_SET, false)
                val newPin = data?.getStringExtra(EXTRA_PIN)
                if (pinSet == true) {
                    mUserManager.savePin(newPin)
                    showToast(R.string.new_pin_saved, Toast.LENGTH_SHORT)
                    autoLockContainerToggle.isChecked = true
                    saveCurrentSettings(newPin)
                }
                autoLockContainerToggle.setOnCheckedChangeListener(usePinCheckListener)
            } else if (requestCode == REQUEST_CODE_VALIDATE_PIN) {
                val pinValid = data!!.getBooleanExtra(EXTRA_PIN_VALID, false)
                if (pinValid) {
                    autoLockContainerToggle.isChecked =
                        user.isUsePin && !TextUtils.isEmpty(mUserManager.getMailboxPin())
                    autoLockContainerToggle.setOnCheckedChangeListener(usePinCheckListener)
                    if (autoLockContainerToggle.isChecked) {
                        changeItemsEnabledState(true)
                    } else {
                        changeItemsEnabledState(false)
                    }
                } else {
                    super.onActivityResult(requestCode, resultCode, data)
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            autoLockContainerToggle.setOnCheckedChangeListener(usePinCheckListener)
            super.onActivityResult(requestCode, resultCode, data)
        } else {
            autoLockContainerToggle.setOnCheckedChangeListener(usePinCheckListener)
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("NewApi")
    private fun saveCurrentSettings(mNewPin: String?) {
        val usePinChanged = autoLockContainerToggle.isChecked != user.isUsePin
        val useFingerprintChanged = useFingerprintToggle.isChecked != user.isUseFingerprint
        if (usePinChanged) {
            user.isUsePin = autoLockContainerToggle.isChecked
            if (user.isUsePin) {
                mUserManager.savePin(mNewPin)
                changeItemsEnabledState(true)
            } else {
                changeItemsEnabledState(false)
            }
        }
        if (useFingerprintChanged) {
            if (useFingerprintToggle.isChecked) {
                mBiometricManager.let {
                    if (it.canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
                        showToast(getString(R.string.no_biometric_data_enrolled), Toast.LENGTH_SHORT)
                        useFingerprintToggle.isChecked = false
                    }
                }
            }
            user.isUseFingerprint = useFingerprintToggle.isChecked
        }
        user.autoLockPINPeriod = mPinTimeoutValue
        val pinChanged = !TextUtils.isEmpty(mNewPin) && mNewPin != mUserManager.getMailboxPin()
        if (pinChanged) {
            mUserManager.savePin(mNewPin)
        }
    }

    private fun saveAndFinish() {
        setResult(RESULT_OK)
        saveLastInteraction()
        finish()
    }

    @SuppressLint("NewApi")
    private fun changeItemsEnabledState(enable: Boolean) {
        if (enable) {
            autoLockOtherSettingsContainer.visibility = View.VISIBLE
            autoLockTimerSpinner.visibility = View.VISIBLE
            mBiometricManager.let {
                changePinCode.foreground = null
                autoLockTimer.foreground = null
                useFingerprint.foreground = null
            }
            changePinCode.isClickable = true
            autoLockTimer.isClickable = true
            useFingerprint.isClickable = true
            useFingerprintToggle.isEnabled = true
        } else {
            autoLockTimerSpinner.visibility = View.GONE
            changePinCode.foreground = ColorDrawable(getColor(R.color.white_30))
            autoLockTimer.foreground = ColorDrawable(getColor(R.color.white_30))
            useFingerprint.foreground = ColorDrawable(getColor(R.color.white_30))
            changePinCode.isClickable = false
            autoLockTimer.isClickable = false
            useFingerprint.isClickable = false
            useFingerprintToggle.isEnabled = false
            useFingerprintToggle.isChecked = false
        }
    }

    private fun isBiometricHardwareDetected() {
        mBiometricManager.let {
            if (
                it.canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ||
                it.canAuthenticate() == BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE
            ) {
                useFingerprint.setHasValue(true)
                useFingerprint.foreground = ColorDrawable(getColor(R.color.white_30))
                useFingerprint.isClickable = false
                useFingerprintToggle.isChecked = false
                useFingerprintToggle.isClickable = false
                useFingerprintToggle.isEnabled = false
            }
        }
    }
}
