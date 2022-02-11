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

import android.app.Activity;
import android.content.Context;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.utils.UiUtil;
import ezvcard.property.Address;

/**
 * Created by dino on 12/16/17.
 */
public class ContactAddressView extends LinearLayout {

    //region common address UI elements
    @BindView(R.id.optionIcon)
    TextView optionIcon;
    @BindView(R.id.optionTitle)
    TextView mOptionTitleView;
    @BindView(R.id.btnOptionType)
    ImageButton mBtnOptionTypeView;
    @BindView(R.id.address_street)
    EditText mAddressStreetView;
    @BindView(R.id.address_street_extended)
    EditText mAddressExtendedStreetView;
    @BindView(R.id.address_city)
    EditText mAddressCityView;
    @BindView(R.id.address_region)
    EditText mAddressRegionView;
    @BindView(R.id.address_postcode)
    EditText mAddressPostcodeView;
    @BindView(R.id.address_po_box)
    EditText mAddressPoBoxView;
    @BindView(R.id.address_country)
    EditText mAddressCountryView;
    @BindView(R.id.address_full_combined)
    TextView mAddressFullCombinedView;
    @BindView(R.id.address_detailed_parent)
    View mAddressDetailsParentView;
    VCardLinearLayout mLinearLayoutParent;
    //endregion

    //region new address UI elements
    @Nullable @BindView(R.id.new_row_parent)
    View mNewRowParent;
    @Nullable @BindView(R.id.title)
    TextView mRowTitleView;
    @Nullable @BindView(R.id.fields_parent)
    View mInputFieldsView;
    //endregion

    private boolean mIsDirty = false;
    private boolean mHasFocus = false;
    private boolean mIsNew;

    //region common values
    private final List<String> mStandardOptionUIValues;
    private final List<String> mStandardOptionValues;
    //endregion
    //region existing address values
    private Address mAddress;
    private String mOptionHint;
    //endregion

    public ContactAddressView(final Context context, final String titleText, final String optionTitleText, final List<String> standardOptionUIValues, final List<String> standardOptionValues, final VCardLinearLayout rootView) {
        super(context, null, 0);
        mIsNew = true;
        mStandardOptionUIValues = standardOptionUIValues;
        mStandardOptionValues = standardOptionValues;
        mLinearLayoutParent = rootView;

        View view = LayoutInflater.from(context).inflate(R.layout.contact_new_vcard_address, this, true);
        ButterKnife.bind(this);
        UiUtil.generateViewId(view);

        setHandlers();
        optionIcon.setText("\ue910");
        mAddressDetailsParentView.setVisibility(View.VISIBLE);
        mAddressFullCombinedView.setVisibility(GONE);
        mAddressFullCombinedView.setOnClickListener(new AddressClickListener(mAddressFullCombinedView, mAddressDetailsParentView, mAddressStreetView, mAddressExtendedStreetView, mAddressPostcodeView, mAddressCityView, mAddressPoBoxView, mAddressRegionView, mAddressCountryView));
        final FragmentManager fragmentManager = ((FragmentActivity) getContext()).getSupportFragmentManager();
        mOptionTitleView.setText(optionTitleText);
        mNewRowParent.setClickable(true);
        mNewRowParent.setOnClickListener(v -> {
            mRowTitleView.setVisibility(View.GONE);
            mInputFieldsView.setVisibility(View.VISIBLE);
            mNewRowParent.setVisibility(View.GONE);
            ContactOptionTypeClickListener optionTypeClickListener =
                    new ContactOptionTypeClickListener(getContext(), fragmentManager, view, "\ue910 "+standardOptionUIValues.get(0), standardOptionUIValues, standardOptionValues);
            mBtnOptionTypeView.setOnClickListener(optionTypeClickListener);
            mOptionTitleView.setOnClickListener(optionTypeClickListener);
            rootView.addView(new ContactAddressView(context, titleText, optionTitleText, standardOptionUIValues, standardOptionValues, rootView));
        });
        // set the values

        mRowTitleView.setText(titleText);


    }

