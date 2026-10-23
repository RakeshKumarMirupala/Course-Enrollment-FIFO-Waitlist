package com.rakeshkumar.course_enrollment_fifo_waitlist.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.Enrollment;
import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.WaitlistEntry;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.EnrollmentRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.WaitlistEntryRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.service.EnrollmentService;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.DropResponse;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.EnrollRequest;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.EnrollmentResponse;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.EnrollmentStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/offerings/{offeringId}")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;

    public EnrollmentController(
            EnrollmentService enrollmentService,
            EnrollmentRepository enrollmentRepository,
            WaitlistEntryRepository waitlistEntryRepository) {
        this.enrollmentService = enrollmentService;
        this.enrollmentRepository = enrollmentRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
    }

    @PostMapping("/enrollments")
    public ResponseEntity<EnrollmentResponse> enroll(
            @PathVariable String offeringId, @Valid @RequestBody EnrollRequest request) {
        EnrollmentResponse response = enrollmentService.enroll(offeringId, request.learnerId());
        HttpStatus status = response.status() == EnrollmentStatus.ENROLLED ? HttpStatus.CREATED : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }

    @DeleteMapping("/enrollments/{learnerId}")
    public DropResponse drop(@PathVariable String offeringId, @PathVariable String learnerId) {
        return enrollmentService.drop(offeringId, learnerId);
    }

    @GetMapping("/enrollments")
    public List<String> listEnrollments(@PathVariable String offeringId) {
        return enrollmentRepository.findByOfferingIdOrderByIdAsc(offeringId).stream()
                .map(Enrollment::getLearnerId)
                .toList();
    }

    @GetMapping("/waitlist")
    public List<String> listWaitlist(@PathVariable String offeringId) {
        return waitlistEntryRepository.findByOfferingIdOrderByIdAsc(offeringId).stream()
                .map(WaitlistEntry::getLearnerId)
                .toList();
    }
}
