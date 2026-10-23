package com.rakeshkumar.course_enrollment_fifo_waitlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** A waitlist slot; ascending {@code id} order is the FIFO queue order. */
@Entity
@Table(name = "waitlist_entry", uniqueConstraints = @UniqueConstraint(columnNames = {"offering_id", "learner_id"}))
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offering_id", nullable = false)
    private String offeringId;

    @Column(name = "learner_id", nullable = false)
    private String learnerId;

    protected WaitlistEntry() {
        // JPA
    }

    public WaitlistEntry(String offeringId, String learnerId) {
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
