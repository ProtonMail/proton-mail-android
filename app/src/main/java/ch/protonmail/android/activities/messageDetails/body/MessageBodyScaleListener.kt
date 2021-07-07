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
package ch.protonmail.android.activities.messageDetails.body

import android.view.ScaleGestureDetector
import android.view.animation.AnimationSet
import android.view.animation.DecelerateInterpolator
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
import android.webkit.WebView
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max
import kotlin.math.min

internal class MessageBodyScaleListener(
    private val wvScrollView: RecyclerView,
    private val messageBodyWebView: WebView,
    private val directParent: LinearLayout
) : ScaleGestureDetector.SimpleOnScaleGestureListener() {

    private var scaleFactor = 1f
    private var preSF = 1f

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        wvScrollView.requestDisallowInterceptTouchEvent(true)
        scaleFactor *= detector.scaleFactor

        // Don't let the object get too small or too large.
        scaleFactor = max(0.1f, min(scaleFactor, 10.0f))

        val a = messageBodyWebView.scrollY
        messageBodyWebView.scrollTo(0, 0)
        directParent.scrollTo(0, 0)

        val animSet = AnimationSet(true)
        animSet.fillAfter = true
        animSet.duration = 1000
        animSet.interpolator = DecelerateInterpolator()
        val translate = TranslateAnimation(0f, 0f, 0f, (-a).toFloat())
        animSet.addAnimation(translate)
        val scale = ScaleAnimation(
            preSF, scaleFactor, preSF, scaleFactor, ScaleAnimation.RELATIVE_TO_PARENT, 0.5f,
            ScaleAnimation.RELATIVE_TO_PARENT, 0.5f
        )
        animSet.addAnimation(scale)

        val animSet3 = AnimationSet(true)
        animSet3.fillAfter = true
        animSet3.duration = 1000
        animSet3.interpolator = DecelerateInterpolator()
        val translate3 = TranslateAnimation(0f, 0f, 0f, (-a).toFloat())
        animSet3.addAnimation(translate3)
        val scale3 = ScaleAnimation(
            preSF, preSF, preSF, scaleFactor, ScaleAnimation.RELATIVE_TO_PARENT, 0f, ScaleAnimation.RELATIVE_TO_PARENT,
            0f
        )
        animSet3.addAnimation(scale3)
        preSF = scaleFactor
        return true
    }
}
