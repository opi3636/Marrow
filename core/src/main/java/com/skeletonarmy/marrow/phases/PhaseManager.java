package com.skeletonarmy.marrow.phases;

import androidx.annotation.NonNull;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpModeManagerImpl;
import com.qualcomm.robotcore.robot.RobotState;
import com.skeletonarmy.marrow.OpModeManager;
import com.skeletonarmy.marrow.TimerEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Manages match phases and automatically transitions based on elapsed time.
 * <p>
 * Create an instance with phases, then call {@link #update()} each loop iteration. Use
 * {@link #getCurrentPhase()} for time-aware logic or {@link #addPhaseListener} for callbacks.
 * <p>
 * Typical usage:
 * <pre>
 * PhaseManager manager = new PhaseManager(this, new Phase("Auto", 30), new Phase("Park", 3));
 * while (opModeIsActive()) {
 *     manager.update();
 *     if (manager.isCurrentPhase("Park")) { ... }
 * }
 * </pre>
 *
 * @see Phase
 */
public class PhaseManager {
    private final OpMode opMode;
    private final TimerEx matchTimer;

    // Configured phases in order
    private final List<Phase> phases;

    // Current state
    private Phase currentPhase;
    private Phase previousPhase;

    // Listeners
    private final List<PhaseListener> phaseListeners;

    /**
     * Creates a phase manager with phases in order.
     * <p>
     * Call once before {@code waitForStart()}.
     *
     * @param opMode the OpMode instance
     * @param phasesToRun phases to transition through in order
     * @throws IllegalArgumentException if no phases are provided
     */
    public PhaseManager(@NonNull OpMode opMode, @NonNull Phase... phasesToRun) {
        if (phasesToRun.length == 0) {
            throw new IllegalArgumentException("At least one phase must be provided");
        }

        this.opMode = opMode;
        this.phases = new ArrayList<>(Arrays.asList(phasesToRun));
        this.matchTimer = new TimerEx(getTotalDuration(phasesToRun), TimeUnit.SECONDS);
        this.currentPhase = phases.get(0);
        this.previousPhase = null;
        this.phaseListeners = new ArrayList<>();
    }

    /**
     * Sums phase durations in seconds.
     */
    private static double getTotalDuration(@NonNull Phase[] phases) {
        double total = 0;
        for (Phase phase : phases) {
            total += phase.getDurationSeconds();
        }
        return total;
    }

    /**
     * Updates the current phase based on elapsed time. Call once per loop.
     */
    public void update() {
        if (phases.isEmpty()) {
            return;
        }

        // Start the timer on first update (when match starts)
        if (!matchTimer.isOn()) {
            try {
                OpModeManagerImpl manager = OpModeManager.getManager();
                RobotState state = manager.getRobotState();
                if (state == RobotState.RUNNING) {
                    matchTimer.start();
                }
            } catch (Exception e) {
                // OpModeManager not available, match hasn't started yet
                return;
            }
        }

        // Calculate elapsed time from match start
        double elapsedSeconds = matchTimer.getElapsed();

        // Find which phase we're in based on elapsed time
        double timeAccumulated = 0;
        Phase newPhase = phases.get(0);
        for (Phase phase : phases) {
            double phaseEnd = timeAccumulated + phase.getDurationSeconds();

            if (elapsedSeconds < phaseEnd) {
                newPhase = phase;
                break;
            }

            timeAccumulated = phaseEnd;
            newPhase = phase; // Stay on last phase if exceeded
        }

        // Notify listeners of phase transitions
        if (!newPhase.equals(currentPhase) && previousPhase != null) {
            previousPhase = currentPhase;
            currentPhase = newPhase;
            notifyPhaseChange();
        } else if (previousPhase == null) {
            previousPhase = currentPhase;
            currentPhase = newPhase;
        } else {
            currentPhase = newPhase;
        }
    }

    /**
     * Gets the current phase.
     */
    public @NonNull Phase getCurrentPhase() {
        return currentPhase != null ? currentPhase : phases.get(0);
    }

    /**
     * Checks if the current phase matches the given phase (by reference).
     */
    public boolean isCurrentPhase(@NonNull Phase phase) {
        return getCurrentPhase().equals(phase);
    }

    /**
     * Checks if the current phase name matches the given name.
     */
    public boolean isCurrentPhase(@NonNull String phaseName) {
        return getCurrentPhase().getName().equals(phaseName);
    }

    /**
     * Gets elapsed time in seconds since the match started (0 if not started).
     */
    public double getElapsedTime() {
        return matchTimer.getElapsed();
    }

    /**
     * Gets remaining time in the match (in seconds).
     */
    public double getTimeRemaining() {
        double remaining = matchTimer.getRemaining();
        return Math.max(0, remaining);
    }

    /**
     * Gets remaining time in the current phase (in seconds).
     */
    public double getPhaseTimeRemaining() {
        if (currentPhase == null || !matchTimer.isOn()) {
            return currentPhase != null ? currentPhase.getDurationSeconds() : 0;
        }

        double phaseStartTime = getPhaseStartTime();
        double elapsedInPhase = getElapsedTime() - phaseStartTime;
        return Math.max(0, currentPhase.getDurationSeconds() - elapsedInPhase);
    }

    /**
     * Calculates when the current phase started (elapsed seconds from match start).
     */
    private double getPhaseStartTime() {
        double start = 0;
        for (Phase phase : phases) {
            if (phase.equals(currentPhase)) {
                break;
            }
            start += phase.getDurationSeconds();
        }
        return start;
    }

    /**
     * Gets total match duration (sum of all phase durations, in seconds).
     */
    public double getTotalMatchDuration() {
        double total = 0;
        for (Phase phase : phases) {
            total += phase.getDurationSeconds();
        }
        return total;
    }

    /**
     * Registers a listener to be notified on phase changes.
     */
    public void addPhaseListener(@NonNull PhaseListener listener) {
        phaseListeners.add(listener);
    }

    /**
     * Unregisters a phase listener.
     */
    public void removePhaseListener(@NonNull PhaseListener listener) {
        phaseListeners.remove(listener);
    }

    /**
     * Clears all phase listeners.
     */
    public void clearPhaseListeners() {
        phaseListeners.clear();
    }

    private void notifyPhaseChange() {
        for (PhaseListener listener : phaseListeners) {
            try {
                listener.onPhaseEntered(currentPhase);
            } catch (Exception e) {
                // Prevent listener exceptions from breaking the match
                opMode.telemetry.addLine("ERROR in phase listener: " + e.getMessage());
            }
        }
    }

    /**
     * Callback for phase transitions.
     * <p>
     * Example: {@code addPhaseListener(phase -> { if ("Endgame".equals(phase.getName())) {...} })}
     */
    @FunctionalInterface
    public interface PhaseListener {
        /**
         * Called when entering a new phase.
         */
        void onPhaseEntered(@NonNull Phase newPhase);
    }
}
