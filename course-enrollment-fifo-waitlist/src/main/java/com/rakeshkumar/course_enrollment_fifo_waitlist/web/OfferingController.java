package com.rakeshkumar.course_enrollment_fifo_waitlist.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.CourseOffering;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.DuplicateOfferingException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.OfferingNotFoundException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.CourseOfferingRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.EnrollmentRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.WaitlistEntryRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.CreateOfferingRequest;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.OfferingResponse;

import jakarta.validation.Valid;

/** Supports test/verification setup: creating an offering and reading back its current counts. */
@RestController
@RequestMapping("/api/offerings")
public class OfferingController {

    private final CourseOfferingRepository courseOfferingRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;

    public OfferingController(
            CourseOfferingRepository courseOfferingRepository,
            EnrollmentRepository enrollmentRepository,
            WaitlistEntryRepository waitlistEntryRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
    }

    @PostMapping
    public ResponseEntity<OfferingResponse> create(@Valid @RequestBody CreateOfferingRequest request) {
        if (courseOfferingRepository.existsById(request.id())) {
            throw new DuplicateOfferingException(request.id());
        }
        CourseOffering saved = courseOfferingRepository.save(new CourseOffering(request.id(), request.capacity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
    }

    @GetMapping("/{offeringId}")
    public OfferingResponse get(@PathVariable String offeringId) {
        CourseOffering offering = courseOfferingRepository.findById(offeringId)
                .orElseThrow(() -> new OfferingNotFoundException(offeringId));
        return toResponse(offering);
    }

    private OfferingResponse toResponse(CourseOffering offering) {
        long enrolledCount = enrollmentRepository.countByOfferingId(offering.getId());
        long waitlistCount = waitlistEntryRepository.countByOfferingId(offering.getId());
        return new OfferingResponse(offering.getId(), offering.getCapacity(), enrolledCount, waitlistCount);
    }
}
