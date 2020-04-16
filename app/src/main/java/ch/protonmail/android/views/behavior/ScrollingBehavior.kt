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

import android.content.Context
import com.google.android.material.appbar.AppBarLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.views.contactDetails.SquareFloatingButtonView

class ScrollingBehavior(
    context: Context?, attrs: AttributeSet? = null
) : CoordinatorLayout.Behavior<SquareFloatingButtonView>(context, attrs) {

    private var parentHeight = 0
    private val toolbarHeight: Int = UiUtil.getToolbarHeight(context)

    override fun onLayoutChild(
            parent: CoordinatorLayout, child: SquareFloatingButtonView,
            layoutDirection: Int
    ): Boolean {
        parentHeight = parent.height
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    override fun layoutDependsOn(
            parent: CoordinatorLayout,
            child: SquareFloatingButtonView,
            dependency: View
    ): Boolean {
        return dependency is AppBarLayout
    }


    override fun onDependentViewChanged(
            parent: CoordinatorLayout, child: SquareFloatingButtonView,
            dependency: View
    ): Boolean {
        if (dependency is AppBarLayout) {
            child.translationY = dependency.getScrollY().toFloat()
            if (dependency.getY() < -2 * toolbarHeight && child.visibility == View.VISIBLE) {
                child.hide()
            } else if (dependency.getY() > -2 * toolbarHeight && child.visibility != View.VISIBLE) {
                child.show()
            }
        }
        return true
    }
}