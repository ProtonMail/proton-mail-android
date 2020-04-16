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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.Country;

/**
 * Created by dkadrikj on 3/29/16.
 */
public class SimpleCountriesAdapter extends ArrayAdapter<Country> {

    private int selectedPosition = 0;
    private int selectedColor;
    private int normalColor;

    public SimpleCountriesAdapter(Context context, List<Country> countries) {
        super(context, R.layout.simple_country_list_item, countries);
        selectedColor = getContext().getResources().getColor(R.color.upgrade_section);
        normalColor = getContext().getResources().getColor(R.color.iron_gray);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.simple_country_list_item, null);
            final ViewHolder viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
            convertView = view;
        }

        final Country item = getItem(position);
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.mCountryName.setText(item.getName());
        viewHolder.mCountryName.setTextColor(selectedColor);
        return convertView;
    }

    public void setSelectedPosition(int selectedPosition) {
        this.selectedPosition = selectedPosition;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.simple_country_list_item, null);
            final ViewHolder viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
            convertView = view;
        }

        final Country item = getItem(position);
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.mCountryName.setText(item.getName());
        if (position == 6) {
            viewHolder.mSeparator.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mSeparator.setVisibility(View.GONE);
        }
        if (selectedPosition > 0 && position == selectedPosition) {
            viewHolder.mCountryName.setTextColor(selectedColor);
        } else {
            viewHolder.mCountryName.setTextColor(normalColor);
        }
        return convertView;
    }

    static class ViewHolder {
        @BindView(R.id.country_name)
        TextView mCountryName;
        @BindView(R.id.separator)
        View mSeparator;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
