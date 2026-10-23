package com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CreateOfferingRequest(
        @NotBlank String id,
        @Positive int capacity) {
}
