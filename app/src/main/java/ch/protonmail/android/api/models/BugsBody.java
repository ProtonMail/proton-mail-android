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

import java.lang.reflect.Type;

import ch.protonmail.android.api.utils.Fields;

public class BugsBody {

    private final String OS;
    private final String OSVersion;
    private final String Client;
    private final String ClientVersion;
    private final String Title;
    private final String Description;
    private final String Username;
    private final String Email;

    public BugsBody(String OS, String OSVersion, String client, String clientVersion, String title, String description, String username, String email) {
        this.OS = OS;
        this.OSVersion = OSVersion;
        this.Client = client;
        this.ClientVersion = clientVersion;
        this.Title = title;
        this.Description = description;
        this.Username = username;
        this.Email = email;
    }

    public String getOS() {
        return OS;
    }

    public String getOSVersion() {
        return OSVersion;
    }

    public String getClient() {
        return Client;
    }

    public String getClientVersion() {
        return ClientVersion;
    }

    public String getTitle() {
        return Title;
    }

    public String getDescription() {
        return Description;
    }

    public String getUsername() {
        return Username;
    }

    public String getEmail() {
        return Email;
    }

    public static class BugsBodySerializer implements JsonSerializer<BugsBody> {

        @Override
        public JsonElement serialize(BugsBody src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject json = new JsonObject();
            json.addProperty(Fields.Bugs.OS, src.getOS());
            json.addProperty(Fields.Bugs.OS_VERSION, src.getOSVersion());
            json.addProperty(Fields.Bugs.CLIENT, src.getClient());
            json.addProperty(Fields.Bugs.CLIENT_VERSION, src.getClientVersion());
            json.addProperty(Fields.Bugs.TITLE, src.getTitle());
            json.addProperty(Fields.Bugs.DESCRIPTION, src.getDescription());
            json.addProperty(Fields.Bugs.USERNAME, src.getUsername());
            json.addProperty(Fields.Bugs.EMAIL, src.getEmail());
            return json;
        }
    }

    public static class BugsBodyDeserializer implements JsonDeserializer<BugsBody> {

        @Override
        public BugsBody deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext
                context) throws JsonParseException {
            JsonObject jsonObject = (JsonObject) json;
            String os = jsonObject.get(Fields.Bugs.OS).getAsString();
            String osVersion = jsonObject.get(Fields.Bugs.OS_VERSION).getAsString();
            String client = jsonObject.get(Fields.Bugs.CLIENT).getAsString();
            String clientVersion = jsonObject.get(Fields.Bugs.CLIENT_VERSION).getAsString();
            String title = jsonObject.get(Fields.Bugs.TITLE).getAsString();
            String description = jsonObject.get(Fields.Bugs.DESCRIPTION).getAsString();
            String username = jsonObject.get(Fields.Bugs.USERNAME).getAsString();
            String email = jsonObject.get(Fields.Bugs.EMAIL).getAsString();

            BugsBody bugsBody = new BugsBody(os, osVersion, client, clientVersion, title,
                    description, username, email);
            return bugsBody;
        }
    }
}
