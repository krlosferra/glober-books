package com.globant.globerbooks.config.sqlite;

import java.util.List;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.FailureAnalyzer;

/**
 * Reports every start refusal with a fixed text (SC-11, SC-37, Task 34a-2). It recognizes a
 * {@link DataDirectoryGuard.Refusal} and the three {@link StartupPolicy} reasons anywhere in the
 * cause chain; the description is the fixed reason, the action is one fixed sentence, and the
 * analysis keeps no cause, so no stack trace, path, owner, mode or book data reaches the console.
 */
public class StartupFailureAnalyzer implements FailureAnalyzer {

    /** The one action text, identical for every refusal. */
    public static final String ACTION = "Fix the data location or the settings named by the reason, then start again.";

    private static final List<String> POLICY_REASONS = List.of(
            StartupPolicy.REASON_ADDRESS, StartupPolicy.REASON_URL, StartupPolicy.REASON_PARAMETERS);

    @Override
    public FailureAnalysis analyze(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof DataDirectoryGuard.Refusal refusal) {
                return new FailureAnalysis(refusal.reason().text() + ".", ACTION, null);
            }
            if (current instanceof IllegalStateException && POLICY_REASONS.contains(current.getMessage())) {
                return new FailureAnalysis(current.getMessage(), ACTION, null);
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return null;
    }
}
