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

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;

/**
 * Created by dkadrikj on 17.7.15.
 */
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

    public static class LabelBodySerializer implements JsonSerializer<LabelBody> {

        @Override
        public JsonElement serialize(LabelBody src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject json = new JsonObject();
            json.addProperty(Fields.Label.NAME, src.getName());
            json.addProperty(Fields.Label.COLOR, src.getColor());
            json.addProperty(Fields.Label.DISPLAY, src.getDisplay());
            json.addProperty(Fields.Label.TYPE, src.getType());
            json.addProperty(Fields.Label.EXCLUSIVE, src.getExclusive());
            json.addProperty(Fields.Label.NOTIFY, src.getNotify());
            return json;
        }
    }

    public static class LabelBodyDeserializer implements JsonDeserializer<LabelBody> {

        @Override
        public LabelBody deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = (JsonObject) json;
            String name = jsonObject.get(Fields.Label.NAME).getAsString();
            String color = jsonObject.get(Fields.Label.COLOR).getAsString();
            int display = jsonObject.get(Fields.Label.DISPLAY).getAsInt();
            int type = jsonObject.get(Fields.Label.TYPE).getAsInt();
            int exclusive = jsonObject.get(Fields.Label.EXCLUSIVE).getAsInt();
            int notify = jsonObject.get(Fields.Label.NOTIFY).getAsInt();

            LabelBody labelBody = new LabelBody(name, color, display, exclusive, type);
            return labelBody;
        }
    }
}
