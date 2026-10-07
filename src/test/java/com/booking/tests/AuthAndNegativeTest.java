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
        int status = RestAssured
                .given()                                    // RETURN: RequestSpecification
                    .spec(SpecFactory.getRequestSpec())     // RETURN: RequestSpecification (this, chained)
                    .pathParam("id", id)                    // RETURN: RequestSpecification (this)
                    .body(replacement)                      // RETURN: RequestSpecification (this)
                .when()                                     // RETURN: RequestSpecification (marks the verb section)
                    .put(Endpoints.BOOKING_BY_ID)           // RETURN: io.restassured.response.Response — fires HTTP PUT
                .then()                                     // RETURN: io.restassured.response.ValidatableResponse
                    .extract()                              // RETURN: io.restassured.response.ExtractableResponse
                    .statusCode();                          // RETURN: int — the raw HTTP status code

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

        // Full BDD chain with RestAssured's predefined keywords, each annotated
        // with its RETURN TYPE — this is the exact chain interviewers ask about:
        int status = RestAssured
                .given()                                    // RETURN: RequestSpecification
                    .spec(SpecFactory.getRequestSpec())     // RETURN: RequestSpecification (injects baseUri+headers+logging)
                .when()                                     // RETURN: RequestSpecification (switch to action section)
                    .get(Endpoints.PING)                    // RETURN: Response — executes HTTP GET /ping
                .then()                                     // RETURN: ValidatableResponse — validation section
                    .extract()                              // RETURN: ExtractableResponse — allows pulling values out
                    .statusCode();                          // RETURN: int — the HTTP status (201 expected here)

        // The Ping endpoint returns 201 with body "Created" when the API is
        // alive (unusual — most health checks use 200 — but that is what THIS
        // API's documentation defines, and the test pins that contract).
        // CI pipelines run this test FIRST as a smoke check: if it fails, there
        // is no point running the rest of the suite against a dead server.
        AssertionActions.assertEquals(status, 201, "Health check /ping should return 201");
    }
}