    public ContactAddressView(final Context context, final String optionType, final String optionUIType, final Address address,
                              final String optionHint, final List<String> standardOptionUIValues, final List<String> standardOptionValues, final VCardLinearLayout rootView) {
        super(context, null, 0);
        mIsNew = false;
        mStandardOptionUIValues = standardOptionUIValues;
        mStandardOptionValues = standardOptionValues;
        mLinearLayoutParent = rootView;

        mAddress = address;
        mOptionHint = optionHint;

        View view = LayoutInflater.from(context).inflate(R.layout.contact_vcard_address_editable, this, true);
        ButterKnife.bind(this);
        UiUtil.generateViewId(view);
        setHandlers();

        optionIcon.setText("\ue910");
        if (standardOptionUIValues == null || standardOptionUIValues.size() == 0) {
            mBtnOptionTypeView.setVisibility(View.GONE);
        }
        final FragmentManager fragmentManager = ((FragmentActivity) getContext()).getSupportFragmentManager();

        ContactOptionTypeClickListener optionTypeClickListener = new ContactOptionTypeClickListener(getContext(), fragmentManager, view, "\uE910 "+optionUIType, standardOptionUIValues, standardOptionValues);
        mBtnOptionTypeView.setOnClickListener(optionTypeClickListener);
        mOptionTitleView.setOnClickListener(optionTypeClickListener);
        mOptionTitleView.setText(optionUIType);
        mOptionTitleView.setTag(optionType);

        fillAddressValues();

        mAddressFullCombinedView.setOnClickListener(new AddressClickListener(mAddressFullCombinedView, mAddressDetailsParentView, mAddressStreetView, mAddressExtendedStreetView, mAddressPostcodeView, mAddressCityView, mAddressPoBoxView, mAddressRegionView, mAddressCountryView));
    }

    private void fillAddressValues(String streetAddress, String extendedStreetAddress, String postalCodeAddress, String localityAddress, String poBoxAddress, String regionAddress, String countryAddress) {
        mAddressStreetView.setText(streetAddress);
        mAddressExtendedStreetView.setText(extendedStreetAddress);
        mAddressCityView.setText(localityAddress);
        mAddressRegionView.setText(regionAddress);
        mAddressPostcodeView.setText(postalCodeAddress);
        mAddressPoBoxView.setText(poBoxAddress);
        mAddressCountryView.setText(countryAddress);

        mAddressFullCombinedView.setHint(mOptionHint);

        List<String> addressParts = new ArrayList<>();
        if (!TextUtils.isEmpty(streetAddress)) {
            addressParts.add(streetAddress);
        }
        if (!TextUtils.isEmpty(extendedStreetAddress)) {
            addressParts.add(extendedStreetAddress);
        }
        if (!TextUtils.isEmpty(postalCodeAddress)) {
            addressParts.add(postalCodeAddress);
        }
        if (!TextUtils.isEmpty(localityAddress)) {
            addressParts.add(localityAddress);
        }
        if (!TextUtils.isEmpty(poBoxAddress)) {
            addressParts.add(poBoxAddress);
        }
        if (!TextUtils.isEmpty(regionAddress)) {
            addressParts.add(regionAddress);
        }
        if (!TextUtils.isEmpty(countryAddress)) {
            addressParts.add(countryAddress);
        }

        boolean isEmpty = TextUtils.isEmpty(streetAddress) && TextUtils.isEmpty(localityAddress) &&
                TextUtils.isEmpty(regionAddress) && TextUtils.isEmpty(postalCodeAddress) && TextUtils.isEmpty(countryAddress);
        if (!isEmpty) {
            mAddressFullCombinedView.setText(TextUtils.join("\n", addressParts));
        }
    }

    private void fillAddressValues() {
        String streetAddress = mAddress.getStreetAddress();
        streetAddress = streetAddress == null ? "" : streetAddress;
        String extendedStreetAddress = mAddress.getExtendedAddress();
        extendedStreetAddress = extendedStreetAddress == null ? "" : extendedStreetAddress;
        String postalCodeAddress = mAddress.getPostalCode();
        postalCodeAddress = postalCodeAddress == null ? "" : postalCodeAddress;
        String localityAddress = mAddress.getLocality();
        localityAddress = localityAddress == null ? "" : localityAddress;
        String poBox = mAddress.getPoBox();
        poBox = poBox == null ? "" : poBox;
        String regionAddress = mAddress.getRegion();
        regionAddress = regionAddress == null ? "" : regionAddress;
        String countryAddress = mAddress.getCountry();
        countryAddress = countryAddress == null ? "" : countryAddress;

        fillAddressValues(streetAddress, extendedStreetAddress, postalCodeAddress, localityAddress, poBox, regionAddress, countryAddress);
    }

