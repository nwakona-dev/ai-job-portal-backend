package com.emmzy.repository;

import com.emmzy.model.AICandidateScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AICandidateScoreRepository
        extends JpaRepository<AICandidateScore, Long> {

    List<AICandidateScore> findByJobIdOrderByMatchScoreDesc(
            Long jobId
    );

    List<AICandidateScore> findByCandidateId(
            Long candidateId
    );
}
