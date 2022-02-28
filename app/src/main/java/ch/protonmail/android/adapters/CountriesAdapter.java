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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.Country;

/**
 * Created by dkadrikj on 3/29/16.
 */
public class CountriesAdapter extends ArrayAdapter<Country> {

    public CountriesAdapter(Context context, List<Country> countries) {
        super(context, R.layout.country_list_item, countries);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.country_list_item, null);
            final ViewHolder viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
            convertView = view;
        }

        final Country item = getItem(position);
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.mCountryName.setText(item.getName());
        viewHolder.mCountryCode.setText(item.getCode());

        int id = getContext().getResources().getIdentifier(item.getCountryCode().toLowerCase(), "drawable",
                getContext().getPackageName());
        viewHolder.mCountryFlag.setImageResource(id);
        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.country_list_item, null);
            final ViewHolder viewHolder = new ViewHolder(view);
            view.setTag(viewHolder);
            convertView = view;
        }

        final Country item = getItem(position);
        final ViewHolder viewHolder = (ViewHolder) convertView.getTag();

        viewHolder.mCountryName.setText(item.getName());
        viewHolder.mCountryCode.setText(item.getCode());
        int id = getContext().getResources().getIdentifier(item.getCountryCode().toLowerCase(), "drawable", getContext().getPackageName());
        if (id != 0) {
            viewHolder.mCountryFlag.setImageResource(id);
        }
        if (position == 5) {
            viewHolder.mSeparator.setVisibility(View.VISIBLE);
        } else {
            viewHolder.mSeparator.setVisibility(View.GONE);
        }
        return convertView;
    }

    static class ViewHolder {
        @BindView(R.id.country_name)
        TextView mCountryName;
        @BindView(R.id.country_code)
        TextView mCountryCode;
        @BindView(R.id.country_flag)
        ImageView mCountryFlag;
        @BindView(R.id.separator)
        View mSeparator;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }
}
