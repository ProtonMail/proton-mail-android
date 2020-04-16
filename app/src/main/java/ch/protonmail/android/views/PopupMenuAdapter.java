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
package ch.protonmail.android.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ch.protonmail.android.R;

/**
 * Created by dkadrikj on 7.10.15.
 */
public class PopupMenuAdapter extends ArrayAdapter<PopupMenuItem> {
    /**
     * Constructor.
     *
     * @param context the context
     * @param items   the items
     */
    public PopupMenuAdapter(Context context, ArrayList<PopupMenuItem> items) {
        super(context, 0, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_popup_menu, parent, false);
        }
        PopupMenuItem item = getItem(position);
        TextView tvTitle = (TextView) convertView.findViewById(R.id.popup_menu_title);
        ImageView ivIcon = (ImageView) convertView.findViewById(R.id.popup_menu_icon);
        // Populate the data into the template view using the data object
        tvTitle.setText(item.getTitle());
        ivIcon.setImageResource(item.getImageResId());
        return convertView;
    }
}
