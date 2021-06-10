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
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import java.util.Arrays;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import ch.protonmail.android.R;
import ch.protonmail.android.utils.UiUtil;
import ezvcard.property.Address;
import me.proton.core.presentation.ui.view.ProtonInput;

public class ContactAddressView extends LinearLayout {

    //region common address UI elements
    @BindView(R.id.optionTitle)
    TextView mOptionTitleView;
    @BindView(R.id.btnOptionType)
    ImageButton mBtnOptionTypeView;
    @BindView(R.id.address_street)
    ProtonInput mAddressStreetView;
    @BindView(R.id.address_city)
    ProtonInput mAddressCityView;
    @BindView(R.id.address_region)
    ProtonInput mAddressRegionView;
    @BindView(R.id.address_postcode)
    ProtonInput mAddressPostcodeView;
    @BindView(R.id.address_country)
    ProtonInput mAddressCountryView;
    @BindView(R.id.address_full_combined)
    TextView mAddressFullCombinedView;
    @BindView(R.id.address_detailed_parent)
    View mAddressDetailsParentView;
    VCardLinearLayout mLinearLayoutParent;
    //endregion

    //region new address UI elements
    @Nullable @BindView(R.id.title)
    TextView mRowTitleView;
    @Nullable @BindView(R.id.fields_parent)
    View mInputFieldsView;
    //endregion

    private boolean mIsDirty = false;
    private boolean mHasFocus = false;

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
        mStandardOptionUIValues = standardOptionUIValues;
        mStandardOptionValues = standardOptionValues;
        mLinearLayoutParent = rootView;

        View view = LayoutInflater.from(context).inflate(R.layout.contact_new_vcard_address, this, true);
        ButterKnife.bind(this);
        UiUtil.generateViewId(view);

        setHandlers();

