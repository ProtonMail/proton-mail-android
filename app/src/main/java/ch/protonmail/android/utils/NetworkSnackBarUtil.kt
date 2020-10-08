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
import dagger.hilt.android.scopes.ActivityScoped
import timber.log.Timber
import javax.inject.Inject

/**
 * Utility methods for displaying network error snackbars.
 */
@ActivityScoped
class NetworkSnackBarUtil @Inject constructor() {

    private var noConnectionSnackBar: Snackbar? = null
    private var checkingConnectionSnackBar: Snackbar? = null

    /**
     * Provides ready [Snackbar] for displaying "No connection" with appropriate styling.
     *
     * @param parentView view to which the snackBar should be attached, e.g. findViewById(android.R.id.content)
     * @param user current user
     * @param callback
     * @param listener retry click listener
     * @param message optional custom message resource
     * @param isTopSnackBar optional flag forcing snackBar to the top
     */
    fun getNoConnectionSnackBar(
        parentView: View,
        user: User,
        callback: INetworkConfiguratorCallback,
        listener: View.OnClickListener?,
        @StringRes message: Int = R.string.no_connectivity_detected_troubleshoot,
        isTopSnackBar: Boolean = false
    ): Snackbar {
        val snackBar = noConnectionSnackBar
            ?: Snackbar.make(parentView, message, Snackbar.LENGTH_INDEFINITE).apply {
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
        noConnectionSnackBar = snackBar
        Timber.d("getNoConnectionSnackBar $snackBar $parentView")
        return snackBar
    }

    /**
     * Provides ready [Snackbar] for displaying checking connection message with an appropriate styling.
     *
     * @param parentView view to which the snackar should be attached, e.g. findViewById(android.R.id.content)
     */
    fun getCheckingConnectionSnackBar(
        parentView: View
    ): Snackbar {
        val snackBar = checkingConnectionSnackBar ?: Snackbar.make(
            parentView,
            parentView.context.getString(R.string.connectivity_checking),
            Snackbar.LENGTH_LONG
        ).apply {
            view.apply {
                setBackgroundColor(ContextCompat.getColor(context, R.color.blue))
                findViewById<TextView>(com.google.android.material.R.id.snackbar_text).apply {
                    setTextColor(Color.WHITE)
                }
            }
        }
        checkingConnectionSnackBar = snackBar
        Timber.d("getCheckingConnectionSnackBar $snackBar $parentView")
        return snackBar
    }

    fun isNoConnectionShown() = noConnectionSnackBar?.isShownOrQueued ?: false

    fun hideNoConnectionSnackBar() {
        noConnectionSnackBar?.dismiss()
        noConnectionSnackBar = null
    }

    fun isCheckingConnectionShown() = checkingConnectionSnackBar?.isShownOrQueued ?: false

    fun hideCheckingConnectionSnackBar() {
        checkingConnectionSnackBar?.dismiss()
        checkingConnectionSnackBar = null
    }

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
