package com.emmzy.repository;

import com.emmzy.model.AIResumeAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

public interface AIResumeAnalysisRepository
        extends JpaRepository<AIResumeAnalysis, Long> {
}

