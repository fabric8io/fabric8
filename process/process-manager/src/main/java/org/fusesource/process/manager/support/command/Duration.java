/*
 * Copyright (C) FuseSource, Inc.
 * http://fusesource.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.process.manager.support.command;

import com.google.common.base.Preconditions;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Duration implements Comparable<Duration>
{
    public static Duration nanosSince(long start)
    {
        long end = System.nanoTime();
        return new Duration(end - start, TimeUnit.NANOSECONDS);
    }

    private final double millis;

    public Duration(double value, TimeUnit timeUnit)
    {
        Preconditions.checkArgument(!Double.isInfinite(value), "value is infinite");
        Preconditions.checkArgument(!Double.isNaN(value), "value is not a number");
        Preconditions.checkArgument(value >= 0, "value is negative");
        Preconditions.checkNotNull(timeUnit, "timeUnit is null");

        double conversionFactor = millisPerTimeUnit(timeUnit);
        millis = value * conversionFactor;
    }

    public double toMillis()
    {
        return millis;
    }

    public double convertTo(TimeUnit timeUnit)
    {
        if (timeUnit == null) {
            throw new NullPointerException("timeUnit is null");
        }
        return convertTo(millis, timeUnit);
    }

    private static double convertTo(double millis, TimeUnit timeUnit)
    {
        double conversionFactor = millisPerTimeUnit(timeUnit);
        return millis / conversionFactor;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Duration duration = (Duration) o;

        if (Double.compare(duration.millis, millis) != 0) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode()
    {
        long temp = Double.doubleToLongBits(millis);
        return (int) (temp ^ (temp >>> 32));
    }

    @Override
    public int compareTo(Duration o)
    {
        return Double.compare(millis, o.millis);
    }

    @Override
    public String toString()
    {
        return toString(TimeUnit.MILLISECONDS);
    }

    public String toString(TimeUnit timeUnit)
    {
        if (timeUnit == null) {
            throw new NullPointerException("timeUnit is null");
        }

        double magnitude = convertTo(millis, timeUnit);
        String timeUnitAbbreviation;
        switch (timeUnit) {
            case MILLISECONDS:
                timeUnitAbbreviation = "ms";
                break;
            case SECONDS:
                timeUnitAbbreviation = "s";
                break;
            case MINUTES:
                timeUnitAbbreviation = "m";
                break;
            case HOURS:
                timeUnitAbbreviation = "h";
                break;
            case DAYS:
                timeUnitAbbreviation = "d";
                break;
            default:
                throw new IllegalArgumentException("Unsupported time unit " + timeUnit);
        }
        return String.format("%.2f%s", magnitude, timeUnitAbbreviation);
    }

    private static double millisPerTimeUnit(TimeUnit timeUnit)
    {
        double conversionFactor;
        switch (timeUnit) {
            case NANOSECONDS:
                conversionFactor = 1.0 / 1000000.0;
                break;
            case MICROSECONDS:
                conversionFactor = 1.0 / 1000.0;
                break;
            case MILLISECONDS:
                conversionFactor = 1;
                break;
            case SECONDS:
                conversionFactor = 1000;
                break;
            case MINUTES:
                conversionFactor = 1000 * 60;
                break;
            case HOURS:
                conversionFactor = 1000 * 60 * 60;
                break;
            case DAYS:
                conversionFactor = 1000 * 60 * 60 * 24;
                break;
            default:
                throw new IllegalArgumentException("Unsupported time unit " + timeUnit);
        }
        return conversionFactor;
    }


    private static final Pattern DURATION_PATTERN = Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*(s|m|h|d|ms)\\s*$");

    public static Duration valueOf(String duration)
            throws IllegalArgumentException
    {
        Preconditions.checkNotNull(duration, "duration is null");
        Preconditions.checkArgument(!duration.isEmpty(), "duration is empty");

        // Parse the duration string
        Matcher matcher = DURATION_PATTERN.matcher(duration);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("duration is not a valid duration string: " + duration);
        }

        // Determine the magnitude of the duration
        String magnitudeString = matcher.group(1);
        double magnitude = Double.parseDouble(magnitudeString);

        // Determine TimeUnit of the duration
        String timeUnitString = matcher.group(2);
        TimeUnit timeUnit;
        if (timeUnitString.equals("ms")) {
            timeUnit = TimeUnit.MILLISECONDS;
        }
        else if (timeUnitString.equals("s")) {
            timeUnit = TimeUnit.SECONDS;
        }
        else if (timeUnitString.equals("m")) {
            timeUnit = TimeUnit.MINUTES;
        }
        else if (timeUnitString.equals("h")) {
            timeUnit = TimeUnit.HOURS;
        }
        else if (timeUnitString.equals("d")) {
            timeUnit = TimeUnit.DAYS;
        }
        else {
            throw new IllegalArgumentException("Unknown time unit: " + timeUnitString);
        }

        return new Duration(magnitude, timeUnit);
    }
}
