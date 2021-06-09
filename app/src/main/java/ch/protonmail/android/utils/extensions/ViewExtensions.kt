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
package ch.protonmail.android.utils.extensions

import android.content.Context
import android.graphics.drawable.Drawable
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StyleRes
import ch.protonmail.android.BuildConfig

/*
 * Extensions for Android's View's
 *
 * Author: Davide Farella
 */

/**
 * @return non null [Drawable] or throw [IllegalStateException]
 */
fun Context.getDrawableOrThrow(@DrawableRes id: Int): Drawable =
    checkNotNull(getDrawable(id))

/** Execute the [listener] on [TextWatcher.onTextChanged] */
inline fun EditText.onTextChange(crossinline listener: (CharSequence) -> Unit): TextWatcher {
    val watcher = object : TextWatcher {
        override fun afterTextChanged(editable: Editable) {
            /* Do nothing */
        }

        override fun beforeTextChanged(text: CharSequence, start: Int, count: Int, after: Int) {
            /* Do nothing */
        }

        override fun onTextChanged(text: CharSequence, start: Int, before: Int, count: Int) {
            listener(text)
        }
    }
    addTextChangedListener(watcher)
    return watcher
}

fun TextView.setStyle(@StyleRes styleId: Int) {
    setTextAppearance(styleId)
}

/**
 * This is useful for use a placeholder resource for Preview.
 * Given a view 'X', that contains a view `Y`, for 'Y' we can't simply use `if ([isInPreviewMode]()) setText("hello")`, "
 *  because we would not be able to correctly preview 'X', using 'B' using another text for 'Y'
 *
 * @return [fromAttributes] if not null, or [forPreview] only if [isInPreviewMode], otherwise throws
 * @throws IllegalArgumentException
 */
fun <T> View.fromAttributesOrPreviewOrThrow(fromAttributes: T?, forPreview: T): T {
    return if (isInPreviewMode())
        fromAttributes ?: forPreview
    else
        requireNotNull(fromAttributes)
}

/**
 * This is useful for use a placeholder resource for Preview.
 * Given a view 'X', that contains a view `Y`, for 'Y' we can't simply use `if ([isInPreviewMode]()) setText("hello")`, "
 *  because we would not be able to correctly preview 'X', using 'B' using another text for 'Y'
 *
 * @return [fromAttributes] if not null, or [forPreview] only if [isInPreviewMode], otherwise `null`
 */
fun <T> View.fromAttributesOrPreviewOrNull(fromAttributes: T?, forPreview: T): T? {
    return if (isInPreviewMode())
        fromAttributes ?: forPreview
    else
        fromAttributes
}

/**
 * @return `true` if we're in Debug configuration and [View.isInEditMode]
 */
fun View.isInPreviewMode() = BuildConfig.DEBUG && isInEditMode

fun TextView.setNotificationIndicatorSize(notificationCount: Int) {
    textSize = when {
        notificationCount > 1000 -> 10f
        notificationCount > 100 -> 11f
        notificationCount > 10 -> 12f
        else -> 13f
    }
}
