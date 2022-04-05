/*
 * Copyright (c) 2022 Proton Technologies AG
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
package ch.protonmail.android.views

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.MailTo
import android.net.Uri
import android.preference.PreferenceManager
import android.text.Html
import android.text.TextUtils
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import ch.protonmail.android.R
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.MessageUtils.addRecipientsToIntent
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showInfoDialogWithTwoButtonsAndCheckbox
import java.io.ByteArrayInputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.Arrays

open class PMWebViewClient(
    private val mUserManager: UserManager,
    private val mActivity: Activity,
    private var mLoadRemote: Boolean
) : WebViewClient() {

    private var mBlockedImages = 0
    private val hyperlinkConfirmationWhitelistedHosts = Arrays.asList(
        "protonmail.com",
        "protonmail.ch",
        "protonvpn.com",
        "protonstatus.com",
        "gdpr.eu",
        "protonvpn.net",
        "pm.me",
        "mail.protonmail.com",
        "account.protonvpn.com",
        "protonirockerxow.onion"
    )

    override fun shouldOverrideUrlLoading(view: WebView, url1: String): Boolean {
        val url = url1.replaceFirst(Constants.DUMMY_URL_PREFIX.toRegex(), "")
        if (url.startsWith("mailto:")) {
            composeMessageWithMailToData(url, view.context.applicationContext)
            return true
        }
        if (url.startsWith("tel:")) {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse(url)
            if (intent.resolveActivity(mActivity.packageManager) != null) {
                mActivity.startActivity(intent)
            } else {
                mActivity.showToast(R.string.no_application_found)
            }
        } else {
            if (!TextUtils.isEmpty(url)) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                if (intent.resolveActivity(mActivity.packageManager) != null) {
                    if (!showHyperlinkConfirmation(url)) {
                        mActivity.startActivity(intent)
                    }
                } else {
                    mActivity.showToast(R.string.no_application_found_or_link_invalid)
                }
            }
        }
        return true
    }

    private fun composeMessageWithMailToData(url: String, context: Context) {
        val intent = AppUtil.decorInAppIntent(
            Intent(context, ComposeMessageActivity::class.java)
        )
        val mt = MailTo.parse(url)
        val user = mUserManager.currentLegacyUser
        addRecipientsToIntent(
            intent,
            ComposeMessageActivity.EXTRA_TO_RECIPIENTS,
            mt.to,
            Constants.MessageActionType.FROM_URL,
            user!!.addresses
        )
        addRecipientsToIntent(
            intent,
            ComposeMessageActivity.EXTRA_CC_RECIPIENTS,
            mt.cc,
            Constants.MessageActionType.FROM_URL,
            user.addresses
        )
        intent.putExtra(ComposeMessageActivity.EXTRA_MAIL_TO, true)
        intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TITLE, mt.subject)
        intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_BODY, mt.body)
        mActivity.startActivity(intent)
    }

    /**
     *
     * @return true if confirmation was shown
     */
    private fun showHyperlinkConfirmation(url: String): Boolean {
        try {
            val parsedUrl = URL(url)

            // only handle http/s protocols
            if ("http" != parsedUrl.protocol.toLowerCase() && "https" != parsedUrl.protocol.toLowerCase()) return false

            // whitelist domains
            for (host in hyperlinkConfirmationWhitelistedHosts) {
                if (parsedUrl.host.endsWith(host)) return false
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        return if (PreferenceManager.getDefaultSharedPreferences(mActivity)
                .getBoolean(Constants.Prefs.PREF_HYPERLINK_CONFIRM, true)) {
            var hyperlink = url // truncate if too long
            if (hyperlink.length > 100) {
                hyperlink = hyperlink.substring(0, 60) + "..." + hyperlink.substring(hyperlink.length - 40)
            }
            val message = Html.fromHtml(
                String.format(mActivity.getString(R.string.hyperlink_confirmation_dialog_text_html), hyperlink)
            )
            showInfoDialogWithTwoButtonsAndCheckbox(mActivity, "", message, mActivity.getString(R.string.cancel),
                mActivity.getString(
                    R.string.cont
                ), mActivity.getString(R.string.dont_ask_again), { unit: Unit ->
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                mActivity.startActivity(intent)
                null
            }, { isChecked: Boolean? ->
                PreferenceManager.getDefaultSharedPreferences(mActivity).edit()
                    .putBoolean(Constants.Prefs.PREF_HYPERLINK_CONFIRM, !isChecked!!).apply()
                null
            }, true
            )
            true
        } else {
            false
        }
    }

    protected fun amountOfRemoteResourcesBlocked(): Int {
        return mBlockedImages
    }

    fun blockRemoteResources(block: Boolean) {
        mBlockedImages = 0
        mLoadRemote = !block
    }

    fun allowLoadingRemoteResources() {
        blockRemoteResources(false)
    }

    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        if (mLoadRemote) {
            return super.shouldInterceptRequest(view, url)
        }
        val uri = Uri.parse(url)
        if (uri.scheme.equals("cid", ignoreCase = true) || uri.scheme.equals("data", ignoreCase = true)) {
            return super.shouldInterceptRequest(view, url)
        }
        if (url.toLowerCase().contains("/favicon.ico")) {
            return super.shouldInterceptRequest(view, url)
        }
        mBlockedImages++
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
    }
}
