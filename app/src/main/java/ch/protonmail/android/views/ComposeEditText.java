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
package ch.protonmail.android.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.EditText;

public class ComposeEditText extends EditText {

    public ComposeEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ComposeEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTextContextMenuItem(int id) {
        boolean consumed = super.onTextContextMenuItem(id);

        switch (id){
            case android.R.id.cut:
            case android.R.id.paste:
            case android.R.id.copy:
                break;
        }
        return consumed;
    }
}
