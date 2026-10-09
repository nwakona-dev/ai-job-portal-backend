package com.emmzy.model;

import com.emmzy.domain.AiShortListStatus;
import com.emmzy.domain.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_seeker_id", nullable = false)
    private Long candidateId;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "company_id", nullable = false)
    private Long companyId;

    @Column(name = "employer_id", nullable = false)
    private Long employerId;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(columnDefinition = "TEXT")
    private String coverLetter;

    private BigDecimal expectedSalary;

    private LocalDate availableFrom;

    @Builder.Default
    @Column(nullable = false)
    private Boolean isStarred = false;

    private Integer aiScore;

    @Enumerated(EnumType.STRING)
    private AiShortListStatus aiShortListStatus;

    private LocalDateTime withDrawnAt;

    private String withdrawnReason;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime appliedAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}

