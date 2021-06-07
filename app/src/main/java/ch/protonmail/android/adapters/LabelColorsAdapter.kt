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
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import java.util.ArrayList;
import java.util.List;

import ch.protonmail.android.R;

/**
 * Created by dkadrikj on 28.7.15.
 */
public class LabelColorsAdapter extends ArrayAdapter<LabelColorsAdapter.LabelColorItem> {

    private List<LabelColorItem> labelColorItemList;
    private int mLayoutResourceId;

    public LabelColorsAdapter(Context context, int[] colors, int layoutId) {
        super(context, layoutId);
        mLayoutResourceId = layoutId;
        labelColorItemList = new ArrayList<>();
        for (int i = 0; i < colors.length; i++) {
            LabelColorItem item = new LabelColorItem();
            item.colorId = colors[i];
            item.isChecked = false;
            labelColorItemList.add(item);
        }
        addAll(labelColorItemList);
        setNotifyOnChange(false);
    }

    public void setChecked(int position) {
        for (LabelColorItem item : labelColorItemList) {
            item.isChecked = false;
        }
        LabelColorItem item = getItem(position);
        item.isChecked = true;
        notifyDataSetChanged();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutResourceId, parent, false);
        }

        LabelColorItem item = getItem(position);

        if (mLayoutResourceId == R.layout.label_color_item_circle) {
            selectCircle(convertView, item);
        } else {
            selectRectangle(convertView, item);
        }

        return convertView;
    }

    private void selectCircle(View view, LabelColorItem item) {
        View circle = view.findViewById(R.id.circle);
        Drawable background = circle.getBackground();
        background.setColorFilter(item.colorId,  PorterDuff.Mode.SRC_IN);

        View checkView = view.findViewById(R.id.circle_selected);
        if (item.isChecked) {
            checkView.setVisibility(View.VISIBLE);
        } else {
            checkView.setVisibility(View.GONE);
        }
    }

    private void selectRectangle(View view, LabelColorItem item) {
        view.setBackgroundColor(item.colorId);

        View checkView = view.findViewById(R.id.label_color_check);
        if (item.isChecked) {
            checkView.setVisibility(View.VISIBLE);
        } else {
            checkView.setVisibility(View.GONE);
        }
    }

    static class LabelColorItem {
        public boolean isChecked;
        int colorId;
    }
}
