package com.booking.testdata;

import com.booking.pojo.Booking;
import com.booking.pojo.BookingDates;

/**
 * TestDataFactory is the SINGLE place where test data is created.
 *
 * Interview explanation: Hard-coding data like "Jim Brown" inside every test
 * causes two problems:
 *   1. Duplication — 20 tests repeat the same six setters.
 *   2. Flaky collisions — two tests using the same name can clash when filtering.
 *
 * This factory uses RANDOM data (MethodUtils-free, plain java.util.Random) so every
 * run creates a unique guest. It follows the Builder idea too: buildStandardBooking()
 * gives a sensible default, and the test overrides only what the scenario needs
 * via setters. This is called the "default + override" test data pattern.
 */
public class BookingTestDataFactory {

    // Random object reused for all data generation in this class.
    private static final java.util.Random RANDOM = new java.util.Random();

    // A pool of first/last names to pick from, so data looks realistic in reports.
    private static final String[] FIRST_NAMES = {"Jim", "James", "Sally", "Mary", "Ravi", "Aisha"};
    private static final String[] LAST_NAMES = {"Brown", "Smith", "Jones", "Kumar", "Khan", "Wilson"};
    private static final String[] NEEDS = {"Breakfast", "Lunch", "Dinner", "Airport pickup", "None"};

    private BookingTestDataFactory() { }

    /**
     * Builds a complete, valid Booking with a random name, price, dates and need.
     * This is the reusable "default booking" every test starts from.
     */
    public static Booking buildStandardBooking() {

        // Pick random values from the pools. Random data makes each run unique,
        // which avoids the "same booking name already exists" style flakiness.
        String firstName = FIRST_NAMES[RANDOM.nextInt(FIRST_NAMES.length)];
        String lastName = LAST_NAMES[RANDOM.nextInt(LAST_NAMES.length)];
        String need = NEEDS[RANDOM.nextInt(NEEDS.length)];

        // Random price between 100 and 999 keeps the data varied but realistic.
        int price = 100 + RANDOM.nextInt(900);

        // Build the nested BookingDates POJO first (object graph), then the main POJO.
        BookingDates dates = new BookingDates("2026-01-01", "2026-01-10");

        // The all-args constructor assembles everything in one readable line.
        return new Booking(firstName, lastName, price, true, dates, need);
    }

    /**
     * Returns a booking with only firstname and lastname set — perfect for
     * PATCH (partial update) tests, where sending a full payload would defeat
     * the purpose of testing partial updates.
     */
    public static Booking buildPartialUpdateNames() {

        // New empty POJO (no-arg constructor) + only the two fields we want changed.
        Booking partial = new Booking();
        partial.setFirstname("James");
        partial.setLastname("Brown");
        return partial;
    }
}
