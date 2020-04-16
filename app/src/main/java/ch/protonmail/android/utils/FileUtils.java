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
package ch.protonmail.android.utils;

import android.content.Context;

import androidx.annotation.NonNull;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import ch.protonmail.android.core.Constants;

/**
 * Created by dkadrikj on 3/29/16.
 */
public class FileUtils {

    private static final String TAG_FILE_UTILS = "FileUtils";


    public static void createDownloadsDir(Context context) {
        File attachmentFile = new File(context.getFilesDir() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS);
        if (!attachmentFile.exists()) {
            attachmentFile.mkdirs();
        }
    }

    public static String readRawTextFile(Context ctx, int resId) {
        InputStream inputStream = ctx.getResources().openRawResource(resId);

        InputStreamReader inputreader = new InputStreamReader(inputStream);
        BufferedReader buffreader = new BufferedReader(inputreader);
        String line;
        StringBuilder text = new StringBuilder();

        try {
            while ((line = buffreader.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }

    public static String buildAttachmentFileName(@NonNull String filename, @NonNull String suffix) {
        String fileNewName = filename;
        fileNewName = fileNewName.replace("/", ":");
        int dotIndex = fileNewName.lastIndexOf(".");
        String extension = "";
        String name = fileNewName;
        if (dotIndex >= 0) {
            extension = fileNewName.substring(dotIndex);
            name = fileNewName.substring(0, dotIndex);
        }
        return name + suffix + extension;
    }

    public static String toString(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            new ObjectOutputStream(out).writeObject(value);
        } catch (IOException e) {
            Logger.doLogException(TAG_FILE_UTILS, "Serialization of recipients failed ", e);
        }

        return Base64.encodeToString(out.toByteArray(),Base64.DEFAULT);
    }

    public static <T> T deserializeStringToObject(String value) {
        if (value==null||value.equals("")) {
            return null;
        }
        ByteArrayInputStream in = new ByteArrayInputStream(Base64.decode(value,Base64.DEFAULT));
        T result = null;
        try {
            result = (T) new ObjectInputStream(in).readObject();
        } catch (Exception e) {
            Logger.doLogException(TAG_FILE_UTILS, "Deserialization of recipients failed", e);
        }
        return result;
    }
}
