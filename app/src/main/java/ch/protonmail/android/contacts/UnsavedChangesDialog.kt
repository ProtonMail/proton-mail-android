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
package ch.protonmail.android.contacts

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import ch.protonmail.android.R

class UnsavedChangesDialog(
    private val context: Context,
    private val negativeButtonListener: (() -> Unit),
    private val positiveButtonListener: (() -> Unit)
) {

    fun build() {
        val builder = AlertDialog.Builder(context)
        val clickListener = DialogInterface.OnClickListener { dialogInterface, i ->
            if (i == DialogInterface.BUTTON_POSITIVE) {
                dialogInterface.dismiss()
                positiveButtonListener.invoke()
            } else if (i == DialogInterface.BUTTON_NEGATIVE) {
                negativeButtonListener.invoke()
                dialogInterface.dismiss()
            }
        }
        builder.setTitle(R.string.unsaved_changes_title)
            .setMessage(R.string.unsaved_changes_subtitle)
            .setNegativeButton(R.string.no, clickListener)
            .setPositiveButton(R.string.yes, clickListener)
            .create()
            .show()
    }
}
