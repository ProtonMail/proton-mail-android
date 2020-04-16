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
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.protonmail.android.R;
import ch.protonmail.android.utils.UiUtil;
/**
 * Created by dkadrikj on 20.7.15.
 */
public class FoldersAdapter extends ArrayAdapter<FoldersAdapter.FolderItem> {

    private List<FolderItem> items;
    private int mStrokeWidth = 2;

    public FoldersAdapter(Context context, List<FolderItem> items) {
        super(context, R.layout.folder_list_item);
        this.items = items;
        addAll(items);
        mStrokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, context.getResources().getDisplayMetrics());
        setNotifyOnChange(false);
    }

    public void updateData(List<FolderItem> items) {
        clear();
        this.items.clear();
        addAll(items);
        this.items = items;
        notifyDataSetChanged();
    }

    public List<FolderItem> getAllItems() {
        if (items == null) {
            return new ArrayList<>();
        }
        return items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.folder_list_item, null);
            final ViewHolder viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
            convertView = view;
        }

        final FolderItem item = getItem(position);
        String labelNameDisplay = item.name;
        if (item.name.length() > 15){
            labelNameDisplay = labelNameDisplay.substring(0, 14) + "...";
        }
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.mFolderName.setText(labelNameDisplay);

        String colorString = item.color;
        int color;
        if (item.icon > 0) {
            viewHolder.mFolderIcon.setImageResource(item.icon);
        } else if (item.icon != -1) {
            viewHolder.mFolderIcon.setImageResource(R.drawable.ic_folder_drawer);
        } else {
            viewHolder.mFolderIcon.setImageResource(R.drawable.add);
            viewHolder.mFolderIcon.setColorFilter(0xCF383A3B, PorterDuff.Mode.SRC_IN);
        }
        if (!TextUtils.isEmpty(colorString)) {
            colorString = UiUtil.normalizeColor(colorString);
            color = Color.parseColor(colorString);
            if (!TextUtils.isEmpty(colorString)) {
                viewHolder.mFolderIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
        }
        return convertView;
    }

    static class ViewHolder {
        @BindView(R.id.folder_name)
        TextView mFolderName;
        @BindView(R.id.folder_icon)
        ImageView mFolderIcon;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public static class FolderItem {
        public String labelId;
        public String name;
        public String color;
        public int display;
        public int order;
        public int icon;
    }
}
