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
package ch.protonmail.android.bl;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Created by dino on 1/9/18.
 */
@Singleton
public class HtmlProcessor {

    private IMessageHtmlHandler mPreviousHandler;
    private IMessageHtmlHandler mFirstHandler;

    @Inject
    public HtmlProcessor() {
        // empty
    }

    public void addHandler(IMessageHtmlHandler handler) {
        if (mPreviousHandler != null) {
            mPreviousHandler.setNext(handler);
        } else {
            mFirstHandler = handler;
        }
        mPreviousHandler = handler;
    }

    public String digestMessage(String message) {
        return mFirstHandler.digestMessage(message);
    }
}
