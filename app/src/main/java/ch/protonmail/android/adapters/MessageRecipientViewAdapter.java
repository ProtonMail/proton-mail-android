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
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.protonmail.android.R;
import ch.protonmail.android.api.models.MessageRecipient;
import ch.protonmail.android.utils.UiUtil;

public class MessageRecipientViewAdapter extends ArrayAdapter<MessageRecipient> {

    private List<MessageRecipient> data;
    private ContactsFilter contactsFilter;

    public MessageRecipientViewAdapter(Context context) {
        super(context, 0);
        data = new ArrayList<>();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.message_recipient_list_item, parent, false);
            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        MessageRecipient messageRecipient = getItem(position);
        viewHolder.text2.setText(messageRecipient.getAddress());
        viewHolder.text1.setText(messageRecipient.getName());

        if(messageRecipient.getName().contains(getContext().getString(R.string.members))) {
            viewHolder.letterIndicator.setVisibility(View.GONE);
            viewHolder.text2.setVisibility(View.GONE);
            viewHolder.groupIcon.setVisibility(View.VISIBLE);
            viewHolder.groupIcon.setColorFilter(new PorterDuffColorFilter(messageRecipient.getGroupColor(), PorterDuff.Mode.SRC_IN
            ));

        } else {
            viewHolder.letterIndicator.setText(UiUtil.extractInitials(messageRecipient.getName()));
            viewHolder.groupIcon.setVisibility(View.GONE);
            viewHolder.letterIndicator.setVisibility(View.VISIBLE);
            viewHolder.text2.setVisibility(View.VISIBLE);
        }

        return convertView;
    }

    static class ViewHolder {

        @BindView(R.id.text1)
        TextView text1;
        @BindView(R.id.text2)
        TextView text2;
        @BindView(R.id.contactIconLetter)
        TextView letterIndicator;
        @BindView(R.id.groupIcon)
        ImageView groupIcon;

        public ViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    public void setData(List<MessageRecipient> data) {
        this.data = data;
        clear();
        addAll(data);
    }

    @Override
    public Filter getFilter() {
        if (contactsFilter == null) {
            contactsFilter = new ContactsFilter();
        }

        return contactsFilter;
    }

    private class ContactsFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0) {
                // No filter implemented we return all the list
                results.values = data;
                results.count = data.size();
            } else {
                List<MessageRecipient> newData = new ArrayList<>();

                for (MessageRecipient p : data) {
                    final String name = p.getName();
                    final String email = p.getAddress();

                    final String pattern = constraint.toString().toLowerCase();
                    if ((name != null && name.toLowerCase()
                            .contains(pattern)) || email.toLowerCase().contains(pattern))
                        newData.add(p);
                }

                results.values = newData;
                results.count = newData.size();
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            MessageRecipientViewAdapter.this.clear();
            if (results.count > 0) {
                MessageRecipientViewAdapter.this.addAll((Collection) results.values);
                MessageRecipientViewAdapter.this.notifyDataSetChanged();
            } else {
                MessageRecipientViewAdapter.this.notifyDataSetInvalidated();
            }
        }
    }
}
