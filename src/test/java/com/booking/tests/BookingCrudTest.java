package com.booking.tests;

// JAVA "package" keyword: declares the folder for this class. Returns nothing;
// it is a compile-time declaration that must match the physical folder path.

import com.booking.pojo.Booking;
// Our main booking POJO. Used here as both request model (payload we build and
// send) and response model (JSON deserialized back into Java). One class, both
// directions — that reuse is the backbone of the framework's reusability.

import com.booking.pojo.BookingResponse;
// Wrapper POJO for the CREATE response: { "bookingid": X, "booking": {...} }.
// We need the separate class because the create response shape differs from the
// plain booking shape — a very common real-world API pattern.

import com.booking.services.AuthService;
// Service keyword class for login. createToken() RETURN TYPE: String.
// Called once in @BeforeClass; every write operation in this class reuses it.

import com.booking.services.BookingService;
// Service keyword class — one reusable method per booking endpoint. Return types
// vary deliberately: POJOs for typed happy paths, raw Response for status checks.

import com.booking.testdata.BookingTestDataFactory;
// Random test-data builder. buildStandardBooking() RETURN TYPE: Booking;
// buildPartialUpdateNames() RETURN TYPE: Booking with only 2 fields set.

import io.qameta.allure.Description;
// Allure annotation — long description text shown on the test's report page.

import io.qameta.allure.Epic;
// Allure top-level grouping. PRODUCES: the outermost node in the report tree.

import io.qameta.allure.Feature;
// Allure second-level grouping under the Epic for this test class's domain.

import io.qameta.allure.Story;
// Allure leaf-level grouping — the user story this test belongs to.

import com.booking.assertions.AssertionActions;
// Our REUSABLE assertion keyword library (replaces direct TestNG Assert calls).
// Every method RETURN TYPE: void, and each one logs the comparison BEFORE
// delegating to TestNG — so failures are diagnosable from the log file alone.
// TestNG hard assertions. Methods RETURN void but THROW AssertionError on
// failure, stopping the test at the first failed check.

import org.testng.annotations.AfterClass;
// TestNG annotation: method runs ONCE after ALL tests in this class finish.
// RETURN/PRODUCES: guaranteed cleanup slot — even if some tests failed, the
// runner still calls it, which is exactly where teardown belongs.

import org.testng.annotations.BeforeClass;
// TestNG annotation: method runs ONCE before the FIRST test of this class.
// RETURN/PRODUCES: a shared setup slot — perfect for one login + one test
// booking, instead of repeating that in every test method (speed + consistency).

import org.testng.annotations.Test;
// Marks a method as a TestNG test. The "priority" attribute controls ORDER:
// lower number runs first. RETURN/PRODUCES: ordered test registration.

import java.util.List;
// JAVA collections interface. java.util.List RETURN TYPE note: RestAssured's
// jsonPath().getList("bookingid") returns List<Object> but we safely bind it to
// List<Integer> because every bookingid in the JSON is a whole number.
// List gives us isEmpty(), size(), subList() — all used in this class.

/**
 * BookingCrudTest covers each operation SEPARATELY (component-level tests),
 * grouped into one class with a shared setup so we don't log in repeatedly.
 *
 * Interview explanation: Alongside the big end-to-end test, a framework also needs
 * SMALL focused tests — one test, one endpoint, one behaviour. When one of these
 * fails you know EXACTLY which operation broke, whereas the e2e test only tells
 * you "somewhere in the flow". Small tests + e2e test together give complete
 * coverage — that is the professional testing pyramid applied to APIs.
 */
@Epic("Restful Booker")
@Feature("Booking CRUD Operations")
public class BookingCrudTest {

    // ----- SHARED STATE for this class -----
    // "private" JAVA modifier: visible only inside this class — nobody outside
    // can mutate our test subject. String/int are JAVA types: token is the auth
    // cookie value, bookingId is the server-generated id under test.
    private String token;
    private int bookingId;

    @BeforeClass
    // Runs once before any test in this class. RETURN: void; TestNG calls it
    // automatically. If this method throws, ALL tests in the class are skipped —
    // which is correct behaviour: no point testing CRUD without setup.
    public void setUp() {

        // AuthService.createToken() RETURN TYPE: String — the token reused by
        // every write test below. ONE login for the whole class = faster suite.
        token = AuthService.createToken();

        // buildStandardBooking() RETURN TYPE: Booking — random valid data.
        Booking booking = BookingTestDataFactory.buildStandardBooking();

        // createBooking(Booking) RETURN TYPE: BookingResponse (wrapper POJO).
        // POST /booking runs; Jackson deserializes into the typed wrapper.
        BookingResponse response = BookingService.createBooking(booking);

        // getBookingid() RETURN TYPE: int — the test subject's id for this class.
        bookingId = response.getBookingid();
    }

