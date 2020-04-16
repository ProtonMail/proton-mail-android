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
package ch.protonmail.tokenautocomplete.example;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import ch.protonmail.tokenautocomplete.FilteredArrayAdapter;

public class PersonAdapter extends FilteredArrayAdapter<Person> {
    @LayoutRes
    private int layoutId;

    PersonAdapter(Context context, @LayoutRes int layoutId, Person[] people) {
        super(context, layoutId, people);
        this.layoutId = layoutId;
    }

    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {

            LayoutInflater l = (LayoutInflater)getContext().getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            convertView = l.inflate(layoutId, parent, false);
        }

        Person p = getItem(position);
        ((TextView)convertView.findViewById(R.id.name)).setText(p.getName());
        ((TextView)convertView.findViewById(R.id.email)).setText(p.getEmail());

        return convertView;
    }

    @Override
    protected boolean keepObject(Person person, String mask) {
        mask = mask.toLowerCase();
        return person.getName().toLowerCase().startsWith(mask) || person.getEmail().toLowerCase().startsWith(mask);
    }
}
