package com.emmzy.repository;

import com.emmzy.model.Education;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EducationRepository extends JpaRepository<Education, Long> {

    List<Education> findByResumeIdOrderByDisplayOrderAsc(Long resumeId);
}
