package com.emmzy.repository;

import com.emmzy.model.AIMatchResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AIMatchResultRepository
        extends JpaRepository<AIMatchResult, Long> {

    Optional<AIMatchResult> findByJobIdAndCandidateId(
            Long jobId,
            Long candidateId
    );
}

