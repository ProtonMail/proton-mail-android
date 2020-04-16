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
package ch.protonmail.android.utils.ui.dialogs

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.Color
import android.text.InputType
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.ToggleButton
import androidx.annotation.StringRes
import ch.protonmail.android.R
import ch.protonmail.android.views.CustomFontButton
import com.google.android.material.snackbar.Snackbar


/**
 * Created by kadrikj on 10/24/18. */
class DialogUtils {
    companion object {
        fun showInfoDialog(context: Context, title: String, message: String, okListener: ((Unit) -> Unit)?) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
                .setMessage(message)
                .setNeutralButton(R.string.okay) { dialog, _ ->
                    run {
                        okListener?.invoke(Unit)
                        dialog.dismiss()
                    }
                }
                .create()
                .show()
        }

        fun showInfoDialogWithCustomView(context: Context, @StringRes title: Int, view: View, okListener: ((Unit) -> Unit)?) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
                    .setView(view)
                    .setNeutralButton(R.string.okay) { dialog, _ ->
                        run {
                            okListener?.invoke(Unit)
                            dialog.dismiss()
                        }
                    }
                    .create()
                    .show()
        }

        fun showDeleteConfirmationDialog(context: Context, title: String, message: String, okListener: (Unit) -> Unit) {
            showInfoDialogWithTwoButtons(
                context, title, message, context.getString(R.string.no),
                context.getString(R.string.yes), okListener, true
            )
        }

        @JvmOverloads
        fun showInfoDialogWithTwoButtons(context: Context, title: String, message: String,
                                         negativeBtnText: String, positiveBtnText: String,
                                         dismissListener: ((Unit) -> Unit)?, okListener: ((Unit) -> Unit)?,
                                         cancelable: Boolean, dismissible: Boolean = true, outsideClickCancellable: Boolean = true): AlertDialog {
            val builder = AlertDialog.Builder(context)
            val dialog = builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeBtnText) { dialog, _ ->
                    run {
                        dismissListener?.invoke(Unit)
                        if (dismissible) {
                            dialog.dismiss()
                        }
                    }
                }
                .setPositiveButton(positiveBtnText) { dialog, _ ->
                    run {
                        okListener?.invoke(Unit)
                        if (dismissible) {
                            dialog.dismiss()
                        }
                    }
                }
                .setCancelable(cancelable)
                .create()
            dialog.setCanceledOnTouchOutside(outsideClickCancellable)
            dialog.show()
            return dialog
        }

        fun showInfoDialogWithTwoButtons(context: Context, title: String, message: String,
                                         negativeBtnText: String, positiveBtnText: String,
                                         okListener: ((Unit) -> Unit)?, cancelable: Boolean) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeBtnText) { dialog, _ -> dialog.dismiss() }
                .setPositiveButton(positiveBtnText) { dialog, _ ->
                    run {
                        okListener?.invoke(Unit)
                        dialog.dismiss()
                    }
                }
                .setCancelable(cancelable)
                .create()
                .show()
        }

        fun showInfoDialogWithTwoButtonsAndCheckbox(context: Context, title: String, message: Spanned,
                                                    negativeBtnText: String, positiveBtnText: String, checkBoxText: String,
                                                    okListener: (Unit) -> Unit, checkedListener: (Boolean) -> Unit, cancelable: Boolean) {

            val builder = AlertDialog.Builder(context)

            val checkBoxView = LayoutInflater.from(builder.context).inflate(R.layout.checkbox, null, false)
            (checkBoxView.findViewById(R.id.checkbox) as CheckBox).apply {
                text = checkBoxText
                setOnCheckedChangeListener { _, isChecked -> checkedListener(isChecked) }
            }

            val dialog = builder
                    .setTitle(title)
                    .setView(checkBoxView)
                    .setMessage(message)
                    .setNegativeButton(negativeBtnText) { dialog, _ -> dialog.dismiss() }
                    .setPositiveButton(positiveBtnText) { dialog, _ ->
                        run {
                            okListener.invoke(Unit)
                            dialog.dismiss()
                        }
                    }
                    .setCancelable(cancelable)
                    .create()

            dialog.show()
        }

        fun warningDialog(context: Context, okButtonText: String, cancelButtonText: String,
                          description: String, okListener: (Unit) -> Unit) {
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.layout_dialog_warning)
            dialog.setCancelable(false)
            val body = dialog.findViewById(R.id.subtitle) as TextView
            body.text = description
            val yesBtn = dialog.findViewById(R.id.ok) as CustomFontButton
            val noBtn = dialog.findViewById(R.id.cancel) as CustomFontButton

            yesBtn.text = okButtonText
            noBtn.text = cancelButtonText
            yesBtn.setOnClickListener {
                okListener.invoke(Unit)
                dialog.dismiss()
            }
            noBtn.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }

        fun showInfoDialogWithThreeButtons(context: Context, title: String, message: String,
                                           negativeBtnText: String, positiveBtnText: String, neultralBtnText: String,
                                           dismissListener: (Unit) -> Unit, okListener: (Unit) -> Unit,
                                           cancelable: Boolean) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
                .setMessage(message)
                .setNegativeButton(negativeBtnText) { dialog, _ ->
                    run {
                        dismissListener.invoke(Unit)
                        dialog.dismiss()
                    }
                }
                .setPositiveButton(positiveBtnText) { dialog, _ ->
                    run {
                        okListener.invoke(Unit)
                        dialog.dismiss()
                    }
                }
                .setNeutralButton(neultralBtnText) { dialog, _ -> dialog.dismiss()
                }
                .setCancelable(cancelable)
                .create()
                .show()
        }

        fun showUndoSnackbar(context: Context, parent: View, message: String, okListener: (Unit) -> Unit, showUndo: Boolean) : Snackbar {
            val undoSnack = Snackbar.make(parent, message, Snackbar.LENGTH_LONG)
            undoSnack.setColorWhite()
            if(showUndo) {
                undoSnack.setActionTextColor(context.resources.getColor(R.color.icon_purple))
                undoSnack.setAction(context.getString(R.string.undo)) {
                    run {
                        okListener.invoke(Unit)
                    }
                }
            }
            return undoSnack
        }

        fun show2FADialog(context: Activity,
                          okListener: (String) -> Unit,
                          cancelListener: () -> Unit): AlertDialog {
            val builder = AlertDialog.Builder(context)
            val dialogView = context.layoutInflater.inflate(R.layout.layout_2fa, null)
            val twoFactorCode = dialogView.findViewById(R.id.two_factor_code) as EditText
            val toggleInputText = dialogView.findViewById(R.id.toggle_input_text) as ToggleButton
            toggleInputText.setOnClickListener { v ->
                if ((v as ToggleButton).isChecked) {
                    twoFactorCode.inputType = InputType.TYPE_CLASS_TEXT
                } else {
                    twoFactorCode.inputType = InputType.TYPE_CLASS_NUMBER
                }
            }
            builder.setView(dialogView)
            builder.setPositiveButton(R.string.enter) { dialog, _ ->
                okListener(twoFactorCode.text.toString())
                dialog.cancel()
            }
            builder.setNegativeButton(R.string.cancel) { dialog, _ ->
                cancelListener()
                dialog.cancel()
            }
            val twoFADialog = builder.create()
            twoFADialog.setOnShowListener {
                twoFADialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.resources.getColor(R.color.iron_gray))
                val positiveButton = twoFADialog.getButton(DialogInterface.BUTTON_POSITIVE)
                positiveButton.setTextColor(context.resources.getColor(R.color.new_purple_dark))
                positiveButton.text = context.getString(R.string.enter)
            }
            twoFADialog.setCanceledOnTouchOutside(false)
            twoFADialog.show()
            return twoFADialog
        }

        fun showSignedInSnack(view: View, message: String) {
            val snackBar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            snackBar.setColorWhite()
            snackBar.show()
        }
    }
}

fun Snackbar.setColorWhite() {
    val snackView = view
    val tv = snackView.findViewById(com.google.android.material.R.id.snackbar_text) as TextView
    tv.setTextColor(Color.WHITE)
}
