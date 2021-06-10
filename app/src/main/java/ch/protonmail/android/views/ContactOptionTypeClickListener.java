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
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;
import java.util.List;

import ch.protonmail.android.R;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.utils.UiUtil;
import me.proton.core.presentation.ui.view.ProtonInput;

public class ContactOptionTypeClickListener implements View.OnClickListener {

    private final View mRowView;
    private final TextView mTextViewTitle;
    private final ProtonInput mEditTextValue;
    private String mCurrentUIValue;
    private ViewGroup mStandardOptionsView;
    private List<String> mStandardOptionUIValues;
    private List<String> mStandardOptionValues;
    private List<RadioButton> mStandardOptionsRadioButtons;
    private List<Integer> mRadioIds = new ArrayList<>();
    private Context mContext;
    private FragmentManager mSupportFragmentManager;

    public ContactOptionTypeClickListener(Context context, FragmentManager fragmentManager, View rowView, String currentUIValue, List<String> standardOptionUIValues, List<String> standardOptionValues) {
        mContext = context;
        mSupportFragmentManager = fragmentManager;
        mRowView = rowView;
        mTextViewTitle = rowView.findViewById(R.id.optionTitle);
        mEditTextValue = rowView.findViewById(R.id.option);
        mCurrentUIValue = currentUIValue;
        mStandardOptionUIValues = standardOptionUIValues;
        mStandardOptionValues = standardOptionValues;
        mStandardOptionsRadioButtons = new ArrayList<>();
    }

    private void resetRadiosState() {
        for (RadioButton radioButton : mStandardOptionsRadioButtons) {
            radioButton.setChecked(false);
        }
    }

    @Override
    public void onClick(View v) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View dialogView = inflater.inflate(R.layout.dialog_email_type, null);
        mStandardOptionsView = dialogView.findViewById(R.id.standard_option_types);
        ((TextView) (mRowView.findViewById(R.id.optionTitle))).setTextColor(ContextCompat.getColor(mContext, R.color.contact_heading));
        String[] currentValueSplit;
        if (mCurrentUIValue.contains(" ")) {
            currentValueSplit = mCurrentUIValue.split(" ");
            mCurrentUIValue = currentValueSplit[1];
        }
        View.OnClickListener rbClickListener = v1 -> {
            resetRadiosState();
            ((RadioButton) v1).setChecked(true);
        };

        mStandardOptionsRadioButtons.clear();
        if (mStandardOptionUIValues != null) {
            for (int i = 0; i < mStandardOptionValues.size(); i++) {
                String optionUI = mStandardOptionUIValues.get(i);
                String option = mStandardOptionValues.get(i);
                RadioButton optionRowView = (RadioButton) inflater.inflate(R.layout.item_vcard_option_dialog, mStandardOptionsView, false);
                mRadioIds.add(UiUtil.generateViewId(optionRowView));
                optionRowView.setText(optionUI);
                optionRowView.setTag(option);
                mStandardOptionsRadioButtons.add(optionRowView);
                optionRowView.setOnClickListener(rbClickListener);
                mStandardOptionsView.addView(optionRowView);
            }
        }
        if (!TextUtils.isEmpty(mCurrentUIValue)) {
            for (RadioButton radioButton : mStandardOptionsRadioButtons) {
                if (radioButton.getText().equals(mCurrentUIValue)) {
                    radioButton.setChecked(true);
                    break;
                }
            }
        }

        builder.setView(dialogView);
        builder.setPositiveButton(mContext.getString(R.string.okay), (dialog, which) -> {
            boolean previousValueBDay = mContext.getString(R.string.vcard_other_option_birthday).equals(mCurrentUIValue);
            for (RadioButton radioButton : mStandardOptionsRadioButtons) {
                if (radioButton.isChecked()) {
                    mCurrentUIValue = radioButton.getText().toString();
                    String currentValue = (String) radioButton.getTag();
                    mTextViewTitle.setText(mCurrentUIValue);
                    mTextViewTitle.setTag(currentValue);
                    mRowView.setTag(Constants.VCardOtherInfoType.Companion.fromName(mCurrentUIValue, mContext));
                    break;
                }
            }
            handleBirthday(previousValueBDay);
        });
        builder.setNegativeButton(mContext.getString(R.string.cancel), (dialog, which) -> {
            // noop
        });
        AlertDialog alert = builder.create();
        alert.setCanceledOnTouchOutside(false);
        alert.show();
    }

    private void handleBirthday(boolean previousValueBDay) {
        boolean currentValueBDay = mContext.getString(R.string.vcard_other_option_birthday).equals(mCurrentUIValue);
        if (previousValueBDay && !currentValueBDay) {
            mEditTextValue.setText("");
            mEditTextValue.setFocusable(true);
            mEditTextValue.setFocusableInTouchMode(true);
        } else if (!previousValueBDay && currentValueBDay) {
            mEditTextValue.setText("");
            mEditTextValue.setFocusable(false);
            mEditTextValue.setFocusableInTouchMode(false);
            mEditTextValue.setOnClickListener(new ContactBirthdayClickListener(mContext, mSupportFragmentManager));
        }
    }

}
