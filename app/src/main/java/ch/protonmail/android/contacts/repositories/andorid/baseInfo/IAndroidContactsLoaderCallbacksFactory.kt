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
package ch.protonmail.android.contacts.repositories.andorid.baseInfo

import androidx.lifecycle.MutableLiveData
import android.database.Cursor
import androidx.loader.app.LoaderManager

/**
 * Created by Kamil Rajtar on 23.08.18.
 */
interface IAndroidContactsLoaderCallbacksFactory<T> {
	fun createAndroidContactsLoaderCallbacks(items:MutableLiveData<List<T>>): LoaderManager.LoaderCallbacks<Cursor>
}