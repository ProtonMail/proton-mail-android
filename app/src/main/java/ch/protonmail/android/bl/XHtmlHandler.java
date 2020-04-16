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
package ch.protonmail.android.bl;

/**
 * Created by dino on 1/9/18.
 */

public final class XHtmlHandler implements IMessageHtmlHandler {

    private IMessageHtmlHandler mNextHandler;
    private final String mLineBreak1 = "<br />";
    private final String mLineBreak2 = "<br/>";
    private final String mNewLine = "\n";

    @Override
    public void setNext(IMessageHtmlHandler messageHandler) {
        mNextHandler = messageHandler;
    }

    @Override
    public String digestMessage(String message) {
        message = message.replaceAll(mLineBreak1, mNewLine).replaceAll(mLineBreak2, mNewLine);
        if (mNextHandler != null) {
            return mNextHandler.digestMessage(message);
        }
        return message;
    }
}
