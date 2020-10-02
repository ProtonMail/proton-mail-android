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
package ch.protonmail.android.activities

import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.StringRes
import butterknife.BindView
import ch.protonmail.android.R
import ch.protonmail.android.jobs.PingJob
import ch.protonmail.android.utils.INetworkConfiguratorCallback
import ch.protonmail.android.utils.NetworkUtil
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.snackbar.SnackbarContentLayout
import timber.log.Timber

abstract class BaseConnectivityActivity : BaseActivity() {

    @BindView(R.id.layout_no_connectivity_info)
    protected lateinit var mSnackLayout: View
    var mNoConnectivitySnack: Snackbar? = null
    protected var mCheckForConnectivitySnack: Snackbar? = null

    protected var pingHandler = Handler()
    protected var pingRunnable: Runnable = Runnable { mJobManager.addJobInBackground(PingJob()) }

    private var connectivityRetryListener = RetryListener()

    protected open inner class RetryListener : View.OnClickListener {

        override fun onClick(v: View) {
            mCheckForConnectivitySnack = NetworkUtil.setCheckingConnectionSnackLayout(
                mSnackLayout,
                this@BaseConnectivityActivity
            )
            mCheckForConnectivitySnack!!.show()
            if (mNoConnectivitySnack != null && mNoConnectivitySnack!!.isShownOrQueued) {
                mNoConnectivitySnack!!.dismiss()
            }
            pingHandler.removeCallbacks(pingRunnable)
            pingHandler.postDelayed(pingRunnable, 3000)

            retryWithDoh()
        }
    }

    protected fun retryWithDoh() {
        if (mNetworkUtil.isConnected()) {
            val thirdPartyConnectionsEnabled = mUserManager.user.allowSecureConnectionsViaThirdParties
            if (thirdPartyConnectionsEnabled) {
                Timber.d("Third party connections enabled, attempting DoH...")
                networkConfigurator.refreshDomainsAsync() // refreshDomains(false) // switch to new here
            }
        }
    }

    @JvmOverloads
    protected fun showNoConnSnack(
        listener: RetryListener? = null,
        @StringRes message: Int = R.string.no_connectivity_detected_troubleshoot,
        view: View = mSnackLayout,
        callback: INetworkConfiguratorCallback
    ) {
        val user = mUserManager.user
        mNoConnectivitySnack = mNoConnectivitySnack ?: NetworkUtil.setNoConnectionSnackLayout(
            view,
            this,
            listener ?: connectivityRetryListener,
            false,
            message,
            user,
            callback
        )
        mNoConnectivitySnack!!.show()
        val contentLayout = (mNoConnectivitySnack!!.view as ViewGroup).getChildAt(0) as SnackbarContentLayout
        val vvv: TextView = contentLayout.actionView
        mCheckForConnectivitySnack?.saveDismiss()

        if (mUserManager.user.allowSecureConnectionsViaThirdParties && autoRetry && !isDohOngoing && !isFinishing) {
            window.decorView.postDelayed(
                { vvv.callOnClick() },
                500
            )
        }
    }

    protected fun hideNoConnSnack() {
        mNoConnectivitySnack?.saveDismiss()
        mCheckForConnectivitySnack?.saveDismiss()
    }

    private fun Snackbar.saveDismiss() {
        if (isShownOrQueued) {
            dismiss()
        }
    }
}
