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
import android.widget.RelativeLayout
import ch.protonmail.android.R
import kotlinx.android.synthetic.main.drawer_header.view.*

class DrawerHeaderView : RelativeLayout {

    private var mDrawerHeaderListener: IDrawerHeaderListener? = null
    var state: State = State.CLOSED

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mDrawerHeaderListener = context as? IDrawerHeaderListener
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onFinishInflate() {
        super.onFinishInflate()
        buttonQuickSnooze.setOnClickListener { onQuickSnoozeClicked() }
        activeUserDetails.setOnClickListener { onUserClicked() }
        buttonExpand.setOnClickListener { onExpandClicked() }
    }

    fun setUser(name: String, emailAddress: String) {
        userName!!.text = name
        userEmailAddress!!.text = emailAddress
    }

    fun switchState() {
        state = state.switch()
        buttonExpand.setImageResource(if (state == State.OPENED) {
            R.drawable.ic_collapse
        } else {
            R.drawable.ic_expand
        })
    }

    private fun onQuickSnoozeClicked() {
        mDrawerHeaderListener?.onQuickSnoozeClicked()
    }

    private fun onExpandClicked() {
        handleUserClicks()
    }

    private fun onUserClicked() {
        handleUserClicks()
    }

    fun refresh(snoozeIsOn: Boolean) {
        buttonQuickSnooze!!.setImageResource(if (snoozeIsOn) R.drawable.ic_notifications_off else R.drawable.ic_notifications_active)
    }

    private fun handleUserClicks() {
        mDrawerHeaderListener?.onUserClicked(state != State.OPENED)
        state = state.switch()
        buttonExpand.setImageResource(if (state == State.OPENED) R.drawable.ic_collapse else R.drawable.ic_expand)
    }

    interface IDrawerHeaderListener {
        fun onQuickSnoozeClicked()
        fun onUserClicked(open: Boolean)
    }

    enum class State {
        CLOSED {
            override fun switch(): State = OPENED
        }, OPENED {
            override fun switch(): State = CLOSED
        };

        abstract fun switch(): State
    }
}
