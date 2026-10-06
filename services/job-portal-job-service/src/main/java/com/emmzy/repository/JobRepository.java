package com.emmzy.repository;

import com.emmzy.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface JobRepository
        extends JpaRepository<Job, Long>,
        JpaSpecificationExecutor<Job> {


    List<Job> findByCompanyIdAndDeletedFalse(Long companyId);

    List<Job> findByEmployerIdAndDeletedFalse(Long employerId);

    List<Job> findByEmployerIdAndDeletedTrue(Long employerId);
}