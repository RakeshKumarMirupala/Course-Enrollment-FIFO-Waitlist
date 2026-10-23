package com.rakeshkumar.course_enrollment_fifo_waitlist.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.WaitlistEntry;

public interface WaitlistEntryRepository extends JpaRepository<WaitlistEntry, Long> {

    boolean existsByOfferingIdAndLearnerId(String offeringId, String learnerId);

    long countByOfferingId(String offeringId);

    List<WaitlistEntry> findByOfferingIdOrderByIdAsc(String offeringId);
}
