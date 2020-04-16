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
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import java.util.Arrays;
import java.util.List;

import ch.protonmail.android.R;

/**
 * Created by dino on 8/8/17.
 */

public class SnoozeRepeatDayView extends androidx.appcompat.widget.AppCompatTextView {

    private boolean mSelected;
    private String mCode;

    public SnoozeRepeatDayView(Context context) {
        super(context);
        init();
    }

    public SnoozeRepeatDayView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SnoozeRepeatDayView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        List<String> snoozeRepeatCodes = Arrays.asList(getResources().getStringArray(R.array.repeating_snooze_days));
        List<String> snoozeRepeatNames = Arrays.asList(getResources().getStringArray(R.array.snooze_repeat_values));
        int id = getId();
        switch (id) {
            case R.id.monday:
                setText(snoozeRepeatNames.get(0));
                mCode = snoozeRepeatCodes.get(0);
                break;
            case R.id.tuesday:
                setText(snoozeRepeatNames.get(1));
                mCode = snoozeRepeatCodes.get(1);
                break;
            case R.id.wednesday:
                setText(snoozeRepeatNames.get(2));
                mCode = snoozeRepeatCodes.get(2);
                break;
            case R.id.thursday:
                setText(snoozeRepeatNames.get(3));
                mCode = snoozeRepeatCodes.get(3);
                break;
            case R.id.friday:
                setText(snoozeRepeatNames.get(4));
                mCode = snoozeRepeatCodes.get(4);
                break;
            case R.id.saturday:
                setText(snoozeRepeatNames.get(5));
                mCode = snoozeRepeatCodes.get(5);
                break;
            case R.id.sunday:
                setText(snoozeRepeatNames.get(6));
                mCode = snoozeRepeatCodes.get(6);
                break;
        }
    }

    public boolean isSelected() {
        return mSelected;
    }

    public String getCode() {
        return mCode;
    }

    public void setSelected(List<String> selectedValues) {
        for (String value : selectedValues) {
            if (value.equals(mCode)) {
                setSelected(true);
            }
        }
    }

    public void setSelected(boolean selected) {
        mSelected = selected;
        if (mSelected) {
            setBackground(getResources().getDrawable(R.drawable.repeat_day_selected_background));
            setTextColor(getResources().getColor(R.color.white));
        } else {
            setBackground(getResources().getDrawable(R.drawable.repeat_day_unselected_background));
            setTextColor(getResources().getColor(R.color.dark_purple_statusbar));
        }
    }

    public void setBackground(Drawable drawable) {
        int sdk = android.os.Build.VERSION.SDK_INT;
        if(sdk < Build.VERSION_CODES.JELLY_BEAN) {
            setBackgroundDrawable(drawable);
        } else {
            super.setBackground(drawable);
        }
    }
}
