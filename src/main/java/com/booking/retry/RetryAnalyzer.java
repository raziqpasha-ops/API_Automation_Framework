package com.booking.retry;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * RetryAnalyzer is TestNG's built-in mechanism to RERUN a FAILED test
 * automatically before marking it as truly failed.
 *
 * Interview explanation: There are two kinds of failures, and confusing them is
 * the classic automation anti-pattern:
 *
 *   1. REAL failure      -> the API behaves wrongly. Must be reported.
 *   2. FLAKY failure     -> network blip, server hiccup, rate limit. The SAME
 *                           test passes if you simply run it again.
 *
 * TestNG supports flaky-failure handling through the IRetryAnalyzer interface.
 * It has exactly ONE method: retry(). When a test fails, TestNG asks this class
 * "should we try again?" If retry() returns true, TestNG reruns the WHOLE test
 * method. If it returns false, the failure is final.
 *
 * How the counter works:
 *   - count starts at 0.
 *   - test fails -> retry() is called -> count becomes 1, 1 < MAX -> return true
 *     -> TestNG reruns the test.
 *   - test fails again -> retry() called -> count 2, 2 < 2 is false -> return
 *     false -> TestNG marks the test FAILED for good.
 *
 * FAANG-style points to mention:
 *   - The MAX_RETRY_COUNT is kept small (2). Blindly retrying 10 times hides
 *     real bugs and inflates execution time.
 *   - In advanced setups, retry() checks result.getThrowable() and retries ONLY
 *     network-type exceptions, never assertion failures — because an assertion
 *     failure is a REAL bug, and rerunning it wastes pipeline time.
 *   - TestNG records every rerun in the report, so retry activity is always
 *     visible and auditable — nothing is hidden.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    // How many reruns we allow AFTER the first failure. 2 reruns = 3 total runs max.
    private static final int MAX_RETRY_COUNT = 2;

    // This object's own counter for the CURRENT test. TestNG creates a fresh
    // RetryAnalyzer instance per failing test, so the counter starts at 0 each time.
    private int count = 0;

    /**
     * Called by TestNG every time a test fails.
     * Return true  = "rerun the test".
     * Return false = "the failure is final, mark it failed".
     */
    @Override
    public boolean retry(ITestResult result) {

        // If we still have reruns left, allow one more attempt and count it.
        if (count < MAX_RETRY_COUNT) {
            count++;
            System.out.println("[RETRY-ANALYZER] Test '" + result.getMethod().getMethodName()
                    + "' failed. Rerunning... attempt " + count + " of " + MAX_RETRY_COUNT);
            return true;
        }

        // Retries exhausted. The failure stands and goes into the report as FAILED.
        System.out.println("[RETRY-ANALYZER] Test '" + result.getMethod().getMethodName()
                + "' still failing after " + MAX_RETRY_COUNT + " retries. Marking FAILED.");
        return false;
    }
}
