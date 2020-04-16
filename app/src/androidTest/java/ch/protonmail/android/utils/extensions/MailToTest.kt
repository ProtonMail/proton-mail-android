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
//package ch.protonmail.android.utils.extensions
//
//import android.content.Intent
//import android.net.Uri
//import ch.protonmail.android.utils.toMailTo
//import com.nhaarman.mockito_kotlin.doReturn
//import com.nhaarman.mockito_kotlin.mock
//import org.junit.Test
//import org.junit.jupiter.api.Assertions.assertEquals
//
///**
// * Test suite for MailToUtils in [ch.protonmail.android.utils]
// * @author Davide Farella
// */
//internal class MailToTest {
//
//    private fun mockIntent(url: String) = mock<Intent> {
//        on { dataString } doReturn url
//        on { scheme } doReturn Uri.parse(url).scheme
//    }
//
//    @Test
//    fun toMailTo_create_MailTo_correctly() {
//        val realUrl = "mailto:sales@langer-emv.de?subject=Enquiry%20for%20Z23-1%20set%20-%20Shielding%20Tent%20(900%20x%20500x%20400)%20mm&body=Dear%20Langer-EMV%20team,%0D%0A%0D%0A%0D%0A---%20Your%20message%20---%0D%0A%0D%0A%0D%0AMy%20contact%20details:%0D%0A--------------------------------------%0D%0AFirst%20name:%0D%0ALast%20name:%0D%0ACompany:%0D%0AStreet%20name:%0D%0AHouse%20number:%0D%0APostcode:%0D%0ACity:%0D%0ACountry:%0D%0AEmail%20adress:%0D%0ATelephone%20number:%0D%0ACompany%20web%20address:%0D%0A--------------------------------------"
//        val realResult = mockIntent(realUrl).toMailTo()
//
//        with(realResult) {
//            assertEquals("sales@langer-emv.de", addresses[0])
//            assert(subject.startsWith("Enquiry"))
//            assert(body.startsWith("Dear Langer"))
//        }
//
//        val completeUrl = "mailto:1@mail.com,2@mail.com,3@mail.com?cc=4@mail.com,5@mail.com,6@mail.com&subject=Some%20title&body=Hello%20World"
//        val completeResult = mockIntent(completeUrl).toMailTo()
//
//        with(completeResult){
//            assertEquals((1..3).map { "$it@mail.com" }, addresses)
//            assertEquals((4..6).map { "$it@mail.com" }, cc)
//            assertEquals("Some title", subject)
//            assertEquals("Hello World", body)
//        }
//    }
//}