package com.emmzy.model;

import com.emmzy.domain.ExperienceLevel;
import com.emmzy.domain.JobStatus;
import com.emmzy.domain.JobType;
import com.emmzy.domain.WorkMode;
import com.emmzy.model.embeddable.JobLocation;
import com.emmzy.model.embeddable.SalaryRange;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Table(name = "jobs")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String requirements;

    private String responsibilities;

    private String benefits;

    @Column(nullable = false)
    private Long companyId;

    @Column(nullable = false)
    private Long employerId;

    @ManyToOne
    private JobCategory category;

    @ManyToMany
    private Set<JobSkill> skills;

    @ManyToMany
    private Set<JobTag> tags;

    @Embedded
    private JobLocation location;

    @Embedded
    private SalaryRange salaryRange;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExperienceLevel experienceLevel;

    /*
     * Jobs are OPEN by default.
     *
     * Builder.Default is important here because Lombok's
     * @Builder does not use a field initializer unless
     * @Builder.Default is specified.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.OPEN;

    @Builder.Default
    @Column(nullable = false)
    private Integer openings = 1;

    private LocalDate applicationDeadline;

    private LocalDate expiredAt;

    /*
     * A newly created job is active and visible to job seekers.
     */
    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime publishedAt;

    private LocalDateTime closedAt;

    /*
     * Protect the entity from accidentally being persisted
     * without the visibility fields populated.
     */
    @PrePersist
    protected void onCreate() {

        if (status == null) {
            status = JobStatus.OPEN;
        }

        if (active == null) {
            active = true;
        }

        if (openings == null) {
            openings = 1;
        }

        if (status == JobStatus.OPEN &&
                publishedAt == null) {

            publishedAt = LocalDateTime.now();
        }
    }

    /*
     * Protect updates from accidentally turning an OPEN job
     * into an invisible job because active/status were omitted.
     */
    @PreUpdate
    protected void onUpdate() {

        if (status == null) {
            status = JobStatus.OPEN;
        }

        if (active == null) {
            active = true;
        }
    }
}