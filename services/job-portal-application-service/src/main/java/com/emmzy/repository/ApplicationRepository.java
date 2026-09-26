package com.emmzy.repository;

import com.emmzy.model.Application;
import com.emmzy.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

    List<Application> findByJobSeekerId(Long jobSeekerId);

    List<Application> findByJobId(Long jobId);

    Optional<Application> findByJobIdAndJobSeekerId(
            Long jobId,
            Long jobSeekerId
    );

    boolean existsByJobIdAndJobSeekerId(
            Long jobId,
            Long jobSeekerId
    );

    List<Application> findByJobSeekerIdAndStatus(
            Long jobSeekerId,
            ApplicationStatus status
    );
}