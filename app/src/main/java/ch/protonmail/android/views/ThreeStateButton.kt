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
import android.view.MotionEvent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.core.content.res.ResourcesCompat
import ch.protonmail.android.R

class ThreeStateButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var numberOfStates = 3

    var state = 0
        set(value) {
            field = value % numberOfStates
            when (field) {
                0 -> setButtonBackground(R.drawable.mail_check)
                1 -> setButtonBackground(R.drawable.mail_check_active)
                2 -> setButtonBackground(R.drawable.mail_check_neutral)
                else -> {
                    throw IllegalStateException("Unsupported view state")
                }
            }
            onStateChangedListener?.onClick(this)
        }

    private var onStateChangedListener: OnClickListener? = null

    init {
        isClickable = true
        isFocusable = true
        state = STATE_UNPRESSED
        setButtonBackground(R.drawable.mail_check)
    }

    private fun setButtonBackground(@DrawableRes backgroundDrawableId: Int) {
        background = ResourcesCompat.getDrawable(resources, backgroundDrawableId, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (event.action == MotionEvent.ACTION_DOWN) {
            state++
            performClick()
            true
        } else
            false
    }

    fun setOnStateChangedListener(listener: OnClickListener?) {
        onStateChangedListener = listener
    }

    override fun isPressed(): Boolean = state == STATE_PRESSED

    companion object {
        const val STATE_UNPRESSED = 0
        const val STATE_CHECKED = 1
        const val STATE_PRESSED = 2
    }
}
