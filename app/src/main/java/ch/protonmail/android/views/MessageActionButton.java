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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import ch.protonmail.android.R;

public class MessageActionButton extends LinearLayout {

    @BindView(R.id.action_icon)
    ImageView mActionIcon;
    @BindView(R.id.action_text)
    TextView mActionText;

    public MessageActionButton(Context context) {
        this(context, null, 0);
    }

    public MessageActionButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MessageActionButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        LayoutInflater.from(context).inflate(R.layout.message_action_button, this, true);
        ButterKnife.bind(this);

        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.MessageActionButton, defStyle, 0);
        int actionType = typedArray.getInteger(R.styleable.MessageActionButton_actionType, 0);
        typedArray.recycle();

        int stringId;
        int drawableId;

        switch (actionType) {
            case 0: // Reply
            default:
                stringId = R.string.reply;
                drawableId = R.drawable.bottom_reply;
                break;
            case 1: // Reply All
                stringId = R.string.reply_all;
                drawableId = R.drawable.bottom_replyall;
                break;
            case 2: // Forward
                stringId = R.string.forward;
                drawableId = R.drawable.bottom_forward;
                break;
        }

        mActionText.setText(stringId);
        mActionText.setTextColor(context.getResources().getColor(R.color.name_gray));
        mActionIcon.setImageDrawable(context.getResources().getDrawable(drawableId));
    }
}
