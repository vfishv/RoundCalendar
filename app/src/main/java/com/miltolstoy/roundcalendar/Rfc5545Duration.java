/*
Round Calendar
Copyright (C) 2020 Mil Tolstoy <miltolstoy@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.miltolstoy.roundcalendar;

import android.text.format.DateUtils;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.miltolstoy.roundcalendar.Logging.TAG;

class Rfc5545Duration {

    static long toMilliSeconds(String duration) throws IllegalArgumentException {
        Log.d(TAG, "Parsing duration: \"" + duration + "\"");
        if (duration == null || duration.length() <= 1) {
            throw new IllegalArgumentException("Duration should be not empty");
        }
        if (!duration.startsWith("P")) {
            throw new IllegalArgumentException("Duration string should start with \"P\" prefix");
        }

        duration = duration.substring(1); // remove "P" constant prefix
        Pattern pattern = Pattern.compile("(\\d+)([WDHMS])");
        Matcher matcher = pattern.matcher(duration);

        long milliSeconds = 0;
        while (matcher.find()) {
            Log.d(TAG, "count: " + matcher.group(1) + ", dimension: " + matcher.group(2));
            milliSeconds += entryToMillis(Integer.parseInt(matcher.group(1)), matcher.group(2));
        }

        if (milliSeconds == 0) {
            throw new IllegalArgumentException("Malformed duration string: \"" + duration + "\"");
        }

        return milliSeconds;
    }

    private static long entryToMillis(int count, String dimension) {
        Map<String, Long> dimensionMap = new HashMap<String, Long>() {{
            put("W", DateUtils.WEEK_IN_MILLIS);
            put("D", DateUtils.DAY_IN_MILLIS);
            put("H", DateUtils.HOUR_IN_MILLIS);
            put("M", DateUtils.MINUTE_IN_MILLIS);
            put("S", DateUtils.SECOND_IN_MILLIS);
        }};

        Long millis = dimensionMap.get(dimension);
        if (millis == null) {
            throw new IllegalArgumentException("Unknown dimension: " + dimension);
        }
        return count * millis;
    }

}
