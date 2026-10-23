package com.rakeshkumar.course_enrollment_fifo_waitlist.exception;

public class OfferingNotFoundException extends RuntimeException {
    public OfferingNotFoundException(String offeringId) {
        super("Course offering not found: " + offeringId);
    }
}
