package com.emmzy.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "saved_jobs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_saved_job_user_job",
                        columnNames = {"user_id", "job_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_saved_job_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_saved_job_job_id",
                        columnList = "job_id"
                )
        }
)
public class SavedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * We intentionally store the user ID rather than creating a
     * JPA relationship to the User entity because the Job Service
     * is a separate microservice.
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /*
     * Job Service owns the Job entity, so this is represented as
     * a simple foreign-reference ID rather than a second Job
     * relationship.
     */
    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}