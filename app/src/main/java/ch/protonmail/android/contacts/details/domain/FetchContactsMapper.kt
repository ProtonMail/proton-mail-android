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

package ch.protonmail.android.contacts.details.domain

import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.utils.crypto.OpenPGP
import ezvcard.Ezvcard
import ezvcard.property.Address
import ezvcard.property.Anniversary
import ezvcard.property.Birthday
import ezvcard.property.Email
import ezvcard.property.Gender
import ezvcard.property.Nickname
import ezvcard.property.Note
import ezvcard.property.Organization
import ezvcard.property.Photo
import ezvcard.property.Role
import ezvcard.property.Telephone
import ezvcard.property.Title
import ezvcard.property.Url
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import java.security.GeneralSecurityException
import javax.inject.Inject

class FetchContactsMapper @Inject constructor(
    userManager: UserManager,
    openPgp: OpenPGP,
    val crypto: UserCrypto = UserCrypto(userManager, openPgp, userManager.requireCurrentUserId())
) {

    fun mapEncryptedDataToResult(encryptedDataList: MutableList<ContactEncryptedData>?): FetchContactDetailsResult? {
        if (!encryptedDataList.isNullOrEmpty()) {

            var decryptedVCardType0: String = EMPTY_STRING
            var decryptedVCardType2: String = EMPTY_STRING
            var decryptedVCardType3: String = EMPTY_STRING
            var vCardType2Signature: String = EMPTY_STRING
            var vCardType3Signature: String = EMPTY_STRING

            for (contactEncryptedData in encryptedDataList) {
                when (getCardTypeFromInt(contactEncryptedData.type)) {
                    Constants.VCardType.SIGNED_ENCRYPTED -> {
                        val tct = CipherText(contactEncryptedData.data)
                        decryptedVCardType3 = crypto.decrypt(tct).decryptedData
                        vCardType3Signature = contactEncryptedData.signature
                    }
                    Constants.VCardType.SIGNED -> {
                        decryptedVCardType2 = contactEncryptedData.data
                        vCardType2Signature = contactEncryptedData.signature
                    }
                    Constants.VCardType.UNSIGNED -> {
                        decryptedVCardType0 = contactEncryptedData.data
                    }
                }
            }
            return mapResultToCardItems(
                decryptedVCardType0,
                decryptedVCardType2,
                decryptedVCardType3,
                vCardType2Signature,
                vCardType3Signature
            )
        }
        return null
    }

    private fun getCardTypeFromInt(vCardTypeValue: Int): Constants.VCardType {
        return Constants.VCardType.values().find {
            vCardTypeValue == it.vCardTypeValue
        } ?: Constants.VCardType.UNSIGNED
    }

    private fun mapResultToCardItems(
        decryptedVCardType0: String, // UNSIGNED
        decryptedVCardType2: String, // SIGNED
        decryptedVCardType3: String, // SIGNED_ENCRYPTED
        vCardType2Signature: String,
        vCardType3Signature: String
    ): FetchContactDetailsResult {

        val vCardType0 = if (decryptedVCardType0.isNotEmpty()) {
            Ezvcard.parse(decryptedVCardType0).first()
        } else null

        val vCardType2 = if (decryptedVCardType2.isNotEmpty()) {
            Ezvcard.parse(decryptedVCardType2).first()
        } else null

        val vCardType3 = if (decryptedVCardType3.isNotEmpty()) {
            Ezvcard.parse(decryptedVCardType3).first()
        } else null

        val vCardToShare = if (decryptedVCardType0.isNotEmpty()) {
            decryptedVCardType0
        } else {
            decryptedVCardType2
        }

        val contactName = when {
            vCardType0?.formattedName?.value != null -> vCardType0.formattedName.value
            vCardType2?.formattedName?.value != null -> vCardType2.formattedName.value
            vCardType3?.formattedName?.value != null -> vCardType3.formattedName.value
            else -> {
                Timber.i("Unable to get name information from available vCard data")
                EMPTY_STRING
            }
        }

        val emails: List<Email> = when {
            vCardType0?.emails?.isNotEmpty() == true -> vCardType0.emails
            vCardType2?.emails?.isNotEmpty() == true -> vCardType2.emails
            vCardType3?.emails?.isNotEmpty() == true -> vCardType3.emails
            else -> {
                Timber.d("Unable to get emails information from available vCard data")
                emptyList()
            }
        }

        val telephoneNumbers: List<Telephone> = when {
            vCardType3?.telephoneNumbers?.isNotEmpty() == true -> vCardType3.telephoneNumbers
            vCardType0?.telephoneNumbers?.isNotEmpty() == true -> vCardType0.telephoneNumbers
            vCardType2?.telephoneNumbers?.isNotEmpty() == true -> vCardType2.telephoneNumbers
            else -> {
                Timber.d("Unable to get telephone numbers information from available vCard data")
                emptyList()
            }
        }

        val addresses: List<Address> = when {
            vCardType3?.addresses?.isNotEmpty() == true -> vCardType3.addresses
            vCardType0?.addresses?.isNotEmpty() == true -> vCardType0.addresses
            vCardType2?.addresses?.isNotEmpty() == true -> vCardType2.addresses
            else -> {
                Timber.d("Unable to get Addresses information from available vCard data")
                emptyList()
            }
        }

        val photos: List<Photo> = when {
            vCardType3?.photos?.isNotEmpty() == true -> vCardType3.photos
            vCardType0?.photos?.isNotEmpty() == true -> vCardType0.photos
            vCardType2?.photos?.isNotEmpty() == true -> vCardType2.photos
            else -> {
                Timber.d("Unable to get photos information from available vCard data")
                emptyList()
            }
        }

        val organizations: List<Organization> = when {
            vCardType3?.organizations?.isNotEmpty() == true -> vCardType3.organizations
            vCardType0?.organizations?.isNotEmpty() == true -> vCardType0.organizations
            vCardType2?.organizations?.isNotEmpty() == true -> vCardType2.organizations
            else -> {
                Timber.d("Unable to get organizations information from available vCard data")
                emptyList()
            }
        }

        val titles: List<Title> = when {
            vCardType3?.titles?.isNotEmpty() == true -> vCardType3.titles
            vCardType0?.titles?.isNotEmpty() == true -> vCardType0.titles
            vCardType2?.titles?.isNotEmpty() == true -> vCardType2.titles
            else -> {
                Timber.d("Unable to get titles information from available vCard data")
                emptyList()
            }
        }

        val nicknames: List<Nickname> = when {
            vCardType3?.nicknames?.isNotEmpty() == true -> vCardType3.nicknames
            vCardType0?.nicknames?.isNotEmpty() == true -> vCardType0.nicknames
            vCardType2?.nicknames?.isNotEmpty() == true -> vCardType2.nicknames
            else -> {
                Timber.d("Unable to get nicknames information from available vCard data")
                emptyList()
            }
        }

        val birthdays: List<Birthday> = when {
            vCardType3?.birthdays?.isNotEmpty() == true -> vCardType3.birthdays
            vCardType0?.birthdays?.isNotEmpty() == true -> vCardType0.birthdays
            vCardType2?.birthdays?.isNotEmpty() == true -> vCardType2.birthdays
            else -> {
                Timber.d("Unable to get birthdays information from available vCard data")
                emptyList()
            }
        }
        val anniversaries: List<Anniversary> = when {
            vCardType3?.anniversaries?.isNotEmpty() == true -> vCardType3.anniversaries
            vCardType0?.anniversaries?.isNotEmpty() == true -> vCardType0.anniversaries
            vCardType2?.anniversaries?.isNotEmpty() == true -> vCardType2.anniversaries
            else -> {
                Timber.d("Unable to get anniversaries information from available vCard data")
                emptyList()
            }
        }
        val roles: List<Role> = when {
            vCardType3?.roles?.isNotEmpty() == true -> vCardType3.roles
            vCardType0?.roles?.isNotEmpty() == true -> vCardType0.roles
            vCardType2?.roles?.isNotEmpty() == true -> vCardType2.roles
            else -> {
                Timber.d("Unable to get roles information from available vCard data")
                emptyList()
            }
        }
        val urls: List<Url> = when {
            vCardType3?.urls?.isNotEmpty() == true -> vCardType3.urls
            vCardType0?.urls?.isNotEmpty() == true -> vCardType0.urls
            vCardType2?.urls?.isNotEmpty() == true -> vCardType2.urls
            else -> {
                Timber.d("Unable to get urls information from available vCard data")
                emptyList()
            }
        }

        val gender: Gender? = when {
            vCardType3?.gender != null -> vCardType3.gender
            vCardType0?.gender != null -> vCardType0.gender
            vCardType2?.gender != null -> vCardType2.gender
            else -> {
                Timber.d("Unable to get gender information from available vCard data")
                null
            }
        }

        val notes: List<Note> = when {
            vCardType3?.notes?.isNotEmpty() != null -> vCardType3.notes
            vCardType0?.notes?.isNotEmpty() != null -> vCardType0.notes
            vCardType2?.notes?.isNotEmpty() != null -> vCardType2.notes
            else -> {
                Timber.d("Unable to get notes information from available vCard data")
                emptyList()
            }
        }

        val contactUid = when {
            vCardType3?.uid?.value != null -> vCardType3.uid.value
            vCardType0?.uid?.value != null -> vCardType0.uid.value
            vCardType2?.uid?.value != null -> vCardType2.uid.value
            else -> {
                Timber.i("Unable to get name information from available vCard data")
                EMPTY_STRING
            }
        }

        val isType2SignatureValid: Boolean? =
            if (vCardType2Signature.isNotEmpty() && decryptedVCardType2.isNotEmpty()) {
                try {
                    crypto.verify(decryptedVCardType2, vCardType2Signature).isSignatureValid
                } catch (exception: GeneralSecurityException) {
                    Timber.w(exception, "VCard type2 verification error")
                    false
                }
            } else {
                null
            }

        val isType3SignatureValid: Boolean? =
            if (vCardType3Signature.isNotEmpty() && decryptedVCardType3.isNotEmpty()) {
                try {
                    crypto.verify(decryptedVCardType3, vCardType3Signature).isSignatureValid
                } catch (exception: GeneralSecurityException) {
                    Timber.w(exception, "VCard type3 verification error")
                    false
                }
            } else {
                null
            }

        return FetchContactDetailsResult(
            contactUid,
            contactName,
            emails,
            telephoneNumbers,
            addresses,
            photos,
            organizations,
            titles,
            nicknames,
            birthdays,
            anniversaries,
            roles,
            urls,
            vCardToShare,
            gender,
            notes,
            isType2SignatureValid,
            isType3SignatureValid,
        )
    }

}
