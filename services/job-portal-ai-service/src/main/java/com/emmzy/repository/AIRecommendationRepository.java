package com.emmzy.repository;

import com.emmzy.model.AIRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIRecommendationRepository
        extends JpaRepository<AIRecommendation, Long> {

    List<AIRecommendation> findByCandidateId(Long candidateId);

    List<AIRecommendation> findByJobId(Long jobId);

    List<AIRecommendation> findByCandidateIdOrderByMatchScoreDesc(
            Long candidateId
    );
}

