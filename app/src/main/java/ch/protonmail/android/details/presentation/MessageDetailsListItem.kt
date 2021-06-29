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

package ch.protonmail.android.details.presentation

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.ui.ExpandableRecyclerAdapter
import ch.protonmail.android.utils.ui.TYPE_HEADER
import ch.protonmail.android.utils.ui.TYPE_ITEM

class MessageDetailsListItem : ExpandableRecyclerAdapter.ListItem {

    var message: Message
    var messageFormattedHtml: String? = null
    var showLoadEmbeddedImagesButton: Boolean = false

    constructor(message: Message) : super(TYPE_HEADER) {
        this.message = message
    }

    constructor(message: Message, content: String?) : super(TYPE_ITEM) {
        this.message = message
        this.messageFormattedHtml = content
    }
}
