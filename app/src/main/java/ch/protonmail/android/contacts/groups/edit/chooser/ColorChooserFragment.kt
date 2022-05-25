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
package ch.protonmail.android.contacts.groups.edit.chooser

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import androidx.annotation.ColorInt
import ch.protonmail.android.R
import ch.protonmail.android.activities.dialogs.AbstractDialogFragment
import ch.protonmail.android.adapters.LabelColorsAdapter
import kotlinx.android.synthetic.main.fragment_color_chooser.*
import java.util.Random

// region constants
private const val TAG_COLOR_CHOOSER_FRAGMENT = "ProtonMail.ColorChooserFragment"
// endregion

class ColorChooserFragment : AbstractDialogFragment(), AdapterView.OnItemClickListener {
    override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
        val colorId = colorOptions[position]
        selectedNewColor = colorId
        colorsAdapter.setChecked(position)
    }

    override fun getFragmentKey(): String = TAG_COLOR_CHOOSER_FRAGMENT

    override fun getStyleResource(): Int = R.style.AppTheme_Dialog_Labels

    override fun initUi(rootView: View?) {
        // noop
    }

    private lateinit var colorsAdapter: LabelColorsAdapter
    private lateinit var colorOptions: IntArray
    private var currentSelection: Int = 0

    @ColorInt
    private var selectedNewColor: Int? = null
    private lateinit var colorChooserListener: IColorChooserListener

    override fun getLayoutResourceId(): Int = R.layout.fragment_color_chooser

    override fun onAttach(context: Context) {
        super.onAttach(context)
        colorChooserListener = context as IColorChooserListener
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initColors()
        randomCheck()
        cancel.setOnClickListener {
            dismissAllowingStateLoss()
        }
        apply.setOnClickListener {
            colorChooserListener.colorChosen(selectedNewColor ?: colorOptions[0])
            dismissAllowingStateLoss()
        }
    }

    private fun initColors() {
        colorOptions = resources.getIntArray(R.array.label_colors)
        colorsAdapter = LabelColorsAdapter(context!!, colorOptions, R.layout.label_color_item_circle)
        colorsGrid.adapter = colorsAdapter
        colorsGrid.onItemClickListener = this
    }

    private fun randomCheck() {
        val random = Random()
        currentSelection = random.nextInt(colorOptions.size)
        val colorId = colorOptions[currentSelection]
        selectedNewColor = colorId
        colorsAdapter.setChecked(currentSelection)
    }

    companion object {

        fun newInstance(): ColorChooserFragment = ColorChooserFragment()
    }

    interface IColorChooserListener {
        fun colorChosen(@ColorInt color: Int)
    }
}
