package com.skeletonarmy.marrow.phases;

import androidx.annotation.NonNull;

import java.util.concurrent.TimeUnit;

/**
 * Represents a named time period within a match.
 * <p>
 * Define phases with a name, duration, and optional time unit (defaults to seconds).
 * <p>
 * Examples:
 * <pre>
 * Phase auto = new Phase("Autonomous", 30);
 * Phase quick = new Phase("Quick", 500, TimeUnit.MILLISECONDS);
 * </pre>
 *
 * @see PhaseManager
 */
public class Phase {
    private final String name;
    private final double duration;
    private final TimeUnit unit;

    /**
     * Creates a phase with a duration in seconds.
     *
     * @param name the phase name
     * @param durationSeconds the duration in seconds
     */
    public Phase(@NonNull String name, double durationSeconds) {
        this(name, durationSeconds, TimeUnit.SECONDS);
    }

    /**
     * Creates a phase with a duration and time unit.
     *
     * @param name the phase name
     * @param duration the duration value
     * @param unit the time unit (SECONDS, MILLISECONDS, NANOSECONDS)
     */
    public Phase(@NonNull String name, double duration, @NonNull TimeUnit unit) {
        this.name = name;
        this.duration = duration;
        this.unit = unit;
    }

    /**
     * Gets the name of this phase.
     *
     * @return the phase name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Gets the duration in the original time unit.
     *
     * @return the duration
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Gets the time unit of this phase.
     *
     * @return the time unit
     */
    @NonNull
    public TimeUnit getUnit() {
        return unit;
    }

    /**
     * Gets the duration in seconds (converted from the original time unit).
     *
     * @return the duration in seconds
     */
    public double getDurationSeconds() {
        return convertToSeconds(duration, unit);
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Phase)) return false;
        Phase that = (Phase) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Converts a duration from the given time unit to seconds.
     *
     * @param duration the duration value
     * @param unit the time unit
     * @return the duration in seconds
     */
    private static double convertToSeconds(double duration, TimeUnit unit) {
        switch (unit) {
            case NANOSECONDS:
                return duration / 1_000_000_000.0;
            case MICROSECONDS:
                return duration / 1_000_000.0;
            case MILLISECONDS:
                return duration / 1_000.0;
            case SECONDS:
                return duration;
            case MINUTES:
                return duration * 60.0;
            case HOURS:
                return duration * 3600.0;
            case DAYS:
                return duration * 86400.0;
            default:
                return duration;
        }
    }
}