        mAddressDetailsParentView.setVisibility(View.VISIBLE);
        mAddressFullCombinedView.setVisibility(GONE);
        mAddressFullCombinedView.setOnClickListener(new AddressClickListener(mAddressFullCombinedView, mAddressDetailsParentView, mAddressStreetView, mAddressCityView, mAddressRegionView, mAddressPostcodeView, mAddressCountryView));
        final FragmentManager fragmentManager = ((FragmentActivity) getContext()).getSupportFragmentManager();
        mOptionTitleView.setText(optionTitleText);
        mRowTitleView.setOnClickListener(v -> {
            mRowTitleView.setVisibility(View.GONE);
            mInputFieldsView.setVisibility(View.VISIBLE);
            mRowTitleView.setVisibility(View.GONE);
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
        mStandardOptionUIValues = standardOptionUIValues;
        mStandardOptionValues = standardOptionValues;
        mLinearLayoutParent = rootView;

        mAddress = address;
        mOptionHint = optionHint;

        View view = LayoutInflater.from(context).inflate(R.layout.contact_vcard_address_editable, this, true);
        ButterKnife.bind(this);
        UiUtil.generateViewId(view);
        setHandlers();

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

        mAddressFullCombinedView.setOnClickListener(new AddressClickListener(mAddressFullCombinedView, mAddressDetailsParentView, mAddressStreetView, mAddressCityView, mAddressRegionView, mAddressPostcodeView, mAddressCountryView));
    }

    private void fillAddressValues(String streetAddress, String localityAddress, String regionAddress, String postalCodeAddress, String countryAddress) {
        mAddressStreetView.setText(streetAddress);
        mAddressCityView.setText(localityAddress);
        mAddressRegionView.setText(regionAddress);
        mAddressPostcodeView.setText(postalCodeAddress);
        mAddressCountryView.setText(countryAddress);

        mAddressFullCombinedView.setHint(mOptionHint);

        boolean isEmpty = TextUtils.isEmpty(streetAddress) && TextUtils.isEmpty(localityAddress) &&
                TextUtils.isEmpty(regionAddress) && TextUtils.isEmpty(postalCodeAddress) && TextUtils.isEmpty(countryAddress);
        if (!isEmpty) {
            mAddressFullCombinedView.setText(TextUtils.join(" ", Arrays.asList(streetAddress, localityAddress, regionAddress, postalCodeAddress, countryAddress)));
        }
    }

    private void fillAddressValues() {
        String streetAddress = mAddress.getStreetAddress();
        streetAddress = streetAddress == null ? "" : streetAddress;
        String localityAddress = mAddress.getLocality();
        localityAddress = localityAddress == null ? "" : localityAddress;
        String regionAddress = mAddress.getRegion();
        regionAddress = regionAddress == null ? "" : regionAddress;
        String postalCodeAddress = mAddress.getPostalCode();
        postalCodeAddress = postalCodeAddress == null ? "" : postalCodeAddress;
        String countryAddress = mAddress.getCountry();
        countryAddress = countryAddress == null ? "" : countryAddress;

        fillAddressValues(streetAddress, localityAddress, regionAddress, postalCodeAddress, countryAddress);
    }

    private void setHandlers() {
        mAddressStreetView.addTextChangedListener(new DirtyWatcher());
        mAddressCityView.addTextChangedListener(new DirtyWatcher());
        mAddressRegionView.addTextChangedListener(new DirtyWatcher());
        mAddressPostcodeView.addTextChangedListener(new DirtyWatcher());
        mAddressCountryView.addTextChangedListener(new DirtyWatcher());

        mAddressStreetView.setOnFocusChangeListener(new FocusWatcher());
        mAddressCityView.setOnFocusChangeListener(new FocusWatcher());
        mAddressRegionView.setOnFocusChangeListener(new FocusWatcher());
        mAddressPostcodeView.setOnFocusChangeListener(new FocusWatcher());
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
            mHasFocus = mAddressStreetView.hasFocus() || mAddressCityView.hasFocus() || mAddressRegionView.hasFocus()
                    || mAddressPostcodeView.hasFocus() || mAddressCountryView.hasFocus();

            if (mHasFocus) {
                mAddressDetailsParentView.setVisibility(View.VISIBLE);
                mAddressFullCombinedView.setVisibility(View.GONE);
            } else {
                mAddressDetailsParentView.setVisibility(View.GONE);
                String streetAddress = mAddressStreetView.getText().toString();
                streetAddress = streetAddress == null ? "" : streetAddress;
                String localityAddress = mAddressCityView.getText().toString();
                localityAddress = localityAddress == null ? "" : localityAddress;
                String regionAddress = mAddressRegionView.getText().toString();
                regionAddress = regionAddress == null ? "" : regionAddress;
                String postalCodeAddress = mAddressPostcodeView.getText().toString();
                postalCodeAddress = postalCodeAddress == null ? "" : postalCodeAddress;
                String countryAddress = mAddressCountryView.getText().toString();
                countryAddress = countryAddress == null ? "" : countryAddress;
                fillAddressValues(streetAddress, localityAddress, regionAddress, postalCodeAddress, countryAddress);
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

        private final ProtonInput addressStreetView;
        private final ProtonInput addressCityView;
        private final ProtonInput addressRegionView;
        private final ProtonInput addressPostcodeView;
        private final ProtonInput addressCountryView;

        public AddressClickListener(TextView addressFullCombined,
                                    View addressDetailsParent,
                                    ProtonInput addressStreetView,
                                    ProtonInput addressCityView,
                                    ProtonInput addressRegionView,
                                    ProtonInput addressPostcodeView,
                                    ProtonInput addressCountryView) {
            this.addressFullCombined = addressFullCombined;
            this.addressDetailsParent = addressDetailsParent;
            this.addressStreetView = addressStreetView;
            this.addressCityView = addressCityView;
            this.addressRegionView = addressRegionView;
            this.addressPostcodeView = addressPostcodeView;
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

                });
            } else {
                addressDetailsParent.setVisibility(View.GONE);
                addressFullCombined.setVisibility(View.VISIBLE);

                String streetAddress = addressStreetView.getText().toString();
                streetAddress = streetAddress == null ? "" : streetAddress;
                String localityAddress = addressCityView.getText().toString();
                localityAddress = localityAddress == null ? "" : localityAddress;
                String regionAddress = addressRegionView.getText().toString();
                regionAddress = regionAddress == null ? "" : regionAddress;
                String postalCodeAddress = addressPostcodeView.getText().toString();
                postalCodeAddress = postalCodeAddress == null ? "" : postalCodeAddress;
                String countryAddress = addressCountryView.getText().toString();
                countryAddress = countryAddress == null ? "" : countryAddress;
                addressStreetView.setText(streetAddress);
                addressCityView.setText(localityAddress);
                addressRegionView.setText(regionAddress);
                addressPostcodeView.setText(postalCodeAddress);
                addressCountryView.setText(countryAddress);

                boolean isEmpty = TextUtils.isEmpty(streetAddress) && TextUtils.isEmpty(localityAddress) &&
                        TextUtils.isEmpty(regionAddress) && TextUtils.isEmpty(postalCodeAddress) && TextUtils.isEmpty(countryAddress);
                if (!isEmpty) {
                    addressFullCombined.setText(TextUtils.join(" ", Arrays.asList(streetAddress, localityAddress, regionAddress, postalCodeAddress, countryAddress)));
                }
            }
        }
    }
}
