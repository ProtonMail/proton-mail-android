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
package ch.protonmail.android.labels.domain.mapper

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.paging.DataSource
import me.proton.core.domain.arch.Mapper

/**
 * [Mapper] for transform Business model to UI model
 */
interface UiModelMapper<in BusinessModel, out UiModel> : Mapper<BusinessModel, UiModel> {

    fun BusinessModel.toUiModel(): UiModel
}

/**
 * Enable to execute a `map` operation on the [LiveData] receiver, passing the [Mapper] as argument.
 * Example: `` myBusinessLiveData.map(myUiModelMapper) { it.toUiModel() } ``
 *
 * @param M type of the [Mapper]
 * @param In source model
 * @param Out result model
 *
 * @return [LiveData] of [Out]
 */
fun <M : Mapper<In, Out>, In, Out> LiveData<In>.map(mapper: M, f: M.(In) -> Out): LiveData<Out> =
    map { mapper.f(it) }

/**
 * Enable to execute a `map` operation on the [DataSource.Factory] receiver, passing the [Mapper] as argument.
 * Example: `` myBusinessDataSourceFactory.map(myUiModelMapper) { it.toUiModel() } ``
 *
 * @param M type of the [Mapper]
 * @param In source model
 * @param Out result model
 *
 * @return [DataSource.Factory] of [Out]
 */
fun <M : Mapper<In, Out>, In, Out> DataSource.Factory<Int, In>.map(
    mapper: M,
    f: M.(In) -> Out
): DataSource.Factory<Int, Out> = map { mapper.f(it) }
