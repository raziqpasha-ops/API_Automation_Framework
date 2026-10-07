package com.booking.pojo;

// JsonInclude is a Jackson annotation. NON_NULL means: when this object is
// SERIALIZED to JSON, any field whose value is null is SKIPPED entirely.
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Booking POJO represents the main booking payload of the API — the object that
 * travels in CreateBooking request, and comes back in GetBooking/UpdateBooking.
 *
 * Interview explanation: One POJO here serves BOTH directions:
 *   Request side  -> passed to .body(pojo), Jackson serializes it to JSON.
 *   Response side -> .as(Booking.class), Jackson deserializes JSON into it.
 * This "model reuse" is exactly what makes the framework 100% reusable: the same
 * class is the contract for create, read, update and partial-update flows.
 *
 * The NON_NULL inclusion is CRITICAL for PATCH (partial update): we build a POJO
 * with only firstname/lastname set and leave everything else null. Because of this
 * annotation those null fields are NOT written into the JSON body — so the PATCH
 * payload truly contains only the fields we want to change. Without it, Jackson
 * would send "totalprice": 0 and "bookingdates": null and DESTROY the booking.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Booking {

    // Each field below matches one JSON key from the API documentation exactly.
    // Matching names matter: Jackson maps JSON "firstname" to field "firstname"
    // automatically with no extra annotations needed.
    private String firstname;
    private String lastname;
    // NOTE: totalprice and depositpaid use the WRAPPER types (Integer, Boolean)
    // instead of primitives (int, boolean). Interview explanation: a primitive int
    // can NEVER be null — its default is 0 — so for a PATCH payload Jackson would
    // wrongly send "totalprice": 0. Wrapper types CAN be null, and combined with
    // @JsonInclude(NON_NULL) above they get skipped from the JSON entirely, which
    // is exactly what a partial update needs.
    private Integer totalprice;
    private Boolean depositpaid;

    // bookingdates is a NESTED object, so its type is another POJO (BookingDates).
    // Jackson handles nested objects automatically — this is called object graph
    // mapping, and it is the reason we made the separate BookingDates class.
    private BookingDates bookingdates;

    // additionalneeds is optional in the API. We keep it as a normal String; when
    // we don't set it, Jackson sends it as null (or we can skip it with a setter).
    private String additionalneeds;

    // No-arg constructor for Jackson (explained in BookingDates).
    public Booking() { }

    // Convenience all-args constructor so tests can build a complete booking in
    // one readable line instead of calling six setters.
    public Booking(String firstname, String lastname, Integer totalprice, Boolean depositpaid,
                   BookingDates bookingdates, String additionalneeds) {
        this.firstname = firstname;
        this.lastname = lastname;
        this.totalprice = totalprice;
        this.depositpaid = depositpaid;
        this.bookingdates = bookingdates;
        this.additionalneeds = additionalneeds;
    }

    // ----- getters (read values out; used by Jackson serialization and assertions) -----
    public String getFirstname() {
        return firstname;
    }

    public String getLastname() {
        return lastname;
    }

    public Integer getTotalprice() {
        return totalprice;
    }

    public Boolean isDepositpaid() {
        return depositpaid;
    }

    public BookingDates getBookingdates() {
        return bookingdates;
    }

    public String getAdditionalneeds() {
        return additionalneeds;
    }

    // ----- setters (put values in; used by Jackson deserialization and by tests) -----
    public void setFirstname(String firstname) {
        this.firstname = firstname;
    }

    public void setLastname(String lastname) {
        this.lastname = lastname;
    }

    public void setTotalprice(Integer totalprice) {
        this.totalprice = totalprice;
    }

    public void setDepositpaid(Boolean depositpaid) {
        this.depositpaid = depositpaid;
    }

    public void setBookingdates(BookingDates bookingdates) {
        this.bookingdates = bookingdates;
    }

    public void setAdditionalneeds(String additionalneeds) {
        this.additionalneeds = additionalneeds;
    }
}
