package com.rakeshkumar.course_enrollment_fifo_waitlist.exception;

public class NotEnrolledException extends RuntimeException {
    public NotEnrolledException(String offeringId, String learnerId) {
        super("Learner " + learnerId + " is not enrolled in offering " + offeringId);
    }
}
