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
package ch.protonmail.android.utils.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.StringRes
import ch.protonmail.android.R
import com.google.android.material.snackbar.Snackbar
import me.proton.core.presentation.ui.view.ProtonButton

class DialogUtils {

    companion object {

        fun showInfoDialog(
            context: Context,
            title: String,
            message: String,
            okListener: ((Unit) -> Unit)?
        ) {
            showInfoDialog(context, title, message, true, okListener)
        }

        fun showInfoDialog(
            context: Context,
            title: String,
            message: String,
            cancellable: Boolean,
            okListener: ((Unit) -> Unit)?
        ) {
            val builder = AlertDialog.Builder(context)

            val messageWithLinks = SpannableString(message)
            Linkify.addLinks(messageWithLinks, Linkify.WEB_URLS)

            builder.setTitle(title)
                .setMessage(messageWithLinks)
                .setNeutralButton(R.string.ok) { dialog, _ ->
                    run {
                        okListener?.invoke(Unit)
                        dialog.dismiss()
                    }
                }
                .setCancelable(cancellable)
                .create()

            val dialog: AlertDialog = builder.show()
            (dialog.findViewById(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
        }

        fun showInfoDialogWithCustomView(
            context: Context,
            title: String,
            view: View,
            okListener: ((Unit) -> Unit)?
        ) {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(title)
                .setView(view)
                .setNeutralButton(R.string.ok) { dialog, _ ->
                    run {
                        okListener?.invoke(Unit)
                        dialog.dismiss()
                    }
                }
                .create()
                .show()
        }

        fun showDeleteConfirmationDialog(
            context: Context,
            title: String,
            message: String,
            okListener: (Unit) -> Unit
        ) {
            showInfoDialogWithTwoButtons(
                context, title, message, context.getString(R.string.no),
                context.getString(R.string.yes), okListener, true
            )
        }

        @Deprecated(
            "Use 'showTwoButtonInfoDialog'",
            ReplaceWith(
                "context.showTwoButtonInfoDialog(\n" +
                    "   title = title,\n" +
                    "   message = message,\n" +
                    "   rightStringId = positiveBtnText,\n" +
                    "   leftStringId = negativeBtnText,\n" +
                    "   dismissOnButtonClick = dismissible,\n" +
                    "   cancelable = cancelable,\n" +
                    "   cancelOnTouchOutside = outsideClickCancellable,\n" +
                    "   onRight = okListener,\n" +
                    "   onLeft = dismissListener\n" +
                    ")"
            )
        )
        @JvmOverloads
        fun showInfoDialogWithTwoButtons(
            context: Context,
            title: String,
            message: String,
            negativeBtnText: String,
            positiveBtnText: String,
            dismissListener: ((Unit) -> Unit)?,
            okListener: ((Unit) -> Unit)?,
            cancelable: Boolean,
            dismissible: Boolean = true,
            outsideClickCancellable: Boolean = true
        ): AlertDialog {
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

        /**
         * Create and show a [AlertDialog] with 2 buttons
         * @throws IllegalArgumentException if none of [title] or [title] is defined
         * @return [AlertDialog]
         *
         * @param title [CharSequence] title of the Dialog
         *  optional if [titleStringId] is defined
         *
         * @param titleStringId [StringRes] for the title of the Dialog
         *  optional if [title] is defined
         *
         * @param message [CharSequence] message of the Dialog
         *  optional
         *
         * @param messageStringId [StringRes] for the message of the Dialog
         *  optional
         *
         * @param positiveStringId [StringRes] for text of the right button
         *  default is [R.string.ok]
         *
         * @param negativeStringId [StringRes] for text of the left button
         *  default is [R.string.cancel]
         *
         * @param cancelable whether the [Dialog] is cancelable
         *  default is `true`
         *
         * @param cancelOnTouchOutside whether the [Dialog] must be cancelled when a touch happens outside of the
         *  [Dialog] itself
         *  default is `true`
         *
         * @param onNegativeButtonClicked will be executed when the negative button is pressed
         *  default is an empty lambda
         *
         * @param onPositiveButtonClicked will be executed when the positive button is pressed
         *  default is an empty lambda
         */
        inline fun Context.showTwoButtonInfoDialog(
            @StringRes titleStringId: Int? = null,
            title: CharSequence? = titleStringId?.let(::getText),
            @StringRes messageStringId: Int? = null,
            message: CharSequence? = messageStringId?.let(::getText),
            @StringRes positiveStringId: Int = R.string.ok,
            @StringRes negativeStringId: Int = R.string.cancel,
            cancelable: Boolean = true,
            cancelOnTouchOutside: Boolean = true,
            crossinline onNegativeButtonClicked: () -> Unit = {},
            crossinline onPositiveButtonClicked: () -> Unit = {}
        ): AlertDialog {
            requireNotNull(title) {
                "One parameter of 'title' or 'titleStringId' is required"
            }
            return AlertDialog.Builder(this)
                .setTitle(title)
                .apply { message?.let(::setMessage) }
                .setNegativeButton(negativeStringId) { dialog, _ ->
                    onNegativeButtonClicked()
                }
                .setPositiveButton(positiveStringId) { dialog, _ ->
                    onPositiveButtonClicked()
                }
                .setCancelable(cancelable)
                .create()
                .apply { setCanceledOnTouchOutside(cancelOnTouchOutside) }
                .also { it.show() }
        }

        @Deprecated(
            "Use 'showTwoButtonInfoDialog'",
            ReplaceWith(
                "context.showTwoButtonInfoDialog(\n" +
                    "   title = title,\n" +
                    "   message = message,\n" +
                    "   rightStringId = positiveBtnText,\n" +
                    "   leftStringId = negativeBtnText,\n" +
                    "   cancelable = cancelable,\n" +
                    "   onRight = okListener\n" +
                    ")"
            )
        )
        fun showInfoDialogWithTwoButtons(
            context: Context,
            title: String,
            message: String,
            negativeBtnText: String,
            positiveBtnText: String,
            okListener: ((Unit) -> Unit)?,
            cancelable: Boolean
        ) {
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

        fun showInfoDialogWithTwoButtonsAndCheckbox(
            context: Context,
            title: String,
            message: Spanned,
            negativeBtnText: String,
            positiveBtnText: String,
            checkBoxText: String,
            okListener: (DialogInterface) -> Unit,
            checkedListener: (Boolean) -> Unit,
            cancelable: Boolean
        ) {

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
                    okListener.invoke(dialog)
                    dialog.dismiss()
                }
                .setCancelable(cancelable)
                .create()

            dialog.show()
        }

        fun warningDialog(
            context: Context,
            okButtonText: String,
            cancelButtonText: String,
            description: String,
            okListener: (Unit) -> Unit
        ) {
            val dialog = Dialog(context)
            dialog.setContentView(R.layout.layout_dialog_warning)
            dialog.setCancelable(false)
            val body = dialog.findViewById(R.id.subtitle) as TextView
            body.text = description
            val yesBtn = dialog.findViewById(R.id.ok) as ProtonButton
            val noBtn = dialog.findViewById(R.id.cancel) as ProtonButton

            yesBtn.text = okButtonText
            noBtn.text = cancelButtonText
            yesBtn.setOnClickListener {
                okListener.invoke(Unit)
                dialog.dismiss()
            }
            noBtn.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }

        fun showUndoSnackbar(
            context: Context,
            parent: View,
            message: String,
            okListener: (Unit) -> Unit,
            showUndo: Boolean
        ): Snackbar {
            val undoSnack = Snackbar.make(parent, message, Snackbar.LENGTH_LONG)
            undoSnack.setTextColor(context.getColor(R.color.text_inverted))
            if (showUndo) {
                undoSnack.setAction(context.getString(R.string.undo)) {
                    okListener(Unit)
                }
            }
            return undoSnack
        }

    }
}
