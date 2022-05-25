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
package ch.protonmail.tokenautocomplete.example;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import ch.protonmail.tokenautocomplete.FilteredArrayAdapter;

public class TagAdapter extends FilteredArrayAdapter<Tag> {

    @LayoutRes
    private int layoutId;

    TagAdapter(Context context, @LayoutRes int layoutId, Tag[] tags) {
        super(context, layoutId, tags);
        this.layoutId = layoutId;
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {

            LayoutInflater l = (LayoutInflater)getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = l.inflate(layoutId, parent, false);
        }

        Tag t = getItem(position);
        ((TextView)convertView.findViewById(R.id.value)).setText(t.getFormattedValue());

        return convertView;
    }

    @Override
    protected boolean keepObject(Tag tag, String mask) {
        mask = mask.toLowerCase();
        return tag.getPrefix() == mask.charAt(0) &&
                tag.getValue().toLowerCase().startsWith(mask.substring(1, mask.length()));
    }
}
