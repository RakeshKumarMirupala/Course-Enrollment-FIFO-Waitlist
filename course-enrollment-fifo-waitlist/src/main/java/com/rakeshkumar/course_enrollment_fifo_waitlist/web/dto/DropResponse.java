package com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto;

/** {@code promotedLearnerId} is {@code null} when no eligible waitlisted learner was available to promote. */
public record DropResponse(String offeringId, String learnerId, String promotedLearnerId) {
}
