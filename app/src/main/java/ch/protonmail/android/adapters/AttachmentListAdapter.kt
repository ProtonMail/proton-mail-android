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
package ch.protonmail.android.adapters

import android.content.Context
import android.content.Intent
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.work.WorkManager
import ch.protonmail.android.R
import ch.protonmail.android.data.local.model.LocalAttachment
import ch.protonmail.android.worker.DeleteAttachmentWorker
import java.io.File
import java.util.ArrayList
import java.util.Comparator

class AttachmentListAdapter(
    context: Context,
    attachmentsList: List<LocalAttachment>?,
    private var numberOfEmbeddedImages: Int,
    private val workManager: WorkManager
) : ArrayAdapter<LocalAttachment>(context, 0, attachmentsList ?: emptyList()) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val listener: IAttachmentListener

    private val attachmentSortComparator = Comparator<LocalAttachment> { lhs, rhs ->
        val embeddedImageCompare = java.lang.Boolean.valueOf(lhs.isEmbeddedImage).compareTo(rhs.isEmbeddedImage)
        if (embeddedImageCompare != 0) {
            embeddedImageCompare
        } else lhs.displayName.compareTo(rhs.displayName, ignoreCase = true)
    }

    private var attachmentsList = attachmentsList?.sortedWith(attachmentSortComparator) ?: emptyList()

    val data: ArrayList<LocalAttachment>
        get() = ArrayList(attachmentsList)

    init {
        listener = context as IAttachmentListener
        sort(attachmentSortComparator)
        notifyDataSetChanged()
    }

    fun updateData(attachmentList: ArrayList<LocalAttachment>, numEmbeddedImages: Int) {
        clear()
        numberOfEmbeddedImages = numEmbeddedImages
        attachmentsList = attachmentList.sortedWith(attachmentSortComparator)
        addAll(attachmentsList)
        sort(attachmentSortComparator)
        notifyDataSetChanged()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        val attachment = getItem(position)
        var previousAttachment: LocalAttachment? = null
        if (position > 0) {
            previousAttachment = getItem(position - 1)
        }
        view = if (attachment!!.isEmbeddedImage && (position == 0 || !previousAttachment!!.isEmbeddedImage)) {
            inflater.inflate(R.layout.attachment_inline_first_list_item, parent, false)
        } else {
            inflater.inflate(R.layout.attachment_list_item, parent, false)
        }

        val attachmentName = view.findViewById<TextView>(R.id.attachment_name)
        val attachmentSize = view.findViewById<TextView>(R.id.attachment_size)
        val embeddedImageHeader = view.findViewById<TextView>(R.id.num_embedded_images_attachments)
        val embeddedImagePrefix = view.findViewById<TextView>(R.id.embedded_image_attachment)
        val removeButton = view.findViewById<ImageButton>(R.id.remove)

        if (embeddedImageHeader != null && attachment.isEmbeddedImage) {
            embeddedImageHeader.visibility = View.VISIBLE
            embeddedImageHeader.text = String.format(context.getString(R.string.inline_header), numberOfEmbeddedImages)
        } else if (embeddedImageHeader != null) {
            embeddedImageHeader.visibility = View.GONE
        }

        if (attachment.isEmbeddedImage) {
            embeddedImagePrefix.visibility = View.VISIBLE
        } else {
            embeddedImagePrefix.visibility = View.GONE
        }

        attachmentName.text = attachment.displayName
        attachmentSize.text =
            context.getString(R.string.attachment_size, Formatter.formatShortFileSize(context, attachment.size))

        removeButton.setOnClickListener {
            val isEmbedded = attachment.isEmbeddedImage
            remove(attachment)
            attachmentsList = attachmentsList.filterNot {
                attachment.attachmentId.isNotEmpty() &&
                    it.attachmentId == attachment.attachmentId ||
                    attachment.attachmentId.isEmpty() &&
                    attachment.displayName == it.displayName
            }
            if (isEmbedded) {
                numberOfEmbeddedImages -= 1
            }
            listener.onAttachmentDeleted(count, numberOfEmbeddedImages)

            DeleteAttachmentWorker.Enqueuer(workManager).enqueue(attachment.attachmentId)
        }

        attachmentName.setOnClickListener {
            if ("file" == attachment.uri.scheme) {
                val localFileUri = FileProvider.getUriForFile(
                    context, context.applicationContext.packageName + ".provider", File(attachment.uri.path)
                )
                val intent = Intent(Intent.ACTION_VIEW).setDataAndType(localFileUri, attachment.mimeType)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            }
        }

        return view
    }

    interface IAttachmentListener {

        fun onAttachmentDeleted(remainingAttachments: Int, embeddedImagesCount: Int)
        fun askStoragePermission()
    }
}
