package com.skeletonarmy.marrow.phases;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class PhaseTests {
    private Phase autonomousPhase;
    private Phase teleopPhase;
    private Phase endgamePhase;

    @Test
    public void matchPhase_creation_withNameAndDuration() {
        Phase phase = new Phase("Autonomous", 30);
        assertEquals("Should have correct name", "Autonomous", phase.getName());
        assertEquals("Should have correct duration", 30.0, phase.getDurationSeconds(), 0.01);
    }

    @Test
    public void matchPhase_toString_returnsName() {
        Phase phase = new Phase("Teleop", 150);
        assertEquals("toString should return name", "Teleop", phase.toString());
    }

    @Test
    public void matchPhase_equality_sameObjectAreEqual() {
        Phase phase = new Phase("Autonomous", 30);
        assertEquals("Same object should be equal to itself", phase, phase);
    }

    @Test
    public void matchPhase_equality_differentObjectsAreNotEqual() {
        Phase phase1 = new Phase("Autonomous", 30);
        Phase phase2 = new Phase("Autonomous", 30);
        assertNotEquals("Different objects should not be equal even with same name", phase1, phase2);
    }

    @Test
    public void matchPhase_variableDuration() {
        Phase shortPhase = new Phase("Short", 5);
        Phase longPhase = new Phase("Long", 120.5);
        
        assertEquals("Short phase should be 5 seconds", 5.0, shortPhase.getDurationSeconds(), 0.01);
        assertEquals("Long phase should be 120.5 seconds", 120.5, longPhase.getDurationSeconds(), 0.01);
    }

    @Test
    public void matchPhase_canCreateMultiplePhases() {
        assertEquals("Auto", "Autonomous", autonomousPhase.getName());
        assertEquals("Teleop", "Teleop", teleopPhase.getName());
        assertEquals("Endgame", "Endgame", endgamePhase.getName());
    }

    @Test
    public void matchPhase_defaultTimeUnitIsSeconds() {
        Phase phase = new Phase("Test", 30);
        assertEquals("Default unit should be SECONDS", TimeUnit.SECONDS, phase.getUnit());
    }

    @Test
    public void matchPhase_withMilliseconds() {
        Phase phase = new Phase("Quick", 5000, TimeUnit.MILLISECONDS);
        assertEquals("Should have correct duration", 5000, phase.getDuration(), 0.01);
        assertEquals("Should have correct unit", TimeUnit.MILLISECONDS, phase.getUnit());
        assertEquals("Should convert to 5 seconds", 5.0, phase.getDurationSeconds(), 0.01);
    }

    @Test
    public void matchPhase_withNanoseconds() {
        Phase phase = new Phase("Precise", 1_000_000_000, TimeUnit.NANOSECONDS);
        assertEquals("Should have correct duration", 1_000_000_000, phase.getDuration(), 0.01);
        assertEquals("Should have correct unit", TimeUnit.NANOSECONDS, phase.getUnit());
        assertEquals("Should convert to 1 second", 1.0, phase.getDurationSeconds(), 0.01);
    }

    @Test
    public void matchPhase_mixedTimeUnits() {
        Phase secondsPhase = new Phase("Seconds", 10);
        Phase millisPhase = new Phase("Millis", 10_000, TimeUnit.MILLISECONDS);
        Phase nanosPhase = new Phase("Nanos", 10_000_000_000L, TimeUnit.NANOSECONDS);
        
        assertEquals("Seconds phase", 10.0, secondsPhase.getDurationSeconds(), 0.01);
        assertEquals("Millis phase", 10.0, millisPhase.getDurationSeconds(), 0.01);
        assertEquals("Nanos phase", 10.0, nanosPhase.getDurationSeconds(), 0.01);
    }

    @Test
    public void matchPhase_fractionalDurations() {
        Phase fractionalSeconds = new Phase("Partial", 2.5);
        Phase fractionalMillis = new Phase("PartialMs", 2500, TimeUnit.MILLISECONDS);
        
        assertEquals("Fractional seconds", 2.5, fractionalSeconds.getDurationSeconds(), 0.01);
        assertEquals("Fractional millis", 2.5, fractionalMillis.getDurationSeconds(), 0.01);
    }
}
