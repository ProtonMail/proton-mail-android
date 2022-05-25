/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.utils;

import java.util.Arrays;
import java.util.List;

/**
 * Class containing a set of constant directions used throughout the package
 *
 * Created by wdullaer on 02.07.14.
 */
public class SwipeDirections {
    // Constants
    public static final int DIRECTION_LEFT = -1;
    public static final int DIRECTION_RIGHT = 1;
    public static final int DIRECTION_NEUTRAL = 0;

    public static List<Integer> getAllDirections(){
        return Arrays.asList(
                DIRECTION_NEUTRAL,
                DIRECTION_LEFT,
                DIRECTION_RIGHT
        );
    }
}