    private void setHandlers() {
        mAddressStreetView.addTextChangedListener(new DirtyWatcher());
        mAddressExtendedStreetView.addTextChangedListener(new DirtyWatcher());
        mAddressPostcodeView.addTextChangedListener(new DirtyWatcher());
        mAddressCityView.addTextChangedListener(new DirtyWatcher());
        mAddressPoBoxView.addTextChangedListener(new DirtyWatcher());
        mAddressRegionView.addTextChangedListener(new DirtyWatcher());
        mAddressCountryView.addTextChangedListener(new DirtyWatcher());

        mAddressStreetView.setOnFocusChangeListener(new FocusWatcher());
        mAddressExtendedStreetView.setOnFocusChangeListener(new FocusWatcher());
        mAddressPostcodeView.setOnFocusChangeListener(new FocusWatcher());
        mAddressCityView.setOnFocusChangeListener(new FocusWatcher());
        mAddressPoBoxView.setOnFocusChangeListener(new FocusWatcher());
        mAddressRegionView.setOnFocusChangeListener(new FocusWatcher());
        mAddressCountryView.setOnFocusChangeListener(new FocusWatcher());
    }

    public boolean isDirty() {
        return mIsDirty;
    }

    @OnClick(R.id.btn_minus)
    public void onMinusClicked() {
        int childCount = mLinearLayoutParent.getChildCount();
        if (childCount <= 2) {
            mAddressStreetView.setText("");
            mAddressCityView.setText("");
            mAddressRegionView.setText("");
            mAddressPostcodeView.setText("");
            mAddressCountryView.setText("");
            mAddressFullCombinedView.setText("");
            mOptionTitleView.setText(mStandardOptionUIValues.get(0));
            mOptionTitleView.setTag(mStandardOptionValues.get(0));
        } else {
            mLinearLayoutParent.removeView(this);
        }
    }

    private class FocusWatcher implements OnFocusChangeListener {

        @Override
        public void onFocusChange(View v, boolean hasFocus) {
            mHasFocus = mAddressStreetView.hasFocus() || mAddressExtendedStreetView.hasFocus()
                    || mAddressCityView.hasFocus() || mAddressPostcodeView.hasFocus()
                    || mAddressPoBoxView.hasFocus() || mAddressRegionView.hasFocus()
                    || mAddressCountryView.hasFocus();

            if (mHasFocus) {
                mAddressDetailsParentView.setVisibility(View.VISIBLE);
                mAddressFullCombinedView.setVisibility(View.GONE);
            } else {
                mAddressDetailsParentView.setVisibility(View.GONE);
                String streetAddress = mAddressStreetView.getText().toString();
                streetAddress = streetAddress == null ? "" : streetAddress;
                String extendedStreetAddress = mAddressExtendedStreetView.getText().toString();
                extendedStreetAddress = extendedStreetAddress == null ? "" : extendedStreetAddress;
                String postalCodeAddress = mAddressPostcodeView.getText().toString();
                postalCodeAddress = postalCodeAddress == null ? "" : postalCodeAddress;
                String localityAddress = mAddressCityView.getText().toString();
                localityAddress = localityAddress == null ? "" : localityAddress;
                String poBoxAddress = mAddressPoBoxView.getText().toString();
                poBoxAddress = poBoxAddress == null ? "" : poBoxAddress;
                String regionAddress = mAddressRegionView.getText().toString();
                regionAddress = regionAddress == null ? "" : regionAddress;
                String countryAddress = mAddressCountryView.getText().toString();
                countryAddress = countryAddress == null ? "" : countryAddress;
                fillAddressValues(streetAddress, extendedStreetAddress, postalCodeAddress, localityAddress, poBoxAddress, regionAddress, countryAddress);
                mAddressFullCombinedView.setVisibility(View.VISIBLE);
            }
        }
    }

    private class DirtyWatcher implements TextWatcher {

        private boolean isFirst;

        public DirtyWatcher() {
            super();
            isFirst = true;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //noop
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // noop
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (!isFirst) {
                mIsDirty = true;
            }
            isFirst = false;
        }
    }

    private class AddressClickListener implements View.OnClickListener {

        private final TextView addressFullCombined;
        private final View addressDetailsParent;

