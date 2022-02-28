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
package ch.protonmail.android.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.protonmail.android.R;
import ch.protonmail.android.utils.UiUtil;
import ch.protonmail.android.views.ThreeStateButton;

@Deprecated // replaced with ManageLabelsActionAdapter
public class LabelsDialogAdapter extends ArrayAdapter<LabelsDialogAdapter.LabelItem> {

    private List<LabelItem> items;
    private final int mStrokeWidth;

    public LabelsDialogAdapter(Context context, List<LabelItem> items) {
        super(context, R.layout.dialog_labels_list_item);
        this.items = items;
        addAll(items);
        mStrokeWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, context
                .getResources().getDisplayMetrics());
        setNotifyOnChange(false);
    }

    public List<LabelItem> getAllItems() {
        if (items == null) {
            return new ArrayList<>();
        }
        return items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_labels_list_item, null);
            final ViewHolder viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
            convertView = view;
        }

        final LabelItem item = getItem(position);
        String labelNameDisplay = item.name;
        if (item.name.length() > 15){
            labelNameDisplay = labelNameDisplay.substring(0, 14) + "...";
        }
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
        viewHolder.mLabelCheck.setOnStateChangedListener(null);
        viewHolder.mLabelCheck.setNumberOfStates(item.states);
        viewHolder.mLabelCheck.setVisibility(View.VISIBLE);
        viewHolder.mLabelCheck.setState(item.isAttached ? ThreeStateButton.STATE_CHECKED
                : ThreeStateButton.STATE_UNPRESSED);
        if (item.states == 3) {
            viewHolder.mLabelCheck.setState(ThreeStateButton.STATE_PRESSED);
        }
        viewHolder.mLabelName.setTag(viewHolder.mLabelCheck);
        viewHolder.mLabelCheck.setOnStateChangedListener(v -> {
            int labelState = ((ThreeStateButton) v).getState();
            if (item.states == 3) {
                item.isAttached = labelState == ThreeStateButton.STATE_CHECKED;
                item.isUnchanged = labelState == ThreeStateButton.STATE_PRESSED;
            } else {
                item.isAttached = labelState == ThreeStateButton.STATE_CHECKED ||
                        labelState == ThreeStateButton.STATE_PRESSED;
            }

        });
        viewHolder.mLabelName.setOnClickListener(v -> {
            final ThreeStateButton threeStateCheckBox = (ThreeStateButton) v.getTag();
            if (threeStateCheckBox != null) {
                threeStateCheckBox.post(threeStateCheckBox::performClick);
            }
        });
        viewHolder.mLabelName.setText(labelNameDisplay);

        GradientDrawable gd = (GradientDrawable) viewHolder.mLabelName.getBackground().getCurrent();

        String colorString = item.color;
        int color;
        if (!TextUtils.isEmpty(colorString)) {
            colorString = UiUtil.normalizeColor(colorString);
            color = Color.parseColor(colorString);
            if (!TextUtils.isEmpty(colorString)) {
                gd.setStroke(mStrokeWidth, color);
                viewHolder.mLabelName.setTextColor(color);
            }
        }

        if (item.numberOfSelectedMessages <= 0) {
            viewHolder.mNumberSelectedMessages.setVisibility(View.GONE);
        } else {
            viewHolder.mNumberSelectedMessages.setVisibility(View.VISIBLE);
            viewHolder.mNumberSelectedMessages.setText(String.valueOf(item.numberOfSelectedMessages));
        }
        return convertView;
    }

    static class ViewHolder {
        @BindView(R.id.label_name_text_view)
        TextView mLabelName;
        @BindView(R.id.label_selected_messages)
        TextView mNumberSelectedMessages;
        @BindView(R.id.label_check_box)
        ThreeStateButton mLabelCheck;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public static class LabelItem {
        public boolean isAttached;
        public String labelId;
        public String name;
        public String color;
        public int display;
        public int order;
        public boolean isUnchanged;
        public int numberOfSelectedMessages;
        public int states;

        public LabelItem(boolean isAttached) {
            this.isAttached = isAttached;
        }

    }
}
