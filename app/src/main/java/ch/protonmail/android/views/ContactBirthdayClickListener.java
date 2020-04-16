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
import androidx.fragment.app.FragmentManager;
import android.view.View;
import android.widget.TextView;

import java.util.Date;

import ch.protonmail.android.activities.fragments.DatePickerFragment;
import ch.protonmail.android.utils.DateUtil;

/**
 * Created by dino on 12/16/17.
 */

public class ContactBirthdayClickListener implements View.OnClickListener {

    private Context mContext;
    private FragmentManager mSupportFragmentManager;

    public ContactBirthdayClickListener(Context context, FragmentManager fragmentManager) {
        mContext = context;
        mSupportFragmentManager = fragmentManager;
    }

    @Override
    public void onClick(View v) {
        DatePickerFragment newFragment = new DatePickerFragment();
        final TextView bDayView = (TextView) v;
        newFragment.setListener(new DatePickerFragment.IDateChooserListener() {
            @Override
            public void onDateSet(Date date) {
                bDayView.setText(DateUtil.formatDate(date));
                bDayView.setTag(date);
            }
        });
        newFragment.show(mSupportFragmentManager, "ContactBdayDatePicker");
    }
}
