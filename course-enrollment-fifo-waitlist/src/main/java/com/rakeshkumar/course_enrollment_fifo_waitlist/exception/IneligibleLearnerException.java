package com.rakeshkumar.course_enrollment_fifo_waitlist.exception;

public class IneligibleLearnerException extends RuntimeException {
    public IneligibleLearnerException(String learnerId) {
        super("Learner " + learnerId + " does not satisfy prerequisites");
    }
}
