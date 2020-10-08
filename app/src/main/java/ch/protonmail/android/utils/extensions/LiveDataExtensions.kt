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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

/**
 * Emits the items that pass through the predicate.
 * As per https://stackoverflow.com/questions/61694154/livedata-with-filters
 * and https://github.com/adibfara/Lives/blob/master/lives/src/main/java/com/snakydesign/livedataextensions/Filtering.kt
 */
inline fun <T> LiveData<T>.filter(crossinline predicate: (T?) -> Boolean): LiveData<T> {
    val mutableLiveData: MediatorLiveData<T> = MediatorLiveData()
    mutableLiveData.addSource(this) {
        if (predicate(it))
            mutableLiveData.value = it
    }
    return mutableLiveData
}
