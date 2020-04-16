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

import androidx.annotation.NonNull;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ch.protonmail.android.api.models.messages.receive.ServerMessage;
import ch.protonmail.android.api.models.room.messages.MessageSender;
import ch.protonmail.android.api.utils.Fields;

public class NewMessage {
    @SerializedName(Fields.Message.MESSAGE)
    private final ServerMessage message;
    private Map<String, String> messageBodyMap = new HashMap<>();
    @SerializedName(Fields.Message.PARENT_ID)
    private String ParentID;
    @SerializedName(Fields.Message.ACTION)
    private int Action;
    private Map<String, String> AttachmentKeyPackets;
    private MessageSender Sender;

    public void setParentID(String parentID) {
        ParentID = parentID;
    }

    public void setAction(int action) {
        Action = action;
    }

    public NewMessage(@NonNull ServerMessage message) {
        this.message = message;
    }

    public ServerMessage getMessage() {
        return message;
    }

    public void addMessageBody(@NonNull String to, @NonNull String messageBody) {
        messageBodyMap.put(to, messageBody);
    }

    public void addAttachmentKeyPacket(String attachmentId, String packet) {
        if (AttachmentKeyPackets == null) {
            AttachmentKeyPackets = new HashMap<>();
        }
        AttachmentKeyPackets.put(attachmentId, packet);
    }

    private String getParentID() {
        return ParentID;
    }

    public int getAction() {
        return Action;
    }

    public void setSender(MessageSender sender) {
        Sender = sender;
    }

    public static class NewMessageSerializer implements JsonSerializer<NewMessage> {

        @Override
        public JsonElement serialize(NewMessage src, Type typeOfSrc, JsonSerializationContext context) {
            final JsonObject json = new JsonObject();
            final JsonObject message = new JsonObject();
            final JsonElement jsonToList = context.serialize(src.getMessage().getToList());
            message.add(Fields.Message.TO_LIST, jsonToList);
            final JsonElement jsonCCList = context.serialize(src.getMessage().getCCList());
            message.add(Fields.Message.CC_LIST, jsonCCList);
            final JsonElement jsonBCCList = context.serialize(src.getMessage().getBCCList());
            message.add(Fields.Message.BCC_LIST, jsonBCCList);
            final JsonElement jsonSubject = context.serialize(src.getMessage().getSubject());
            message.add(Fields.Message.SUBJECT, jsonSubject);
            final JsonElement jsonAddress = context.serialize(src.getMessage().getAddressID());
            message.add(Fields.Message.ADDRESS_ID,jsonAddress);
            for (Map.Entry<String, String> entry : src.messageBodyMap.entrySet()) {
                String body = entry.getValue();
                if (body != null) {
                    message.add(Fields.Message.MESSAGE_BODY, new JsonPrimitive(body));
                }
            }
            json.add(Fields.Message.MESSAGE, message);
            if (src.getParentID() != null) {
                json.add(Fields.Message.PARENT_ID, new JsonPrimitive(src.getParentID()));
                json.add(Fields.Message.ACTION, new JsonPrimitive(src.getAction()));
            }
            if (src.AttachmentKeyPackets != null && src.AttachmentKeyPackets.size() > 0) {
                JsonElement jsonElement = context.serialize(src.AttachmentKeyPackets);
                json.add("AttachmentKeyPackets", jsonElement);
            }
            if (src.Sender != null) {
                final JsonElement jsonSender = context.serialize(src.Sender);
                message.add(Fields.Message.SENDER, jsonSender);
            }
            return json;
        }
    }
}
