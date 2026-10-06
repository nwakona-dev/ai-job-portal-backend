package com.emmzy.service.impl;

import com.emmzy.client.CompanyServiceClient;
import com.emmzy.client.JobServiceClient;
import com.emmzy.client.ResumeServiceClient;
import com.emmzy.client.UserServiceClient;
import com.emmzy.domain.ApplicationStatus;
import com.emmzy.dto.ApplicationResponse;
import com.emmzy.dto.CompanyResponse;
import com.emmzy.dto.JobResponse;
import com.emmzy.dto.ResumeResponse;
import com.emmzy.dto.response.UserResponse;
import com.emmzy.mapper.ApplicationMapper;
import com.emmzy.model.Application;
import com.emmzy.payload.CompanyApplicationFilterRequest;
import com.emmzy.payload.CreateApplicationRequest;
import com.emmzy.payload.WithdrawApplicationRequest;
import com.emmzy.repository.ApplicationRepository;
import com.emmzy.repository.ApplicationSpecification;
import com.emmzy.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private final ApplicationRepository applicationRepository;

    private final JobServiceClient jobServiceClient;
    private final ResumeServiceClient resumeServiceClient;
    private final UserServiceClient userServiceClient;
    private final CompanyServiceClient companyServiceClient;

    @Override
    public ApplicationResponse createApplication(
            Long candidateId,
            CreateApplicationRequest req
    ) throws Exception {

        if (applicationRepository.existsByCandidateIdAndJobId(
                candidateId,
                req.getJobId())) {

            throw new Exception(
                    "You have already applied for this job"
            );
        }

        JobResponse job =
                jobServiceClient.getJobById(
                        req.getJobId()
                );

        if (job == null) {
            throw new Exception("Job not found");
        }

        if (job.getStatus() !=
                com.emmzy.domain.JobStatus.OPEN) {

            throw new Exception(
                    "This job is not currently open for applications"
            );
        }

        if (job.getCompany() == null ||
                job.getCompany().getId() == null) {

            throw new Exception(
                    "Company information not found for this job"
            );
        }

        ResumeResponse resume =
                resumeServiceClient.getResumeById(
                        req.getResumeId(),
                        candidateId
                );

        if (resume == null) {
            throw new Exception("Resume not found");
        }

        UserResponse candidate =
                userServiceClient.getUserById(
                        candidateId
                );

        if (candidate == null) {
            throw new Exception("Candidate not found");
        }

        Long companyId =
                job.getCompany().getId();

        Long employerId =
                job.getEmployerId();

        if (employerId == null) {
            throw new Exception(
                    "Employer information not found for this job"
            );
        }

        Application application =
                ApplicationMapper.toEntity(
                        req,
                        candidateId,
                        companyId,
                        employerId
                );

        Application savedApplication =
                applicationRepository.save(
                        application
                );

        return ApplicationMapper.toResponse(
                savedApplication,
                job,
                job.getCompany(),
                candidate
        );
    }

    @Override
    public ApplicationResponse getApplicationById(
            Long id
    ) throws Exception {

        Application application =
                getApplicationEntity(id);

        return buildFullResponse(application);
    }

    @Override
    public List<ApplicationResponse> getMyApplication(
            Long candidateId
    ) {

        return applicationRepository
                .findByCandidateId(candidateId)
                .stream()
                .map(this::buildFullResponse)
                .toList();
    }

    @Override
    public List<ApplicationResponse> getApplicationForJob(
            Long jobId
    ) {

        return applicationRepository
                .findByJobId(jobId)
                .stream()
                .map(this::buildFullResponse)
                .toList();
    }

    @Override
    public List<ApplicationResponse> getApplicationForCompany(
            Long userId,
            CompanyApplicationFilterRequest filter
    ) throws Exception {

        CompanyResponse company =
                companyServiceClient.getMyCompany(
                        userId
                );

        if (company == null ||
                company.getId() == null) {

            throw new Exception(
                    "Company not found for this user"
            );
        }

        Long companyId =
                company.getId();

        if (filter == null) {
            filter =
                    new CompanyApplicationFilterRequest();
        }

        Sort sort =
                buildSort(
                        filter.getSortBy()
                );

        return applicationRepository.findAll(
                        ApplicationSpecification
                                .forCompanyWithFilters(
                                        companyId,
                                        filter.getJobId(),
                                        filter.getStatus(),
                                        filter.getIsStarred(),
                                        filter.getAiShortListStatus(),
                                        filter.getMinAiScore()
                                ),
                        sort
                )
                .stream()
                .map(this::buildFullResponse)
                .toList();
    }

    @Override
    public Map<String, Object> getCandidatePreview(
            Long applicationId,
            Long employerId
    ) throws Exception {

        Application application =
                getApplicationEntity(
                        applicationId
                );

        assertEmployer(
                application,
                employerId
        );

        JobResponse job =
                jobServiceClient.getJobById(
                        application.getJobId()
                );

        if (job == null) {
            throw new Exception(
                    "Job not found: " +
                            application.getJobId()
            );
        }

        UserResponse candidate =
                userServiceClient.getUserById(
                        application.getCandidateId()
                );

        if (candidate == null) {
            throw new Exception(
                    "Candidate not found: " +
                            application.getCandidateId()
            );
        }

        ResumeResponse resume =
                resumeServiceClient.getResumeById(
                        application.getResumeId(),
                        application.getCandidateId()
                );

        if (resume == null) {
            throw new Exception(
                    "Resume not found: " +
                            application.getResumeId()
            );
        }

        ApplicationResponse applicationResponse =
                ApplicationMapper.toResponse(
                        application,
                        job,
                        job.getCompany(),
                        candidate
                );

        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put(
                "application",
                applicationResponse
        );

        response.put(
                "candidate",
                candidate
        );

        response.put(
                "resume",
                resume
        );

        return response;
    }

    @Override
    public ApplicationResponse updateStatus(
            Long applicationId,
            Long employerId,
            ApplicationStatus status
    ) throws Exception {

        Application application =
                getApplicationEntity(
                        applicationId
                );

        assertEmployer(
                application,
                employerId
        );

        if (application.getStatus() ==
                ApplicationStatus.WITHDRAWN) {

            throw new Exception(
                    "Candidate have already withdrawn"
            );
        }

        application.setStatus(status);

        Application savedApplication =
                applicationRepository.save(
                        application
                );

        return buildFullResponse(
                savedApplication
        );
    }

    @Override
    public ApplicationResponse withdraw(
            Long applicationId,
            Long candidateId,
            WithdrawApplicationRequest req
    ) throws Exception {

        Application application =
                getApplicationEntity(
                        applicationId
                );

        assertCandidate(
                application,
                candidateId
        );

        if (application.getStatus() == null) {

            throw new Exception(
                    "Application status is required"
            );
        }

        if (application.getStatus() ==
                ApplicationStatus.WITHDRAWN) {

            throw new Exception(
                    "Application has already been withdrawn"
            );
        }

        if (application.getStatus() ==
                ApplicationStatus.HIRED) {

            throw new Exception(
                    "A hired application cannot be withdrawn"
            );
        }

        application.setStatus(
                ApplicationStatus.WITHDRAWN
        );

        application.setWithDrawnAt(
                LocalDateTime.now()
        );

        application.setWithdrawnReason(
                req.getReason()
        );

        Application savedApplication =
                applicationRepository.save(
                        application
                );

        return buildFullResponse(
                savedApplication
        );
    }

    @Override
    public ApplicationResponse toggleStar(
            Long applicationId,
            Long employerId
    ) throws Exception {

        Application application =
                getApplicationEntity(
                        applicationId
                );

        assertEmployer(
                application,
                employerId
        );

        application.setIsStarred(
                !Boolean.TRUE.equals(
                        application.getIsStarred()
                )
        );

        Application savedApplication =
                applicationRepository.save(
                        application
                );

        return buildFullResponse(
                savedApplication
        );
    }

    @Override
    public void deleteApplication(
            Long applicationId,
            Long candidateId
    ) throws Exception {

        Application application =
                getApplicationEntity(
                        applicationId
                );

        assertCandidate(
                application,
                candidateId
        );

        applicationRepository.delete(
                application
        );
    }

    @Override
    public Application getApplicationEntity(
            Long id
    ) throws Exception {

        return applicationRepository
                .findById(id)
                .orElseThrow(
                        () -> new Exception(
                                "Application not found"
                        )
                );
    }


    public ApplicationResponse buildFullResponse(
            Application application
    ) {

        JobResponse job =
                jobServiceClient.getJobById(
                        application.getJobId()
                );


        if (job == null) {

            job = JobResponse.builder()
                    .id(application.getJobId())
                    .title("Job no longer available")
                    .description(
                            "This job is no longer available."
                    )
                    .employerId(
                            application.getEmployerId()
                    )
                    .active(false)
                    .deleted(true)
                    .build();
        }

        UserResponse candidate =
                userServiceClient.getUserById(
                        application.getCandidateId()
                );

        if (candidate == null) {

            throw new RuntimeException(
                    "Candidate not found: " +
                            application.getCandidateId()
            );
        }

        CompanyResponse company =
                job.getCompany();

        return ApplicationMapper.toResponse(
                application,
                job,
                company,
                candidate
        );
    }

    private void assertCandidate(
            Application application,
            Long candidateId
    ) throws Exception {

        if (!java.util.Objects.equals(
                application.getCandidateId(),
                candidateId
        )) {

            throw new Exception(
                    "You are not the owner of this application"
            );
        }
    }

    private void assertEmployer(
            Application application,
            Long employerId
    ) throws Exception {

        if (!java.util.Objects.equals(
                application.getEmployerId(),
                employerId
        )) {

            throw new Exception(
                    "You are not the employer for this application"
            );
        }
    }

    private Sort buildSort(
            String sortBy
    ) {

        if ("AI_SCORE_DESC".equals(sortBy)) {

            return Sort.by(
                    Sort.Order
                            .desc("aiScore")
                            .with(
                                    Sort.NullHandling.NULLS_LAST
                            )
            );

        } else if ("AI_SCORE_ASC".equals(sortBy)) {

            return Sort.by(
                    Sort.Order
                            .asc("aiScore")
                            .with(
                                    Sort.NullHandling.NULLS_LAST
                            )
            );
        }

        return Sort.by(
                Sort.Direction.DESC,
                "appliedAt"
        );
    }
}