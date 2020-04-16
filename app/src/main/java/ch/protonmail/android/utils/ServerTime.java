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
package ch.protonmail.android.utils;

/**
 * Created by kaylukas on 22/06/2018.
 */
public class ServerTime {

    private static long serverTime;
    private static long lastClientTime;

    public static void updateServerTime(long serverTime) {
        ServerTime.serverTime = serverTime;
        lastClientTime = System.nanoTime();
    }

    public static long currentTimeMillis() {
        if (serverTime == 0) {
            return System.currentTimeMillis();
        }
        long timeDiff = (System.nanoTime() - lastClientTime)/ 1000000;
        return serverTime + timeDiff;
    }
}
