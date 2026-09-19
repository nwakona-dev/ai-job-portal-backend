package com.emmzy.repository;

import com.emmzy.model.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, Long> {

    List<ResumeSkill> findByResumeIdOrderByDisplayOrderAsc(Long resumeId);
}
