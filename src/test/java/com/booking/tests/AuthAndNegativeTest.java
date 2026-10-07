package com.booking.tests;

// JAVA "package" keyword: declares this class lives in com.booking.tests.
// Returns nothing; it is a compile-time declaration matching the folder path.

import com.booking.constants.Endpoints;
// Our constants class holding every API path (AUTH, BOOKING, BOOKING_BY_ID, PING).
// Using constants instead of raw strings means a path change is fixed in ONE
// place, and a typo becomes a COMPILE error instead of a runtime 404.

import com.booking.pojo.Booking;
// The booking POJO — used here to build payloads for the negative-auth test.

import com.booking.services.AuthService;
// Service keyword class for login. createToken() RETURN TYPE: String.
// Used for both the positive auth test and cleanup after negative tests.

import com.booking.services.BookingService;
// Service keyword class for booking operations — reused here to create real
// bookings that the negative scenarios then try to attack/modify.

import com.booking.spec.SpecFactory;
// Our reusable RequestSpecification factory. getRequestSpec() RETURN TYPE:
// io.restassured.specification.RequestSpecification — the pre-built request
// template (base URI + JSON content type + logging filters) shared by all calls.

import com.booking.testdata.BookingTestDataFactory;
// Random data factory. buildStandardBooking() RETURN TYPE: Booking.

import io.qameta.allure.Description;
// Allure annotation — attaches the long description text to the report page.

import io.qameta.allure.Epic;
// Allure top-level grouping for the report tree.

import io.qameta.allure.Feature;
// Allure second-level grouping: "Auth & Negative Scenarios".

import io.qameta.allure.Story;
// Allure leaf grouping — the story each negative test belongs to.

import io.restassured.RestAssured;
// The RestAssured ENTRY class. Its static given() method RETURN TYPE is
// io.restassured.specification.RequestSpecification — the start of every
// BDD chain: given() -> when() -> then(). Everything begins from this class.

import com.booking.assertions.AssertionActions;
// Our REUSABLE assertion keyword library — replaces direct TestNG Assert calls.
// Every method RETURN TYPE: void, each one logs what it checked BEFORE
// delegating to TestNG. One place to evolve assertion behaviour for the
// whole framework; tests just call readable keywords.

import org.testng.annotations.Test;
// TestNG's test marker. RETURN/PRODUCES: the method becomes a runnable test.

/**
 * AuthAndNegativeTest proves the SECURITY and ERROR-HANDLING behaviour of the API.
 *
 * Interview explanation: A framework is not complete with happy paths alone.
 * Interviewers specifically look for negative scenarios because that is where real
 * production bugs hide. Here we verify:
 *   - write operations WITHOUT a token are rejected (403),
 *   - unknown booking id returns 404,
 *   - the health check endpoint is up.
 */
@Epic("Restful Booker")
@Feature("Auth & Negative Scenarios")
public class AuthAndNegativeTest {

    @Test(description = "Auth token creation works with valid credentials")
    // @Test RETURN/PRODUCES: registers this method with the TestNG runner and
    // shows the description string in surefire and Allure reports.

    @Description("POST /auth with admin credentials should return a non-empty token")
    @Story("Positive auth")
    public void createTokenSuccessfully() {

        // AuthService.createToken() RETURN TYPE: java.lang.String.
        // Internally: builds AuthRequest POJO from config -> RestAssured POST
        // /auth -> .as(AuthResponse.class) deserialization -> returns the token.
        // The service keyword handles everything; we only assert the outcome.
        String token = AuthService.createToken();

        // assertNotNull RETURN: void; THROWS AssertionError if token is null.
        // A null token would mean the response had no "token" field at all.
        AssertionActions.assertNotNull(token, "Token should not be null");

        // assertFalse(condition) RETURN: void; throws if condition is true.
        // isEmpty() is a JAVA String method RETURN TYPE: boolean — true when the
        // string has zero characters. Both null AND empty are auth failures.
        AssertionActions.assertFalse(token.isEmpty(), "Token should not be empty");
    }

