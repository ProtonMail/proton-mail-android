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
package ch.protonmail.android.utils

import android.content.Context
import android.graphics.Color
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import ch.protonmail.android.R
import ch.protonmail.android.api.models.User
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showInfoDialogWithCustomView
import com.google.android.material.snackbar.Snackbar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility methods for displaying network error seacoasts.
 *
 * Defined as Singleton in order to be able to determine if a given snackbar instance is still displayed.
 */
@Singleton
class NetworkUtil @Inject constructor() {

    private var noConnectionSnackBar: Snackbar? = null
    private var checkingConnectionSnackBar: Snackbar? = null

    fun setNoConnectionSnackLayout(
        snackBarLayout: View,
        context: Context,
        listener: View.OnClickListener?,
        @StringRes message: Int = R.string.no_connectivity_detected_troubleshoot,
        user: User,
        callback: INetworkConfiguratorCallback,
        isTopSnackBar: Boolean = false
    ): Snackbar {
        val snackbar = noConnectionSnackBar
            ?: Snackbar.make(snackBarLayout, message, Snackbar.LENGTH_INDEFINITE).apply {
                setAction(context.getString(R.string.retry), listener)
                setActionTextColor(ContextCompat.getColor(context, R.color.white))
                view.apply {
                    setBackgroundColor(ContextCompat.getColor(context, R.color.red))
                    isClickable = true
                    isFocusable = true
                    setOnClickListener { showNoConnectionTroubleshootDialog(context, user, callback) }
                    applyTopGravity(isTopSnackBar)

                    findViewById<TextView>(com.google.android.material.R.id.snackbar_text).apply {
                        setTextColor(Color.WHITE)
                    }
                }
            }
        noConnectionSnackBar = snackbar
        return snackbar
    }

    fun setCheckingConnectionSnackLayout(
        snackBarLayout: View,
        context: Context
    ): Snackbar {
        val snackbar = checkingConnectionSnackBar ?: Snackbar.make(
            snackBarLayout,
            context.getString(R.string.connectivity_checking),
            Snackbar.LENGTH_LONG
        ).apply {
            view.apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
                findViewById<TextView>(com.google.android.material.R.id.snackbar_text).apply {
                    setTextColor(Color.WHITE)
                }
            }
        }
        checkingConnectionSnackBar = snackbar
        return snackbar
    }

    fun isNoConnectionShown() = noConnectionSnackBar?.isShownOrQueued ?: false

    fun isCheckingConnectionShown() = checkingConnectionSnackBar?.isShownOrQueued ?: false

    private fun showNoConnectionTroubleshootDialog(
        context: Context,
        user: User,
        callback: INetworkConfiguratorCallback
    ) {
        val troubleshootMessageView = LayoutInflater.from(context).inflate(R.layout.dialog_message_troubleshoot, null)
        troubleshootMessageView.findViewById<TextView>(R.id.troubleshoot_message).apply {
            text = Html.fromHtml(context.getString(R.string.troubleshoot_dialog_message))
            movementMethod = LinkMovementMethod.getInstance()
        }
        troubleshootMessageView.findViewById<TextView>(R.id.troubleshoot_switch_description).apply {
            movementMethod = LinkMovementMethod.getInstance()
        }
        troubleshootMessageView.findViewById<SwitchCompat>(R.id.troubleshoot_switch).apply {
            isChecked = user.allowSecureConnectionsViaThirdParties
            setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
                // update the uri from here
                // callback.onApiProvidersChanged();
                user.allowSecureConnectionsViaThirdParties = isChecked
            }
        }
        showInfoDialogWithCustomView(
            context,
            context.getString(R.string.troubleshoot_dialog_title),
            troubleshootMessageView
        ) {
            callback.onApiProvidersChanged()
        }
    }

    private fun View.applyTopGravity(isTopSnackBar: Boolean) {
        if (isTopSnackBar) {
            layoutParams = (layoutParams as FrameLayout.LayoutParams).apply {
                gravity = Gravity.TOP
            }
        }
    }
}
