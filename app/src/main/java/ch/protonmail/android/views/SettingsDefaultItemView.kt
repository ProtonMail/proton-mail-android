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
package ch.protonmail.android.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnPreDraw
import androidx.core.widget.doAfterTextChanged
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.settings_item_layout.view.*

// region constants
private const val TYPE_INFO = 0
private const val TYPE_DRILL_DOWN = 1
private const val TYPE_BUTTON = 2
private const val TYPE_TOGGLE = 3
private const val TYPE_SPINNER = 4
private const val TYPE_EDIT_TEXT = 5
private const val TYPE_TOGGLE_N_EDIT = 6
// endregion

class SettingsDefaultItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var mAttrs: AttributeSet? = attrs
    private var mHeading: CharSequence? = ""
    private var mValue: CharSequence? = ""
    private var mHint: CharSequence? = ""
    private var mHasValue: Boolean = true
    private var mType: Int = 0
    private var mDisabled: Boolean = false
    private var mDescription: String = ""

    init {
        LayoutInflater.from(context).inflate(R.layout.settings_item_layout, this, true)

        mAttrs?.let {

            context.theme.obtainStyledAttributes(it, R.styleable.SettingsDefaultItemView, 0, 0).apply {

                try {
                    mHeading = getText(R.styleable.SettingsDefaultItemView_settingsHeading)
                    mValue = getText(R.styleable.SettingsDefaultItemView_settingsValue)
                    mHasValue = getBoolean(R.styleable.SettingsDefaultItemView_hasValue, false)
                    mType = getInteger(R.styleable.SettingsDefaultItemView_settingType, 0)
                    mHint = getText(R.styleable.SettingsDefaultItemView_settingsHint)
                } finally {
                    recycle()
                }
            }
        }

        doOnPreDraw {
            setSettingHeading(mHeading.toString())
            setHasValue(mHasValue)
            setItemType(mType)
            setItemTag(mHeading)
        }
    }

    fun setSettingHeading(heading: String?) {
        mHeading = heading
        headingText.text = heading
    }

    fun setSettingValue(value: String?) {
        mValue = value
        valueText.text = value
    }

    fun setEditableValue(value: String?) {
        mValue = value
        editText.setText(value)
    }

    fun setSettingHint(value: String?) {
        editText.hint = value
    }

    fun setHasValue(hasValue: Boolean) {
        mHasValue = hasValue
        if (mHasValue) {
            valueText.visibility = View.VISIBLE
            setSettingValue(mValue.toString())
        } else {
            valueText.visibility = View.GONE
            setSettingValue("")

        }
    }

    fun getSpinner(): View {
        return timeoutSpinner
    }

    fun getToggle(): SwitchCompat {
        return actionSwitch
    }

    fun checkToggle(value: Boolean) {
        actionSwitch.isChecked = value
    }

    fun setSettingDisabled(value: Boolean, description: String?) {
        mDisabled = value
        if (mDisabled) {
            isEnabled = false
            alpha = 0.5f
            isClickable = false
            this.forEachChildView {
                it.isClickable = false
                it.isFocusable = false
            }

            valueText.visibility = View.VISIBLE
            mDescription = description ?: ""
            valueText.text = mDescription
        }
    }


    fun setToggleChangedListener(listener: ((View, Boolean) -> Unit)?) {
        if (listener != null) {
            actionSwitch.setOnCheckedChangeListener { view, isChecked ->
                listener.invoke(view, isChecked)
            }
        } else {
            actionSwitch.setOnCheckedChangeListener(null)
        }
    }

    fun setEditTextOnFocusChangeListener(listener: ((View) -> Unit)?) {
        editText.setOnFocusChangeListener { view, hasFocus ->
            if (!hasFocus)
                listener?.invoke(view)
        }
    }

    fun setEditTextOnTextChangeListener(listener: ((String) -> Unit)?) {
        editText.doAfterTextChanged {
            listener?.invoke(it.toString())
        }
    }

    fun setItemType(type: Int) {
        mType = type
        when (mType) {
            TYPE_INFO -> {
                isEnabled = false
                buttonsContainer.visibility = View.GONE
            }
            TYPE_DRILL_DOWN -> {
                openArrow.visibility = View.VISIBLE
                clearCacheButton.visibility = View.GONE
                actionSwitch.visibility = View.GONE

                setConstraints(buttonsContainer, true, false, false, false, guideline.id)
                setConstraints(headingContainer, false, true, false, false, guideline.id)
                buttonsContainer.gravity = Gravity.CENTER_VERTICAL
            }
            TYPE_BUTTON -> {
                isEnabled = false
                openArrow.visibility = View.GONE
                clearCacheButton.visibility = View.VISIBLE
                actionSwitch.visibility = View.GONE

                setConstraints(buttonsContainer, true, false, false, false, guideline_02.id)
                setConstraints(headingContainer, false, true, false, false, guideline_02.id)
                buttonsContainer.gravity = Gravity.CENTER_VERTICAL

                clearCacheButton.setOnClickListener {
                    callOnClick()
                }
            }
            TYPE_TOGGLE -> {
                openArrow.visibility = View.GONE
                clearCacheButton.visibility = View.GONE
                actionSwitch.visibility = View.VISIBLE

                setConstraints(buttonsContainer, true, false, false, true, guideline_01.id)
                setConstraints(headingContainer, false, true, false, false, guideline_01.id)
                buttonsContainer.gravity = Gravity.TOP
            }
            TYPE_SPINNER -> {
                openArrow.visibility = View.VISIBLE
                clearCacheButton.visibility = View.GONE
                actionSwitch.visibility = View.GONE
                valueText.visibility = View.GONE
                timeoutSpinner.visibility = View.VISIBLE

                setConstraints(buttonsContainer, true, false, false, false, guideline.id)
                setConstraints(headingContainer, false, true, false, false, guideline.id)
                buttonsContainer.gravity = Gravity.CENTER_VERTICAL
            }
            TYPE_EDIT_TEXT -> {
                openArrow.visibility = View.GONE
                clearCacheButton.visibility = View.GONE
                actionSwitch.visibility = View.GONE
                if (!mDisabled)
                    valueText.visibility = View.GONE
                timeoutSpinner.visibility = View.GONE
                editText.visibility = View.VISIBLE
                editText.minLines = 1

                setConstraints(buttonsContainer, true, false, false, true, guideline_01.id)
                setConstraints(headingContainer, false, false, true, false, buttonsContainer.id)

                buttonsContainer.gravity = Gravity.TOP
            }
            TYPE_TOGGLE_N_EDIT -> {
                openArrow.visibility = View.GONE
                clearCacheButton.visibility = View.GONE
                actionSwitch.visibility = View.VISIBLE
                timeoutSpinner.visibility = View.GONE
                editText.visibility = View.VISIBLE
                editText.minLines = 8
                editText.maxLines = 8
                editText.setOnTouchListener { v, event ->
                    if (v.id == R.id.editText) {
                        v.parent.requestDisallowInterceptTouchEvent(true)
                        when (event.action and MotionEvent.ACTION_MASK) {
                            MotionEvent.ACTION_UP -> {
                                v.parent.requestDisallowInterceptTouchEvent(false)
                                v.performClick()
                            }
                        }
                    }
                    false
                }
                if (!mDisabled) {
                    valueText.visibility = View.GONE
                } else {
                    setSettingDisabled(mDisabled, mDescription)
                }

                setConstraints(buttonsContainer, true, false, false, true, guideline_01.id)
                setConstraints(headingContainer, false, false, true, false, buttonsContainer.id)

                buttonsContainer.gravity = Gravity.TOP
            }
        }
    }

    private fun setItemTag(tag: CharSequence?) {
        this.tag = tag?.toString()
    }

    private fun setConstraints(view: View, startToStart: Boolean, endToStart: Boolean, endToEnd: Boolean, bottomToBottom: Boolean, viewId: Int) {
        val constraintSet = ConstraintSet()
        constraintSet.clone(settingsItemWrapper)

        if (startToStart)
            constraintSet.connect(view.id, ConstraintSet.START, viewId, ConstraintSet.START, 0)

        if (endToStart)
            constraintSet.connect(view.id, ConstraintSet.END, viewId, ConstraintSet.START, 0)

        if (endToEnd)
            constraintSet.connect(view.id, ConstraintSet.END, viewId, ConstraintSet.END, 0)

        if (bottomToBottom)
            constraintSet.connect(view.id, ConstraintSet.BOTTOM, view.id, ConstraintSet.BOTTOM, 0)

        constraintSet.applyTo(settingsItemWrapper)
    }

    private fun View.forEachChildView(closure: (View) -> Unit) {
        closure(this)
        val groupView = this as? ViewGroup ?: return
        val size = groupView.childCount - 1
        for (i in 0..size) {
            groupView.getChildAt(i).forEachChildView(closure)
        }
    }
}
