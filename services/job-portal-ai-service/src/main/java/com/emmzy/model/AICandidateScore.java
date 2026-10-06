package com.emmzy.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_candidate_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AICandidateScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long candidateId;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false)
    private Double matchScore;

    private Integer ranking;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}

