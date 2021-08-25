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

package ch.protonmail.android.ui.layout

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import ch.protonmail.android.R

/**
 * A [LinearLayout] that will show items as much as they can fit, then show a "+N" text
 */
@SuppressWarnings("TooManyFunctions") // Android's View overrides
open class MoreItemsLinearLayout @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    /**
     * Override for *declared minimum width for a TextView*
     *
     * Normally a View will be visible only if can be shown in its totality, but if:
     *  * the [getOrientation] of this [MoreItemsLinearLayout] is [LinearLayout.HORIZONTAL]
     *  * the View is a [TextView]
     *  it will be shown, only if the available space is bigger than this value [minTextViewWidth]
     */
    open val minTextViewWidth = 0

    open val maxVisibleChildrenCount = MAX_NUMBER_OF_VISIBLE_CHILDREN_UNDEFINED

    val allChildren: Sequence<View> get() = object : Sequence<View> {
        override fun iterator() = this@MoreItemsLinearLayout.iterator()
    }

    // Created for shadow the homonymous extension function in ViewGroup.kt
    @Deprecated("Use allChildren", ReplaceWith("allChildren"))
    val children: Sequence<View> get() = allChildren

    @Suppress("DEPRECATION")
    val allChildCount get() = childCount - 1
    val visibleChildCount get() = allChildren.filter { it.isVisible }.toList().size
    val hiddenChildCount get() = allChildren.filterNot { it.isVisible }.toList().size

    private val moreTextView = TextView(context).apply {
        id = R.id.more_items_ll_more_text_view
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            gravity = Gravity.CENTER_VERTICAL
        }
        setTextAppearance(R.style.Proton_Text_Caption)
    }

    init {
        @Suppress("LeakingThis")
        addView(moreTextView)
    }

    operator fun ViewGroup.iterator(): MutableIterator<View> = object : MutableIterator<View> {
        private var index = 0
        override fun hasNext() = index < allChildCount
        override fun next() = getChildAt(index++) ?: throw IndexOutOfBoundsException()
        override fun remove() = removeViewAt(--index)
    }

    // region Measurements
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        // Measure this layout
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        var availableRelevantSize = getRelevantSizeFor(this)

        var childrenAddedInThisPass = 0
        var desiredWidth = 0

        // Measure children
        var limitReached = false
        for ((index, child) in allChildren.toList().withIndex()) {

            if (limitReached) {
                // We already reached the limit, just set invisible
                child.isVisible = false
                continue
            }

            // Update "more" text for mere measurement purpose
            moreTextView.apply {
                text = "+${allChildCount - index}"
                visibility = INVISIBLE
            }
            measureChild(moreTextView, widthMeasureSpec, heightMeasureSpec)
            val effectiveAvailableSize = availableRelevantSize - getRelevantSizeFor(moreTextView)

            // measure child
            measureChild(
                child,
                getChildWidthMeasureSpec(effectiveAvailableSize, widthMeasureSpec),
                getChildHeightMeasureSpec(effectiveAvailableSize, heightMeasureSpec)
            )
            val relevantSize = getRelevantSizeFor(child)
            val maxNumberOfChildrenReached = maxVisibleChildrenCount != MAX_NUMBER_OF_VISIBLE_CHILDREN_UNDEFINED
                && childrenAddedInThisPass >= maxVisibleChildrenCount

            child.isVisible = relevantSize <= effectiveAvailableSize && !maxNumberOfChildrenReached
            limitReached = relevantSize > effectiveAvailableSize || maxNumberOfChildrenReached
            if (relevantSize <= effectiveAvailableSize) {
                availableRelevantSize -= relevantSize
            }

            childrenAddedInThisPass++
            desiredWidth += child.measuredWidth
        }

        // Update "more" text for final result
        moreTextView.apply {
            text = "+$hiddenChildCount"
            isVisible = limitReached
            if (isVisible.not()) width = 0
        }
        desiredWidth += moreTextView.measuredWidth

        setMeasuredDimension(
            getWidthMeasureSpec(widthMeasureSpec, desiredWidth),
            getHeightMeasureSpec(heightMeasureSpec)
        )
    }

    private fun getWidthMeasureSpec(widthMeasureSpec: Int, desiredWidth: Int): Int {
        return if (orientation == VERTICAL) {
            val maxChildrenWidth = allChildren.maxOfOrNull { it.measuredWidth } ?: 0
            MeasureSpec.makeMeasureSpec(maxChildrenWidth, MeasureSpec.EXACTLY)
        } else {
            val requestedWidth = MeasureSpec.getSize(widthMeasureSpec)
            return when (widthMeasureSpec) {
                MeasureSpec.EXACTLY -> requestedWidth
                MeasureSpec.AT_MOST -> requestedWidth.coerceAtMost(desiredWidth)
                else -> desiredWidth
            }
        }
    }

    private fun getHeightMeasureSpec(heightMeasureSpec: Int): Int {
        return if (orientation == HORIZONTAL) {
            val maxChildrenHeight = allChildren.maxOfOrNull { it.measuredHeight } ?: 0
            MeasureSpec.makeMeasureSpec(maxChildrenHeight, MeasureSpec.EXACTLY)
        } else {
            heightMeasureSpec
        }
    }

    private fun getChildWidthMeasureSpec(availableRelevantSize: Int, widthMeasureSpec: Int): Int {
        return if (orientation == HORIZONTAL) {
            MeasureSpec.makeMeasureSpec(availableRelevantSize.coerceAtLeast(minTextViewWidth), MeasureSpec.AT_MOST)
        } else {
            widthMeasureSpec
        }
    }

    private fun getChildHeightMeasureSpec(availableRelevantSize: Int, heightMeasureSpec: Int): Int {
        return if (orientation == VERTICAL) {
            MeasureSpec.makeMeasureSpec(availableRelevantSize, MeasureSpec.AT_MOST)
        } else {
            heightMeasureSpec
        }
    }
    // endregion

    private fun View.asOrGetTextViewOrNull(): TextView? =
        this as? TextView ?: (this as? ViewGroup)?.getChildAt(0) as? TextView

    // region add
    override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        val fixedIndex = if (index in itemsRange()) index else allChildCount
        super.addView(child, fixedIndex, params)
    }

    fun addViewInLayout(child: View, params: ViewGroup.LayoutParams = generateDefaultLayoutParams()): Boolean =
        addViewInLayout(child, allChildCount, params)

    override fun addViewInLayout(
        child: View,
        index: Int,
        params: ViewGroup.LayoutParams?,
        preventRequestLayout: Boolean
    ): Boolean {
        val fixedIndex = if (index in itemsRange()) index else allChildCount
        return super.addViewInLayout(child, fixedIndex, params, preventRequestLayout)
    }
    // endregion

    // region remove
    override fun removeView(view: View) {
        if (indexOfChild(view) !in itemsRange()) return
        super.removeView(view)
    }

    override fun removeViewAt(index: Int) {
        if (index !in itemsRange()) return
        super.removeViewAt(index)
    }

    override fun removeViews(start: Int, count: Int) {
        val fixedCount = count.coerceAtMost(allChildCount - start)
        super.removeViews(start, fixedCount)
    }

    override fun removeViewInLayout(view: View) {
        if (indexOfChild(view) !in itemsRange()) return
        super.removeViewInLayout(view)
    }
    // endregion

    // region remove all
    override fun removeAllViewsInLayout() {
        removeViewsInLayout(0, allChildCount)
    }

    override fun removeViewsInLayout(start: Int, count: Int) {
        val fixedCount = count.coerceAtMost(allChildCount - start)
        super.removeViewsInLayout(start, fixedCount)
    }
    // endregion

    @Deprecated(
        "Use allChildCount, visibleChildCount or hiddenChildCount",
        ReplaceWith("allChildCount")
    )
    override fun getChildCount() = super.getChildCount()

    private fun itemsRange() = 0 until allChildCount

    private fun getRelevantSizeFor(measuredView: View): Int {
        return if (orientation == HORIZONTAL) measuredView.measuredWidth
        else measuredView.measuredHeight
    }

    private companion object {
        const val MAX_NUMBER_OF_VISIBLE_CHILDREN_UNDEFINED = -1
    }
}
