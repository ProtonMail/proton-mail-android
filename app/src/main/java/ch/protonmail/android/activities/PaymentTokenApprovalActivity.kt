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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import ch.protonmail.android.R
import ch.protonmail.android.api.models.GetPaymentTokenResponse
import ch.protonmail.android.api.models.PaymentToken
import ch.protonmail.android.core.Constants
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToLogin
import ch.protonmail.android.views.PMWebView
import com.squareup.otto.Subscribe
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// region constants
const val EXTRA_PAYMENT_TOKEN = "EXTRA_PAYMENT_TOKEN"
const val EXTRA_PAYMENT_TYPE_STRING = "EXTRA_PAYMENT_TYPE_STRING"
const val EXTRA_PAYMENT_RETURN_HOST_STRING = "EXTRA_PAYMENT_RETURN_HOST_STRING"
const val EXTRA_RESULT_STATUS_STRING = "RESULT_EXTRA_STATUS_STRING"
const val EXTRA_RESULT_PAYMENT_TOKEN_STRING = "RESULT_EXTRA_PAYMENT_TOKEN_STRING"

const val RESULT_CODE_ERROR = 5

private const val POLLING_INTERVAL_IN_MS = 2_000L
private const val POLLING_TIMEOUT_IN_MS = 60_000L
// endregion

/**
 * Activity designated for additional approval of PaymentTokens.
 */

class PaymentTokenApprovalActivity : BaseActivity() {

    private val webView by lazy {
        findViewById<PMWebView>(R.id.webView).apply {
            settings.javaScriptEnabled = true // JavaScript support is needed for PayPal

            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    url?.let { return handleCancelRedirection(Uri.parse(it)) }
                    return false
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        request?.url?.let { return handleCancelRedirection(it) }
                    }
                    return false
                }
            }
        }
    }

    private val handler = Handler()
    private lateinit var paymentToken: String
    private lateinit var paymentReturnHost: String
    private var cancelled = false
    private var pollingStartTimestamp = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
        }

        intent?.data?.apply {
            webView.loadUrl(this.toString())
        }

        paymentToken = intent.getStringExtra(EXTRA_PAYMENT_TOKEN) ?: ""
        paymentReturnHost = intent.getStringExtra(EXTRA_PAYMENT_RETURN_HOST_STRING) ?: ""
    }

    private val checkStatusRunnable = object : Runnable {
        override fun run() {
            val runnable = this

            mApi.getPaymentToken(paymentToken).enqueue(object : Callback<GetPaymentTokenResponse> {
                override fun onFailure(call: Call<GetPaymentTokenResponse>, t: Throwable) {

                    if (cancelled) return

                    if (System.currentTimeMillis() - pollingStartTimestamp > POLLING_TIMEOUT_IN_MS) {
                        setResult(RESULT_CODE_ERROR, intent)
                        finish()
                    } else {
                        // ignore network error and poll less often
                        handler.postDelayed(runnable, POLLING_INTERVAL_IN_MS * 2)
                    }
                }

                override fun onResponse(call: Call<GetPaymentTokenResponse>, response: Response<GetPaymentTokenResponse>) {

                    if (cancelled) return

                    if (System.currentTimeMillis() - pollingStartTimestamp > POLLING_TIMEOUT_IN_MS) {
                        setResult(RESULT_CODE_ERROR, intent)
                        finish()
                        return
                    }

                    if (response.isSuccessful) {
                        response.body()?.let {

                            val intent = Intent()
                                    .putExtra(EXTRA_RESULT_STATUS_STRING, it.status.name)
                                    .putExtra(EXTRA_RESULT_PAYMENT_TOKEN_STRING, paymentToken)

                            when {
                                it.status == PaymentToken.Status.PENDING -> {
                                    // still waiting for approval
                                    handler.postDelayed(runnable, POLLING_INTERVAL_IN_MS)
                                }
                                it.status == PaymentToken.Status.CHARGEABLE -> {
                                    // success, payment has been confirmed
                                    showToast(R.string.payment_approval_success)
                                    setResult(Activity.RESULT_OK, intent)
                                    finish()
                                }
                                else -> {
                                    setResult(RESULT_CODE_ERROR, intent)
                                    finish()
                                }
                            }
                        }
                    } else {
                        handler.postDelayed(runnable, POLLING_INTERVAL_IN_MS)
                    }

                }
            })

            handler.removeCallbacks(runnable, POLLING_INTERVAL_IN_MS)
        }
    }

    /**
     * @return true if we want to handle this Uri, not leave it to WebView
     */
    private fun handleCancelRedirection(uri: Uri) : Boolean {
        return if (uri.host == Constants.SECURE_ENDPOINT_HOST || uri.host == paymentReturnHost) { // TODO leave the SECURE_ENDPOINT_HOST to be on the safe side
            if (uri.getQueryParameter("cancel") == "1") { // user explicitly cancelled
                setResult(Activity.RESULT_CANCELED)
                finish()
            } else {
                pollingStartTimestamp = System.currentTimeMillis()
                handler.postDelayed(checkStatusRunnable, 0)
            }
            true
        } else false
    }

    override fun onResume() {
        super.onResume()
        if (cancelled) {
            cancelled = false
            pollingStartTimestamp = System.currentTimeMillis()
            handler.postDelayed(checkStatusRunnable, POLLING_INTERVAL_IN_MS)
        }
    }

    override fun onPause() {
        super.onPause()
        cancelled = true // stop polling PaymentToken status
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.payment_approval_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_cancel -> {
                setResult(Activity.RESULT_CANCELED)
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_payment_token_approval
    }

    companion object {

        fun createApprovalIntent(context: Context, approvalUrl: String, paymentToken: String, paymentType: String, returnHost: String): Intent {
            return Intent(context, PaymentTokenApprovalActivity::class.java)
                    .putExtra(EXTRA_PAYMENT_TOKEN, paymentToken)
                    .putExtra(EXTRA_PAYMENT_TYPE_STRING, paymentType)
                    .putExtra(EXTRA_PAYMENT_RETURN_HOST_STRING, returnHost)
                    .setData(Uri.parse(approvalUrl))
        }
    }

    @Subscribe
    @Suppress("unused", "UNUSED_PARAMETER")
    fun onLogoutEvent(event: LogoutEvent?) {
        moveToLogin()
    }
}
