package com.rakeshkumar.course_enrollment_fifo_waitlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** An active enrollment of one learner in one course offering. */
@Entity
@Table(name = "enrollment", uniqueConstraints = @UniqueConstraint(columnNames = {"offering_id", "learner_id"}))
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false)
    private String offeringId;

    @Column(name = "learner_id", nullable = false)
    private String learnerId;

    protected Enrollment() {
        // JPA
    }

    public Enrollment(String offeringId, String learnerId) {
        this.offeringId = offeringId;
        this.learnerId = learnerId;
    }

    public Long getId() {
        return id;
    }

    public String getOfferingId() {
        return offeringId;
    }

    public String getLearnerId() {
        return learnerId;
    }
}
