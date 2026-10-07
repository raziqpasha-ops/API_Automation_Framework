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
 */
public class SpecFactory {

    // We keep the request spec in a variable so it is built only on first use.
    // This is lazy initialization — we do not pay the cost until it is needed.
    private static RequestSpecification requestSpec;

    // Private constructor: utility class, no objects should be created.
    private SpecFactory() { }

    /**
     * Returns a ready-to-use RequestSpecification with base URI, content type
     * and request/response logging switched on.
     */
    public static RequestSpecification getRequestSpec() {

        // Build only once; every later call re-uses the same object (thread-safe enough
        // for our sequential TestNG run and it saves setup time).
        if (requestSpec == null) {

            // RequestSpecBuilder is the fluent builder for request specifications.
            // We chain settings one after another and finish with .build().
            requestSpec = new RequestSpecBuilder()
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
                    .build();
        }
        return requestSpec;
    }

    /**
     * Returns a ResponseSpecification that every JSON success response must satisfy.
     * We check the status code 200 and that the body is JSON, in ONE place.
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
