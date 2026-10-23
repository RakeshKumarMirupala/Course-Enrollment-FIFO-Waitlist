package com.rakeshkumar.course_enrollment_fifo_waitlist.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.CourseOffering;

import jakarta.persistence.LockModeType;

public interface CourseOfferingRepository extends JpaRepository<CourseOffering, String> {

    /**
     * Locks the offering row for the duration of the transaction so that capacity checks,
     * enrollment, and waitlist promotion for the same offering are serialized across
     * concurrent requests.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CourseOffering c where c.id = :id")
    Optional<CourseOffering> lockById(@Param("id") String id);
}