        private final EditText addressStreetView;
        private final EditText addressExtendedStreetView;
        private final EditText addressPostcodeView;
        private final EditText addressCityView;
        private final EditText addressPoBoxView;
        private final EditText addressRegionView;
        private final EditText addressCountryView;

        public AddressClickListener(TextView addressFullCombined, View addressDetailsParent,
                                    EditText addressStreetView, EditText addressExtendedStreetView,
                                    EditText addressPostcodeView, EditText addressCityView,
                                    EditText addressPoBoxView, EditText addressRegionView,
                                    EditText addressCountryView) {
            this.addressFullCombined = addressFullCombined;
            this.addressDetailsParent = addressDetailsParent;
            this.addressStreetView = addressStreetView;
            this.addressExtendedStreetView = addressExtendedStreetView;
            this.addressPostcodeView = addressPostcodeView;
            this.addressCityView = addressCityView;
            this.addressPoBoxView = addressPoBoxView;
            this.addressRegionView = addressRegionView;
            this.addressCountryView = addressCountryView;
        }

        @Override
        public void onClick(View v) {
            boolean addressCombinedVisible = addressFullCombined.getVisibility() == View.VISIBLE;
            if (addressCombinedVisible) {
                addressDetailsParent.setVisibility(View.VISIBLE);
                addressFullCombined.setVisibility(View.GONE);
                mAddressStreetView.post(() -> {
                    mAddressStreetView.requestFocusFromTouch();
                    UiUtil.showKeyboard((Activity) getContext(), mAddressStreetView);
                });
            } else {
                addressDetailsParent.setVisibility(View.GONE);
                addressFullCombined.setVisibility(View.VISIBLE);

                String streetAddress = addressStreetView.getText().toString();
                streetAddress = streetAddress == null ? "" : streetAddress;
                String extendedStreetAddress = addressExtendedStreetView.getText().toString();
                extendedStreetAddress = extendedStreetAddress == null ? "" : extendedStreetAddress;
                String postalCodeAddress = addressPostcodeView.getText().toString();
                postalCodeAddress = postalCodeAddress == null ? "" : postalCodeAddress;
                String localityAddress = addressCityView.getText().toString();
                localityAddress = localityAddress == null ? "" : localityAddress;
                String poBoxAddress = addressPoBoxView.getText().toString();
                poBoxAddress = poBoxAddress == null ? "" : poBoxAddress;
                String regionAddress = addressRegionView.getText().toString();
                regionAddress = regionAddress == null ? "" : regionAddress;
                String countryAddress = addressCountryView.getText().toString();
                countryAddress = countryAddress == null ? "" : countryAddress;
                addressStreetView.setText(streetAddress);
                addressExtendedStreetView.setText(extendedStreetAddress);
                addressPostcodeView.setText(postalCodeAddress);
                addressCityView.setText(localityAddress);
                addressPoBoxView.setText(poBoxAddress);
                addressRegionView.setText(regionAddress);
                addressCountryView.setText(countryAddress);

                List<String> addressParts = new ArrayList<>();
                if (!TextUtils.isEmpty(streetAddress)) {
                    addressParts.add(streetAddress);
                }
                if (!TextUtils.isEmpty(extendedStreetAddress)) {
                    addressParts.add(extendedStreetAddress);
                }
                if (!TextUtils.isEmpty(postalCodeAddress)) {
                    addressParts.add(postalCodeAddress);
                }
                if (!TextUtils.isEmpty(localityAddress)) {
                    addressParts.add(localityAddress);
                }
                if (!TextUtils.isEmpty(poBoxAddress)) {
                    addressParts.add(poBoxAddress);
                }
                if (!TextUtils.isEmpty(regionAddress)) {
                    addressParts.add(regionAddress);
                }
                if (!TextUtils.isEmpty(countryAddress)) {
                    addressParts.add(countryAddress);
                }

                boolean isEmpty = TextUtils.isEmpty(streetAddress) && TextUtils.isEmpty(extendedStreetAddress)
                        && TextUtils.isEmpty(postalCodeAddress) && TextUtils.isEmpty(localityAddress)
                        && TextUtils.isEmpty(poBoxAddress) && TextUtils.isEmpty(regionAddress)
                        && TextUtils.isEmpty(countryAddress);
                if (!isEmpty) {
                    addressFullCombined.setText(TextUtils.join("\n", addressParts));
                }
            }
        }
    }
}
