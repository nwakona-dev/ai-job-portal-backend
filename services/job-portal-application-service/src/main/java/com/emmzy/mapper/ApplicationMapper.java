package com.emmzy.mapper;

import com.emmzy.domain.ApplicationStatus;
import com.emmzy.dto.ApplicationResponse;
import com.emmzy.dto.CompanyResponse;
import com.emmzy.dto.JobResponse;
import com.emmzy.dto.response.UserResponse;
import com.emmzy.model.Application;
import com.emmzy.payload.CreateApplicationRequest;

import java.util.List;

public class ApplicationMapper {

    public static Application toEntity(CreateApplicationRequest req,
                                       Long candidateId,
                                       Long companyId,
                                       Long employerId) {
        if(req == null) return null;

        return Application.builder()
                .candidateId(candidateId)
                .jobId(req.getJobId())
                .companyId(companyId)
                .employerId(employerId)
                .resumeId(req.getResumeId())
                .coverLetter(req.getCoverLetter())
                .expectedSalary(req.getExpectedSalary())
                .availableFrom(req.getAvailableFrom())
                .status(ApplicationStatus.PENDING)
                .isStarred(false)
                .build();
    }

    public static ApplicationResponse toResponse(Application application,
//                                                 List<ApplicationNote> notes,
                                                 JobResponse job,
                                                 CompanyResponse company,
                                                 UserResponse candidate
//                                                 ApplicationScreening screening
    ) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .candidate(candidate)
                .employerId(application.getEmployerId())
                .job(job)
                .company(company)
                .status(application.getStatus())

                .resumeId(application.getResumeId())
                .coverLetter(application.getCoverLetter())

                .expectedSalary(application.getExpectedSalary())

                .availableFrom(application.getAvailableFrom())

                .isStarred(application.getIsStarred())
//                .notes(toNoteResponseList(notes))
                .aiScore(application.getAiScore())
                .aiShortListStatus(application.getAiShortListStatus())
                .withDrawnAt(application.getWithDrawnAt())
                .withdrawnReason(application.getWithdrawnReason())
                .appliedAt(application.getAppliedAt())
                .updatedAt(application.getUpdatedAt())
//                .screening(toScreeningResponse(screening))
                .build();
    }
}
