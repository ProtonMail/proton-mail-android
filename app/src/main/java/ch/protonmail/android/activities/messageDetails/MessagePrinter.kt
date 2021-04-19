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
package ch.protonmail.android.activities.messageDetails

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import ch.protonmail.android.R
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.extensions.showToast
import timber.log.Timber

internal class MessagePrinter(
    private val context: Context,
    private val resources: Resources,
    private val printManager: PrintManager,
    private val loadRemoteImages: Boolean
) {

    private fun StringBuilder.appendRecipientsList(recipients: List<MessageRecipient>, @StringRes header: Int) {
        if (recipients.isEmpty()) {
            return
        }
        val first = recipients.first()
        append(String.format(resources.getString(header), first.emailAddress))
        recipients.drop(1).forEach {
            append(String.format(resources.getString(header), it.emailAddress))
            append("<br/>")
        }
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    fun printMessage(message: Message, bodyString: String) {
        val webView = WebView(context)
        webView.webViewClient = PrinterWebViewClient(message)
        webView.settings.blockNetworkImage = !loadRemoteImages
        val messageString = StringBuilder("<p>")
        val imagePath = "file:///android_asset/logo_print.png"
        messageString.append("<img src=\"$imagePath\" height=\"42\"")
        messageString.append("<br/>")
        messageString.append("<br/>")
        messageString.append("<hr>")
        messageString.append(String.format(resources.getString(R.string.print_from_template), message.sender))
        messageString.append("<br/>")
        messageString.appendRecipientsList(message.toList, R.string.print_to_template)
        messageString.appendRecipientsList(message.ccList, R.string.print_cc_template)
        messageString.appendRecipientsList(message.bccList, R.string.print_bcc_template)
        messageString.append(String.format(resources.getString(R.string.print_date_template), DateUtil.formatDetailedDateTime(context, message.timeMs)))
        messageString.append("<br/>")
        val attachmentList = message.Attachments
        val attachmentsCount = attachmentList.size
        messageString.append(resources.getQuantityString(R.plurals.attachments_non_descriptive,
                attachmentsCount, attachmentsCount))
        messageString.append("<br/>")
        attachmentList.forEach { attachment ->
            messageString.append(String.format(resources.getString(R.string.print_attachment_template), attachment.fileName))
            messageString.append("<br/>")
        }
        messageString.append("<br/>")
        messageString.append("</p>")
        messageString.append("<hr>")
        messageString.append(bodyString)
        webView.loadDataWithBaseURL(null, messageString.toString(), "text/HTML", "UTF-8", null)

        // Keep a reference to WebView object until you pass the PrintDocumentAdapter
        // to the PrintManager
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private inner class PrinterWebViewClient(private val message: Message?) : WebViewClient() {

        override fun onPageFinished(view: WebView, url: String) {
            val printAdapter: PrintDocumentAdapter
            if (message != null) {
                val jobName = resources.getString(R.string.app_name) + message.subject!!
                printAdapter = view.createPrintDocumentAdapter(jobName)
                try {
                    printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
                } catch (e: Exception) {
                    Timber.e(e)
                    context.showToast(R.string.print_error)
                }
            }
        }
    }
}
