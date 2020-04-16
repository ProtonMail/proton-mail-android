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

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.util.Locale;

public class Tag implements Parcelable {
    private char prefix;
    private String value;

    Tag(char prefix, @NonNull String value) {
        this.prefix = prefix;
        this.value = value;
    }

    public static Tag[] sampleTags() {
        return new Tag[] {
                new Tag('@', "TokenAutoComplete"),
                new Tag('@', "Admin"),
                new Tag('@', "Support"),
                new Tag('#', "autocomplete"),
                new Tag('#', "token"),
                new Tag('#', "tokenizer"),
                new Tag('#', "email"),
                new Tag('#', "comma"),
                new Tag('#', "parser"),
                new Tag('#', "rfc2822"),
                new Tag('#', "hashtags"),
                new Tag('#', "usernames")
        };
    }

    public char getPrefix() {
        return prefix;
    }

    @NonNull
    public String getValue() {
        return value;
    }

    @NonNull
    public String getFormattedValue() {
        return String.format(Locale.US, "%c%s", prefix, value);
    }

    @Override
    public String toString() {
        return getFormattedValue();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeCharArray(new char[]{prefix});
        dest.writeString(this.value);
    }

    private Tag(Parcel in) {
        char[] temp = new char[1];
        in.readCharArray(temp);
        this.prefix = temp[0];
        this.value = in.readString();
    }

    public static final Parcelable.Creator<Tag> CREATOR = new Parcelable.Creator<Tag>() {
        @Override
        public Tag createFromParcel(Parcel source) {
            return new Tag(source);
        }

        @Override
        public Tag[] newArray(int size) {
            return new Tag[size];
        }
    };
}
