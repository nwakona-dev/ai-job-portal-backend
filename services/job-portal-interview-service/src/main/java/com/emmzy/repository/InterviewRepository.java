package com.emmzy.repository;

import com.emmzy.model.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    List<Interview> findByCandidateId(Long candidateId);

    List<Interview> findByEmployerId(Long employerId);

    List<Interview> findByApplicationId(Long applicationId);
}