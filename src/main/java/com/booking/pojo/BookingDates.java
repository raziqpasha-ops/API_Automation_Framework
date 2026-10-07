package com.booking.pojo;

/**
 * BookingDates is a POJO representing the nested "bookingdates" object in the API:
 *      "bookingdates": { "checkin": "2018-01-01", "checkout": "2019-01-01" }
 *
 * Interview explanation: POJO stands for Plain Old Java Object. It is a simple
 * class with private fields, public getters and setters, and nothing else.
 *
 * Why do we need POJOs in API automation?
 * SERIALIZATION  : RestAssured + Jackson convert this object into a JSON body when
 *                  we pass it as .body(bookingPojo). So we never hand-write JSON.
 * DESERIALIZATION: RestAssured converts the JSON response back into this object
 *                  using .as(Booking.class), so we can assert with real getters.
 *
 * This gives us compile-time safety — if a field name changes, our code fails to
 * compile instead of failing at runtime with a mysterious null.
 */
public class BookingDates {

    // private fields: outside classes cannot touch them directly, they must go
    // through getters/setters. This is the "encapsulation" principle.
    private String checkin;
    private String checkout;

    // No-argument constructor is REQUIRED by Jackson. During deserialization
    // Jackson first creates an empty object with this constructor, then fills
    // each field using the setters. If we remove it, deserialization breaks.
    public BookingDates() { }

    // Convenience constructor: lets tests create a full object in one line,
    // like new BookingDates("2018-01-01", "2019-01-01").
    public BookingDates(String checkin, String checkout) {
        this.checkin = checkin;
        this.checkout = checkout;
    }

    // Getter for checkin: during SERIALIZATION Jackson calls this to read the
    // value and put it into JSON. During our assertions we call it to compare.
    public String getCheckin() {
        return checkin;
    }

    // Setter for checkin: Jackson calls this during DESERIALIZATION to fill the
    // value from the JSON response into the object.
    public void setCheckin(String checkin) {
        this.checkin = checkin;
    }

    public String getCheckout() {
        return checkout;
    }

    public void setCheckout(String checkout) {
        this.checkout = checkout;
    }
}
