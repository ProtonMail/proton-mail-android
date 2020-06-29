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

import org.jetbrains.annotations.Contract
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Nikola Nolchevski on 12-Apr-20.
 */
class TestData @Contract(pure = true) private constructor(
    val internalEmailAddressTrustedKeys: String,
    val internalEmailAddressNotTrustedKeys: String,
    val externalEmailAddressPGPEncrypted: String,
    val externalEmailAddressPGPSigned: String,
    val messageSubject: String,
    val messageBody: String) {

    companion object {
        //SEARCH MESSAGE
        const val searchMessageSubject = "Random Subject"
        const val searchMessageSubjectNotFound = "MessageNotFound :O"

        //CONTACT DATA
        const val newContactName = "A new contact"
        val editContactName = "Edited on $dateAndTime"
        const val editEmailAddress = "test@pmautomation.test"

        //GROUP DATA
        private val random = Random()
        private val randomInt = random.nextInt()
        val newGroupName = "A New group #$randomInt"
        val editGroupName = "Group edited on $dateAndTime"
        private val dateAndTime: String
            get() {
                val cal = Calendar.getInstance()
                val dateFormat: DateFormat = SimpleDateFormat("MMM/dd/yyyy HH:mm ")
                return dateFormat.format(cal.time)
            }

        val randomWord: String
            get() {
                val myDictionary = RandomDictionary().dictionary
                val index = Random().nextInt(myDictionary.size)
                return myDictionary.removeAt(index)
            }

        @Contract(value = " -> new", pure = true)
        fun composerData(): TestData {
            return TestData(
                "PMAutomationRobot3@protonmail.com",  //pass: auto123
                "PMAutomationRobot4@protonmail.com",  //pass: auto123
                "protonextaddress2@gmail.com",  //pass: Protonextaddress1!
                "protonextaddress1@gmail.com",  //pass: Protonextaddress1!
                "Random Subject: ${System.currentTimeMillis()}",
                "\n\nHello ProtonMail!\nRandom body:\n\n${System.currentTimeMillis()}")
        }
    }
}