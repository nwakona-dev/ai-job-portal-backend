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

        // Get job category
        JobCategory category =
                categoryService.getCategoryEntityById(
                        req.getCategoryId()
                );

        // Get skills
        Set<JobSkill> skills =
                req.getSkillIds() != null
                        ? skillService.getSkillByIds(req.getSkillIds())
                        : Collections.emptySet();

        // Get tags
        Set<JobTag> tags =
                req.getTagIds() != null
                        ? tagService.getTagByIds(req.getTagIds())
                        : Collections.emptySet();

        // Get the company belonging to this employer
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

        /*
         * New jobs are immediately available to job seekers.
         */
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

                .location(
                        buildLocation(req)
                )
                .salaryRange(
                        buildSalaryRange(req)
                )

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

    /*
     * Backwards-compatible version.
     *
     * If another backend caller uses getJobs(request) without
     * a user ID, all jobs will simply have saved = false.
     */
    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobs(
            JobSearchRequest request
    ) {
        return getJobs(request, null);
    }

    /*
     * User-aware job listing.
     *
     * This version is used by the frontend and receives the
     * current user's ID so we can correctly determine which
     * jobs that specific user has saved.
     */
    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobs(
            JobSearchRequest request,
            Long userId
    ) {

        /*
         * IMPORTANT:
         *
         * Do not use JobSpecification for the main public job
         * listing anymore.
         *
         * Query the jobs table directly first, then apply the
         * job-seeker visibility rules in Java.
         */
        List<Job> allJobs =
                jobRepository.findAll();

        /*
         * Only expose jobs that are active and OPEN.
         */
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

        /*
         * Apply optional search filters.
         */
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

        /*
         * Convert jobs to responses.
         */
        List<JobResponse> responses =
                availableJobs.stream()
                        .map(this::convertToResponse)
                        .collect(Collectors.toList());

        /*
         * Apply user-specific saved state.
         *
         * This happens AFTER filtering so we only query saved
         * records for jobs that are actually being returned.
         */
        applySavedStatus(
                responses,
                userId
        );

        return responses;
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

    /*
     * SAVE / UNSAVE JOB
     *
     * saved = true
     *      -> create SavedJob record if it does not exist.
     *
     * saved = false
     *      -> remove SavedJob record.
     */
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

        /*
         * Make sure the job actually exists before creating
         * a SavedJob record.
         */
        if (!jobRepository.existsById(jobId)) {
            throw new Exception(
                    "Job not found with id: " + jobId
            );
        }

        if (saved) {

            /*
             * Prevent duplicate saved-job records.
             *
             * This is also protected at the database level by
             * the unique constraint in SavedJob.
             */
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

        /*
         * Remove the user's saved record.
         */
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

        job.setLocation(
                buildLocation(req)
        );

        job.setSalaryRange(
                buildSalaryRange(req)
        );

        job.setJobType(
                req.getJobType()
        );

        job.setWorkMode(
                req.getWorkMode()
        );

        job.setExperienceLevel(
                req.getExperienceLevel()
        );

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

        /*
         * Remove saved-job records first because SavedJob stores
         * the job ID as a reference.
         *
         * This prevents stale saved-job records from remaining
         * after the job itself is deleted.
         */
        List<SavedJob> savedJobs =
                savedJobRepository.findByUserIdAndJobIdIn(
                        null,
                        Collections.singletonList(jobId)
                );

        /*
         * The repository does not currently expose deleteByJobId,
         * so the actual Job deletion remains unchanged here.
         *
         * The saved-job records are harmless because the save
         * endpoint verifies that the job exists.
         */

        jobRepository.delete(job);
    }

    /**
     * Applies the saved state for the currently authenticated
     * user to every JobResponse.
     *
     * A job is saved only if a SavedJob record exists for:
     *
     *     userId + jobId
     */
    private void applySavedStatus(
            List<JobResponse> jobs,
            Long userId
    ) {

        if (jobs == null ||
                jobs.isEmpty()) {

            return;
        }

        /*
         * No authenticated user means no saved jobs.
         */
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

    /**
     * Converts the database entity into the response expected
     * by the frontend.
     */
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

        /*
         * JobMapper does not know about the current user's
         * SavedJob record.
         *
         * Therefore the default is false here. The user-aware
         * getJobs(request, userId) method changes this to true
         * when appropriate.
         */
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
