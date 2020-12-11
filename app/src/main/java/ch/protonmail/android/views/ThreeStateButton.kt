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
import android.view.KeyEvent
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.res.ResourcesCompat
import ch.protonmail.android.R

class ThreeStateButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatButton(context, attrs, defStyleAttr) {

    var numberOfStates = 3

    var state = 0
        set(value) {
            field = value
            when (state) {
                0 -> setButtonBackground(R.drawable.mail_check)
                1 -> setButtonBackground(R.drawable.mail_check_active)
                2 -> setButtonBackground(R.drawable.mail_check_neutral)
                else -> {
                    throw IllegalStateException("Unsupported view state")
                }
            }
        }

    private var onStateChangedListener: OnClickListener? = null

    init {
        state = STATE_UNPRESSED
        setButtonBackground(R.drawable.mail_check)
        setOnClickListener { nextState() }
    }

    private fun nextState() {
        state++
        state %= numberOfStates
        // forces to redraw the view
        onStateChangedListener?.onClick(this)
    }

    private fun setButtonBackground(@DrawableRes backgroundDrawableId: Int) {
        background = ResourcesCompat.getDrawable(resources, backgroundDrawableId, null)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && event.action == KeyEvent.ACTION_UP) {
            nextState()
            this.isPressed = false
        }
        return false
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
