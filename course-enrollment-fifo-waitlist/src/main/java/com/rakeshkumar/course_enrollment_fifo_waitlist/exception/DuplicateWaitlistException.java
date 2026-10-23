package com.rakeshkumar.course_enrollment_fifo_waitlist.exception;

public class DuplicateWaitlistException extends RuntimeException {
    public DuplicateWaitlistException(String offeringId, String learnerId) {
        super("Learner " + learnerId + " is already waitlisted for offering " + offeringId);
    }
}
