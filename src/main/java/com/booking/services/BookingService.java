package com.booking.services;

import com.booking.constants.Endpoints;
import com.booking.pojo.Booking;
import com.booking.pojo.BookingResponse;
import com.booking.spec.SpecFactory;
import com.booking.utils.LoggerManager;
import com.booking.utils.RetryExecutor;
import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * BookingService is the heart of the framework — one reusable method per API
 * operation. Each method is a simple, predictable "keyword" that any test can use.
 *
 * Interview explanation: The naming is deliberate and maps 1:1 to the API docs —
 *   getBookingIds, getBooking, createBooking, updateBooking, partialUpdateBooking,
 *   deleteBooking. A new team member can read the test and instantly know which
 *   endpoint is being exercised, without opening the service class at all.
 *
 * Two kinds of return types are used on purpose:
 *   - Typed POJO returns (Booking, BookingResponse) for happy-path validation.
 *   - Raw Response returns where the test needs full control (status codes,
 *     headers, negative scenarios).
 */
public class BookingService {

    // Framework logger for this class. Every API call made by this service gets
    // a log line at INFO, and errors get logged at ERROR with full context —
    // this is how failures become diagnosable from logs alone.
    private static final Logger log = LoggerManager.get(BookingService.class);

    /**
     * GET /booking  — returns the list of all booking ids. Optional filters
     * (firstname, lastname, checkin, checkout) are passed as query params.
     */
    @Step("Get all booking ids")
    public static Response getBookingIds() {

        // Wrapped in RetryExecutor: if the hosted API rate-limits us (429/5xx),
        // the same GET is retried automatically instead of failing the test.
        return RetryExecutor.executeWithRetry(() ->
                RestAssured
                        .given()
                            .spec(SpecFactory.getRequestSpec())
                        .when()
                            .get(Endpoints.BOOKING));
    }

    /**
     * GET /booking with QUERY PARAMETERS — the filter search.
     * Query params filter the result set, exactly like the API docs describe.
     */
    @Step("Get booking ids filtered by firstname={firstname}")
    public static Response getBookingIdsByFilter(String paramName, String paramValue) {

        // .queryParam() appends ?paramName=paramValue to the URL. RestAssured takes
        // care of URL encoding, so special characters in names are handled safely.
        return RestAssured
                .given()
                    .spec(SpecFactory.getRequestSpec())
                    .queryParam(paramName, paramValue)
                .when()
                    .get(Endpoints.BOOKING);
    }

    /**
     * GET /booking/{id} — fetch ONE booking as a deserialized Booking POJO.
     */
    @Step("Get booking by id = {id}")
    public static Booking getBooking(int id) {

        // .pathParam("id", id) fills the {id} placeholder inside Endpoints.BOOKING_BY_ID.
        // .as(Booking.class) converts the JSON body into our POJO — this is
        // deserialization, and it lets assertions read like Java, like
        // booking.getFirstname().equals("James").
        return RestAssured
                .given()
                    .spec(SpecFactory.getRequestSpec())
                    .pathParam("id", id)
                .when()
                    .get(Endpoints.BOOKING_BY_ID)
                .then()
                    .assertThat()
                    .statusCode(200)
                    .extract()
                    .as(Booking.class);
    }

    /**
     * GET /booking/{id} — raw version, used for negative tests (e.g. id 999999
     * must give 404) where we need the status code, not the POJO.
     */
    @Step("Get booking raw response for id = {id}")
    public static Response getBookingRaw(int id) {

        // Also retried — negative tests deserve the same flakiness protection as
        // happy-path tests, otherwise a rate-limit blip fakes a "pass".
        return RetryExecutor.executeWithRetry(() ->
                RestAssured
                        .given()
                            .spec(SpecFactory.getRequestSpec())
                            .pathParam("id", id)
                        .when()
                            .get(Endpoints.BOOKING_BY_ID));
    }

