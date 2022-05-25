/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.tasks;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Pair;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import ch.protonmail.android.core.Constants;
import ch.protonmail.android.core.ProtonMailApplication;
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent;
import ch.protonmail.android.jobs.helper.EmbeddedImage;
import ch.protonmail.android.utils.Logger;

/**
 * Created by kaylukas on 21/05/2018.
 */
public abstract class AbstractEmbeddedImagesThread extends AsyncTask<Void, Void, Pair<String, String>> {

    private static final String TAG_ABSTRACT_EMBEDDED_IMAGES_THREAD = "AbstractEmbeddedImagesThread";

    private DownloadEmbeddedImagesEvent event;
    private String bodyString;
    private String nonBrokenEmail;

    public AbstractEmbeddedImagesThread(DownloadEmbeddedImagesEvent event, String bodyString, String nonBrokenEmail) {
        this.event = event;
        this.bodyString = bodyString;
        this.nonBrokenEmail = nonBrokenEmail;
    }

    @Override
    protected Pair<String, String> doInBackground(Void... params) {
        if (TextUtils.isEmpty(bodyString)) {
            return new Pair<>(bodyString, nonBrokenEmail);
        }
        Document document = Jsoup.parse(bodyString);
        document.outputSettings().indentAmount(0).prettyPrint(false);
        Document nonBrokenDocument = null;
        if (!TextUtils.isEmpty(nonBrokenEmail)) {
            nonBrokenDocument = Jsoup.parse(nonBrokenEmail);
            nonBrokenDocument.outputSettings().indentAmount(0).prettyPrint(false);
        }
        List<EmbeddedImage> images = event.getImages();
        try {
            for (EmbeddedImage embeddedImage : images) {
                String fileName = embeddedImage.getLocalFileName();
                String messageId = embeddedImage.getMessageId();
                File file = new File(ProtonMailApplication.getApplication().getFilesDir() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS + messageId + "/" + fileName);
                int size = (int) file.length();
                byte[] bytes = new byte[size];
                boolean success = false;
                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();
                    success = true;
                } catch (IOException e) {
                    Logger.doLog(TAG_ABSTRACT_EMBEDDED_IMAGES_THREAD, e.toString());
                }
                if (!success) {
                    continue;
                }
                String encoding = embeddedImage.getEncoding().toLowerCase();
                String contentType = embeddedImage.getContentType().toLowerCase().replace("\r", "").replace("\n","").replaceFirst(";.*$", "");
                String contentId = embeddedImage.getContentId();
                if (TextUtils.isEmpty(contentId)) {
                    continue;
                }
                String cidType1 = "";
                String cidType2 = contentId;
                if (contentId.length() > 2) {
                    cidType1 = contentId.substring(1, contentId.length() - 1);
                }
                Elements element = document.select("img[src=cid:" + cidType2 + "]");
                if (element.size() == 0 && !cidType1.isEmpty()) {
                    element = document.select("img[src=cid:" + cidType1 + "]");
                }
                if (element.size() == 0) {
                    element = document.select("img[rel=" + cidType2 + "]");
                }
                if (element.size() == 0 && !cidType1.isEmpty()) {
                    element = document.select("img[rel=" + cidType1 + "]");
                }
                if (element.size() == 0) {
                    element = document.select("img[src=" + cidType2 + "]");
                }
                if (element.size() == 0 && !cidType1.isEmpty()) {
                    element = document.select("img[src=" + cidType1 + "]");
                }
                if (element.size() == 0) {
                    element = document.select("img[src=cid:" + cidType2 + "]");
                }
                if (element.size() == 0 && !cidType1.isEmpty()) {
                    element = document.select("img[src=cid:" + cidType1 + "]");
                }
                Elements nonBrokenElement = null;
                if (nonBrokenDocument != null) {
                    String contentIdRefined = contentId.substring(1, contentId.length() - 1);
                    nonBrokenElement = nonBrokenDocument.select("img[src=cid:" + contentIdRefined + "]");
                    if (nonBrokenElement.size() == 0) {
                        nonBrokenElement = nonBrokenDocument.select("img[rel=" + contentIdRefined + "]");
                    }
                }
                String image = Base64.encodeToString(bytes, Base64.DEFAULT);
                element.attr("src", "data:" + contentType + ";" + encoding + "," + image);
                if (nonBrokenElement != null) {
                    nonBrokenElement.attr("src", "data:" + contentType + ";" + encoding + "," + image);
                }
            }
        } catch (Exception e) {
            Logger.doLogException(e);
        }
        if (nonBrokenDocument != null) {
            nonBrokenEmail = nonBrokenDocument.toString();
        }
        return new Pair<>(document.toString(), nonBrokenEmail);
    }
}
