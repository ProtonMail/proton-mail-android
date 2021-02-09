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

import android.text.TextUtils;
import android.util.Base64;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.extensions.StringExtensionsKt;

/**
 * Created by dkadrikj on 7/16/16.
 */
public class AttachmentHeaders implements Serializable {
    private static final String TAG_ATTACHMENT_HEADERS = "AttachmentHeaders";
    @SerializedName(Fields.Attachment.CONTENT_TYPE)
    private String contentType;
    @SerializedName(Fields.Attachment.CONTENT_TRANSFER_ENCODING)
    private String contentTransferEncoding;
    @SerializedName(Fields.Attachment.CONTENT_DISPOSITION)
    private List<String> contentDisposition;
    @SerializedName(Fields.Attachment.CONTENT_ID)
    private List<String> contentId;
    @SerializedName(Fields.Attachment.CONTENT_LOCATION)
    private String contentLocation;
    @SerializedName(Fields.Attachment.CONTENT_ENCRYPTION)
    private String contentEncryption;
    private Long serialVersionUID = -8741548902749037534L;

    // region getters
    public String getContentType() {
        return contentType;
    }

    public String getContentTransferEncoding() {
        return contentTransferEncoding;
    }

    public List<String> getContentDisposition() {
        return contentDisposition;
    }

    @Nullable
    public String getContentId() {
        if (contentId != null && !contentId.isEmpty()) {
            return contentId.get(0);
        }
        return null;
    }

    public String getContentLocation() {
        return contentLocation;
    }

    public String getContentEncryption() {
        return contentEncryption;
    }
    // endregion

    public AttachmentHeaders() {
    }

    public AttachmentHeaders(String contentType, String contentTransferEncoding, List<String> contentDisposition, List<String> contentId, String contentLocation, String contentEncryption) {
        this.contentType = contentType;
        this.contentTransferEncoding = contentTransferEncoding;
        this.contentDisposition = contentDisposition;
        this.contentId = contentId;
        this.contentLocation = contentLocation;
        this.contentEncryption = contentEncryption;
    }

    public String toString() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(out).writeObject(this);
        } catch (IOException e) {
            Logger.doLogException(TAG_ATTACHMENT_HEADERS, "Serialization of att headers failed ", e);
        }

        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
    }

    public static AttachmentHeaders fromString(String value) {
        ByteArrayInputStream in = new ByteArrayInputStream(Base64.decode(value, Base64.DEFAULT));
        AttachmentHeaders result = null;
        try {
            result = (AttachmentHeaders) new ObjectInputStream(in).readObject();
        } catch (Exception e) {
            Logger.doLogException(TAG_ATTACHMENT_HEADERS, "DeSerialization of recipients failed", e);
        }
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        // If the object is compared with itself then return true
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof AttachmentHeaders)) {
            return false;
        }

        AttachmentHeaders attachmentHeaders = (AttachmentHeaders) obj;

        return StringExtensionsKt.compare(contentType, attachmentHeaders.contentType) &&
                StringExtensionsKt.compare(contentTransferEncoding, attachmentHeaders.contentTransferEncoding) &&
                StringExtensionsKt.compare(contentLocation, attachmentHeaders.contentLocation) &&
                StringExtensionsKt.compare(contentEncryption, attachmentHeaders.contentEncryption) &&
                (contentId != null && contentId.equals(attachmentHeaders.contentId)) &&
                (contentDisposition != null && contentDisposition.equals(attachmentHeaders.contentDisposition));
    }

    public static class AttachmentHeadersDeserializer implements JsonDeserializer<AttachmentHeaders> {

        @Override
        public AttachmentHeaders deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = json.getAsJsonObject();
            String contentType = "";
            JsonElement contentTypeElement = jsonObject.get(Fields.Attachment.CONTENT_TYPE);
            if (contentTypeElement != null) {
                if (contentTypeElement.isJsonArray()) {
                    contentType = contentTypeElement.getAsJsonArray().get(0).getAsString();
                } else {
                    contentType = contentTypeElement.getAsString();
                }
            }

            String contentTransferEncoding = "";
            JsonElement contentTransferEncodingElement = jsonObject.get(Fields.Attachment.CONTENT_TRANSFER_ENCODING);
            if (contentTransferEncodingElement != null) {
                if (contentTransferEncodingElement.isJsonArray()) {
                    contentTransferEncoding = contentTransferEncodingElement.getAsJsonArray().get(0).getAsString();
                } else {
                    contentTransferEncoding = contentTransferEncodingElement.getAsString();
                }
            }

            List<String> contentId = new ArrayList<>();
            JsonElement contentIdElement = jsonObject.get(Fields.Attachment.CONTENT_ID);
            if (contentIdElement != null) {
                if (contentIdElement.isJsonArray()) {
                    JsonArray jsonArray = jsonObject.get(Fields.Attachment.CONTENT_ID).getAsJsonArray();
                    Iterator<JsonElement> iterator = jsonArray.iterator();
                    while (iterator.hasNext()) {
                        contentId.add(iterator.next().getAsString());
                    }
                } else {
                    String contentIdString = jsonObject.get(Fields.Attachment.CONTENT_ID).getAsString();
                    contentId.add(contentIdString);
                }
            }

            String contentLocation = "";
            JsonElement contentLocationElement = jsonObject.get(Fields.Attachment.CONTENT_LOCATION);
            if (contentLocationElement != null) {
                if (contentLocationElement.isJsonArray()) {
                    contentLocation = contentLocationElement.getAsJsonArray().get(0).getAsString();
                } else {
                    contentLocation = contentLocationElement.getAsString();
                }
            }

            List<String> contentDisposition = new ArrayList<>();
            JsonElement contentDispositionElement = jsonObject.get(Fields.Attachment.CONTENT_DISPOSITION);
            if (contentDispositionElement != null) {
                if (contentDispositionElement.isJsonArray()) {
                    JsonArray jsonArray = jsonObject.get(Fields.Attachment.CONTENT_DISPOSITION).getAsJsonArray();
                    Iterator<JsonElement> iterator = jsonArray.iterator();
                    while (iterator.hasNext()) {
                        String disposition = iterator.next().getAsString();
                        // apparently sometimes null objects are received from the api
                        if (disposition != null) {
                            contentDisposition.add(disposition);
                        }
                    }
                } else {
                    String contentDispositionString = jsonObject.get(Fields.Attachment.CONTENT_DISPOSITION).getAsString();
                    // apparently sometimes null objects are received from the api
                    if (contentDispositionString != null) {
                        contentDisposition.add(contentDispositionString);
                    }
                }
            }

            String contentEncryption = "";
            JsonElement contentEncryptionElement = jsonObject.get(Fields.Attachment.CONTENT_ENCRYPTION);
            if (contentEncryptionElement != null) {
                if (contentEncryptionElement.isJsonArray()) {
                    contentEncryption = contentEncryptionElement.getAsJsonArray().get(0).getAsString();
                } else {
                    contentEncryption = contentEncryptionElement.getAsString();
                }
            }

            return new AttachmentHeaders(contentType, contentTransferEncoding, contentDisposition, contentId, contentLocation, contentEncryption);
        }
    }
}
