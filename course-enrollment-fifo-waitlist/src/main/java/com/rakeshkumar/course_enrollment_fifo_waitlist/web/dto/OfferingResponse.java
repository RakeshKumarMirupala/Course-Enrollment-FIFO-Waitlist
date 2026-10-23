package com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto;

public record OfferingResponse(String id, int capacity, long enrolledCount, long waitlistCount) {
}
