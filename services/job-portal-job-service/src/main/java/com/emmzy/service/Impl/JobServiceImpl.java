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

    @Override
    public JobResponse createJob(
            Long employerId,
            JobRequest req
    ) throws Exception {

        if (employerId == null) {
            throw new Exception(
                    "Employer ID is required"
            );
        }

        JobCategory category =
                categoryService.getCategoryEntityById(
                        req.getCategoryId()
                );

        Set<JobSkill> skills =
                req.getSkillIds() != null
                        ? skillService.getSkillByIds(req.getSkillIds())
                        : Collections.emptySet();

        Set<JobTag> tags =
                req.getTagIds() != null
                        ? tagService.getTagByIds(req.getTagIds())
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

        LocalDateTime publishedAt =
                LocalDateTime.now();

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
                .applicationDeadline(
                        req.getApplicationDeadline()
                )
                .expiredAt(
                        req.getExpiredAt()
                )
                .active(true)
                .status(JobStatus.OPEN)
                .publishedAt(publishedAt)
                .build();

        Job savedJob =
                jobRepository.save(job);

        return convertToResponse(savedJob);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(
            Long id
    ) throws Exception {

        Job job =
                jobRepository.findById(id)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found with id: " + id
                                )
                        );

        return convertToResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobs(
            JobSearchRequest request
    ) {
        return getJobs(request, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobs(
            JobSearchRequest request,
            Long userId
    ) {

        List<Job> allJobs =
                jobRepository.findAll();

        List<Job> availableJobs =
                allJobs.stream()
                        .filter(job ->
                                Boolean.TRUE.equals(
                                        job.getActive()
                                )
                        )
                        .filter(job ->
                                job.getStatus() ==
                                        JobStatus.OPEN
                        )
                        .collect(Collectors.toList());

        if (request != null) {

            if (request.getKeyword() != null &&
                    !request.getKeyword().isBlank()) {

                String keyword =
                        request.getKeyword()
                                .trim()
                                .toLowerCase();

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        containsIgnoreCase(
                                                job.getTitle(),
                                                keyword
                                        )
                                                ||
                                                containsIgnoreCase(
                                                        job.getDescription(),
                                                        keyword
                                                )
                                                ||
                                                containsIgnoreCase(
                                                        job.getRequirements(),
                                                        keyword
                                                )
                                )
                                .collect(Collectors.toList());
            }

            if (request.getJobType() != null) {

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        job.getJobType() ==
                                                request.getJobType()
                                )
                                .collect(Collectors.toList());
            }

            if (request.getWorkMode() != null) {

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        job.getWorkMode() ==
                                                request.getWorkMode()
                                )
                                .collect(Collectors.toList());
            }

            if (request.getExperienceLevel() != null) {

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        job.getExperienceLevel() ==
                                                request.getExperienceLevel()
                                )
                                .collect(Collectors.toList());
            }

            if (request.getCompanyId() != null) {

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        request.getCompanyId()
                                                .equals(
                                                        job.getCompanyId()
                                                )
                                )
                                .collect(Collectors.toList());
            }

            if (request.getCategoryId() != null) {

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        job.getCategory() != null
                                                &&
                                                request.getCategoryId()
                                                        .equals(
                                                                job.getCategory()
                                                                        .getId()
                                                        )
                                )
                                .collect(Collectors.toList());
            }

            if (request.getLocation() != null &&
                    !request.getLocation().isBlank()) {

                String location =
                        request.getLocation()
                                .trim()
                                .toLowerCase();

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        job.getLocation() != null
                                                &&
                                                (
                                                        containsIgnoreCase(
                                                                job.getLocation()
                                                                        .getCity(),
                                                                location
                                                        )
                                                                ||
                                                                containsIgnoreCase(
                                                                        job.getLocation()
                                                                                .getState(),
                                                                        location
                                                                )
                                                                ||
                                                                containsIgnoreCase(
                                                                        job.getLocation()
                                                                                .getCountry(),
                                                                        location
                                                                )
                                                )
                                )
                                .collect(Collectors.toList());
            }

            if (request.getMinSalary() != null) {

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        job.getSalaryRange() != null
                                                &&
                                                job.getSalaryRange()
                                                        .getMinSalary() != null
                                                &&
                                                job.getSalaryRange()
                                                        .getMinSalary()
                                                        .compareTo(
                                                                request.getMinSalary()
                                                        ) >= 0
                                )
                                .collect(Collectors.toList());
            }

            if (request.getMaxSalary() != null) {

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        job.getSalaryRange() != null
                                                &&
                                                job.getSalaryRange()
                                                        .getMaxSalary() != null
                                                &&
                                                job.getSalaryRange()
                                                        .getMaxSalary()
                                                        .compareTo(
                                                                request.getMaxSalary()
                                                        ) <= 0
                                )
                                .collect(Collectors.toList());
            }

            if (request.getMinOpenings() != null) {

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        job.getOpenings() != null
                                                &&
                                                job.getOpenings()
                                                        >= request.getMinOpenings()
                                )
                                .collect(Collectors.toList());
            }

            if (request.getMaxOpenings() != null) {

                availableJobs =
                        availableJobs.stream()
                                .filter(job ->
                                        job.getOpenings() != null
                                                &&
                                                job.getOpenings()
                                                        <= request.getMaxOpenings()
                                )
                                .collect(Collectors.toList());
            }
        }

        List<JobResponse> responses =
                availableJobs.stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList());

        applySavedStatus(
                responses,
                userId
        );

        return responses;
    }

    /*
     * Returns every job saved by the current user.
     *
     * This uses the SavedJob records as the source of truth,
     * then loads the corresponding Job entities.
     */
    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getSavedJobs(
            Long userId
    ) {

        if (userId == null) {
            return Collections.emptyList();
        }

        List<SavedJob> savedJobs =
                savedJobRepository.findByUserIdAndJobIdIn(
                        userId,
                        jobRepository.findAll()
                                .stream()
                                .map(Job::getId)
                                .filter(id -> id != null)
                                .collect(Collectors.toList())
                );

        if (savedJobs.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> savedJobIds =
                savedJobs.stream()
                        .map(SavedJob::getJobId)
                        .filter(id -> id != null)
                        .distinct()
                        .collect(Collectors.toList());

        if (savedJobIds.isEmpty()) {
            return Collections.emptyList();
        }

        return jobRepository
                .findAllById(savedJobIds)
                .stream()
                .map(this::convertToResponse)
                .peek(job ->
                        job.setSaved(true)
                )
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByCompany(
            Long companyId
    ) {

        if (companyId == null) {
            return Collections.emptyList();
        }

        return jobRepository
                .findByCompanyId(companyId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByEmployer(
            Long employerId
    ) {

        if (employerId == null) {
            return Collections.emptyList();
        }

        return jobRepository
                .findByEmployerId(employerId)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobsAdmin() {

        return jobRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean toggleSaveJob(
            Long jobId,
            Long userId,
            boolean saved
    ) throws Exception {

        if (userId == null) {
            throw new IllegalArgumentException(
                    "User ID is required to save a job"
            );
        }

        if (jobId == null) {
            throw new IllegalArgumentException(
                    "Job ID is required"
            );
        }

        if (!jobRepository.existsById(jobId)) {
            throw new Exception(
                    "Job not found with id: " + jobId
            );
        }

        if (saved) {

            if (!savedJobRepository.existsByUserIdAndJobId(
                    userId,
                    jobId
            )) {

                SavedJob savedJob =
                        SavedJob.builder()
                                .userId(userId)
                                .jobId(jobId)
                                .build();

                savedJobRepository.save(savedJob);
            }

            return true;
        }

        savedJobRepository.deleteByUserIdAndJobId(
                userId,
                jobId
        );

        return false;
    }

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
                                        "Job not found with id: " + jobId
                                )
                        );

        assertEmployer(
                job,
                employerId
        );

        JobCategory category =
                categoryService.getCategoryEntityById(
                        req.getCategoryId()
                );

        Set<JobSkill> skills =
                req.getSkillIds() != null
                        ? skillService.getSkillByIds(req.getSkillIds())
                        : Collections.emptySet();

        Set<JobTag> tags =
                req.getTagIds() != null
                        ? tagService.getTagByIds(req.getTagIds())
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

        job.setApplicationDeadline(
                req.getApplicationDeadline()
        );

        job.setExpiredAt(
                req.getExpiredAt()
        );

        return convertToResponse(
                jobRepository.save(job)
        );
    }

    @Override
    public JobResponse publishJob(
            Long jobId,
            Long employerId
    ) throws Exception {

        Job job =
                jobRepository.findById(jobId)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found with id: " + jobId
                                )
                        );

        assertEmployer(
                job,
                employerId
        );

        if (job.getStatus() == JobStatus.CLOSED ||
                job.getStatus() == JobStatus.EXPIRED) {

            throw new Exception(
                    "Job with id " +
                            jobId +
                            " is expired or closed and cannot be published"
            );
        }

        job.setStatus(
                JobStatus.OPEN
        );

        job.setPublishedAt(
                LocalDateTime.now()
        );

        job.setActive(true);

        return convertToResponse(
                jobRepository.save(job)
        );
    }

    @Override
    public JobResponse closeJob(
            Long jobId,
            Long employerId
    ) throws Exception {

        Job job =
                jobRepository.findById(jobId)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found with id: " + jobId
                                )
                        );

        assertEmployer(
                job,
                employerId
        );

        job.setStatus(
                JobStatus.CLOSED
        );

        job.setClosedAt(
                LocalDateTime.now()
        );

        job.setActive(false);

        return convertToResponse(
                jobRepository.save(job)
        );
    }

    @Override
    public void deleteJob(
            Long jobId,
            Long employerId
    ) throws Exception {

        Job job =
                jobRepository.findById(jobId)
                        .orElseThrow(
                                () -> new Exception(
                                        "Job not found with id: " + jobId
                                )
                        );

        assertEmployer(
                job,
                employerId
        );

        List<SavedJob> savedJobs =
                savedJobRepository.findByUserIdAndJobIdIn(
                        null,
                        Collections.singletonList(jobId)
                );

        jobRepository.delete(job);
    }

    private void applySavedStatus(
            List<JobResponse> jobs,
            Long userId
    ) {

        if (jobs == null ||
                jobs.isEmpty()) {

            return;
        }

        if (userId == null) {

            jobs.forEach(job ->
                    job.setSaved(false)
            );

            return;
        }

        List<Long> jobIds =
                jobs.stream()
                        .map(JobResponse::getId)
                        .filter(id -> id != null)
                        .collect(Collectors.toList());

        if (jobIds.isEmpty()) {
            return;
        }

        Set<Long> savedJobIds =
                savedJobRepository
                        .findByUserIdAndJobIdIn(
                                userId,
                                jobIds
                        )
                        .stream()
                        .map(SavedJob::getJobId)
                        .collect(Collectors.toSet());

        jobs.forEach(job ->
                job.setSaved(
                        savedJobIds.contains(
                                job.getId()
                        )
                )
        );
    }

    private JobResponse convertToResponse(
            Job savedJob
    ) {

        CompanyResponse companyResponse = null;

        if (savedJob.getCompanyId() != null) {

            companyResponse =
                    companyServiceClient.getCompanyById(
                            savedJob.getCompanyId()
                    );
        }

        JobResponse response =
                JobMapper.toResponse(
                        savedJob,
                        companyResponse
                );

        response.setSaved(false);

        return response;
    }

    private SalaryRange buildSalaryRange(
            JobRequest req
    ) {

        return SalaryRange.builder()
                .minSalary(req.getMinSalary())
                .maxSalary(req.getMaxSalary())
                .build();
    }

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

    private boolean containsIgnoreCase(
            String value,
            String search
    ) {

        return value != null &&
                search != null &&
                value.toLowerCase().contains(search);
    }

    private void assertEmployer(
            Job job,
            Long employerId
    ) throws Exception {

        if (job.getEmployerId() == null ||
                employerId == null ||
                !job.getEmployerId().equals(employerId)) {

            throw new Exception(
                    "Unauthorized: Employer does not own job with id: "
                            + job.getId()
            );
        }
    }


}