    @Test(description = "PUT without token is rejected with 403")
    @Description("Security check: updating a booking without auth must fail with 403 Forbidden")
    @Story("Negative auth")
    public void updateWithoutTokenIsForbidden() {

        // buildStandardBooking() RETURN TYPE: Booking — random valid payload.
        Booking booking = BookingTestDataFactory.buildStandardBooking();

        // createBooking(Booking) RETURN TYPE: BookingResponse wrapper POJO.
        // We create a REAL booking first so the unauthorized PUT attempt targets
        // a genuinely existing resource — that makes the 403 assertion meaningful.
        int id = BookingService.createBooking(booking).getBookingid();
        // ^ chained call: getBookingid() RETURN TYPE: int — read straight off the
        // deserialized wrapper in one line, no intermediate variable needed.

        // Deliberately call PUT with NO .cookie(...) — simulating an attacker or
        // a client that forgot to log in. given() RETURN TYPE:
        // RequestSpecification; we then chain .pathParam (RETURN: same spec,
        // fluent builder) and .body (RETURN: same spec — every builder method
        // hands back the specification so calls chain indefinitely).
        Booking replacement = BookingTestDataFactory.buildStandardBooking();

        // THE BDD CHAIN, KEYWORD BY KEYWORD — this is the exact flow every
        // RestAssured call follows, and the #1 thing interviewers ask about.
        // BDD = Behaviour Driven Development style: the code reads like a
        // sentence — "GIVEN these conditions, WHEN I send this request,
        // THEN I can check the response." Each keyword below is explained
        // line by line with its RETURN TYPE so the chain is never a mystery.

        // .given()  — THE START OF THE CHAIN. It opens the "request setup"
        //   section where we describe everything the request NEEDS: base URL,
        //   headers, path params, body, cookies. Think of it as saying
        //   "GIVEN a request with these details...". It is a STATIC method
        //   on the RestAssured entry class, so no object creation is needed.

        //   RETURN TYPE: io.restassured.specification.RequestSpecification —
        //   an "interface" (a Java contract object) that COLLECTS all the
        //   request details you add. Nothing is sent to the server yet —
        //   given() only opens the collector.

        int status = RestAssured
                .given()                                    // RETURN: RequestSpecification — opens the request-setup section; collects details, sends nothing yet

        // .spec(...)  — "INJECT THE SHARED BLUEPRINT". Our SpecFactory already
        //   built a ready-made request containing the base URL, JSON content
        //   type, and logging filters. Instead of repeating those settings in
        //   every test, .spec() copies them all in with ONE line.
        //   This is the framework's biggest reuse point: every test uses it.

        //   RETURN TYPE: RequestSpecification (the SAME object, "this") —
        //   every setup method hands back itself so the next .method() can
        //   chain immediately. This is called the FLUENT BUILDER pattern:
        //   the return value of one call is the receiver of the next.

                    .spec(SpecFactory.getRequestSpec())     // RETURN: RequestSpecification (this, chained) — copies baseUri + JSON + logging from our shared blueprint

        // .pathParam("id", id)  — FILL A URL PLACEHOLDER. Our endpoint constant
        //   is "/booking/{id}" and {id} is a placeholder written in braces.
        //   This method REPLACES {id} with the real value at runtime, so the
        //   final URL becomes /booking/543 (whatever "id" holds).
        //   Path params identify ONE resource — here, the booking to attack.

        //   RETURN TYPE: RequestSpecification (this) — again the same object,
        //   so the chain continues. Because the URL is built from a constant
        //   + a parameter, there are zero hand-built URL strings anywhere.

                    .pathParam("id", id)                    // RETURN: RequestSpecification (this) — replaces {id} in /booking/{id} with the real booking number

        // .body(replacement)  — ATTACH THE REQUEST PAYLOAD. We pass our Booking
        //   POJO (a plain Java object) — NOT a JSON string. RestAssured sees
        //   a POJO and calls Jackson behind the scenes to SERIALIZE it into
        //   the JSON request body automatically. This is SERIALIZATION.

        //   Benefit: compile-time safety. If a field name is wrong in the
        //   POJO, the COMPILER catches it — a hand-typed JSON string would
        //   fail silently at runtime with confusing server errors instead.

        //   RETURN TYPE: RequestSpecification (this) — same collector object,
        //   now holding the payload, ready for the next link in the chain.

                    .body(replacement)                      // RETURN: RequestSpecification (this) — serializes the Booking POJO into a JSON request body via Jackson

        // .when()  — THE TURNING POINT OF THE SENTENCE. In BDD grammar,
        //   "GIVEN the setup ... WHEN I perform the action ...". This method
        //   does not change any request data — it simply CLOSES the setup
        //   section and OPENS the "action" section where the HTTP verb
        //   (get/post/put/patch/delete) is chosen. It exists purely to make
        //   the code read like the Given-When-Then sentence structure.

        //   RETURN TYPE: still RequestSpecification (this) — same object with
        //   a flag flipped internally, so the next call must be an HTTP verb.

                .when()                                     // RETURN: RequestSpecification (this) — closes the setup section; next call must be an HTTP verb

        // .put(Endpoints.BOOKING_BY_ID)  — FIRE THE ACTUAL HTTP REQUEST. This
        //   is the moment network traffic happens: RestAssured combines the
        //   spec's base URL + the path param + the body, sends the real PUT
        //   to the server, waits for the reply, and wraps the whole reply
        //   (status line, headers, body) into one Response object.
        //   Here it deliberately has NO cookie — testing that the API blocks
        //   unauthorized updates (the 403 security contract).

        //   RETURN TYPE: io.restassured.response.Response — a NEW object
        //   representing everything the server sent back. This is the first
        //   method in the chain with a DIFFERENT return type, because the
        //   "request phase" has ended and the "response phase" has begun.

                    .put(Endpoints.BOOKING_BY_ID)           // RETURN: Response — the HTTP PUT actually fires here; the server's full reply is wrapped in this object

        // .then()  — OPEN THE VALIDATION SECTION. "WHEN I did the action ...
        //   THEN I can check things." The Response object is now handed over
        //   to its "validatable" twin, which offers assertion methods like
        //   statusCode(200), body("firstname", ...), time(lessThan(...)).
        //   Same pattern as given(): a section marker that changes what you
        //   may call next, without touching any data.

        //   RETURN TYPE: io.restassured.response.ValidatableResponse — the
        //   response wearing a "checker" hat: from here on, every method
        //   either asserts something or extracts something.

                .then()                                     // RETURN: ValidatableResponse — switches into the validation section; all assert/extract methods live here

        // .extract()  — SWITCH FROM "CHECK" TO "TAKE". By default then() methods
        //   ASSERT (pass or fail the test). .extract() flips the meaning: the
        //   next method is used to PULL A VALUE out of the response instead of
        //   checking it. We want the status code as a NUMBER to store in our
        //   "status" variable, not just to assert it blindly.

        //   RETURN TYPE: io.restassured.response.ExtractableResponse — a view
        //   of the same response whose methods RETURN values (statusCode()
        //   -> int, path("x") -> value, as(Class) -> POJO) instead of asserting.

                    .extract()                              // RETURN: ExtractableResponse — flips the next call from "assert this" to "give me this value"

        // .statusCode()  — THE FINAL LINK: hand back the HTTP status number.
        //   Because extract() was called first, this RETURNS the int instead
        //   of asserting it. The value (expected: 403) lands in our variable,
        //   and the very next line asserts it with our reusable keyword.
        //   So the full chain reads: given setup -> when PUT fired ->
        //   then validated -> extracted -> asserted. One readable sentence.

        //   RETURN TYPE: int — the raw HTTP status code (403 expected here),
        //   which the assertEquals keyword below turns into a pass/fail.

                    .statusCode();                          // RETURN: int — the HTTP status code as a number, stored in "status" for the assertion below

        // assertEquals(int, int) RETURN: void; throws if the API FAILED to block
        // us. 403 is the security contract: no token, no write access.
        AssertionActions.assertEquals(status, 403,
                "PUT without a token must be rejected with 403 Forbidden");

        // Clean up the booking created for this test. deleteBooking RETURN TYPE:
        // Response; createToken() RETURN TYPE: String feeds the required cookie.
        // Leaving no test data behind is professional test-environment hygiene.
        BookingService.deleteBooking(id, AuthService.createToken());
    }

