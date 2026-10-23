package com.rakeshkumar.course_enrollment_fifo_waitlist.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.CourseOfferingRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.EnrollmentRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.LearnerRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.repository.WaitlistEntryRepository;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.CreateLearnerRequest;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.CreateOfferingRequest;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.EnrollRequest;
import com.rakeshkumar.course_enrollment_fifo_waitlist.web.dto.EnrollmentResponse;

/** Verifies HTTP-level behavior: status codes, validation, and clear error responses. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class EnrollmentApiTest {

    @LocalServerPort
    private int port;
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private CourseOfferingRepository courseOfferingRepository;
    @Autowired
    private LearnerRepository learnerRepository;
    @Autowired
    private EnrollmentRepository enrollmentRepository;
    @Autowired
    private WaitlistEntryRepository waitlistEntryRepository;

    @BeforeEach
    void cleanDatabase() {
        waitlistEntryRepository.deleteAll();
        enrollmentRepository.deleteAll();
        learnerRepository.deleteAll();
        courseOfferingRepository.deleteAll();
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    // 10. Invalid requests return clear results without corrupting state.
    @Test
    void createOffering_rejectsNonPositiveCapacity() {
        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                url("/api/offerings"), new CreateOfferingRequest("CS101", 0), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(courseOfferingRepository.existsById("CS101")).isFalse();
    }

    @Test
    void enroll_rejectsBlankLearnerId() {
        restTemplate.postForEntity(url("/api/offerings"), new CreateOfferingRequest("CS101", 2), Void.class);

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                url("/api/offerings/CS101/enrollments"), new EnrollRequest(" "), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void enroll_returnsNotFoundForUnknownOffering() {
        restTemplate.postForEntity(url("/api/learners"), new CreateLearnerRequest("L1", true), Void.class);

        ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
                url("/api/offerings/UNKNOWN/enrollments"), new EnrollRequest("L1"), ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void enroll_returnsCreatedWhenSeatAvailable() {
        restTemplate.postForEntity(url("/api/offerings"), new CreateOfferingRequest("CS101", 1), Void.class);
        restTemplate.postForEntity(url("/api/learners"), new CreateLearnerRequest("L1", true), Void.class);

        ResponseEntity<EnrollmentResponse> response = restTemplate.postForEntity(
                url("/api/offerings/CS101/enrollments"), new EnrollRequest("L1"), EnrollmentResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void drop_returnsNotFoundWhenLearnerNotEnrolled() {
        restTemplate.postForEntity(url("/api/offerings"), new CreateOfferingRequest("CS101", 1), Void.class);

        ResponseEntity<ProblemDetail> response = restTemplate.exchange(
                url("/api/offerings/CS101/enrollments/L1"), org.springframework.http.HttpMethod.DELETE,
                null, ProblemDetail.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
