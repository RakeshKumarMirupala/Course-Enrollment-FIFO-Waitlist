package com.rakeshkumar.course_enrollment_fifo_waitlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A learner and whether they currently satisfy prerequisites for enrollment/promotion. */
@Entity
@Table(name = "learner")
public class Learner {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "eligible", nullable = false)
    private boolean eligible;

    protected Learner() {
        // JPA
    }

    public Learner(String id, boolean eligible) {
        this.id = id;
        this.eligible = eligible;
    }

    public String getId() {
        return id;
    }

    public boolean isEligible() {
        return eligible;
    }

    public void setEligible(boolean eligible) {
        this.eligible = eligible;
    }
}
