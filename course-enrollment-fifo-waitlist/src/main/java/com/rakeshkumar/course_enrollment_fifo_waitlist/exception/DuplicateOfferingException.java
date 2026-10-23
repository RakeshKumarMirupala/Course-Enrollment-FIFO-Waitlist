package com.rakeshkumar.course_enrollment_fifo_waitlist.exception;

public class DuplicateOfferingException extends RuntimeException {
    public DuplicateOfferingException(String offeringId) {
        super("Course offering already exists: " + offeringId);
    }
}
