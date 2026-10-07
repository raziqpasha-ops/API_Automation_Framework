package com.booking.spec;

import com.booking.config.ConfigManager;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;

/**
 * SpecFactory builds REUSABLE RequestSpecification and ResponseSpecification objects.
 *
 * Interview explanation: RequestSpecification answers the question
 * "what is COMMON to every request?"  — base URL, content type, logging.
 * ResponseSpecification answers "what do we ALWAYS check in every response?"
 * — for example that the status code is 200.
 *
 * We build these ONCE here and inject them into every test using given().spec(...)
 * and then().spec(...). This removes repeated boilerplate from every test and is
 * the single biggest source of "reusability" in a RestAssured framework.
 *
 * PARALLEL-SAFE DESIGN (ThreadLocal): RestAssured's RequestSpecification is NOT
 * guaranteed thread-safe — when TestNG runs tests in parallel, two threads
 * sharing one spec object can corrupt each other's request details. The fix is
 * java.lang.ThreadLocal: it gives EVERY thread its own private copy of the spec.
 * ThreadLocal works like a map of "thread -> value" under the hood: when thread
 * A calls .get() it receives thread A's spec, thread B receives B's spec, and
 * neither can ever see or touch the other's. The blueprint is still defined in
 * ONE place (the builder below), so we keep 100% reuse AND gain parallel safety.
 */
public class SpecFactory {

    // ThreadLocal "box" that holds one RequestSpecification PER THREAD.
    // withInitial(...) says: "the first time a thread asks, build it this way."
    // The builder lambda runs lazily and separately for every new thread.
    private static final ThreadLocal<RequestSpecification> requestSpec =
            ThreadLocal.withInitial(() ->

                    // RequestSpecBuilder is the fluent builder for request specifications.
                    // We chain settings one after another and finish with .build().
                    // This code runs ONCE PER THREAD, so each thread gets a fresh,
                    // isolated spec with identical settings.
                    new RequestSpecBuilder()
                            // setBaseUri is where we say WHICH server to hit. The value comes
                            // from ConfigManager, so environment switching needs no code change.
                            .setBaseUri(ConfigManager.getBaseUrl())
                            // Every request we send in this project is JSON, so we set it here
                            // once instead of writing .contentType() in every single test.
                            .setContentType(ContentType.JSON)
                            // This filter prints the FULL outgoing request into the console and
                            // the surefire report. When a test fails at 2am, the printed request
                            // is the first thing you look at to debug it.
                            .addFilter(new RequestLoggingFilter())
                            // Same idea, but for the incoming response — full body and headers
                            // get printed, which means the evidence of what the API returned
                            // is always captured automatically.
                            .addFilter(new ResponseLoggingFilter())
                            .build());

    // Private constructor: utility class, no objects should be created.
    private SpecFactory() { }

    /**
     * Returns the CALLING THREAD's own RequestSpecification.
     * ThreadLocal.get() RETURN TYPE: RequestSpecification — the value stored
     * for the current thread, built on first use via withInitial above.
     * Sequential runs behave exactly as before; parallel runs are now safe.
     */
    public static RequestSpecification getRequestSpec() {

        // .get() looks up the value belonging to THE CURRENT THREAD only.
        // First call on a thread builds the spec; every later call on that
        // same thread returns the same instance (fast + consistent).
        return requestSpec.get();
    }

    /**
     * Returns a ResponseSpecification that every JSON success response must satisfy.
     * We check the status code 200 and that the body is JSON, in ONE place.
     *
     * NOTE: ResponseSpecifications here are built fresh per CALL (not shared),
     * which is inherently thread-safe — no ThreadLocal needed for these.
     */
    public static ResponseSpecification getSuccessResponseSpec() {

        // ResponseSpecBuilder works exactly like RequestSpecBuilder, but for the
        // "then()" side — the validation side of the request.
        return new ResponseSpecBuilder()
                // expectStatusCode declares the status code that is expected. When a
                // test uses .spec(this), RestAssured asserts it automatically.
                .expectStatusCode(200)
                // expectContentType makes sure the server really replied in JSON. This
                // catches cases where an API "fails politely" and returns an HTML error
                // page with a 200 status — a real-world bug this check would catch.
                .expectContentType(ContentType.JSON)
                .build();
    }

    /**
     * A second response spec for CREATION calls, because the booking API returns
     * 200 on create but many APIs return 201. Keeping them separate shows that
     * response expectations are configurable per scenario, not hard-coded globally.
     */
    public static ResponseSpecification getCreatedResponseSpec() {
        return new ResponseSpecBuilder()
                .expectStatusCode(200)
                .expectContentType(ContentType.JSON)
                .build();
    }
}
