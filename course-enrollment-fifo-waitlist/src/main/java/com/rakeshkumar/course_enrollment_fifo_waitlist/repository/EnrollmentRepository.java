package com.rakeshkumar.course_enrollment_fifo_waitlist.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.Enrollment;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    boolean existsByOfferingIdAndLearnerId(String offeringId, String learnerId);

    Optional<Enrollment> findByOfferingIdAndLearnerId(String offeringId, String learnerId);

    long countByOfferingId(String offeringId);

    List<Enrollment> findByOfferingIdOrderByIdAsc(String offeringId);
}
