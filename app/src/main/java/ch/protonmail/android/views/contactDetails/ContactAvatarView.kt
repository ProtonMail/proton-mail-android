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
package ch.protonmail.android.views.contactDetails

import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import ch.protonmail.android.R
import ch.protonmail.android.utils.UiUtil
import kotlinx.android.synthetic.main.cv_contact_avatar.view.*

// region constants
const val TYPE_INITIALS = 0
const val TYPE_PHOTO = 1
// endregion

class ContactAvatarView : FrameLayout {

    private var mContext: Context? = null
    private var mAttrs: AttributeSet? = null
    private var mDefStyleRes: Int? = 0
    private var mDefStyleAttr: Int? = 0
    private var mName: CharSequence? = ""
    private var mType: Int? = 0
    private var mRadius: Float? = 0.toFloat()

    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    )
            : super(context, attrs, defStyleAttr) {
        mContext = context
        mAttrs = attrs
        mDefStyleAttr = defStyleAttr
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    )
            : super(context, attrs, defStyleAttr, defStyleRes) {
        mContext = context
        mAttrs = attrs
        mDefStyleAttr = defStyleAttr
        mDefStyleRes = defStyleRes
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.cv_contact_avatar, this, true)

        mAttrs?.let {
            val typedArray = context.obtainStyledAttributes(
                it,
                R.styleable.ContactAvatarView, 0, 0
            )

            mName = resources.getText(
                typedArray
                    .getResourceId(
                        R.styleable
                            .ContactAvatarView_contactInitials,
                        0
                    )
            )

            contactInitials.text = UiUtil.extractInitials(mName.toString())

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                contactImage.setImageDrawable(
                    resources.getDrawable(
                        typedArray
                            .getResourceId(
                                R.styleable
                                    .ContactAvatarView_contactPhoto,
                                0
                            ), null
                    )
                )
            } else {
                contactImage.setImageDrawable(
                    resources.getDrawable(
                        typedArray
                            .getResourceId(
                                R.styleable
                                    .ContactAvatarView_contactPhoto,
                                0
                            )
                    )
                )
            }

            setAvatarType(
                resources.getInteger(
                    typedArray
                        .getResourceId(
                            R.styleable
                                .ContactAvatarView_avatarType,
                            0
                        )
                )
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val typedValue = TypedValue()
                resources.getValue(
                    typedArray.getResourceId(
                        R.styleable.ContactAvatarView_contactPhotoCornerRadius,
                        height / 2
                    ), typedValue, true
                )
                photoCardViewWrapper.radius = typedValue.float
            } else {
                photoCardViewWrapper.preventCornerOverlap = false
                photoCardViewWrapper.radius = 0.toFloat()
            }

            typedArray.recycle()
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val contactImageLP = contactImage.layoutParams as FrameLayout.LayoutParams
        contactImageLP.width = w
        contactImageLP.height = h
        contactImage.layoutParams = contactImageLP
        contactImage.layoutParams = contactImageLP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setPhotoCornerRadius((h / 2).toFloat())
        } else {
            setPhotoCornerRadius(0.toFloat())
        }
        val photoCardViewWrapperLP = photoCardViewWrapper.layoutParams as FrameLayout.LayoutParams
        photoCardViewWrapperLP.width = w
        photoCardViewWrapperLP.height = h
        val contactInitialsLP = contactInitials.layoutParams as FrameLayout.LayoutParams
        contactInitialsLP.width = w
        contactInitialsLP.height = h
        contactInitials.layoutParams = contactInitialsLP
    }

    fun setName(name: String) {
        mName = name
        contactInitials.text = UiUtil.extractInitials(name)
    }

    fun setImage(image: Bitmap) {
        try {
            contactImage.setImageBitmap(image)
        } catch (e: Exception) {
            // TODO: sentry log
        }
    }

    fun setAvatarType(type: Int) {
        mType = type
        if (mType == TYPE_INITIALS) {
            contactInitials.visibility = View.VISIBLE
            contactImage.visibility = View.GONE
        } else if (mType == TYPE_PHOTO) {
            contactInitials.visibility = View.GONE
            contactImage.visibility = View.VISIBLE
        }
    }

    fun setPhotoCornerRadius(radius: Float) {
        mRadius = radius
        photoCardViewWrapper.radius = mRadius!!
    }
}