package com.booking.utils;

import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

/**
 * RetryExecutor solves a real-world problem: FLAKY API calls.
 *
 * Interview explanation: Hosted test environments (like restful-booker on Heroku)
 * sometimes throw temporary errors — rate limits, network blips, 503s. These are
 * NOT product bugs; rerunning the same call a second later succeeds. If we don't
 * handle this, CI shows red builds that nobody can reproduce — the #1 trust
 * killer for automation teams.
 *
 * The solution is a small reusable "retry wrapper": call the API, and if the
 * response is a temporary failure, wait and try again up to N times.
 *
 * Design choices interviewers like:
 *   - Only TEMPORARY failures are retried (429, 5xx). A 404 or 403 is a real
 *     answer from the API — retrying would hide a genuine bug and waste time.
 *   - A small delay between attempts gives the server time to recover.
 *   - It is a static utility — any service method can wrap any call in one line.
 */
public class RetryExecutor {

    // Logger for retry activity. Retries are logged at WARN — they are not
    // errors yet, but they ARE a signal the environment is unstable, and WARN
    // lines survive in the log file for later flakiness analysis.
    private static final Logger log = LoggerManager.get(RetryExecutor.class);

    // How many total attempts we allow (1 first try + 2 retries).
    private static final int MAX_ATTEMPTS = 3;

    // Milliseconds to wait between attempts. Short enough not to slow the suite,
    // long enough for a rate limiter window to pass.
    private static final long WAIT_MILLIS = 1500;

    private RetryExecutor() { }

    /**
     * Runs the given API call, retrying only on temporary failures.
     * The call is passed as a lambda/functional interface, so the caller writes
     * the request ONCE and this class decides whether to repeat it.
     */
    public static Response executeWithRetry(java.util.function.Supplier<Response> apiCall) {

        Response response = null;

        // Attempt loop: we try up to MAX_ATTEMPTS times before giving up.
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            // supplier.get() actually fires the HTTP request built by the caller.
            response = apiCall.get();

            // If the status is NOT temporary, return immediately — this is a real,
            // final answer from the server (200, 404, 403...) and needs no retry.
            if (!isTemporaryFailure(response.getStatusCode())) {
                return response;
            }

            // If this was our LAST allowed attempt, return whatever we got and
            // let the caller's assertions fail with the real status code.
            if (attempt == MAX_ATTEMPTS) {
                return response;
            }

            // Still here means: temporary failure + attempts left. Log it, wait,
            // and loop around for another try. Logged at WARN so flakiness is
            // VISIBLE in logs/framework.log, never silently swallowed.
            log.warn("Temporary failure status {} on attempt {} of {}. Retrying in {}ms...",
                    response.getStatusCode(), attempt, MAX_ATTEMPTS, WAIT_MILLIS);
            try {
                Thread.sleep(WAIT_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Retry wait interrupted", e);
            }
        }

        // Java needs a return outside the loop; in practice we never reach here.
        return response;
    }

    /**
     * Decides whether a status code is a TEMPORARY failure worth retrying:
     * 429 (Too Many Requests / rate limited) and all 5xx server errors.
     */
    private static boolean isTemporaryFailure(int statusCode) {
        return statusCode == 429 || statusCode >= 500;
    }
}