    /**
     * POST /booking — create a new booking from a Booking POJO.
     * The POJO is serialized to JSON automatically and the full typed
     * response (bookingid + booking) is returned as a BookingResponse POJO.
     */
    @Step("Create booking for {booking.firstname} {booking.lastname}")
    public static BookingResponse createBooking(Booking booking) {

        log.info("CREATE booking for {} {}", booking.getFirstname(), booking.getLastname());

        // .body(booking) — SERIALIZATION happens here. Jackson converts the POJO
        // into a JSON request body, so the test never writes JSON strings by hand.
        // .as(BookingResponse.class) — DESERIALIZATION gives us bookingid in a
        // type-safe way, which the end-to-end test then reuses for update/delete.
        BookingResponse response =
                RestAssured
                        .given()
                            .spec(SpecFactory.getRequestSpec())
                            .body(booking)
                        .when()
                            .post(Endpoints.BOOKING)
                        .then()
                            .assertThat()
                            .statusCode(200)
                            .extract()
                            .as(BookingResponse.class);

        // Log the generated id — the piece of state every later step depends on.
        log.info("CREATED booking id={} for {} {}",
                response.getBookingid(), booking.getFirstname(), booking.getLastname());
        return response;
    }

    /**
     * PUT /booking/{id} — FULL update. Requires the token cookie for authorization.
     */
    @Step("Fully update booking id = {id}")
    public static Response updateBooking(int id, Booking updatedBooking, String token) {

        // .cookie("token", token) is how RestAssured sends the auth cookie
        // "Cookie: token=<value>" that the PUT endpoint requires. Without it the
        // server returns 403 Forbidden — which is itself a negative test we have.
        return RestAssured
                .given()
                    .spec(SpecFactory.getRequestSpec())
                    .pathParam("id", id)
                    .cookie("token", token)
                    .body(updatedBooking)     // full payload, all fields replaced
                .when()
                    .put(Endpoints.BOOKING_BY_ID);
    }

    /**
     * PATCH /booking/{id} — PARTIAL update. Send only the fields you want changed.
     */
    @Step("Partially update booking id = {id}")
    public static Response partialUpdateBooking(int id, Booking partialBooking, String token) {

        // Same chain as PUT but with the PATCH verb. The API merges the partial
        // payload into the existing booking — that behaviour is exactly what the
        // end-to-end test verifies (only firstname changed, price stayed same).
        return RestAssured
                .given()
                    .spec(SpecFactory.getRequestSpec())
                    .pathParam("id", id)
                    .cookie("token", token)
                    .body(partialBooking)
                .when()
                    .patch(Endpoints.BOOKING_BY_ID);
    }

    /**
     * DELETE /booking/{id} — remove the booking. Needs the token cookie.
     * Returns the raw Response because the success body is empty text ("Created").
     */
    @Step("Delete booking id = {id}")
    public static Response deleteBooking(int id, String token) {

        log.info("DELETE booking id={}", id);
        Response response = RestAssured
                .given()
                    .spec(SpecFactory.getRequestSpec())
                    .pathParam("id", id)
                    .cookie("token", token)
                .when()
                    .delete(Endpoints.BOOKING_BY_ID);

        // Log the actual delete outcome — a failed delete leaves orphan test data
        // behind, so this line in the log is the first clue during cleanup checks.
        log.info("DELETE booking id={} -> status {}", id, response.getStatusCode());
        return response;
    }

    /**
     * Small helper that lists ids of the FIRST n bookings (used by tests to pick
     * an existing booking safely). Returns a typed List<Integer> via JsonPath.
     */
    @Step("Pick first {n} existing booking ids")
    public static List<Integer> getFirstNBookingIds(int n) {

        // jsonPath().getList("bookingid") walks the JSON array and pulls only the
        // "bookingid" value from each element into a Java List<Integer>. This is
        // RestAssured's JsonPath in action — much cleaner than manual string parsing.
        List<Integer> allIds =
                RestAssured
                        .given()
                            .spec(SpecFactory.getRequestSpec())
                        .when()
                            .get(Endpoints.BOOKING)
                        .then()
                            .statusCode(200)
                            .extract()
                            .jsonPath()
                            .getList("bookingid");

        // Protect against the API having fewer bookings than requested — return
        // whatever exists so the caller never gets an IndexOutOfBounds.
        return allIds.size() > n ? allIds.subList(0, n) : allIds;
    }
}
