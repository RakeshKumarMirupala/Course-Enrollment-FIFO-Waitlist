package com.rakeshkumar.course_enrollment_fifo_waitlist.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.Learner;

public interface LearnerRepository extends JpaRepository<Learner, String> {
}
