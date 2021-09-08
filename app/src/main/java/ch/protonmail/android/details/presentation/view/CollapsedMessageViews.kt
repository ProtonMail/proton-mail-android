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

package ch.protonmail.android.details.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
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
        if (labels.isNotEmpty()) {
            setLabels(labels)
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    private fun TextView.showExpirationTimeOrHide(expirationTime: Long) {
        if (expirationTime > 0) {
            val remainingSeconds = expirationTime -
                TimeUnit.MILLISECONDS.toSeconds(serverTimeProvider.currentTimeMillis())
            text = DateUtil.formatTheLargestAvailableUnitOnly(context, remainingSeconds)
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }
}
