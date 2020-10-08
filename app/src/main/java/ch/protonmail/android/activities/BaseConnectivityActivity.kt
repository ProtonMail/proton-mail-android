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

import android.view.View
import androidx.annotation.StringRes
import butterknife.BindView
import ch.protonmail.android.R
import ch.protonmail.android.utils.INetworkConfiguratorCallback
import ch.protonmail.android.utils.NetworkSnackBarUtil
import timber.log.Timber
import javax.inject.Inject

abstract class BaseConnectivityActivity : BaseActivity() {

    @Inject
    lateinit var networkSnackBarUtil: NetworkSnackBarUtil

//    @Inject
//    lateinit var sendPing: SendPing

    @BindView(R.id.layout_no_connectivity_info)
    protected lateinit var mSnackLayout: View

//    protected var pingHandler = Handler()
//    protected var pingRunnable: Runnable = Runnable { sendPing() }

    fun onRetryDefaultListener() {
        networkSnackBarUtil.getCheckingConnectionSnackBar(
            mSnackLayout
        ).show()
        networkSnackBarUtil.hideCheckingConnectionSnackBar()

//            pingHandler.removeCallbacks(pingRunnable)
//            pingHandler.postDelayed(pingRunnable, 3000)

        retryWithDoh()
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
    @Deprecated("Use [NetworkSnackBarUtil] instead")
    protected fun showNoConnSnack(
        listener: View.OnClickListener? = null,
        @StringRes message: Int = R.string.no_connectivity_detected_troubleshoot,
        view: View = mSnackLayout,
        callback: INetworkConfiguratorCallback
    ) {
        Timber.d("showNoConnSnack listener:$listener")
        listener?.onClick(view)
        val user = mUserManager.user
        val noConnectivitySnack = networkSnackBarUtil.getNoConnectionSnackBar(
            view,
            user,
            callback,
            { listener?.onClick(view) ?: onRetryDefaultListener() },
            message
        )
        noConnectivitySnack.show()

//        val contentLayout = (noConnectivitySnack.view as ViewGroup).getChildAt(0) as SnackbarContentLayout
//        val button: TextView = contentLayout.actionView
        networkSnackBarUtil.hideCheckingConnectionSnackBar()

//        if (mUserManager.user.allowSecureConnectionsViaThirdParties && autoRetry && !isDohOngoing && !isFinishing) {
//            window.decorView.postDelayed(
//                { button.callOnClick() },
//                500
//            )
//        }
    }

    protected fun hideNoConnSnack() {
        networkSnackBarUtil.hideCheckingConnectionSnackBar()
        networkSnackBarUtil.hideNoConnectionSnackBar()
    }
}
