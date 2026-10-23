package com.rakeshkumar.course_enrollment_fifo_waitlist.exception;

public class DuplicateEnrollmentException extends RuntimeException {
    public DuplicateEnrollmentException(String offeringId, String learnerId) {
        super("Learner " + learnerId + " is already enrolled in offering " + offeringId);
    }
}
