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
package ch.protonmail.android.crypto

import com.proton.gopenpgp.armor.Armor
import com.proton.gopenpgp.constants.Constants
import com.proton.gopenpgp.crypto.PGPSplitMessage

class CipherText private constructor (
    val armored: String,
    val keyPacket: ByteArray,
    val dataPacket: ByteArray
) {

    constructor(armored: String) : this(armored, PGPSplitMessage(armored))

    constructor(keyPacket: ByteArray, dataPacket: ByteArray) :
        this(makeArmored(keyPacket, dataPacket), keyPacket, dataPacket)


    private constructor(armored: String, splitMessage: PGPSplitMessage) :
        this(armored, splitMessage.keyPacket, splitMessage.dataPacket)

    private companion object {

        fun makeArmored(keyPacket: ByteArray, dataPacket: ByteArray): String =
            Armor.armorWithType(keyPacket + dataPacket, Constants.PGPMessageHeader)
    }
}
