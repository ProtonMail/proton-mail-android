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
package ch.protonmail.android.utils.crypto;

import com.proton.gopenpgp.armor.Armor;
import com.proton.gopenpgp.constants.Constants;
import com.proton.gopenpgp.crypto.PGPSplitMessage;

import java.util.Arrays;

public class AbstractCiphertext {

    protected String armored;
    protected byte[] keyPacket;
    protected byte[] dataPacket;

    protected AbstractCiphertext() {
        armored = null;
        keyPacket = null;
        dataPacket = null;
    }

    public String getArmored() throws Exception {
        if (armored == null) {
            byte[] packetList = Arrays.copyOf(keyPacket, keyPacket.length + dataPacket.length);
            System.arraycopy(keyPacket, 0, packetList, keyPacket.length, dataPacket.length);
            armored = Armor.armorWithType(packetList, Constants.PGPMessageHeader);
        }
        return armored;
    }

    private void splitArmored() throws Exception {
        PGPSplitMessage splitMessage = new PGPSplitMessage(armored);
        keyPacket = splitMessage.getKeyPacket();
        dataPacket = splitMessage.getDataPacket();
    }

    public byte[] getKeyPacket() throws Exception {
        if (keyPacket == null) {
            splitArmored();
        }
        return keyPacket;
    }

    public byte[] getDataPacket() throws Exception {
        if (dataPacket == null) {
            splitArmored();
        }
        return dataPacket;
    }
}
