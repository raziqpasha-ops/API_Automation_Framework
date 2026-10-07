package com.booking.tests;

// "package" is a JAVA keyword that declares which folder this class lives in.
// It returns nothing — it is a declaration, not an executable statement.
// Every Java file starts with exactly one package statement, and the package
// path must match the folder path (com/booking/tests) or compilation fails.
// Packages prevent name collisions: two classes named "Test" can coexist in
// different packages. Here we keep all test classes under com.booking.tests.

import com.booking.pojo.Booking;
// "import" is a JAVA keyword that makes another class visible by its short name.
// Booking is our POJO — the class representing the booking JSON payload.
// Without this import we would have to write the full name
// "com.booking.pojo.Booking" everywhere, which is unreadable.
// Imports return nothing; they are resolved at COMPILE time, not runtime.

import com.booking.pojo.BookingResponse;
// BookingResponse is the WRAPPER POJO for the create-booking response:
// { "bookingid": 123, "booking": {...} }. It exists because the create response
// is not a plain booking — it bundles the server-generated id with the booking.
// We import it here so the E2E test can capture and carry the generated id.

import com.booking.services.AuthService;
// AuthService is our reusable service-layer keyword class for authentication.
// It hides the POST /auth call, the request POJO and the deserialization behind
// one static method: AuthService.createToken() which RETURNS a String token.
// Tests call it in one line — that is the whole point of the service layer.

import com.booking.services.BookingService;
// BookingService is the reusable keyword class for every booking operation.
// Each method maps 1:1 to an endpoint (getBooking, createBooking, updateBooking,
// partialUpdateBooking, deleteBooking) and returns either a typed POJO or a
// raw Response — the E2E test consumes both styles, as you will see below.

import com.booking.testdata.BookingTestDataFactory;
// TestDataFactory builds fresh, RANDOM test data for every run.
// It returns a fully populated Booking POJO from buildStandardBooking(), so no
// test ever hard-codes "Jim Brown" again. Random data also prevents flaky
// collisions between parallel runs or leftover records on the shared server.

import io.qameta.allure.Description;
// Allure annotation. It attaches a long human-readable description to the test.
// RETURN/PRODUCES: text inside the Allure HTML report under the test's page.
// It has zero effect on execution — purely report documentation.

import io.qameta.allure.Epic;
// Allure annotation that groups tests under a top-level business "Epic".
// PRODUCES: the left-side tree in the Allure report (Epic -> Feature -> Story).
// Interview line: "Allure annotations give the report a BDD-style hierarchy."

import io.qameta.allure.Feature;
// Allure annotation, second level of the report hierarchy, under the Epic.
// PRODUCES: a "Feature" grouping in the report, e.g. "Booking End-to-End Flow".

import io.qameta.allure.Story;
// Allure annotation, third level — the user story inside the feature.
// PRODUCES: the deepest grouping in the Allure report tree.

import com.booking.assertions.AssertionActions;
// Our REUSABLE assertion keyword library — replaces direct TestNG Assert calls.
// Every method RETURN TYPE: void (except the composite ones), and each keyword
// LOGS the comparison before delegating to TestNG's proven assert engine.
// Interview line: "assertions are reusable framework keywords, not raw calls —
// one place to change behaviour, and every check is self-documenting in logs."

import org.testng.annotations.Test;
// TestNG's @Test annotation marks a method as an executable test case.
// RETURN/PRODUCES: the method becomes discoverable by the TestNG runner and
// appears in testng.xml execution, surefire reports and Allure. Without this
// annotation the method is just dead code that nothing will ever run.

/**
 * E2E_BookingFlowTest is the END-TO-END scenario — the full life cycle of a booking:
 *      create -> read -> full update -> partial update -> delete -> verify deleted.
 *
 * Interview explanation: This is what interviewers mean by "end to end" — we do
 * NOT just test one endpoint in isolation. The output of one step (the bookingid
 * from CREATE) becomes the input of the next steps (read/update/delete). The state
 * flows through the whole test, proving the API works as one connected system.
 */
@Epic("Restful Booker")                 // Allure: top-level group for all tests
@Feature("Booking End-to-End Flow")     // Allure: business feature under the Epic
public class E2E_BookingFlowTest {

    // "public class" is a JAVA declaration. The "public" modifier returns no
    // value but controls VISIBILITY: TestNG (running in a different package)
    // must be able to see and instantiate this class, hence public.

