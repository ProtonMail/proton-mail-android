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
package ch.protonmail.android.utils.extensions

import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.contact_groups_email_list_item_closeable.view.*

/**
 * Created by kadrikj on 9/6/18. */
fun <T : RecyclerView.ViewHolder> T.listen(event: (position: Int) -> Unit) {
    itemView.setOnClickListener {
        event.invoke(layoutPosition)
    }
}
fun <T : RecyclerView.ViewHolder> T.listenForDelete(event: (position: Int) -> Unit) {
    itemView.delete.setOnClickListener {
        event.invoke(layoutPosition)
    }
}