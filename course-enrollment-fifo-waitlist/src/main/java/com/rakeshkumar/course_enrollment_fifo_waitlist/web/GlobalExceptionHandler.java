package com.rakeshkumar.course_enrollment_fifo_waitlist.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.DuplicateEnrollmentException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.DuplicateOfferingException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.DuplicateWaitlistException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.IneligibleLearnerException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.LearnerNotFoundException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.NotEnrolledException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.OfferingNotFoundException;

/** Maps domain exceptions to clear, non-leaking HTTP error responses. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({OfferingNotFoundException.class, LearnerNotFoundException.class, NotEnrolledException.class})
    public ProblemDetail handleNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({DuplicateEnrollmentException.class, DuplicateWaitlistException.class, DuplicateOfferingException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(IneligibleLearnerException.class)
    public ProblemDetail handleIneligible(IneligibleLearnerException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }
}
