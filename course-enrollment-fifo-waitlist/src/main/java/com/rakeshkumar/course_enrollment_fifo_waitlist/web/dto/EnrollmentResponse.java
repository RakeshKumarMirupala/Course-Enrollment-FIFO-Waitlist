package com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto;

/** {@code waitlistPosition} is 1-based and only set when {@code status} is {@code WAITLISTED}. */
public record EnrollmentResponse(String offeringId, String learnerId, EnrollmentStatus status, Long waitlistPosition) {

    public static EnrollmentResponse enrolled(String offeringId, String learnerId) {
        return new EnrollmentResponse(offeringId, learnerId, EnrollmentStatus.ENROLLED, null);
    }

    public static EnrollmentResponse waitlisted(String offeringId, String learnerId, long position) {
        return new EnrollmentResponse(offeringId, learnerId, EnrollmentStatus.WAITLISTED, position);
    }
}
