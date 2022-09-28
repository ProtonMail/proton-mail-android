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

import android.content.Context;
import android.text.format.DateUtils;
import android.text.format.Time;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ch.protonmail.android.R;

public class DateUtil {

    private static final String TIMESTAMP_FORMAT = "yyyyMMdd_HHmmss";

    public static String formatDateTime(Context context, long time) {
        int flags;

        if (DateUtils.isToday(time)) {
            flags = DateUtils.FORMAT_SHOW_TIME;
        } else if (isThisYear(time)) {
            flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_NO_YEAR;
        } else {
            flags = DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_YEAR;
        }
        return DateUtils.formatDateTime(context, time, flags);
    }

    public static String formatDate(Date date) {
        DateFormat iso8601format = SimpleDateFormat.getDateInstance();
        return iso8601format.format(date);
    }

    public static String formatDetailedDateTime(Context context, long time) {
        return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_SHOW_TIME);
    }

    public static String formatDateMonthYearAtTime(Context context, long time) {
        return DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_SHOW_YEAR) + " at " + DateUtils.formatDateTime(context, time, DateUtils.FORMAT_SHOW_TIME);
    }

    public static String formatDaysAndHours(Context context, long seconds) {
        int days = (int) (seconds / (24 * 60 * 60));
        seconds = (int) (seconds - days * (24 * 60 * 60));
        int hours = (int) (seconds / (60 * 60));
        seconds = (int) (seconds - hours * (60 * 60));
        int minutes = (int) (seconds / 60);
        return formatDaysAndHours(context, days, hours, minutes);
    }

    public static String formatTheLargestAvailableUnitOnly(Context context, long seconds) {
        int days = (int) (seconds / (24 * 60 * 60));
        if (days > 0) {
            return context.getResources().getString(R.string.expiration_days, days);
        }
        seconds = (int) (seconds - days * (24 * 60 * 60));
        int hours = (int)(seconds / (60 * 60));
        if (hours > 0) {
            return context.getResources().getString(R.string.expiration_hours, hours);
        }
        seconds = (int) (seconds - hours * (60 * 60));
        int minutes = (int) (seconds / 60);
        return context.getResources().getString(R.string.expiration_minutes, minutes);
    }

    public static String formatDaysAndHours(Context context, int days, int hours, int minutes) {
        if (days == 0 && hours == 0 & minutes == 0) {
            return "";
        } else if (days == 0) {
            String hoursString = context.getResources().getString(R.string.expiration_hours, hours);
            String minutesString = context.getResources().getString(R.string.expiration_minutes, minutes);
            return String.format("%s %s", hoursString, minutesString);
        } else if (hours == 0) {
            String daysString = context.getResources().getString(R.string.expiration_days, days);
            String minutesString = context.getResources().getString(R.string.expiration_minutes, minutes);
            return String.format("%s %s", daysString, minutesString);
        } else {
            String daysString = context.getResources().getString(R.string.expiration_days, days);
            String hoursString = context.getResources().getString(R.string.expiration_hours, hours);
            String minutesString = context.getResources().getString(R.string.expiration_minutes, minutes);
            return String.format("%s %s %s", daysString, hoursString, minutesString);
        }
    }

    public static String generateTimestamp() {
        return new SimpleDateFormat(TIMESTAMP_FORMAT).format(System.currentTimeMillis());
    }

    private static boolean isThisYear(long when) {
        Time time1 = new Time();
        time1.set(when);
        Time time2 = new Time();
        time2.set(System.currentTimeMillis());
        return (time1.year == time2.year);
    }
}
