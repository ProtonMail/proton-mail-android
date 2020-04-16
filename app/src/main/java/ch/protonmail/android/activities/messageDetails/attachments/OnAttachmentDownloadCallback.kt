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

import ch.protonmail.android.permissions.PermissionHelper
import java.util.concurrent.atomic.AtomicReference

/**
 * Created by Kamil Rajtar on 15.08.18.
 */
internal class OnAttachmentDownloadCallback(private val storagePermissionHelper: PermissionHelper,
											private val attachmentToDownloadId: AtomicReference<String>) : Function1<String, Unit> {

	override fun invoke(attachmentId:String) {
		attachmentToDownloadId.set(attachmentId)
		storagePermissionHelper.checkPermission()
	}
}
