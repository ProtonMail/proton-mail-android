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
package ch.protonmail.android.activities.messageDetails.attachments

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.annotation.DrawableRes
import ch.protonmail.android.R
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.views.messageDetails.AttachmentDetailView

class MessageDetailsAttachmentListAdapter(
    context: Context,
    private val downloadListener: (String) -> Unit
) : ArrayAdapter<Attachment>(context, R.layout.view_attachment_detail) {
    private val downloadingIds = mutableSetOf<String>()
    private var pgpEncrypted: Boolean = false

    @DrawableRes
    private fun String?.getIconRes(): Int {
        return when (this) {
            "image/jpeg", "image/pjpeg" -> R.drawable.mail_attachment_jpeg
            "image/png" -> R.drawable.mail_attachment_png
            "application/zip", "application/x-compressed", "application/x-zip-compressed", "multipart/x-zip" -> R.drawable.mail_attachment_zip
            "text/plain" -> R.drawable.mail_attachment_txt
            "application/pdf" -> R.drawable.mail_attachment_pdf
            "application/msword" -> R.drawable.mail_attachment_doc
            else -> R.drawable.mail_attachment_file
        }
    }

    fun setList(attachmentsList: List<Attachment>) {
        downloadingIds.clear()
        clear()
        addAll(attachmentsList)
    }

    fun setDownloaded(attachmentId: String, isDownloaded: Boolean) {
        if (isDownloaded) {
            downloadingIds.remove(attachmentId)
        } else {
            downloadingIds.add(attachmentId)
        }
        notifyDataSetChanged()
    }

    fun setIsPgpEncrypted(isPgpEncrypted: Boolean) {
        pgpEncrypted = isPgpEncrypted
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {

        val view = convertView as? AttachmentDetailView ?: AttachmentDetailView(context)
        val attachment = getItem(position) ?: return view

        val fileName = attachment.fileName
        val fileSize = attachment.fileSize
        val mimeType = attachment.mimeType
        val attachmentSpecificIcon = mimeType.getIconRes()
        val headers = attachment.headers
        val showWarningIcon = headers != null && headers.contentEncryption == "on-delivery" && pgpEncrypted
        val attachmentId = attachment.attachmentId
        val isDownloading = downloadingIds.contains(attachmentId)
        view.bind(fileName, fileSize, attachmentSpecificIcon, showWarningIcon, isDownloading)

        val onClickListener = View.OnClickListener { downloadListener(attachmentId!!) }
        view.setOnClickListener(onClickListener)

        return view
    }
}
