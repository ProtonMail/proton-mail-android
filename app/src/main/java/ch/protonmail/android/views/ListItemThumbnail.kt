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
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Rect
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.content.withStyledAttributes
import ch.protonmail.android.R
import timber.log.Timber

private const val TEXT_SIZE_PROPORTION = 2.5f
private const val DEFAULT_CIRCLE_SIZE = 100

/**
 * Round/oval list item thumbnail with custom background color, icon or text.
 */
class ListItemThumbnail @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val basePaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.interaction_weak)
    }
    private val borderPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.cornflower_blue)
        style = Paint.Style.STROKE
        strokeWidth = context.resources.getDimension(R.dimen.padding_xxs)
    }
    private val textPaint = TextPaint(ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.text_norm)
        textAlign = Paint.Align.LEFT
    }

    private val textRectangle = Rect()

    private var circleSize: Int = DEFAULT_CIRCLE_SIZE
        set(value) {
            // also set proportionally text size
            textPaint.textSize = value.toFloat().div(TEXT_SIZE_PROPORTION)
            Timber.v("new circleSize: $value")
            field = value
            invalidate()
        }

    @DrawableRes
    var iconResource = R.drawable.ic_contact_groups_filled
        set(value) {
            iconDrawable = AppCompatResources.getDrawable(context, value)
            text = ""
        }
    private var iconDrawable = AppCompatResources.getDrawable(context, iconResource)

    private val checkIconDrawable = AppCompatResources.getDrawable(context, R.drawable.ic_check_white)

    private var isMultiselectModeActive: Boolean = false

    private var text: String = ""

    init {
        context.withStyledAttributes(
            attrs,
            R.styleable.ListItemThumbnail
        ) {
            val defaultCircleSize = context.resources.getDimensionPixelSize(R.dimen.padding_3xl)
            circleSize = getDimensionPixelSize(R.styleable.ListItemThumbnail_circleSize, defaultCircleSize)
            iconResource = getResourceId(R.styleable.ListItemThumbnail_icon, R.drawable.ic_contact_groups_filled)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(
            circleSize,
            circleSize
        )
    }

    override fun onDraw(canvas: Canvas) {
        val centerX = width / 2f
        val centerY = height / 2f

        if (isSelected) {
            drawCheckSign(centerX, centerY, canvas)
            return
        } else {
            canvas.drawCircle(centerX, centerY, centerX, basePaint)
            if (isMultiselectModeActive) {
                // draw border
                borderPaint.style = Paint.Style.STROKE
                canvas.drawCircle(centerX, centerY, centerX - borderPaint.strokeWidth, borderPaint)
                return
            }
        }

        if (text.isNotBlank()) { // if text not empty draw it
            textPaint.getTextBounds(text, 0, text.length, textRectangle)
            canvas.drawText(
                text,
                centerX - textRectangle.width() / 2 - textRectangle.left,
                centerY + textRectangle.height() / 2 - textRectangle.bottom,
                textPaint
            )
        } else {
            drawIcon(centerX, centerY, canvas)
        }
    }

    private fun drawCheckSign(centerX: Float, centerY: Float, canvas: Canvas) {
        borderPaint.style = Paint.Style.FILL
        canvas.drawCircle(centerX, centerY, centerX, borderPaint)

        val imageWidth = checkIconDrawable?.intrinsicWidth?.div(2) ?: 0
        val imageHeight = checkIconDrawable?.intrinsicHeight?.div(2) ?: 0
        checkIconDrawable?.setBounds(
            centerX.toInt() - imageWidth,
            centerY.toInt() - imageHeight,
            centerX.toInt() + imageWidth,
            centerY.toInt() + imageHeight
        )
        checkIconDrawable?.draw(canvas)
    }

    private fun drawIcon(centerX: Float, centerY: Float, canvas: Canvas) {
        val imageWidth = iconDrawable?.intrinsicWidth?.div(2) ?: 0
        val imageHeight = iconDrawable?.intrinsicHeight?.div(2) ?: 0
        iconDrawable?.setBounds(
            centerX.toInt() - imageWidth,
            centerY.toInt() - imageHeight,
            centerX.toInt() + imageWidth,
            centerY.toInt() + imageHeight
        )
        iconDrawable?.draw(canvas)
    }

    fun bind(
        isSelectedActive: Boolean,
        isMultiselectActive: Boolean,
        initials: String = "",
        @ColorInt circleColor: Int? = null
    ) {
        Timber.v("on bind $isSelectedActive, $isMultiselectActive, $initials, $circleColor")
        isSelected = isSelectedActive
        isMultiselectModeActive = isMultiselectActive
        text = initials
        circleColor?.let {
            basePaint.color = it
        }
        invalidate()
    }
}