    @Test(priority = 1, description = "GetBookingIds returns a non-empty list of ids")
    // priority = 1 makes this the FIRST executed test of the class. RETURN/PRODUCES:
    // ordered execution; lower priority numbers run earlier.

    @Description("GET /booking should return at least one booking id, each element containing bookingid")
    @Story("Read all bookings")
    public void getBookingIdsReturnsIds() {

        // getFirstNBookingIds(5) RETURN TYPE: java.util.List<Integer>.
        // Inside the service: RestAssured GET + .jsonPath().getList("bookingid")
        // walks the JSON array and extracts each "bookingid" value into a typed
        // Java List. This is JsonPath — RestAssured's built-in JSON query language.
        List<Integer> ids = BookingService.getFirstNBookingIds(5);

        // assertFalse(condition) RETURN: void; THROWS AssertionError if condition
        // is true. The playground API always has bookings, so empty list = failure.
        AssertionActions.assertFalse(ids.isEmpty(), "Booking ids list should not be empty");
    }

    @Test(priority = 2, description = "GetBookingIds filter by firstname returns results")
    @Description("GET /booking?firstname=X should return a list (filter endpoint works)")
    @Story("Filter bookings")
    public void getBookingIdsWithFilter() {

        // getBooking(int) RETURN TYPE: Booking POJO — deserialized GET response.
        // We read OUR OWN created booking's firstname, so the filter is guaranteed
        // to have a match — a self-contained, non-flaky test design.
        Booking created = BookingService.getBooking(bookingId);

        // getFirstname() RETURN TYPE: String — the value we filter the API by.
        // getBookingIdsByFilter(String, String) RETURN TYPE: Response (raw).
        // Inside the service: .queryParam("firstname", value) appends
        // ?firstname=value to the URL, with automatic URL-encoding.
        var response = BookingService.getBookingIdsByFilter("firstname", created.getFirstname());

        // getStatusCode() RETURN TYPE: int — RestAssured's raw HTTP status.
        // 200 means the filter query was accepted and processed by the API.
        AssertionActions.assertEquals(response.getStatusCode(), 200,
                "Filtered GET /booking should return 200");
    }

    @Test(priority = 3, description = "GetBooking returns correct deserialized data")
    @Description("GET /booking/{id} should deserialize into the Booking POJO with correct values")
    @Story("Read one booking")
    public void getBookingReturnsPojo() {

        // getBooking(int) RETURN TYPE: Booking. DESERIALIZATION in action: the
        // JSON response becomes a Java object, and every assertion below reads
        // a typed getter. Any mismatch fails with our clear custom message.
        Booking booking = BookingService.getBooking(bookingId);

        // getFirstname() RETURN TYPE: String.
        // assertNotNull(object) RETURN: void; throws if the object is null.
        // A null here would mean Jackson found no "firstname" in the JSON —
        // i.e. the response shape broke, and we catch it immediately.
        AssertionActions.assertNotNull(booking.getFirstname(), "Firstname should not be null");

        // getLastname() RETURN TYPE: String — same shape check for lastname.
        AssertionActions.assertNotNull(booking.getLastname(), "Lastname should not be null");

        // getTotalprice() RETURN TYPE: Integer (wrapper). assertTrue RETURN: void.
        // Price must be positive — a zero or negative price is a data bug.
        AssertionActions.assertTrue(booking.getTotalprice() > 0, "Total price should be positive");

        // getBookingdates() RETURN TYPE: BookingDates (nested POJO). A null here
        // means the nested JSON object was missing from the response.
        AssertionActions.assertNotNull(booking.getBookingdates(), "Booking dates should not be null");

        // getCheckin() RETURN TYPE: String — the innermost field of the object
        // graph: Booking -> BookingDates -> checkin. Chained getters, no parsing.
        AssertionActions.assertNotNull(booking.getBookingdates().getCheckin(), "Checkin should not be null");
    }

