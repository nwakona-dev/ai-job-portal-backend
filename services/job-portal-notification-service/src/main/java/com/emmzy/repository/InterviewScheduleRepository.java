package com.emmzy.repository;

import com.emmzy.domain.InterviewStatus;
import com.emmzy.model.InterviewSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InterviewScheduleRepository
        extends JpaRepository<InterviewSchedule, Long> {

    Optional<InterviewSchedule> findByInterviewId(Long interviewId);

    List<InterviewSchedule> findByStatus(InterviewStatus status);

    List<InterviewSchedule> findByStatusInAndScheduledDateGreaterThanEqual(
            List<InterviewStatus> statuses,
            LocalDate date
    );
}