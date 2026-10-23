package com.rakeshkumar.course_enrollment_fifo_waitlist.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.rakeshkumar.course_enrollment_fifo_waitlist.domain.Learner;
import com.rakeshkumar.course_enrollment_fifo_waitlist.exception.LearnerNotFoundException;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.LearnerRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.CreateLearnerRequest;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.LearnerResponse;

import jakarta.validation.Valid;

/**
 * Registers learners and their prerequisite-eligibility flag. Re-posting an existing id
 * updates eligibility, which is how a waitlisted learner's eligibility can change over time.
 */
@RestController
@RequestMapping("/api/learners")
public class LearnerController {

    private final LearnerRepository learnerRepository;

    public LearnerController(LearnerRepository learnerRepository) {
        this.learnerRepository = learnerRepository;
    }

    @PostMapping
    public ResponseEntity<LearnerResponse> create(@Valid @RequestBody CreateLearnerRequest request) {
        boolean eligible = request.eligible() == null || request.eligible();
        Learner learner = learnerRepository.findById(request.id())
                .map(existing -> {
                    existing.setEligible(eligible);
                    return existing;
                })
                .orElseGet(() -> new Learner(request.id(), eligible));
        Learner saved = learnerRepository.save(learner);
        return ResponseEntity.status(HttpStatus.CREATED).body(new LearnerResponse(saved.getId(), saved.isEligible()));
    }

    @GetMapping("/{learnerId}")
    public LearnerResponse get(@PathVariable String learnerId) {
        Learner learner = learnerRepository.findById(learnerId)
                .orElseThrow(() -> new LearnerNotFoundException(learnerId));
        return new LearnerResponse(learner.getId(), learner.isEligible());
    }
}