    @Test(priority = 4, description = "UpdateBooking changes all fields")
    @Description("PUT /booking/{id} should replace all fields and the change should persist")
    @Story("Full update")
    public void updateBookingChangesAllFields() {

        // Build the full replacement payload. RETURN TYPE: Booking.
        // PUT semantics = REPLACE everything, so a complete object is required.
        Booking replacement = BookingTestDataFactory.buildStandardBooking();
        replacement.setFirstname("UpdatEdFirstName");

        // updateBooking(int, Booking, String) RETURN TYPE: Response (raw).
        // Inside: PUT /booking/{id} + .cookie("token", token). PUT also returns
        // the full updated booking in its body, so we deserialize that directly —
        // faster AND immune to the hosted API's rate limiter.
        var putResponse = BookingService.updateBooking(bookingId, replacement, token);

        // getStatusCode() RETURN TYPE: int. PUT success = 200 per the API docs.
        AssertionActions.assertEquals(putResponse.getStatusCode(), 200, "PUT should return 200");

        // Response.as(Booking.class) RETURN TYPE: Booking — RestAssured's
        // on-demand deserialization of the response body into our POJO.
        Booking afterUpdate = putResponse.as(Booking.class);

        // getFirstname() RETURN TYPE: String — prove the update PERSISTED
        // (the response is the server's own record, not our request copy).
        AssertionActions.assertEquals(afterUpdate.getFirstname(), "UpdatEdFirstName",
                "Firstname should be updated after PUT");
    }

    @Test(priority = 5, description = "PartialUpdateBooking changes only sent fields")
    @Description("PATCH /booking/{id} should change firstname only and leave totalprice untouched")
    @Story("Partial update")
    public void partialUpdateChangesOnlySentFields() {

        // Capture the price BEFORE the patch. getBooking RETURN TYPE: Booking,
        // getTotalprice RETURN TYPE: Integer. We need this baseline so we can
        // PROVE the price survived the patch unchanged.
        int priceBefore = BookingService.getBooking(bookingId).getTotalprice();

        // buildPartialUpdateNames() RETURN TYPE: Booking with ONLY the two name
        // fields set. All other fields are null and get SKIPPED by
        // @JsonInclude(NON_NULL) during serialization — the PATCH payload is
        // therefore genuinely partial, which is the whole point of this test.
        Booking partial = BookingTestDataFactory.buildPartialUpdateNames();
        partial.setFirstname("PatchFirstName");

        // partialUpdateBooking RETURN TYPE: Response (raw). PATCH verb + token.
        var patchResponse = BookingService.partialUpdateBooking(bookingId, partial, token);

        // getStatusCode() RETURN TYPE: int. PATCH success = 200.
        AssertionActions.assertEquals(patchResponse.getStatusCode(), 200, "PATCH should return 200");

        // Response.as(Booking.class) RETURN TYPE: Booking — the PATCH response
        // contains the FULL updated booking, so we deserialize it directly
        // instead of firing an extra GET (fewer requests = less rate limiting).
        Booking afterPatch = patchResponse.as(Booking.class);

        // getFirstname() RETURN TYPE: String. PATCH contract #1: sent field changed.
        AssertionActions.assertEquals(afterPatch.getFirstname(), "PatchFirstName",
                "PATCH should change the firstname");

        // getTotalprice() RETURN TYPE: Integer. PATCH contract #2 (the critical
        // one): fields NOT sent must remain untouched. If this fails, the API
        // (or our payload) wiped data — the classic partial-update bug.
        AssertionActions.assertEquals(afterPatch.getTotalprice(), priceBefore,
                "PATCH should NOT change the price — that is the partial-update contract");
    }

    @Test(priority = 6, description = "DeleteBooking removes the booking")
    @Description("DELETE /booking/{id} should return 201 and the booking should become 404")
    @Story("Delete")
    public void deleteBookingRemovesIt() {

        // deleteBooking(int, String) RETURN TYPE: Response (raw). DELETE verb +
        // token cookie. The docs say DELETE responds with HTTP 201 Created.
        var deleteResponse = BookingService.deleteBooking(bookingId, token);

        // getStatusCode() RETURN TYPE: int — assert the documented 201.
        AssertionActions.assertEquals(deleteResponse.getStatusCode(), 201,
                "DELETE should return 201 per API documentation");

        // getBookingRaw(int) RETURN TYPE: Response. A deleted booking has no
        // body to deserialize, so raw is correct here. getStatusCode() RETURN
        // TYPE: int — a follow-up GET must return 404 Not Found, proving the
        // delete PERSISTED rather than the server just claiming success.
        AssertionActions.assertEquals(BookingService.getBookingRaw(bookingId).getStatusCode(), 404,
                "GET after DELETE should return 404");
    }

    @AfterClass
    // Runs once after the LAST test of the class, even if tests failed.
    // RETURN: void; TestNG calls it automatically as guaranteed cleanup.
    public void tearDown() {

        // Attempt cleanup defensively. If the last test already deleted the
        // subject booking this is a harmless no-op; if not, we remove our test
        // data so the shared API is not polluted — good test-environment hygiene.
        BookingService.deleteBooking(bookingId, token);
    }
}