    @Test(description = "Full booking life cycle: create, read, update, patch, delete")
    // @Test RETURNS a "processed" registration — TestNG records this method as a
    // test with the given description string shown in reports. The description
    // attribute is what appears as the test name in surefire and Allure.

    @Description("End-to-end: create a booking, verify it, fully update it, partially update it, delete it and verify it is gone.")
    // Allure's longer narrative for the report page. Returns nothing at runtime.

    @Story("Happy path life cycle")
    // Allure story label — the leaf of the report hierarchy for this test.

    public void fullBookingLifeCycle() {

        // "public void" — JAVA: public = callable from anywhere (TestNG needs
        // this), void = the test method itself returns nothing to the caller.
        // TestNG calls it, assertions inside decide pass/fail, not a return value.

        // ---------- STEP 1: GET THE AUTH TOKEN ----------
        // AuthService.createToken() RETURN TYPE: java.lang.String.
        // Internally it performs POST /auth, deserializes the JSON response
        // { "token": "..." } into an AuthResponse POJO using RestAssured's
        // .as(AuthResponse.class), and returns only the token string.
        // AssertionActions.assertNotNull RETURN: void; it THROWS AssertionError if null.
        String token = AuthService.createToken();

        // assertTrue(condition, message) RETURN: void, throws AssertionError on
        // failure. We guard the token here because every later step depends on
        // it — failing fast with a clear message beats a confusing 403 later.
        AssertionActions.assertNotNull(token, "Token should not be null — auth API failed");

        // ---------- STEP 2: CREATE A BOOKING ----------
        // BookingTestDataFactory.buildStandardBooking() RETURN TYPE: com.booking.pojo.Booking.
        // It builds a random, complete booking object in one line. Because the
        // data is random each run, two runs never collide on the shared server.
        Booking newBooking = BookingTestDataFactory.buildStandardBooking();

        // BookingService.createBooking(Booking) RETURN TYPE: BookingResponse POJO.
        // Inside, RestAssured's .body(booking) SERIALIZES the POJO to JSON via
        // Jackson, POST /booking runs, and .as(BookingResponse.class)
        // DESERIALIZES the response back into the typed wrapper POJO.
        // This is the single most important line of the whole framework.
        BookingResponse createResponse = BookingService.createBooking(newBooking);

        // getBookingid() RETURN TYPE: int (primitive). It reads the id field that
        // the SERVER generated. This single int is the state that carries the
        // entire end-to-end flow forward into every following step.
        int bookingId = createResponse.getBookingid();

        // assertTrue RETURN: void; passes only if the server really generated an id.
        AssertionActions.assertTrue(bookingId > 0, "Created booking id should be positive");

        // getBooking() RETURN TYPE: Booking (the nested POJO inside the wrapper).
        // The server echoes our payload back; we keep it to compare field by field.
        Booking returnedBooking = createResponse.getBooking();

        // getFirstname() RETURN TYPE: java.lang.String.
        // assertEquals(actual, expected, message) RETURN: void; THROWS
        // AssertionError with our message if the two Strings are not equal.
        // We compare what we SENT vs what the server ECHOED — the create contract.
        AssertionActions.assertEquals(returnedBooking.getFirstname(), newBooking.getFirstname(),
                "Firstname echoed back should match what we sent");

        // getLastname() RETURN TYPE: String. Same echo contract, different field.
        AssertionActions.assertEquals(returnedBooking.getLastname(), newBooking.getLastname(),
                "Lastname echoed back should match");

        // getTotalprice() RETURN TYPE: java.lang.Integer (wrapper). TestNG's
        // assertEquals uses .equals() for objects, which compares the VALUE
        // 180 vs 180 correctly — that is why wrappers are safe here.
        AssertionActions.assertEquals(returnedBooking.getTotalprice(), newBooking.getTotalprice(),
                "Price echoed back should match");

        // getBookingdates() RETURN TYPE: BookingDates (nested POJO), and then
        // getCheckin() RETURN TYPE: String. Chained getters walk the object graph
        // Jackson built during deserialization — no JSON parsing anywhere.
        AssertionActions.assertEquals(returnedBooking.getBookingdates().getCheckin(),
                newBooking.getBookingdates().getCheckin(), "Checkin date should match");

        // ---------- STEP 3: READ THE BOOKING BACK (GET) ----------
        // BookingService.getBooking(int) RETURN TYPE: Booking POJO.
        // Inside: GET /booking/{id} with .pathParam("id", id), then
        // .as(Booking.class) deserializes the response. This proves the data
        // really PERSISTED on the server, not just that the echo was pretty.
        Booking fetchedBooking = BookingService.getBooking(bookingId);

        // Compare persisted state against what we originally sent.
        AssertionActions.assertEquals(fetchedBooking.getFirstname(), newBooking.getFirstname(),
                "GET should return the same firstname we created");

        // ---------- STEP 4: FULL UPDATE (PUT) ----------
        // Fresh random payload — PUT replaces ALL fields, so we build a complete
        // Booking again, then override the names to known values we can assert.
        Booking updatedBooking = BookingTestDataFactory.buildStandardBooking();
        updatedBooking.setFirstname("James");
        updatedBooking.setLastname("Brown");

        // BookingService.updateBooking(int, Booking, String) RETURN TYPE:
        // io.restassured.response.Response (the RAW response object).
        // Inside: PUT /booking/{id} with .cookie("token", token) for authorization.
        // We keep it raw here because the next line converts it to a POJO.
        var putResponse = BookingService.updateBooking(bookingId, updatedBooking, token);

        // Response.as(Class) RETURN TYPE: whatever class you pass — here Booking.
        // This is RestAssured's deserialization on demand: it takes the response
        // body JSON and maps it into a Java object using Jackson internally.
        Booking afterPut = putResponse.as(Booking.class);

        // getFirstname() RETURN TYPE: String — assert the PUT actually replaced it.
        AssertionActions.assertEquals(afterPut.getFirstname(), "James",
                "After PUT the firstname should be the updated one");

        // Integer comparison again — the price must equal the new random value.
        AssertionActions.assertEquals(afterPut.getTotalprice(), updatedBooking.getTotalprice(),
                "After PUT the price should be the updated one");

        // ---------- STEP 5: PARTIAL UPDATE (PATCH) ----------
        // buildPartialUpdateNames() RETURN TYPE: Booking, but with ONLY the two
        // name fields set — every other field is null. Thanks to
        // @JsonInclude(NON_NULL) on the POJO, those nulls are SKIPPED during
        // serialization, so the PATCH body truly contains only what we send.
        Booking partialUpdate = BookingTestDataFactory.buildPartialUpdateNames();

        // partialUpdateBooking RETURN TYPE: Response (raw). PATCH verb + token
        // cookie. The API merges the partial payload into the stored booking.
        var patchResponse = BookingService.partialUpdateBooking(bookingId, partialUpdate, token);

        // Deserialize the PATCH response (which contains the FULL updated booking)
        // into a Booking POJO. Deserializing the response instead of firing an
        // extra GET makes the test faster and immune to rate-limit blips.
        Booking afterPatch = patchResponse.as(Booking.class);

        // PATCH contract #1: the fields we sent WERE changed.
        AssertionActions.assertEquals(afterPatch.getFirstname(), "James",
                "PATCH should update the firstname");
        AssertionActions.assertEquals(afterPatch.getLastname(), "Brown",
                "PATCH should update the lastname");

        // PATCH contract #2 (the critical one): fields we did NOT send stayed
        // UNTOUCHED. If this price assertion fails, the API wrongly wiped data.
        AssertionActions.assertEquals(afterPatch.getTotalprice(), updatedBooking.getTotalprice(),
                "PATCH must NOT change fields that were not sent (price stays same)");

        // ---------- STEP 6: DELETE THE BOOKING ----------
        // deleteBooking(int, String) RETURN TYPE: Response (raw). DELETE verb +
        // token cookie. Per the API docs, success returns HTTP 201.
        BookingService.deleteBooking(bookingId, token);

        // ---------- STEP 7: VERIFY IT IS REALLY GONE ----------
        // getBookingRaw(int) RETURN TYPE: Response (raw) — we deliberately do NOT
        // deserialize here, because a deleted booking has no body to map; we only
        // need the status code. Retried automatically on 429/5xx by RetryExecutor.
        int statusAfterDelete = BookingService.getBookingRaw(bookingId).getStatusCode();

        // getStatusCode() RETURN TYPE: int (primitive) — RestAssured's raw status.
        // assertEquals asserts 404, proving the delete PERSISTED on the server.
        AssertionActions.assertEquals(statusAfterDelete, 404,
                "After DELETE, getting the booking should return 404 Not Found");
    }
}
