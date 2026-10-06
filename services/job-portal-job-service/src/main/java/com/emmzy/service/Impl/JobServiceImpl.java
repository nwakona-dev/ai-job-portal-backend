package com.emmzy.service.Impl;

import com.emmzy.client.CompanyServiceClient;
import com.emmzy.domain.JobStatus;
import com.emmzy.dto.CompanyResponse;
import com.emmzy.dto.JobRequest;
import com.emmzy.dto.JobResponse;
import com.emmzy.mapper.JobMapper;
import com.emmzy.model.Job;
import com.emmzy.model.JobCategory;
import com.emmzy.model.JobSkill;
import com.emmzy.model.JobTag;
import com.emmzy.model.SavedJob;
import com.emmzy.model.embeddable.JobLocation;
import com.emmzy.model.embeddable.SalaryRange;
import com.emmzy.payload.JobSearchRequest;
import com.emmzy.repository.JobRepository;
import com.emmzy.repository.JobSpecification;
import com.emmzy.repository.SavedJobRepository;
import com.emmzy.service.JobCategoryService;
import com.emmzy.service.JobService;
import com.emmzy.service.JobSkillService;
import com.emmzy.service.JobTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobCategoryService categoryService;
    private final JobSkillService skillService;
    private final JobTagService tagService;
    private final CompanyServiceClient companyServiceClient;
    private final SavedJobRepository savedJobRepository;


    // CREATE JOB
    @Override
    public JobResponse createJob(
            Long employerId,
            JobRequest req
    ) throws Exception {

        JobCategory category =
                categoryService.getCategoryEntityById(
                        req.getCategoryId()
                );

        Set<JobSkill> skills =
                req.getSkillIds() != null
                        ? skillService.getSkillByIds(
                        req.getSkillIds()
                )
                        : Collections.emptySet();

        Set<JobTag> tags =
                req.getTagIds() != null
                        ? tagService.getTagByIds(
                        req.getTagIds()
                )
                        : Collections.emptySet();

        CompanyResponse company =
                companyServiceClient.getMyCompany(
                        employerId
                );

        if (company == null ||
                company.getId() == null) {

            throw new Exception(
                    "Employer does not have a company"
            );
        }

        Long companyId = company.getId();

        Job job = Job.builder()
                .title(req.getTitle())
                .description(req.getDescription())
                .requirements(req.getRequirements())
                .responsibilities(req.getResponsibilities())
                .benefits(req.getBenefits())
                .companyId(companyId)
                .employerId(employerId)
                .category(category)
                .skills(skills)
                .tags(tags)
                .location(buildLocation(req))
                .salaryRange(buildSalaryRange(req))
                .jobType(req.getJobType())
                .workMode(req.getWorkMode())
                .experienceLevel(req.getExperienceLevel())
                .openings(
                        req.getOpenings() != null
                                ? req.getOpenings()
                                : 1
                )
                .applicationDeadline(req.getApplicationDeadline())
                .expiredAt(req.getExpiredAt())
                .active(true)
                .status(JobStatus.DRAFT)
                .deleted(false)
                .build();

        Job savedJob =
                jobRepository.save(job);

        return convertToResponse(savedJob);
    }

    // GET JOB BY ID
    @Override
    public JobResponse getJobById(Long id)
            throws Exception {

        Job job =
                jobRepository.findById(id)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found with id: " +
                                                id
                                )
                        );

        return convertToResponse(job);
    }

    // GET ALL JOBS
    @Override
    public List<JobResponse> getJobs(
            JobSearchRequest request
    ) {

        List<Job> jobs =
                jobRepository.findAll(
                        JobSpecification.build(request)
                );

        return jobs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }


    // GET JOBS BY COMPANY
    @Override
    public List<JobResponse> getJobsByCompany(
            Long companyId
    ) {

        List<Job> jobs =
                jobRepository
                        .findByCompanyIdAndDeletedFalse(
                                companyId
                        );

        return jobs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // GET JOBS BY EMPLOYER
    @Override
    public List<JobResponse> getJobsByEmployer(
            Long employerId
    ) {

        List<Job> jobs =
                jobRepository
                        .findByEmployerIdAndDeletedFalse(
                                employerId
                        );

        return jobs.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // UPDATE JOB
    @Override
    public JobResponse updateJob(
            Long jobId,
            Long employerId,
            JobRequest req
    ) throws Exception {

        Job job =
                jobRepository.findById(jobId)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found with id: " +
                                                jobId
                                )
                        );

        assertEmployer(
                job,
                employerId
        );

        if (Boolean.TRUE.equals(job.getDeleted())) {

            throw new Exception(
                    "Cannot update a deleted job"
            );
        }

        JobCategory category =
                categoryService.getCategoryEntityById(
                        req.getCategoryId()
                );

        Set<JobSkill> skills =
                req.getSkillIds() != null
                        ? skillService.getSkillByIds(
                        req.getSkillIds()
                )
                        : Collections.emptySet();

        Set<JobTag> tags =
                req.getTagIds() != null
                        ? tagService.getTagByIds(
                        req.getTagIds()
                )
                        : Collections.emptySet();

        job.setTitle(req.getTitle());
        job.setDescription(req.getDescription());
        job.setRequirements(req.getRequirements());
        job.setResponsibilities(req.getResponsibilities());
        job.setBenefits(req.getBenefits());
        job.setCategory(category);
        job.setSkills(skills);
        job.setTags(tags);
        job.setLocation(buildLocation(req));
        job.setSalaryRange(buildSalaryRange(req));
        job.setJobType(req.getJobType());
        job.setWorkMode(req.getWorkMode());
        job.setExperienceLevel(req.getExperienceLevel());
        job.setOpenings(
                req.getOpenings() != null
                        ? req.getOpenings()
                        : job.getOpenings()
        );
        job.setApplicationDeadline(req.getApplicationDeadline());
        job.setExpiredAt(req.getExpiredAt());

        return convertToResponse(jobRepository.save(job));
    }

    // PUBLISH JOB
    @Override
    public JobResponse publishJob(
            Long jobId,
            Long employerId
    ) throws Exception {

        Job job =
                jobRepository.findById(jobId)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found"
                                )
                        );

        assertEmployer(
                job,
                employerId
        );

        if (Boolean.TRUE.equals(job.getDeleted())) {

            throw new Exception(
                    "Cannot publish a deleted job"
            );
        }

        if (job.getStatus() != JobStatus.DRAFT) {

            throw new Exception(
                    "Only draft jobs can be published"
            );
        }

        job.setStatus(JobStatus.OPEN);
        job.setPublishedAt(LocalDateTime.now());
        job.setActive(true);

        return convertToResponse(jobRepository.save(job));
    }

    // CLOSE JOB
    @Override
    public JobResponse closeJob(
            Long jobId,
            Long employerId
    ) throws Exception {

        Job job =
                jobRepository.findById(jobId)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found"
                                )
                        );

        assertEmployer(
                job,
                employerId
        );

        if (Boolean.TRUE.equals(job.getDeleted())) {

            throw new Exception(
                    "Job has already been deleted"
            );
        }

        job.setStatus(JobStatus.CLOSED);
        job.setClosedAt(LocalDateTime.now());
        job.setActive(false);

        return convertToResponse(jobRepository.save(job));
    }

    // DELETE JOB - SOFT DELETE
    @Override
    public void deleteJob(
            Long jobId,
            Long employerId
    ) throws Exception {

        Job job =
                jobRepository.findById(jobId)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found"
                                )
                        );

        assertEmployer(
                job,
                employerId
        );

        if (Boolean.TRUE.equals(job.getDeleted())) {
            return;
        }

        job.setDeleted(true);
        job.setActive(false);
        job.setStatus(JobStatus.CLOSED);
        job.setClosedAt(LocalDateTime.now());

        jobRepository.save(job);
    }

    // GET ALL JOBS - ADMIN
    @Override
    public List<JobResponse> getAllJobsAdmin() {
//        Admin can see deleted jobs.
        return jobRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // SAVE / UNSAVE JOB
    @Override
    public void toggleSaveJob(
            Long jobId,
            Long userId,
            boolean saved
    ) throws Exception {

        Job job =
                jobRepository.findById(jobId)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found with id: " +
                                                jobId
                                )
                        );

