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
package ch.protonmail.android.domain.entity.user

import ch.protonmail.android.domain.entity.Bytes
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.Validated

/**
 * Representation of a server user.
 * @author Davide Farella
 *
 * TODO remove 👇 before merge!
 * https://docs.protontech.ch/#user-get-user-info-get
 *
"User": {
    "ID": "MJLke8kWh1BBvG95JBIrZvzpgsZ94hNNgjNHVyhXMiv4g9cn6SgvqiIFR5cigpml2LD_iUk_3DkV29oojTt3eA==",
    "Name": "jason",
    "UsedSpace": 96691332,
    "Currency": "USD",
    "Credit": 0, TODO
    "MaxSpace": 10737418240,
    "MaxUpload": 26214400,
    "Role": 2, TODO
    "Private": 1, TODO
    "Subscribed": 1, TODO
    "Services": 1, TODO
    "Delinquent": 0,
    "OrganizationPrivateKey": "-----BEGIN PGP PRIVATE KEY BLOCK-----*", TODO
    "Email": "jason@protonmail.ch",
    "DisplayName": "Jason",
    "Keys": [
        {
        "ID": "IlnTbqicN-2HfUGIn-ki8bqZfLqNj5ErUB0z24Qx5g-4NvrrIc6GLvEpj2EPfwGDv28aKYVRRrSgEFhR_zhlkA==",
        "Version": 3,
        "PrivateKey": "-----BEGIN PGP PRIVATE KEY BLOCK-----*-----END PGP PRIVATE KEY BLOCK-----",
        "Fingerprint": "c93f767df53b0ca8395cfde90483475164ec6353", // DEPRECATED
        "Primary": 1
        }
    ]
}
 */
@Validated
data class User( // TODO: consider naming UserInfo or simialar
    val id: Id,
    val name: Name,
    val displayName: Name,
    val email: EmailAddress,
    val keys: Keys,
    val currency: NotBlankString, // might not be worth to have an endless enum
    val subscribed: Boolean,
    val delinquent: Boolean,
    /**
     * Size limit for a Message + relative attachments
     * TODO does this include mail body, signature, sender, receivers & co?
     */
    val totalUploadLimit: Bytes,
    val dedicatedSpace: UserSpace
)

// TODO can this entity be used on other spaces under a different name?
data class UserSpace(val total: Bytes, val used: Bytes)
