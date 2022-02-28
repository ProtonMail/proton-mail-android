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
package ch.protonmail.android.views;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;

import ch.protonmail.android.R;

public class MessageExpirationDialog extends DialogFragment {

    private static final int DAYS_MIN = 0;
    private static final int DAYS_MAX = 28;
    private static final int HOURS_MIN = 0;
    private static final int HOURS_MAX = 23;

    private MessageExpirationListener mListener;
    private int mDays;
    private int mHours;

    @Override
    @NonNull
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.message_expiration_dialog, null);
        setRetainInstance(true);

        final NumberPicker daysPicker = view.findViewById(R.id.days_picker);
        daysPicker.setMinValue(DAYS_MIN);
        daysPicker.setMaxValue(DAYS_MAX);
        daysPicker.setValue(mDays);
        final NumberPicker hoursPicker = view.findViewById(R.id.hours_picker);
        hoursPicker.setMinValue(HOURS_MIN);
        hoursPicker.setMaxValue(HOURS_MAX);
        hoursPicker.setValue(mHours);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), getTheme());
        builder.setTitle(R.string.set_message_expiration);
        builder.setView(view);
        builder.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
            if (mListener != null) {
                mListener.onCancel();
            }
        });

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
            if (mListener != null) {
                mListener.onMessageExpirationSet(daysPicker.getValue(), hoursPicker.getValue());
            }
        });

        return builder.create();
    }

    public void init(@NonNull MessageExpirationListener listener, int days, int hours) {
        this.mListener = listener;
        this.mDays = days;
        this.mHours = hours;
    }

    public interface MessageExpirationListener {
        void onMessageExpirationSet(int days, int hours);
        void onCancel();
    }
}
