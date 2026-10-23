package com.rakeshkumar.course_enrollment_fifo_waitlist.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.CourseOffering;
import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.Enrollment;
import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.Learner;
import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.WaitlistEntry;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.DuplicateEnrollmentException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.DuplicateWaitlistException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.IneligibleLearnerException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.LearnerNotFoundException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.NotEnrolledException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.OfferingNotFoundException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.CourseOfferingRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.EnrollmentRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.LearnerRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.WaitlistEntryRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.DropResponse;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.EnrollmentResponse;

/**
 * Core enrollment/waitlist/promotion logic for a single course offering.
 *
 * <p>Every operation first acquires a pessimistic write lock on the {@link CourseOffering} row
 * via {@link CourseOfferingRepository#lockById(String)}. That lock serializes all enroll/drop/
 * promote requests for the same offering within a single transaction, which is what keeps the
 * capacity check-and-write and the FIFO promotion scan race-free under concurrency.
 */
@Service
public class EnrollmentService {

    private final CourseOfferingRepository courseOfferingRepository;
    private final LearnerRepository learnerRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;

    public EnrollmentService(
            CourseOfferingRepository courseOfferingRepository,
            LearnerRepository learnerRepository,
            EnrollmentRepository enrollmentRepository,
            WaitlistEntryRepository waitlistEntryRepository) {
        this.courseOfferingRepository = courseOfferingRepository;
        this.learnerRepository = learnerRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
    }

    @Transactional
    public EnrollmentResponse enroll(String offeringId, String learnerId) {
        CourseOffering offering = courseOfferingRepository.lockById(offeringId)
                .orElseThrow(() -> new OfferingNotFoundException(offeringId));
        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException(learnerId));

        if (enrollmentRepository.existsByOfferingIdAndLearnerId(offeringId, learnerId)) {
            throw new DuplicateEnrollmentException(offeringId, learnerId);
        }
        if (waitlistEntryRepository.existsByOfferingIdAndLearnerId(offeringId, learnerId)) {
            throw new DuplicateWaitlistException(offeringId, learnerId);
        }
        if (!learner.isEligible()) {
            throw new IneligibleLearnerException(learnerId);
        }

        long enrolledCount = enrollmentRepository.countByOfferingId(offeringId);
        if (enrolledCount < offering.getCapacity()) {
            enrollmentRepository.save(new Enrollment(offeringId, learnerId));
            return EnrollmentResponse.enrolled(offeringId, learnerId);
        }

        long position = waitlistEntryRepository.countByOfferingId(offeringId) + 1;
        waitlistEntryRepository.save(new WaitlistEntry(offeringId, learnerId));
        return EnrollmentResponse.waitlisted(offeringId, learnerId, position);
    }

    @Transactional
    public DropResponse drop(String offeringId, String learnerId) {
        courseOfferingRepository.lockById(offeringId)
                .orElseThrow(() -> new OfferingNotFoundException(offeringId));

        Enrollment enrollment = enrollmentRepository.findByOfferingIdAndLearnerId(offeringId, learnerId)
                .orElseThrow(() -> new NotEnrolledException(offeringId, learnerId));
        enrollmentRepository.delete(enrollment);

        String promotedLearnerId = promoteNextEligible(offeringId).map(WaitlistEntry::getLearnerId).orElse(null);
        return new DropResponse(offeringId, learnerId, promotedLearnerId);
    }

    /**
     * Scans the waitlist in FIFO (ascending id) order and promotes the first eligible learner.
     * Ineligible learners are skipped but retained on the waitlist, per the documented
     * skip-and-retain policy, so they remain in line if they later become eligible.
     */
    private Optional<WaitlistEntry> promoteNextEligible(String offeringId) {
        List<WaitlistEntry> queue = waitlistEntryRepository.findByOfferingIdOrderByIdAsc(offeringId);
        for (WaitlistEntry entry : queue) {
            boolean eligible = learnerRepository.findById(entry.getLearnerId())
                    .map(Learner::isEligible)
                    .orElse(false);
            if (eligible) {
                waitlistEntryRepository.delete(entry);
                enrollmentRepository.save(new Enrollment(offeringId, entry.getLearnerId()));
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }
}
