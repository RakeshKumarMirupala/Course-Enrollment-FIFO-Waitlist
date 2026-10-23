package com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto;

import jakarta.validation.constraints.NotBlank;

public record EnrollRequest(@NotBlank String learnerId) {
}
