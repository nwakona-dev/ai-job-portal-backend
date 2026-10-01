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
import java.util.List;
import java.util.Objects;

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

        // 1. Check if the candidate has already applied for this job
        if (applicationRepository.existsByCandidateIdAndJobId(
                candidateId,
                req.getJobId())) {

            throw new Exception("You have already applied for this job");
        }

        // 2. Get the job from Job Service
        JobResponse job = jobServiceClient.getJobById(req.getJobId());

        if (job == null) {
            throw new Exception("Job not found");
        }

        // 3. Check if the job is open
        if (job.getStatus() != com.emmzy.domain.JobStatus.OPEN) {
            throw new Exception(
                    "This job is not currently open for applications"
            );
        }

        // 4. Check if the job has a company
        if (job.getCompany() == null ||
                job.getCompany().getId() == null) {

            throw new Exception(
                    "Company information not found for this job"
            );
        }

        // 5. Get resume from Resume Service
        ResumeResponse resume =
                resumeServiceClient.getResumeById(
                        req.getResumeId(),
                        candidateId
                );

        if (resume == null) {
            throw new Exception("Resume not found");
        }

        // 6. Get candidate information from User Service
        UserResponse candidate =
                userServiceClient.getUserById(candidateId);

        if (candidate == null) {
            throw new Exception("Candidate not found");
        }

        // 7. Get company and employer information from JobResponse
        Long companyId = job.getCompany().getId();
        Long employerId = job.getEmployerId();

        if (employerId == null) {
            throw new Exception(
                    "Employer information not found for this job"
            );
        }

        // 8. Create the application
        Application application =
                ApplicationMapper.toEntity(
                        req,
                        candidateId,
                        companyId,
                        employerId
                );

        // 9. Save application
        Application savedApplication =
                applicationRepository.save(application);

        // 10. AI Screening can run in the background later
        // TODO: Trigger AI screening asynchronously

        // 11. Return complete application response
        return ApplicationMapper.toResponse(
                savedApplication,
                job,
                job.getCompany(),
                candidate,
                resume
        );
    }

    @Override
    public ApplicationResponse getApplicationById(Long id)
            throws Exception {

        Application application =
                getApplicationEntity(id);

        return buildFullResponse(application);
    }

    @Override
    public List<ApplicationResponse> getMyApplication(
            Long candidateId) {

        return applicationRepository
                .findByCandidateId(candidateId)
                .stream()
                .map(this::buildFullResponse)
                .toList();
    }

    @Override
    public List<ApplicationResponse> getApplicationForJob(
            Long jobId) {

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

        /*
         * An employer account may exist before a company has been created.
         * In that situation there cannot be any company applications yet.
         *
         * The Company Service now returns 404 when no company belongs to
         * this user. Treat that as an empty application list rather than
         * allowing it to become a 500 from the application service.
         */
        CompanyResponse company;

        try {
            company = companyServiceClient.getMyCompany(userId);
        } catch (Exception e) {
            if (isCompanyNotFoundException(e)) {
                return List.of();
            }

            throw e;
        }

        if (company == null || company.getId() == null) {
            return List.of();
        }

        // Get the real company ID
        Long companyId = company.getId();

        // If no filter was provided, create an empty filter
        if (filter == null) {
            filter = new CompanyApplicationFilterRequest();
        }

        // Build sorting
        Sort sort = buildSort(filter.getSortBy());

        // Find applications belonging to this company
        return applicationRepository.findAll(
                        ApplicationSpecification.forCompanyWithFilters(
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

    /**
     * Determines whether an exception from the Company Service represents
     * the normal "this employer does not have a company yet" situation.
     *
     * Feign/HTTP clients can wrap the original 404 in several different
     * exception types, so the complete cause chain is inspected.
     */
    private boolean isCompanyNotFoundException(Throwable throwable) {

        Throwable current = throwable;

        while (current != null) {

            String message = current.getMessage();

            if (message != null) {

                String lowerMessage =
                        message.toLowerCase();

                if (lowerMessage.contains("404")
                        || lowerMessage.contains("not found")
                        || lowerMessage.contains("no company found")
                        || lowerMessage.contains("company not found")) {

                    return true;
                }
            }

            current = current.getCause();
        }

        return false;
    }

    @Override
    public ApplicationResponse updateStatus(
            Long applicationId,
            Long employerId,
            ApplicationStatus status
    ) throws Exception {

        Application application =
                getApplicationEntity(applicationId);

        assertEmployer(application, employerId);

        if (application.getStatus() ==
                ApplicationStatus.WITHDRAWN) {

            throw new Exception(
                    "Candidate has already withdrawn"
            );
        }

        if (status == null) {
            throw new Exception(
                    "Application status is required"
            );
        }

        application.setStatus(status);

        Application savedApplication =
                applicationRepository.save(application);

        return buildFullResponse(savedApplication);
    }

    @Override
    public ApplicationResponse withdraw(
            Long applicationId,
            Long candidateId,
            WithdrawApplicationRequest req
    ) throws Exception {

        Application application =
                getApplicationEntity(applicationId);

        assertCandidate(application, candidateId);

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

        if (req != null) {
            application.setWithdrawnReason(
                    req.getReason()
            );
        }

        Application savedApplication =
                applicationRepository.save(application);

        return buildFullResponse(savedApplication);
    }

    @Override
    public ApplicationResponse toggleStar(
            Long applicationId,
            Long employerId
    ) throws Exception {

        Application application =
                getApplicationEntity(applicationId);

        assertEmployer(application, employerId);

        application.setIsStarred(
                !Boolean.TRUE.equals(
                        application.getIsStarred()
                )
        );

        Application savedApplication =
                applicationRepository.save(application);

        return buildFullResponse(savedApplication);
    }

    @Override
    public void deleteApplication(
            Long applicationId,
            Long candidateId
    ) throws Exception {

        Application application =
                getApplicationEntity(applicationId);

        assertCandidate(application, candidateId);

        applicationRepository.delete(application);
    }

    @Override
    public Application getApplicationEntity(Long id)
            throws Exception {

        return applicationRepository
                .findById(id)
                .orElseThrow(
                        () -> new Exception(
                                "Application not found"
                        )
                );
    }

    public ApplicationResponse buildFullResponse(
            Application application) {

        // Get job
        JobResponse job =
                jobServiceClient.getJobById(
                        application.getJobId()
                );

        if (job == null) {
            throw new RuntimeException(
                    "Job not found: " +
                            application.getJobId()
            );
        }

        // Get candidate
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

        // Get resume
        ResumeResponse resume =
                resumeServiceClient.getResumeById(
                        application.getResumeId(),
                        application.getCandidateId()
                );

        if (resume == null) {
            throw new RuntimeException(
                    "Resume not found: " +
                            application.getResumeId()
            );
        }

        /*
         * Company information is already part of JobResponse.
         * Do not make another Company Service call here.
         */
        CompanyResponse company =
                job.getCompany();

        // Build complete response
        return ApplicationMapper.toResponse(
                application,
                job,
                company,
                candidate,
                resume
        );
    }

    private void assertCandidate(
            Application application,
            Long candidateId
    ) throws Exception {

        if (!Objects.equals(
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

        if (!Objects.equals(
                application.getEmployerId(),
                employerId
        )) {
            throw new Exception(
                    "You are not the employer for this application"
            );
        }
    }

    private Sort buildSort(String sortBy) {

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