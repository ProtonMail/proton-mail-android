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

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.material.appbar.AppBarLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import android.util.AttributeSet
import android.view.View
import ch.protonmail.android.R
import ch.protonmail.android.views.contactDetails.ContactAvatarView

class AvatarImageBehavior(context: Context, attrs: AttributeSet?) :
    CoordinatorLayout.Behavior<ContactAvatarView>(context, attrs) {
    private var mContext: Context? = context
    private var mCustomFinalYPosition: Float
    private var mCustomStartXPosition: Float
    private var mCustomStartToolbarPosition: Float
    private var mCustomStartHeight: Float
    private var mCustomFinalHeight: Float
    private var mAvatarMaxSize: Float
    private var mFinalLeftAvatarPadding: Float
    private val mStartPosition: Float
    private var mStartXPosition: Int = 0
    private var mStartToolbarPosition: Float
    private var mStartYPosition: Int = 0
    private var mFinalYPosition: Int = 0
    private var mStartHeight: Int = 0
    private var mFinalXPosition: Int = 0
    private var mChangeBehaviorPoint: Float

    init {
        this.mCustomFinalYPosition = 0.toFloat()
        this.mCustomStartXPosition = 0.toFloat()
        this.mCustomStartToolbarPosition = 0.toFloat()
        this.mCustomStartHeight = 0.toFloat()
        this.mCustomFinalHeight = 0.toFloat()
        this.mAvatarMaxSize = 0.toFloat()
        this.mFinalLeftAvatarPadding = 0.toFloat()
        this.mStartPosition = 0.toFloat()
        this.mStartToolbarPosition = 0.toFloat()
        this.mChangeBehaviorPoint = 0.toFloat()
        if (attrs != null) {
            val a = context.obtainStyledAttributes(attrs, R.styleable.AvatarImageBehavior)
            mCustomFinalYPosition =
                    a.getDimension(R.styleable.AvatarImageBehavior_finalYPosition, 0f)
            mCustomStartXPosition =
                    a.getDimension(R.styleable.AvatarImageBehavior_startXPosition, 0f)
            mCustomStartToolbarPosition =
                    a.getDimension(R.styleable.AvatarImageBehavior_startToolbarPosition, 0f)
            mCustomStartHeight = a.getDimension(R.styleable.AvatarImageBehavior_startHeight, 0f)
            mCustomFinalHeight = a.getDimension(R.styleable.AvatarImageBehavior_finalHeight, 0f)

            a.recycle()
        }
        bindDimensions()
        mFinalLeftAvatarPadding = context.resources.getDimension(R.dimen.spacing_normal)
    }

    private fun bindDimensions() {
        mAvatarMaxSize = mContext!!.resources.getDimension(R.dimen.image_width)
    }

    override fun layoutDependsOn(
            parent: CoordinatorLayout,
            child: ContactAvatarView,
            dependency: View
    ): Boolean {
        return dependency is AppBarLayout
    }

    override fun onDependentViewChanged(
            parent: CoordinatorLayout,
            child: ContactAvatarView,
            dependency: View
    ): Boolean {
        maybeInitProperties(child, dependency)
        val maxScrollDistance = mStartToolbarPosition.toInt()
        val expandedPercentageFactor =
            ((dependency.height - dependency.y * (-1)) / maxScrollDistance)


        if (expandedPercentageFactor < mChangeBehaviorPoint) {
            val heightFactor =
                2 * (mChangeBehaviorPoint - expandedPercentageFactor) / mChangeBehaviorPoint
            val distanceXToSubtract =
                (mStartXPosition - mFinalXPosition) * heightFactor + child.height / 2
            val distanceYToSubtract =
                (mStartYPosition - mFinalYPosition) * (1f - expandedPercentageFactor) + child.height / 2

            child.y = mStartYPosition - distanceYToSubtract
            val heightToSubtract = (mStartHeight - mCustomFinalHeight) * heightFactor
            val lp = child.layoutParams as CoordinatorLayout.LayoutParams
            lp.width = (mStartHeight - heightToSubtract).toInt()
            lp.height = (mStartHeight - heightToSubtract).toInt()
            child.layoutParams = lp
        } else {
            val distanceYToSubtract =
                (mStartYPosition - mFinalYPosition) * (1f - expandedPercentageFactor) + mStartHeight / 2

            child.x = mStartXPosition.toFloat() - child.width / 2
            child.y = mStartYPosition - distanceYToSubtract
            val lp = child.layoutParams as CoordinatorLayout.LayoutParams
            lp.width = mStartHeight
            lp.height = mStartHeight
            child.layoutParams = lp
        }
        return true
    }

    @SuppressLint("PrivateResource")
    private fun maybeInitProperties(child: ContactAvatarView, dependency: View) {
        if (mStartYPosition == 0) {
            mStartYPosition = getStatusBarHeight() + dependency.height / 2
        }

        if (mFinalYPosition == 0) {
            mFinalYPosition = 0
        }

        if (mStartHeight == 0)
            mStartHeight = child.height

        if (mStartXPosition == 0)
            mStartXPosition = (child.x + child.width / 2).toInt()

        if (mFinalXPosition == 0)
            mFinalXPosition = (child.x + child.width / 2).toInt()

        if (mStartToolbarPosition == 0.toFloat())
            mStartToolbarPosition = dependency.height - dependency.y * (-1)

        if (mChangeBehaviorPoint == 0.toFloat()) {
            mChangeBehaviorPoint = 1.toFloat()
        }
    }

    private fun getStatusBarHeight(): Int {
        var result = 0
        val resourceId =
            mContext!!.resources.getIdentifier("status_bar_height", "dimen", "android")

        if (resourceId > 0) {
            result = mContext!!.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
}