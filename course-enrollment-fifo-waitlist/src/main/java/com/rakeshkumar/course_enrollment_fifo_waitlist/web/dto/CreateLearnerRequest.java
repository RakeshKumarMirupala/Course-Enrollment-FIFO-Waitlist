package com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto;

import jakarta.validation.constraints.NotBlank;

/** {@code eligible} defaults to {@code true} when omitted. */
public record CreateLearnerRequest(
        @NotBlank String id,
        Boolean eligible) {
}
