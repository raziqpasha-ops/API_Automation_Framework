package com.booking.pojo;

/**
 * BookingResponse POJO is the WRAPPER that the CreateBooking endpoint returns:
 *      { "bookingid": 1, "booking": { ...booking fields... } }
 *
 * Interview explanation: Notice the create response is not the booking alone —
 * it is a booking PLUS the generated id. So we need a second, wrapper POJO.
 * This is a very common real-world pattern: request model and response model are
 * different shapes, so we model them as different classes.
 */
public class BookingResponse {

    // bookingid is the id the server generated for the new booking. In end-to-end
    // tests we capture this value and reuse it to GET, UPDATE and DELETE the booking.
    private int bookingid;

    // booking is the full booking object echoed back by the server. Its type is
    // our existing Booking POJO, so nested deserialization works out of the box.
    private Booking booking;

    // Required by Jackson for the same reason as the other POJOs.
    public BookingResponse() { }

    public int getBookingid() {
        return bookingid;
    }

    public void setBookingid(int bookingid) {
        this.bookingid = bookingid;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }
}