    @Test(description = "GET unknown booking id returns 404")
    @Description("GET /booking/999999999 should return 404 Not Found")
    @Story("Negative read")
    public void getUnknownBookingReturns404() {

        // getBookingRaw(int) RETURN TYPE: io.restassured.response.Response.
        // 999999999 is effectively guaranteed not to exist. We use the RAW
        // service method because a 404 body has nothing to deserialize — the
        // assertion here is purely on the status code. RetryExecutor inside
        // protects even this negative test from rate-limit blips.
        int status = BookingService.getBookingRaw(999999999).getStatusCode();
        // ^ getStatusCode() RETURN TYPE: int — chained off the raw Response.

        // assertEquals RETURN: void. The API's error contract for unknown ids
        // is 404; any other code (200, 500) would be a bug worth filing.
        AssertionActions.assertEquals(status, 404, "Unknown booking id should return 404");
    }

    @Test(description = "Health check endpoint is up")
    @Description("GET /ping should return 201 Created proving the service is healthy")
    @Story("Health check")
    public void healthCheckIsUp() {

        // THE SAME BDD CHAIN, SHORT VERSION — for a simple GET with no body
        // and no path params. Still Given -> When -> Then, just fewer links:
        // no .pathParam needed (GET /ping has no {placeholder}), no .body
        // needed (a GET carries no payload). Every keyword's job and its
        // RETURN TYPE is identical to the long chain explained above, so
        // read that one for the full keyword-by-keyword walkthrough.

        int status = RestAssured
                .given()                                    // RETURN: RequestSpecification — opens the request-setup section; collects details, sends nothing yet
                    .spec(SpecFactory.getRequestSpec())     // RETURN: RequestSpecification (this) — one line injects baseUri + JSON + logging from the shared blueprint; the framework's biggest reuse point
                .when()                                     // RETURN: RequestSpecification (this) — closes the setup section; the next call must be an HTTP verb
                    .get(Endpoints.PING)                    // RETURN: Response — the HTTP GET actually fires here; the server's full reply is wrapped in this object
                .then()                                     // RETURN: ValidatableResponse — switches into the validation section; all assert/extract methods live here
                    .extract()                              // RETURN: ExtractableResponse — flips the next call from "assert this" to "give me this value"
                    .statusCode();                          // RETURN: int — the HTTP status code as a number (201 expected for a healthy /ping)

        // The Ping endpoint returns 201 with body "Created" when the API is
        // alive (unusual — most health checks use 200 — but that is what THIS
        // API's documentation defines, and the test pins that contract).
        // CI pipelines run this test FIRST as a smoke check: if it fails, there
        // is no point running the rest of the suite against a dead server.
        AssertionActions.assertEquals(status, 201, "Health check /ping should return 201");
    }
}
