package com.booking.constants;

/**
 * Endpoints class keeps every API path in ONE place, as constants.
 *
 * Interview explanation: In real projects APIs have many endpoints and they change.
 * If endpoints were typed as raw strings inside every test and tomorrow the path
 * changes, we would have to fix it in 50 places — a maintenance nightmare.
 *
 * By keeping them as "public static final String" constants here:
 *   - There is only ONE place to update if an endpoint changes.
 *   - Tests stay readable  : given().post(Endpoints.CREATE_BOOKING).
 *   - Typos cause compile errors rather than runtime failures.
 */
public final class Endpoints {

    // Private constructor so nobody creates an object of this constants class.
    // Constants classes are meant to be used as Endpoints.AUTH directly.
    private Endpoints() { }

    // "/auth" is used to create the token. It is defined in the API documentation
    // as the login endpoint. We do NOT put the full URL here, only the path, because
    // the base part (https://restful-booker.herokuapp.com) comes from config.
    public static final String AUTH = "/auth";

    // "/booking" is a multi-purpose path: GET returns all booking ids,
    // and POST on the same path creates a new booking. Same path, different verbs.
    public static final String BOOKING = "/booking";

    // "/booking/{id}" is the path for ONE specific booking. "{id}" is a path
    // parameter placeholder — RestAssured replaces it at runtime when we call
    // .pathParam("id", 123). This single constant serves GET (read), PUT (full
    // update), PATCH (partial update) and DELETE (remove) for one booking.
    public static final String BOOKING_BY_ID = "/booking/{id}";

    // "/ping" is the health check. CI jobs hit this first as a smoke test —
    // if the server is down, there is no point running the rest of the suite.
    public static final String PING = "/ping";
}
