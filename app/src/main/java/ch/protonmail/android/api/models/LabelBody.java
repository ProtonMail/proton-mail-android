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

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;

/**
 * Created by dkadrikj on 17.7.15.
 */
@Deprecated // replaced with LabelRequestBody
public class LabelBody {
    @SerializedName(Fields.Label.NAME)
    private final String name;
    @SerializedName(Fields.Label.COLOR)
    private final String color;
    @SerializedName(Fields.Label.DISPLAY)
    private final int display;
    @SerializedName(Fields.Label.EXCLUSIVE)
    private final int exclusive;
    @SerializedName(Fields.Label.TYPE)
    private final int type;
    @SerializedName(Fields.Label.NOTIFY)
    private final int notify;

    public LabelBody(String name, String color, int display, int exclusive) {
        this.name = name;
        this.color = color;
        this.display = display;
        this.exclusive = exclusive;
        this.type = Constants.LABEL_TYPE_MESSAGE; // by default it is message label
        this.notify = 0;
    }

    public LabelBody(String name, String color, int display, int exclusive, int type) {
        this.name = name;
        this.color = color;
        this.display = display;
        this.type = type;
        this.exclusive = exclusive;
        this.notify = 0;
    }

    public String getName() {
        return name;
    }

    public String getColor() {
        return color;
    }

    public int getDisplay() {
        return display;
    }

    public int getType() {
        return type;
    }

    public int getExclusive() {
        return exclusive;
    }

    public int getNotify() {
        return notify;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LabelBody labelBody = (LabelBody) o;
        return display == labelBody.display &&
                exclusive == labelBody.exclusive &&
                type == labelBody.type &&
                notify == labelBody.notify &&
                Objects.equals(name, labelBody.name) &&
                Objects.equals(color, labelBody.color);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, color, display, exclusive, type, notify);
    }

}
