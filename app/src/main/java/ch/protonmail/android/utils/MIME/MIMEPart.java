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
package ch.protonmail.android.utils.MIME;

import android.os.SystemClock;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;

import ch.protonmail.android.utils.ServerTime;
import timber.log.Timber;


/**
 * Created by kaylukas on 22/05/2018.
 */
public class MIMEPart extends MimeMultipart {

    private static int id = 0;

    private String random128BitHex() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            ByteBuffer b = ByteBuffer.allocate(52);
            b.putInt(getUniqueId());
            b.putInt(this.hashCode());
            b.putInt(md.hashCode());
            b.putLong(System.nanoTime());
            b.putLong(ServerTime.currentTimeMillis());
            b.putLong(System.currentTimeMillis());
            b.putLong(SystemClock.uptimeMillis());
            b.putLong(SystemClock.elapsedRealtime());
            byte[] data = md.digest(b.array());
            return String.format("%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x%02x", data[0],
                    data[1], data[2], data[3], data[4], data[5], data[6], data[7], data[8], data[9],
                    data[10], data[11], data[12], data[13], data[14], data[15]);
        }
        catch(NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    public MIMEPart(String subtype) {
        super(subtype);

        // override generated boundary to hide personal information included in the original.
        String boundary = "---------------------" + random128BitHex();
        String contentType = "multipart/" + subtype + "; boundary=\"" + boundary + "\"";
        this.contentType = contentType;
    }

    public MIMEPart(DataSource ds) throws MessagingException {
        super(ds);
    }

    public BodyPart asBodyPart() throws IOException, MessagingException {
        final PipedOutputStream pos = new PipedOutputStream();
        PipedInputStream pis = new PipedInputStream(pos);
        MimeWriter writer = new MimeWriter(pos);
        writer.start();
        MimeBodyPart bodypart = new MimeBodyPart(pis);
        writer.spitOutException();
        bodypart.setHeader("Content-Type", getContentType());
        return bodypart;
    }

    @Override
    public synchronized void writeTo(OutputStream os) throws IOException, MessagingException {
        try {
            updateHeaders();
        } catch (Exception exception) {
            Timber.e(exception, "Update headers exception");
        }
        super.writeTo(os);
    }

    public synchronized void writeHeaders(OutputStream os) throws IOException {
        os.write(("Content-Type: " + getContentType() + "\r\n\r\n").getBytes());
    }

    private static synchronized int getUniqueId() {
        return id++;
    }

    class MimeWriter extends Thread {

        OutputStream os;
        IOException ioe;
        MessagingException me;
        MimeWriter(OutputStream os) {
            this.os = os;
            ioe = null;
            me = null;
        }

        public void run () {
                try {
                    MIMEPart.this.writeHeaders(os);
                    MIMEPart.this.writeTo(os);
                } catch (IOException e) {
                    this.ioe = e;
                } catch (MessagingException e) {
                    // swallow
                    Timber.e(e, "MessagingException");
                    this.me = e;
                } finally {
                    try {
                        os.close();
                    } catch (IOException e) {
                        ;
                    }
                }
        }

        public void spitOutException() throws MessagingException, IOException {
            try {
                join();
            } catch (InterruptedException e) {
            }
            if (ioe != null) {
                throw ioe;
            }
            if (me != null) {
                throw me;
            }
        }
    }
}
