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
package ch.protonmail.android.api.models;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Ignore;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import static ch.protonmail.android.data.local.model.ContactDataKt.COLUMN_CONTACT_DATA_NAME;
import static ch.protonmail.android.data.local.model.ContactEmailKt.COLUMN_CONTACT_EMAILS_EMAIL;


public class MessageRecipient implements Serializable, Comparable<MessageRecipient> {
    @ColumnInfo(name = COLUMN_CONTACT_DATA_NAME)
    final String Name;
    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_EMAIL)
    final String Address;
    @Ignore
    int mIcon = 0; // for pgp
    @Ignore
    int mIconColor = 0; // for pgp
    @Ignore
    int mDescription = 0; // for clicking description
    @Ignore
    boolean mIsPGP = false;
    @Ignore
    String Group;
    @Ignore
    int groupIcon = 0;
    @Ignore
    int groupColor = 0;
    @Ignore
    List<MessageRecipient> groupRecipients;
    @Ignore
    boolean selected;
    private static final long serialVersionUID = -110723370017912622L;

    public MessageRecipient(String Name, String Address, String Group) {
        this.Name = Name;
        this.Address = Address;
        this.Group = Group;
    }

    public MessageRecipient(String Name, String Address) {
        this.Name = Name;
        this.Address = Address;
    }

    public String getName() {
        return Name;
    }

    public String getEmailAddress() {
        return Address;
    }

    /**
     * For room database. Do not use.
     *
     * @deprecated Use instead getEmailAddress()
     */
    public String getAddress() {
        return Address;
    }

    public int getIcon() {
        return mIcon;
    }

    public void setIcon(int icon) {
        this.mIcon = icon;
    }

    public int getIconColor() {
        return mIconColor;
    }

    public void setIconColor(int iconColor) {
        this.mIconColor = iconColor;
    }

    public int getDescription() {
        return mDescription;
    }

    public void setDescription(int description) {
        this.mDescription = description;
    }

    public boolean isPGP() {
        return mIsPGP;
    }

    public void setIsPGP(boolean isPGP) {
        this.mIsPGP = isPGP;
    }

    public String getGroup() {
        return Group;
    }

    public void setGroup(String group) {
        Group = group;
    }

    public int getGroupIcon() {
        return groupIcon;
    }

    public void setGroupIcon(int icon) {
        this.groupIcon = icon;
    }

    public int getGroupColor() {
        return groupColor;
    }

    public void setGroupColor(int groupColor) {
        this.groupColor = groupColor;
    }

    public List<MessageRecipient> getGroupRecipients() {
        return groupRecipients;
    }

    public void setGroupRecipients(List<MessageRecipient> groupRecipients) {
        this.groupRecipients = groupRecipients;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public String toString() {
        return Name + " " + Address;
    }

    @Override
    public int compareTo(@NonNull MessageRecipient another) {
        return Name.compareTo(another.Name);
    }

    // the code below is Android 6 bug fix
    public static class MessageRecipientSerializer implements JsonSerializer<MessageRecipient> {
        @Override
        public JsonElement serialize(MessageRecipient src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject json = new JsonObject();
            json.addProperty("Name", src.getName());
            json.addProperty("Address", src.getEmailAddress());
            json.addProperty("Group", src.getGroup());
            return json;
        }
    }

    public static class MessageRecipientDeserializer implements JsonDeserializer<MessageRecipient> {
        @Override
        public MessageRecipient deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = (JsonObject) json;
            String address = jsonObject.get("Address").getAsString();
            String name = jsonObject.get("Name").getAsString();
            JsonElement groupJsonElement = jsonObject.get("Group");
            MessageRecipient messageRecipient = new MessageRecipient(name, address);
            if (groupJsonElement != null) {
                String group = groupJsonElement.getAsString();
                messageRecipient = new MessageRecipient(name, address, group);
            }
            return messageRecipient;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageRecipient that = (MessageRecipient) o;
        return Objects.equals(Name, that.Name) &&
                Objects.equals(Address, that.Address) &&
                Objects.equals(Group, that.Group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Name, Address, Group);
    }
}
