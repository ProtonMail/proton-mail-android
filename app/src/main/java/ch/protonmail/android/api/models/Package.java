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
package ch.protonmail.android.api.models;


public class Package {
    private String Address;
    private int Type;
    private String Body;
    private KeyPacket[] KeyPackets;
    private String Token;
    private String EncToken;
    private String PasswordHint;

    public Package(String address, int type, String body, KeyPacket[] keyPackets, String token, String encToken, String passwordHint) {
        Address = address;
        Type = type;
        Body = body;
        KeyPackets = keyPackets;
        Token = token;
        EncToken = encToken;
        PasswordHint = passwordHint;
    }

    public Package(String address, int type, String body, KeyPacket[] keyPackets) {
        Address = address;
        Type = type;
        Body = body;
        KeyPackets = keyPackets;
    }

    public String getAddress() { return Address; }

    public int getType() { return Type; }

    public String getBody() {
        return Body;
    }

    public String getToken() { return Token; }

    public void setAddress(String address) {
        Address = address;
    }

    public void setType(int type) {
        Type = type;
    }

    public void setBody(String body) {
        Body = body;
    }

    public void setToken(String token) {
        Token = token;
    }
}
