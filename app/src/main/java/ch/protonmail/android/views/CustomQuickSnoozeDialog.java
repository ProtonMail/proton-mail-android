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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import ch.protonmail.android.R;

public class CustomQuickSnoozeDialog extends DialogFragment {

    private static final int MINUTES_MIN = 0;
    private static final int MINUTES_MAX = 59;
    private static final int HOURS_MIN = 0;
    private static final int HOURS_MAX = 24;

    private CustomQuickSnoozeListener mListener;
    private int mMinutes;
    private int mHours;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.custom_quick_snooze_dialog, null, false);
        setRetainInstance(true);

        final NumberPicker hoursPicker = view.findViewById(R.id.hours_picker);
        hoursPicker.setMinValue(HOURS_MIN);
        hoursPicker.setMaxValue(HOURS_MAX);
        hoursPicker.setValue(mHours);

        final NumberPicker minutesPicker = view.findViewById(R.id.minutes_picker);
        minutesPicker.setMinValue(MINUTES_MIN);
        minutesPicker.setMaxValue(MINUTES_MAX);
        minutesPicker.setValue(mMinutes);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), getTheme());
        builder.setTitle(R.string.set_custom_quick_snooze);
        builder.setView(view);
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            if (mListener != null) {
                mListener.onCancel();
            }
        });

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            if (mListener != null) {
                mListener.onCustomQuickSnoozeSet(hoursPicker.getValue() * 60 + minutesPicker.getValue());
            }
        });

        return builder.create();
    }

    public void init(@NonNull CustomQuickSnoozeListener listener, int hours, int minutes) {
        this.mListener = listener;
        this.mMinutes = minutes;
        this.mHours = hours;
    }

    public interface CustomQuickSnoozeListener {
        void onCustomQuickSnoozeSet(int minutes);
        void onCancel();
    }
}