//        Deleted jobs cannot be saved.
        if (Boolean.TRUE.equals(job.getDeleted())) {

            throw new Exception(
                    "This job is no longer available"
            );
        }

//        SAVE JOB
        if (saved) {

            boolean alreadySaved =
                    savedJobRepository
                            .existsByUserIdAndJobId(
                                    userId,
                                    jobId
                            );

            if (!alreadySaved) {

                SavedJob savedJob =
                        SavedJob.builder()
                                .userId(userId)
                                .jobId(jobId)
                                .build();

                savedJobRepository.save(savedJob);
            }

        }
//        UNSAVE JOB
        else {

            savedJobRepository
                    .deleteByUserIdAndJobId(
                            userId,
                            jobId
                    );
        }
    }

    // CONVERT JOB TO RESPONSE
    private JobResponse convertToResponse(
            Job savedJob
    ) {

        CompanyResponse companyResponse =
                companyServiceClient.getCompanyById(
                        savedJob.getCompanyId()
                );

        return JobMapper.toResponse(
                savedJob,
                companyResponse
        );
    }

    // GET SAVED JOB IDS
    @Override
    public List<Long> getSavedJobIds(
            Long userId
    ) {

        return savedJobRepository
                .findByUserId(userId)
                .stream()
                .map(SavedJob::getJobId)
                .toList();
    }

    // BUILD SALARY RANGE
    private SalaryRange buildSalaryRange(
            JobRequest req
    ) {

        return SalaryRange.builder()
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .build();
    }

    // BUILD LOCATION
    private JobLocation buildLocation(
            JobRequest req
    ) {

        return JobLocation.builder()
                .address(req.getAddress())
                .city(req.getCity())
                .state(req.getState())
                .country(req.getCountry())
                .zipCode(req.getZipCode())
                .build();
    }

    // CHECK EMPLOYER OWNERSHIP
    private void assertEmployer(
            Job job,
            Long employerId
    ) throws Exception {

        if (!job.getEmployerId().equals(employerId)) {

            throw new Exception(
                    "Unauthorized: Employer does not own this job"
            );
        }
    }
}