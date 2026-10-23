package com.rakeshkumar.course_enrollment_fifo_waitlist.exception;

public class LearnerNotFoundException extends RuntimeException {
    public LearnerNotFoundException(String learnerId) {
        super("Learner not found: " + learnerId);
    }
}
