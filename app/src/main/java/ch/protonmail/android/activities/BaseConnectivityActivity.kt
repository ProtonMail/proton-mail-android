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
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import android.view.View
import butterknife.BindView
import ch.protonmail.android.R
import ch.protonmail.android.jobs.PingJob
import ch.protonmail.android.utils.NetworkUtil

/**
 * Created by dkadrikj on 11/2/15.
 */
abstract class BaseConnectivityActivity: BaseActivity() {

	@BindView(R.id.layout_no_connectivity_info)
	protected lateinit var mSnackLayout: View
	var mNoConnectivitySnack: Snackbar? = null
	protected var mCheckForConnectivitySnack: Snackbar? = null

	protected var pingHandler = Handler()
	protected var pingRunnable: Runnable = Runnable { mJobManager.addJobInBackground(PingJob()) }

	private var connectivityRetryListener = RetryListener()

	protected open inner class RetryListener : View.OnClickListener {

		override fun onClick(v: View) {
			mCheckForConnectivitySnack = NetworkUtil.setCheckingConnectionSnackLayout(mSnackLayout, this@BaseConnectivityActivity)
			mCheckForConnectivitySnack!!.show()
			if (mNoConnectivitySnack != null && mNoConnectivitySnack!!.isShownOrQueued) {
				mNoConnectivitySnack!!.dismiss()
			}
			pingHandler.removeCallbacks(pingRunnable)
			pingHandler.postDelayed(pingRunnable, 3000)
		}
	}

	@JvmOverloads
	protected fun showNoConnSnack(listener: RetryListener? = null, @StringRes message: Int = R.string.no_connectivity_detected,
								  view: View = mSnackLayout) {
        mNoConnectivitySnack = mNoConnectivitySnack ?: NetworkUtil.setNoConnectionSnackLayout(
				view, this, listener ?: connectivityRetryListener, false, message)
		mNoConnectivitySnack!!.show()
		mCheckForConnectivitySnack?.saveDismiss()
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
