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
package ch.protonmail.android.contacts.list.listView

data class ContactItem @JvmOverloads constructor(val isProtonMailContact: Boolean,
                                                 var contactId: String? = null,
                                                 private var name: String? = null,
                                                 private var email: String? = null,
                                                 var additionalEmailsCount: Int = 0,
                                                 var labels: List<String>? = null,
                                                 var isChecked: Boolean = false) {
    val firstChar: Char
        get() {
            val name = getName()
            val email = getEmail()
            return when {
                !name.isEmpty() -> name[0]
                !email.isEmpty() -> email[0]
                else -> 'U'
            }
        }

    fun getName(): String {
        return this.name ?: ""
    }

    fun getEmail(): String {
        return email ?: ""
    }

    fun setName(name: String) {
        this.name = name
    }

    fun setEmail(email: String) {
        this.email = email
    }

}