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
package ch.protonmail.android.views.behavior

import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.ViewCompat
import android.view.View
import com.github.clans.fab.FloatingActionMenu



class ScrollAwareFABBehavior : CoordinatorLayout.Behavior<FloatingActionMenu>() {

    val TAG = "ScrollAwareFABBehavior"

    override fun onStartNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: FloatingActionMenu,
            directTargetChild: View,
            target: View,
            nestedScrollAxes: Int,
            type: Int
    ): Boolean {
        var ret = false
        ret = if (nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL) {
            true
        } else {
            super.onStartNestedScroll(
                coordinatorLayout,
                child,
                directTargetChild,
                target,
                nestedScrollAxes
            )
        }

        return ret
    }

    override fun onNestedScroll(
            coordinatorLayout: CoordinatorLayout,
            child: FloatingActionMenu,
            target: View,
            dxConsumed: Int,
            dyConsumed: Int,
            dxUnconsumed: Int,
            dyUnconsumed: Int,
            type: Int
    ) {
        super.onNestedScroll(
            coordinatorLayout,
            child,
            target,
            dxConsumed,
            dyConsumed,
            dxUnconsumed,
            dyUnconsumed,
            type
        )

        if (dyConsumed>0 || dyUnconsumed > 0) {
            if (child.visibility == View.VISIBLE) {
                child.visibility = View.INVISIBLE
            }
        } else if ((dyConsumed < 0 || dyUnconsumed < 0)&& child.visibility != View.VISIBLE) {
            child.visibility = View.VISIBLE
        }
    }
}