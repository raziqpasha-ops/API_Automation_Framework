package com.booking.listeners;

import com.booking.utils.LoggerManager;
import org.apache.logging.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * TestListener is a TestNG hook class — it "listens" to test events and reacts,
 * now with REAL Log4j2 logging including full failure diagnosis.
 *
 * Interview explanation: TestNG fires events during a run — test started,
 * passed, failed, skipped, and suite finished. An ITestListener implements
 * methods that run at those moments. This gives framework-level CROSS-CUTTING
 * behaviour in ONE place instead of pasting the same code into every test.
 *
 * The failure handler below is the interview highlight: when a test fails we
 * log at ERROR level (1) which test failed, (2) the exception type and message,
 * (3) the test class/method, (4) the total retry context — everything an
 * engineer needs to start debugging WITHOUT opening the stack trace first.
 *
 * The class doesn't need test-side wiring — TestNG wires it via testng.xml.
 */
public class TestListener implements ITestListener {

    // One shared framework logger. All lines from this listener are tagged with
    // "com.booking.listeners.TestListener" in the log output, so they are easy
    // to grep in a big CI log file.
    private static final Logger log = LoggerManager.get(TestListener.class);

    // Runs once BEFORE the suite starts. Good place to log the environment info
    // so every log file is self-describing ("which run, against which env?").
    @Override
    public void onStart(ITestContext context) {
        log.info("===== SUITE STARTED: {} on {} =====",
                context.getSuite().getName(), context.getStartDate());
    }

    // Runs once AFTER everything finished, with the final scoreboard. This is
    // the line the release manager reads in the Jenkins console.
    @Override
    public void onFinish(ITestContext context) {
        log.info("===== SUITE FINISHED: {} =====", context.getSuite().getName());
        log.info("RESULTS -> Passed: {}, Failed: {}, Skipped: {}",
                context.getPassedTests().size(),
                context.getFailedTests().size(),
                context.getSkippedTests().size());
    }

    // Runs the moment a test method STARTS. Useful for a readable CI timeline.
    @Override
    public void onTestStart(ITestResult result) {
        log.info("STARTED: {}.{}",
                result.getTestClass().getName(), result.getMethod().getMethodName());
    }

    // Runs when a test PASSES. Kept at info — it is good news, low detail.
    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("PASSED : {} ({} ms)",
                result.getMethod().getMethodName(), result.getEndMillis() - result.getStartMillis());
    }

    /**
     * Runs when a test FAILS — the most important handler.
     * We log the FULL failure diagnosis at ERROR level: exception type, message,
     * and the class+method where it happened. Because log4j2 also writes to the
     * rolling file (logs/framework.log), the failure evidence survives even if
     * the Jenkins console buffer scrolls away.
     */
    @Override
    public void onTestFailure(ITestResult result) {
        log.error("FAILED : {}", result.getMethod().getMethodName());

        // Throwable is the actual exception/assertion that killed the test.
        Throwable failure = result.getThrowable();
        if (failure != null) {
            log.error("  Exception type : {}", failure.getClass().getName());
            log.error("  Message        : {}", failure.getMessage());
        }

        // Where exactly: full class + method, so anyone can jump to the line.
        log.error("  Location       : {}.{}",
                result.getTestClass().getName(), result.getMethod().getMethodName());

        // Parameters the test ran with (e.g. data-driven inputs) — often the
        // fastest clue, because failures are frequently data-specific.
        Object[] params = result.getParameters();
        if (params != null && params.length > 0) {
            log.error("  Parameters     : {}", (Object) params);
        }

        // Finally log the stack trace itself at ERROR so the complete evidence
        // chain (summary first, deep trace second) is in one place.
        if (failure != null) {
            log.error("  Stack trace:", failure);
        }
    }

    // Runs when a test is SKIPPED (e.g. a dependency failed, or retry gave up).
    // Skips must be VISIBLE — silent skips are lost coverage.
    @Override
    public void onTestSkipped(ITestResult result) {
        log.warn("SKIPPED: {}", result.getMethod().getMethodName());
    }
}
