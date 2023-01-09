/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.views.messageDetails

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import ch.protonmail.android.databinding.LayoutBottomActionsBinding

/**
 * A bottom action view
 */
class BottomActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val firstActionImageButton: ImageButton
    private val secondActionImageButton: ImageButton
    private val thirdActionImageButton: ImageButton
    private val fourthActionImageButton: ImageButton
    private val moreActionImageButton: ImageButton

    init {
        val binding = LayoutBottomActionsBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

        firstActionImageButton = binding.firstActionImageButton
        secondActionImageButton = binding.secondActionImageButton
        thirdActionImageButton = binding.thirdActionImageButton
        fourthActionImageButton = binding.fourthActionImageButton
        moreActionImageButton = binding.moreActionImageButton
    }

    fun bind(actionsUiModel: UiModel) {
        with(actionsUiModel) {
            firstActionIcon?.let { setAction(ActionPosition.ACTION_FIRST, true, it) }
            secondActionIcon?.let { setAction(ActionPosition.ACTION_SECOND, true, it) }
            thirdActionIcon?.let { setAction(ActionPosition.ACTION_THIRD, true, it) }
            fourthActionIcon?.let { setAction(ActionPosition.ACTION_FOURTH, true, it) }
        }
    }

    private fun getButtonForPosition(actionPosition: ActionPosition): ImageButton =
        when (actionPosition) {
            ActionPosition.ACTION_FIRST -> firstActionImageButton
            ActionPosition.ACTION_SECOND -> secondActionImageButton
            ActionPosition.ACTION_THIRD -> thirdActionImageButton
            ActionPosition.ACTION_FOURTH -> fourthActionImageButton
        }

    fun setAction(
        actionPosition: ActionPosition,
        isVisible: Boolean,
        @DrawableRes actionIcon: Int? = null,
        contentDescription: String = ""
    ) {
        val actionButton = getButtonForPosition(actionPosition)
        if (isVisible && actionIcon != null) {
            actionButton.visibility = View.VISIBLE
            actionButton.setImageDrawable(ContextCompat.getDrawable(context, actionIcon))
            actionButton.contentDescription = contentDescription.ifEmpty { actionButton.contentDescription }
        } else {
            actionButton.visibility = View.GONE
        }
    }

    fun setOnFirstActionClickListener(onFirstActionClickListener: () -> Unit) {
        firstActionImageButton.setOnClickListener { onFirstActionClickListener() }
    }

    fun setOnSecondActionClickListener(onSecondActionClickListener: () -> Unit) {
        secondActionImageButton.setOnClickListener { onSecondActionClickListener() }
    }

    fun setOnThirdActionClickListener(onThirdActionClickListener: () -> Unit) {
        thirdActionImageButton.setOnClickListener { onThirdActionClickListener() }
    }

    fun setOnFourthActionClickListener(onFourthActionClickListener: () -> Unit) {
        fourthActionImageButton.setOnClickListener { onFourthActionClickListener() }
    }

    fun setOnMoreActionClickListener(onMoreActionClickListener: () -> Unit) {
        moreActionImageButton.setOnClickListener { onMoreActionClickListener() }
    }

    data class UiModel(
        @DrawableRes val firstActionIcon: Int? = null,
        @DrawableRes val secondActionIcon: Int? = null,
        @DrawableRes val thirdActionIcon: Int? = null,
        @DrawableRes val fourthActionIcon: Int? = null
    )

    enum class ActionPosition {
        ACTION_FIRST,
        ACTION_SECOND,
        ACTION_THIRD,
        ACTION_FOURTH
    }
}
