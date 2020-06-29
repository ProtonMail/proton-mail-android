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
package ch.protonmail.android.uitests.testsHelper

import ch.protonmail.android.core.ProtonMailApplication
import java.io.DataInputStream
import java.io.IOException
import java.util.*

/**
 * Created by Nikola Nolchevski on 14-Apr-20.
 */
internal class RandomDictionary {
    val dictionary: ArrayList<String>
        get() {
            val dictionary = ArrayList<String>()
            var textFileStream: DataInputStream? = null
            try {
                textFileStream = DataInputStream(ProtonMailApplication.getApplication().assets
                    .open("RandomWordsDictionary.txt"))
            } catch (e: IOException) {
                e.printStackTrace()
            }
            val sc = Scanner(textFileStream)
            while (sc.hasNextLine()) {
                val line = sc.nextLine()
                dictionary.add(line)
            }
            sc.close()
            return dictionary
        }
}