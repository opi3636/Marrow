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
 * Initialize with phases, then call {@link #update()} each loop iteration. Use
 * {@link #getCurrentPhase()} for time-aware logic or {@link #addPhaseListener} for callbacks.
 * <p>
 * Typical usage:
 * <pre>
 * PhaseManager.init(this, new Phase("Auto", 30), new Phase("Park", 3));
 * while (opModeIsActive()) {
 *     PhaseManager.update();
 *     if (PhaseManager.isCurrentPhase("Park")) { ... }
 * }
 * </pre>
 *
 * @see Phase
 */
public class PhaseManager {
    private static OpMode currentOpMode = null;
    private static TimerEx matchTimer = null;

    // Configured phases in order
    private static final List<Phase> phases = new ArrayList<>();

    // Current state
    private static int currentPhaseIndex = 0;
    private static int previousPhaseIndex = -1;
    private static Phase currentPhase = null;

    // Listeners
    private static final List<PhaseListener> phaseListeners = new ArrayList<>();

    private PhaseManager() {}

    /**
     * Initializes the manager with phases in order.
     * <p>
     * Call once before {@code waitForStart()}.
     *
     * @param opMode the OpMode instance
     * @param phasesToRun phases to transition through in order
     * @throws IllegalArgumentException if no phases are provided
     */
    public static void init(@NonNull OpMode opMode, @NonNull Phase... phasesToRun) {
        if (phasesToRun.length == 0) {
            throw new IllegalArgumentException("At least one phase must be provided");
        }

        currentOpMode = opMode;
        matchTimer = new TimerEx(getTotalDuration(phasesToRun), TimeUnit.SECONDS);
        phases.clear();
        phases.addAll(Arrays.asList(phasesToRun));
        currentPhaseIndex = 0;
        previousPhaseIndex = -1;
        currentPhase = phases.get(0);
    }

    /**
     * Sums phase durations in seconds.
     */
    private static double getTotalDuration(Phase[] phases) {
        double total = 0;
        for (Phase phase : phases) {
            total += phase.getDurationSeconds();
        }
        return total;
    }

    /**
     * Updates the current phase based on elapsed time. Call once per loop.
     */
    public static void update() {
        if (currentOpMode == null || phases.isEmpty()) {
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
        for (int i = 0; i < phases.size(); i++) {
            Phase phase = phases.get(i);
            double phaseEnd = timeAccumulated + phase.getDurationSeconds();

            if (elapsedSeconds < phaseEnd || i == phases.size() - 1) {
                // We're in this phase (or stay on last phase if exceeded)
                currentPhaseIndex = i;
                currentPhase = phase;
                break;
            }

            timeAccumulated = phaseEnd;
        }

        // Notify listeners of phase transitions
        if (currentPhaseIndex != previousPhaseIndex && previousPhaseIndex != -1) {
            notifyPhaseChange();
        }
        previousPhaseIndex = currentPhaseIndex;
    }

    /**
     * Gets the current phase.
     */
    public static @NonNull Phase getCurrentPhase() {
        return currentPhase != null ? currentPhase : phases.get(0);
    }

    /**
     * Checks if the current phase matches the given phase (by name).
     */
    public static boolean isCurrentPhase(@NonNull Phase phase) {
        return getCurrentPhase().equals(phase);
    }

    /**
     * Checks if the current phase name matches the given name.
     */
    public static boolean isCurrentPhase(@NonNull String phaseName) {
        return getCurrentPhase().getName().equals(phaseName);
    }

    /**
     * Gets elapsed time in seconds since the match started (0 if not started).
     */
    public static double getElapsedTime() {
        return matchTimer != null ? matchTimer.getElapsed() : 0;
    }

    /**
     * Gets remaining time in the match (in seconds).
     */
    public static double getTimeRemaining() {
        if (matchTimer == null) {
            return getTotalMatchDuration();
        }

        double remaining = matchTimer.getRemaining();
        return Math.max(0, remaining);
    }

    /**
     * Gets remaining time in the current phase (in seconds).
     */
    public static double getPhaseTimeRemaining() {
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
    private static double getPhaseStartTime() {
        double start = 0;
        for (int i = 0; i < currentPhaseIndex; i++) {
            start += phases.get(i).getDurationSeconds();
        }
        return start;
    }

    /**
     * Gets total match duration (sum of all phase durations, in seconds).
     */
    public static double getTotalMatchDuration() {
        double total = 0;
        for (Phase phase : phases) {
            total += phase.getDurationSeconds();
        }
        return total;
    }

    /**
     * Registers a listener to be notified on phase changes.
     */
    public static void addPhaseListener(@NonNull PhaseListener listener) {
        phaseListeners.add(listener);
    }

    /**
     * Unregisters a phase listener.
     */
    public static void removePhaseListener(@NonNull PhaseListener listener) {
        phaseListeners.remove(listener);
    }

    /**
     * Clears all phase listeners.
     */
    public static void clearPhaseListeners() {
        phaseListeners.clear();
    }

    private static void notifyPhaseChange() {
        for (PhaseListener listener : phaseListeners) {
            try {
                listener.onPhaseEntered(currentPhase);
            } catch (Exception e) {
                // Prevent listener exceptions from breaking the match
                if (currentOpMode != null) {
                    currentOpMode.telemetry.addLine("ERROR in phase listener: " + e.getMessage());
                }
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
