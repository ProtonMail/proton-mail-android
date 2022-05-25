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

package ch.protonmail.android.details.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.databinding.LayoutCollapsedMessageViewsBinding
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.ui.view.SingleLineCollapsedLabelGroupView
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.ServerTimeProvider
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class CollapsedMessageViews @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val messageExpirationTextView: TextView
    private val collapsedLabelsView: SingleLineCollapsedLabelGroupView

    @Inject lateinit var serverTimeProvider: ServerTimeProvider

    init {
        val binding = LayoutCollapsedMessageViewsBinding.inflate(LayoutInflater.from(context), this)

        messageExpirationTextView = binding.messageExpirationTextView
        collapsedLabelsView = binding.collapsedLabelsView
    }

    fun bind(message: Message, labels: List<LabelChipUiModel>) {
        collapsedLabelsView.showLabelsOrHide(labels)
        messageExpirationTextView.showExpirationTimeOrHide(message.expirationTime)
    }

    private fun SingleLineCollapsedLabelGroupView.showLabelsOrHide(labels: List<LabelChipUiModel>) {
        isVisible = if (labels.isNotEmpty()) {
            setLabels(labels)
            true
        } else {
            false
        }
    }

    private fun TextView.showExpirationTimeOrHide(expirationTime: Long) {
        if (expirationTime > 0) {
            val remainingSeconds = expirationTime -
                TimeUnit.MILLISECONDS.toSeconds(serverTimeProvider.currentTimeMillis())
            text = DateUtil.formatTheLargestAvailableUnitOnly(context, remainingSeconds)
            isVisible = true
        } else {
            isVisible = false
        }
    }
}
