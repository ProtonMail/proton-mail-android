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
package ch.protonmail.android.adapters;

import android.content.Context;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.protonmail.android.R;

/**
 * Created by dino on 6/9/17.
 */

public class QuickSnoozeOptionAdapter extends ArrayAdapter<QuickSnoozeOptionAdapter.QuickSnoozeItem> {

    private List<QuickSnoozeItem> mItems;

    public QuickSnoozeOptionAdapter(Context context, List<QuickSnoozeItem> items) {
        super(context, R.layout.quick_snooze_list_item);
        mItems = items;
        addAll(mItems);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.quick_snooze_list_item, null);
            final ViewHolder viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
            convertView = view;
        }

        QuickSnoozeItem item = getItem(position);
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.mOptionName.setText(item.name);
        return convertView;
    }

    static class ViewHolder {
        @BindView(R.id.quick_snooze_name)
        TextView mOptionName;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public static class QuickSnoozeItem {
        public boolean selected;
        public String name;

        public QuickSnoozeItem(boolean selected, String name) {
            this.selected = selected;
            this.name = name;
        }
    }
}
