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
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.widget.SwitchCompat
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.doOnPreDraw
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.settings_item_layout.view.*
import me.proton.core.util.kotlin.EMPTY_STRING

// region constants
private const val TYPE_INFO = 0
private const val TYPE_DRILL_DOWN = 1
private const val TYPE_BUTTON = 2
private const val TYPE_TOGGLE = 3
private const val TYPE_SPINNER = 4
// endregion

class SettingsDefaultItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private var mAttrs: AttributeSet? = attrs
    private var mHeading: CharSequence? = EMPTY_STRING
    private var mValue: CharSequence? = EMPTY_STRING
    private var mHint: CharSequence? = EMPTY_STRING
    private var mHasValue: Boolean = true
    private var mType: Int = 0
    private var mDisabled: Boolean = false
    private var mDescription: String = EMPTY_STRING

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

    fun setIconVisibility(visibility: Int) {
        icon.visibility = visibility
    }

    fun getSpinner(): View = timeoutSpinner

    fun getToggle(): SwitchCompat = actionSwitch

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

    fun setItemType(type: Int) {
        mType = type
        when (mType) {
            TYPE_INFO -> {
                isEnabled = false
                buttonsContainer.visibility = View.GONE
            }
            TYPE_DRILL_DOWN -> {
                clearCacheButton.visibility = View.GONE
                actionSwitch.visibility = View.GONE
            }
            TYPE_BUTTON -> {
                isEnabled = false
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
                buttonsContainer.visibility = View.GONE
                actionSwitch.visibility = View.VISIBLE

            }
            TYPE_SPINNER -> {
                clearCacheButton.visibility = View.GONE
                actionSwitch.visibility = View.GONE
                valueText.visibility = View.GONE
                timeoutSpinner.visibility = View.VISIBLE

                setConstraints(buttonsContainer, true, false, false, false, guideline.id)
                setConstraints(headingContainer, false, true, false, false, guideline.id)
                buttonsContainer.gravity = Gravity.CENTER_VERTICAL
            }
        }
    }

    private fun setItemTag(tag: CharSequence?) {
        this.tag = tag?.toString()
    }

    private fun setConstraints(
        view: View,
        startToStart: Boolean,
        endToStart: Boolean,
        endToEnd: Boolean,
        bottomToBottom: Boolean,
        viewId: Int
    ) {
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
