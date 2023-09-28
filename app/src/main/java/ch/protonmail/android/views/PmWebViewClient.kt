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
package ch.protonmail.android.views

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.MailTo
import android.net.Uri
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.core.text.HtmlCompat
import ch.protonmail.android.R
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.DUMMY_URL_PREFIX
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.settings.data.AccountSettingsRepository
import ch.protonmail.android.utils.MessageUtils.addRecipientsToIntent
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showInfoDialogWithTwoButtonsAndCheckbox
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import kotlinx.coroutines.runBlocking
import me.proton.core.presentation.utils.showToast
import me.proton.core.util.kotlin.startsWith
import timber.log.Timber
import java.io.ByteArrayInputStream
import java.net.MalformedURLException
import java.net.URL
import java.util.Locale

open class PmWebViewClient(
    private val userManager: UserManager,
    private val accountSettingsRepository: AccountSettingsRepository,
    private val activity: Activity,
    private var shouldLoadRemoteContent: Boolean
) : WebViewClient() {

    private var blockedImages = 0
    private var isPhishingMessage = false
    private val hyperlinkConfirmationWhitelistedHosts = listOf(
        "protonmail.com",
        "protonmail.ch",
        "protonvpn.com",
        "protonstatus.com",
        "gdpr.eu",
        "protonvpn.net",
        "pm.me",
        "mail.protonmail.com",
        "account.protonvpn.com",
        "protonirockerxow.onion",
        "proton.me"
    )

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
        val fixedUrl = url.replaceFirst(DUMMY_URL_PREFIX.toRegex(), "")
        if (fixedUrl startsWith "mailto:") {
            composeMessageWithMailToData(fixedUrl, view.context.applicationContext)
            return true
        }
        if (fixedUrl startsWith "tel:") {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse(fixedUrl)
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            } else {
                activity.showToast(R.string.no_application_found)
            }
        } else {
            if (fixedUrl.isNotBlank()) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(fixedUrl)
                if (showHyperlinkConfirmation(fixedUrl).not()) {
                    try {
                        activity.startActivity(intent)
                    } catch (notFoundException: ActivityNotFoundException) {
                        Timber.i(notFoundException, "Unable to open link")
                        activity.showToast(R.string.no_application_found)
                    }
                }
            }
        }
        return true
    }

    fun setPhishingCheck(isPhishingMessage: Boolean) {
        this.isPhishingMessage = isPhishingMessage
    }

    private fun composeMessageWithMailToData(url: String, context: Context) {
        val intent = Intent(context, ComposeMessageActivity::class.java)

        val mailTo = MailTo.parse(url)
        val user = userManager.currentUser
            ?: return
        addRecipientsToIntent(
            intent = intent,
            extraName = ComposeMessageActivity.EXTRA_TO_RECIPIENTS,
            recipientList = mailTo.to,
            messageAction = Constants.MessageActionType.FROM_URL,
            userAddresses = user.addresses
        )
        addRecipientsToIntent(
            intent = intent,
            extraName = ComposeMessageActivity.EXTRA_CC_RECIPIENTS,
            recipientList = mailTo.cc,
            messageAction = Constants.MessageActionType.FROM_URL,
            userAddresses = user.addresses
        )
        intent.putExtra(ComposeMessageActivity.EXTRA_MAIL_TO, true)
            .putExtra(ComposeMessageActivity.EXTRA_MESSAGE_TITLE, mailTo.subject)
            .putExtra(ComposeMessageActivity.EXTRA_MESSAGE_BODY, mailTo.body)
        activity.startActivity(intent)
    }

    /**
     * @return true if confirmation was shown
     */
    private fun showHyperlinkConfirmation(url: String): Boolean {
        try {
            val parsedUrl = URL(url)

            // only handle http/s protocols
            val protocol = parsedUrl.protocol.lowercase(Locale.getDefault())
            if (protocol != "http" && protocol != "https") return false

            // whitelist domains
            for (host in hyperlinkConfirmationWhitelistedHosts) {
                if (parsedUrl.host.endsWith(host)) return false
            }
        } catch (e: MalformedURLException) {
            e.printStackTrace()
        }
        val doesRequireHyperlinkConfirmation = runBlocking {
            accountSettingsRepository
                .getShouldShowLinkConfirmationSetting(userManager.requireCurrentUserId())
        }

        return when {
            isPhishingMessage -> {
                showPhishingHyperlinkConfirmation(url)
                true
            }
            doesRequireHyperlinkConfirmation -> {
                showRegularHyperlinkConfirmation(url)
                true
            }
            else -> {
                false
            }
        }
    }

    private fun showRegularHyperlinkConfirmation(url: String) {
        val message = HtmlCompat.fromHtml(
            activity.getString(R.string.hyperlink_confirmation_dialog_text_html, ellipsesUrlIfTooLong(url)),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        showInfoDialogWithTwoButtonsAndCheckbox(
            context = activity,
            title = "",
            message = message,
            negativeBtnText = activity.getString(R.string.cancel),
            positiveBtnText = activity.getString(
                R.string.cont
            ),
            checkBoxText = activity.getString(R.string.dont_ask_again),
            okListener = {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse(url)
                try {
                    activity.startActivity(intent)
                } catch (notFoundException: ActivityNotFoundException) {
                    Timber.i(notFoundException, "Unable to open link")
                    activity.showToast(R.string.no_application_found)
                    it.dismiss()
                }
            },
            checkedListener = { isChecked ->
                runBlocking {
                    accountSettingsRepository.saveShouldShowLinkConfirmationSetting(
                        shouldShowHyperlinkConfirmation = isChecked.not(),
                        userId = userManager.requireCurrentUserId()
                    )
                }
            },
            cancelable = true
        )
    }

    private fun showPhishingHyperlinkConfirmation(url: String) {
        val message = HtmlCompat.fromHtml(
            activity.getString(R.string.details_hyperlink_phishing_dialog_content, ellipsesUrlIfTooLong(url)),
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        activity.showTwoButtonInfoDialog(
            titleStringId = R.string.details_hyperlink_phishing_dialog_title,
            message = message,
            positiveStringId = R.string.details_hyperlink_phishing_dialog_confirm_action,
            negativeStringId = R.string.details_hyperlink_phishing_dialog_cancel_action
        ) {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            try {
                activity.startActivity(intent)
            } catch (notFoundException: ActivityNotFoundException) {
                Timber.i(notFoundException, "Unable to open link")
                activity.showToast(R.string.no_application_found)
            }
        }
    }

    private fun ellipsesUrlIfTooLong(url: String) =
        if (url.length > 100) {
            url.substring(0, 60) + "..." + url.substring(url.length - 40)
        } else {
            url
        }

    protected fun amountOfRemoteResourcesBlocked(): Int =
        blockedImages

    fun showRemoteResources(show: Boolean) {
        if (show) blockedImages = 0
        shouldLoadRemoteContent = show
    }

    @Deprecated("Deprecated in Java")
    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        if (shouldLoadRemoteContent) {
            return super.shouldInterceptRequest(view, url)
        }
        val uri = Uri.parse(url)
        if (uri.scheme.equals("cid", ignoreCase = true) || uri.scheme.equals("data", ignoreCase = true)) {
            return super.shouldInterceptRequest(view, url)
        }
        blockedImages++
        return WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
    }
}
