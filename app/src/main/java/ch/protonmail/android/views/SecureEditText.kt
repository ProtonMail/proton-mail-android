/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.views

import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Vibrator
import android.text.TextUtils
import android.text.method.PasswordTransformationMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.settings.pin.PinAction
import com.google.android.material.textfield.TextInputEditText
import java.lang.ref.WeakReference

class SecureEditText @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    LinearLayout(context, attrs, defStyleAttr) {

    private val inputField by lazy { findViewById<TextInputEditText>(R.id.pin_input) }
    private val attempts by lazy { findViewById<TextView>(R.id.attempts) }

    private var mActionType = PinAction.VALIDATE
    private val mListener: ISecurePINListener = (context as ContextWrapper).baseContext as ISecurePINListener

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    val pin: String
        get() = inputField.text.toString()

    fun isValid(wantedPin: String?): Boolean {
        val pin: String = inputField.text.toString()
        var isValid = false
        if (!TextUtils.isEmpty(pin) && pin.length >= 4) {
            isValid = true
        }
        return isValid && pin == wantedPin
    }

    val isValid: Boolean
        get() {
            val pin: String = inputField.text.toString()
            var isValid = false
            if (!TextUtils.isEmpty(pin) && pin.length >= 4) {
                isValid = true
            }
            return isValid
        }

    fun setActionType(actionType: PinAction) {
        mActionType = actionType
    }

    fun enterKey(keyValue: String?) {
        val currentValue: StringBuilder = StringBuilder(inputField.text.toString())
        if (!TextUtils.isEmpty(currentValue) && currentValue.length >= 4) {
            val mVibrator =
                ProtonMailApplication.getApplication().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            mVibrator.vibrate(150)
            return
        }
        currentValue.append(keyValue)
        inputField.setText(currentValue.toString())
        inputField.setSelection(currentValue.length)
        if (currentValue.length >= 4) {
            when (mActionType) {
                PinAction.CREATE -> if (!TextUtils.isEmpty(currentValue) && currentValue.length >= 4) {
                    mListener.onPinMaxDigitReached()
                }
                PinAction.CONFIRM -> {
                }
                PinAction.VALIDATE -> {
                    val pin = ProtonMailApplication.getApplication().userManager.getMailboxPin()
                    val userManager = ProtonMailApplication.getApplication().userManager
                    if (pin != null && pin == currentValue.toString()) {
                        mListener.onPinSuccess()
                        ProtonMailApplication.getApplication().userManager.resetPinAttempts()
                        attempts.text = ""
                    } else if (currentValue.length == 4) {
                        userManager.increaseIncorrectPinAttempt()
                        handlePinErrorUI(userManager)
                        mListener.onPinError()
                        val mVibrator = ProtonMailApplication.getApplication()
                            .getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        mVibrator.vibrate(450)
                        mEditTextHandler.postDelayed(ClearTextRunnable(this), 350)
                    }
                }
            }
        }
    }

    private fun handlePinErrorUI(userManager: UserManager) {
        val currentAttempts = userManager.incorrectPinAttempts
        val remainingAttempts = Constants.MAX_INCORRECT_PIN_ATTEMPTS - currentAttempts
        if (remainingAttempts == Constants.MAX_INCORRECT_PIN_ATTEMPTS) {
            return
        }
        inputField.setTextColor(resources.getColor(R.color.notification_error))
        if (currentAttempts >= Constants.MAX_INCORRECT_PIN_ATTEMPTS - 3) {
            attempts.text = resources.getQuantityString(
                R.plurals.incorrect_pin_remaining_attempts_wipe, remainingAttempts, remainingAttempts
            )
        } else {
            attempts.text =
                resources.getQuantityString(
                    R.plurals.incorrect_pin_remaining_attempts, remainingAttempts, remainingAttempts
                )
        }
    }

    private fun resetContent() {
        inputField.setText("")
    }

    fun setText(string: String) {
        inputField.setText(string)
    }

    fun setSelection(length: Int) {
        inputField.setSelection(length)
    }

    fun setTextColor(color: Int) {
        inputField.setTextColor(color)
    }

    fun setErrorText(string: String) {
        attempts.text = string
    }

    private class EditTextHandler : Handler() { // non leaky handler
    }

    private val mEditTextHandler = EditTextHandler()

    private class ClearTextRunnable internal constructor(secureEditText: SecureEditText) : Runnable {

        // non leaky runnable
        private val secureEditTextWeakReference: WeakReference<SecureEditText> = WeakReference(secureEditText)
        override fun run() {
            val secureEditText = secureEditTextWeakReference.get()
            secureEditText?.resetContent()
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.secure_edit_view, this, true)
        handlePinErrorUI(ProtonMailApplication.getApplication().userManager)
        inputField.transformationMethod = BiggerDotPasswordTransformationMethod
    }
}

/**
 * A transformation to increase the size of the dots displayed in the text.
 */
private object BiggerDotPasswordTransformationMethod : PasswordTransformationMethod() {

    override fun getTransformation(source: CharSequence, view: View): CharSequence {
        return PasswordCharSequence(super.getTransformation(source, view))
    }

    private class PasswordCharSequence(
        val transformation: CharSequence
    ) : CharSequence by transformation {

        override fun get(index: Int): Char = if (transformation[index] == DOT) {
            BIGGER_DOT
        } else {
            transformation[index]
        }
    }

    private const val DOT = '\u2022'
    private const val BIGGER_DOT = '●'
}

interface ISecurePINListener {

    fun onPinSuccess()
    fun onPinError()
    fun onPinMaxDigitReached()
}
