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
@file:JvmName("SerializationUtils") // Name for Java
@file:Suppress("unused")

package ch.protonmail.android.utils.extensions

import com.google.gson.Gson
import java.io.Serializable

/*
 * This files contains utilities for serialize / deserialize objects
 * We use Gson as default serializer
 *
 * Author: Davide Farella
 */

/** A Singleton instance of [Gson] for serialize / deserialize objects */
val DEFAULT_SERIALIZER = Gson()

/** @return [String] Json of the receiver [Serializable] */
fun <T : Serializable> T.serialize(): String = DEFAULT_SERIALIZER.toJson(this)

/** @return [T] [Serializable] from the receiver Json [String] */
inline fun <reified T : Serializable> String.deserialize() = deserialize(T::class.java)

/**
 * @return [T] [Serializable] from the receiver Json [String]
 * We use this because reified can not be used from Java
 */
fun <T : Serializable> String.deserialize(clazz: Class<T>): T =
    DEFAULT_SERIALIZER.fromJson<T>(this, clazz)
