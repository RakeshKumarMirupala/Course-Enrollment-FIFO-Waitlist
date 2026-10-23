package com.rakeshkumar.course_enrollment_fifo_waitlist.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A single course offering with a fixed enrollment capacity. */
@Entity
@Table(name = "course_offering")
public class CourseOffering {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private String id;

    @Column(name = "capacity", nullable = false)
    private int capacity;

    protected CourseOffering() {
        // JPA
    }

    public CourseOffering(String id, int capacity) {
        this.id = id;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public int getCapacity() {
        return capacity;
    }
}
