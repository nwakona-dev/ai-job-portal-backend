package com.emmzy.service.impl;

import com.emmzy.client.ApplicationServiceClient;
import com.emmzy.client.JobServiceClient;
import com.emmzy.client.ResumeServiceClient;
import com.emmzy.client.UserServiceClient;
import com.emmzy.config.AIConfig;
import com.emmzy.dto.ai.JobMatchAIResult;
import com.emmzy.dto.request.CandidateRankingRequest;
import com.emmzy.dto.request.JobMatchRequest;
import com.emmzy.dto.request.ResumeAnalysisRequest;
import com.emmzy.dto.response.CandidateRankingResponse;
import com.emmzy.dto.response.CandidateRecommendationResponse;
import com.emmzy.dto.response.JobMatchingResponse;
import com.emmzy.dto.response.JobRecommendationResponse;
import com.emmzy.dto.response.ResumeAnalysisResponse;
import com.emmzy.dto.response.SkillMatchResponse;
import com.emmzy.exception.ExternalServiceException;
import com.emmzy.model.CandidateData;
import com.emmzy.model.JobData;
import com.emmzy.model.SkillData;
import com.emmzy.service.AIRecommendationService;
import com.emmzy.service.GeminiService;
import com.emmzy.service.SkillMatchingService;
import com.emmzy.util.MatchingScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIRecommendationServiceImpl
        implements AIRecommendationService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AIRecommendationServiceImpl.class
            );

    private final JobServiceClient jobServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final UserServiceClient userServiceClient;
    private final ResumeServiceClient resumeServiceClient;

    /*
     * Rule-based matching service.
     *
     * This is still used by:
     *
     * - Job recommendations
     * - Skill matching
     * - Candidate ranking
     * - Candidate recommendations
     *
     * matchJob() uses REAL OpenAI instead.
     */
    private final SkillMatchingService skillMatchingService;

    private final AIConfig aiConfig;

    /*
     * REAL OPENAI SERVICE.
     *
     * matchJob() sends the complete job and latest resume
     * to OpenAI through this service.
     */
    private final GeminiService geminiService;


    // ================================================================
    // JOB RECOMMENDATIONS
    // ================================================================

    @Override
    public List<JobRecommendationResponse> getJobRecommendations(
            Long candidateId) {

        CandidateData candidate =
                getCandidate(candidateId);

        if (candidate == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> jobs =
                jobServiceClient.getJobs();

        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }

        return jobs.stream()
                .filter(Objects::nonNull)
                .filter(this::isRecommendationEligibleJob)
                .map(this::convertToJobData)
                .filter(Objects::nonNull)
                .map(job ->
                        buildJobRecommendation(
                                candidate,
                                job
                        )
                )
                .filter(response ->
                        response.getMatchScore() != null
                                && response.getMatchScore()
                                >= aiConfig
                                .getRecommendations()
                                .getMinimumScore()
                )
                .sorted(
                        Comparator.comparing(
                                JobRecommendationResponse
                                        ::getMatchScore,
                                Comparator.reverseOrder()
                        )
                )
                .limit(
                        Math.max(
                                0,
                                aiConfig
                                        .getRecommendations()
                                        .getMaxResults()
                        )
                )
                .collect(Collectors.toList());
    }


    // ================================================================
    // REAL OPENAI JOB MATCHING
    // ================================================================

    /**
     * Matches a candidate against a specific job using REAL OPENAI.
     *
     * OpenAI generates:
     *
     * - matchScore
     * - matchingSkills
     * - missingSkills
     * - explanation
     *
     * The local application only calculates matchLevel from
     * the OpenAI-generated score.
     */
    @Override
    public JobMatchingResponse matchJob(
            JobMatchRequest request) {

        // ------------------------------------------------------------
        // STEP 1: VALIDATE REQUEST
        // ------------------------------------------------------------

        if (request == null
                || request.getJobId() == null
                || request.getCandidateId() == null) {

            return JobMatchingResponse.builder()
                    .matchScore(0.0)
                    .matchLevel("LOW")
                    .matchingSkills(
                            Collections.emptyList()
                    )
                    .missingSkills(
                            Collections.emptyList()
                    )
                    .explanation(
                            "Job ID and candidate ID are required."
                    )
                    .build();
        }

        Long jobId =
                request.getJobId();

        Long candidateId =
                request.getCandidateId();


        // ------------------------------------------------------------
        // STEP 2: FETCH JOB
        // ------------------------------------------------------------

        Map<String, Object> rawJob;

        try {

            log.info(
                    "Fetching job {} from Job Service",
                    jobId
            );

            rawJob =
                    jobServiceClient.getJobById(
                            jobId
                    );

            log.debug(
                    "Job Service response for job {}: {}",
                    jobId,
                    rawJob
            );

        } catch (ExternalServiceException exception) {

            log.error(
                    "Job Service failed while fetching job {}",
                    jobId,
                    exception
            );

            throw exception;

        } catch (Exception exception) {

            log.error(
                    "Unexpected error while fetching job {}",
                    jobId,
                    exception
            );

            throw new ExternalServiceException(
                    "Unable to retrieve job "
                            + jobId
                            + " from Job Service. Cause: "
                            + getRootCauseMessage(exception),
                    exception
            );
        }


        if (rawJob == null || rawJob.isEmpty()) {

            log.warn(
                    "Job Service returned empty data for job {}",
                    jobId
            );

            return JobMatchingResponse.builder()
                    .jobId(jobId)
                    .candidateId(candidateId)
                    .matchScore(0.0)
                    .matchLevel("LOW")
                    .matchingSkills(
                            Collections.emptyList()
                    )
                    .missingSkills(
                            Collections.emptyList()
                    )
                    .explanation(
                            "Job information could not be found."
                    )
                    .build();
        }


        // ------------------------------------------------------------
        // STEP 3: FETCH CANDIDATE
        // ------------------------------------------------------------

        CandidateData candidate;

        try {

            log.info(
                    "Fetching candidate {}",
                    candidateId
            );

            candidate =
                    getCandidate(candidateId);

            if (candidate != null) {

                log.info(
                        "Candidate {} loaded successfully",
                        candidateId
                );
            }

        } catch (ExternalServiceException exception) {

            log.error(
                    "Failed to retrieve candidate {}",
                    candidateId,
                    exception
            );

            throw exception;

        } catch (Exception exception) {

            log.error(
                    "Unexpected error while retrieving candidate {}",
                    candidateId,
                    exception
            );

            throw new ExternalServiceException(
                    "Unable to retrieve candidate "
                            + candidateId
                            + ". Cause: "
                            + getRootCauseMessage(exception),
                    exception
            );
        }


        if (candidate == null) {

            log.warn(
                    "Candidate {} was not found",
                    candidateId
            );

            return JobMatchingResponse.builder()
                    .jobId(jobId)
                    .candidateId(candidateId)
                    .matchScore(0.0)
                    .matchLevel("LOW")
                    .matchingSkills(
                            Collections.emptyList()
                    )
                    .missingSkills(
                            Collections.emptyList()
                    )
                    .explanation(
                            "Candidate information could not be found."
                    )
                    .build();
        }


        // ------------------------------------------------------------
        // STEP 4: FETCH RESUMES
        // ------------------------------------------------------------

        List<Map<String, Object>> resumes;

        try {

            log.info(
                    "Fetching resumes for candidate {}",
                    candidateId
            );

            resumes =
                    resumeServiceClient
                            .getResumesByCandidate(
                                    candidateId
                            );

            log.info(
                    "Resume Service returned {} resumes for candidate {}",
                    resumes != null
                            ? resumes.size()
                            : 0,
                    candidateId
            );

        } catch (ExternalServiceException exception) {

            log.error(
                    "Resume Service failed for candidate {}",
                    candidateId,
                    exception
            );

            throw exception;

        } catch (Exception exception) {

            log.error(
                    "Unexpected error while fetching resumes for candidate {}",
                    candidateId,
                    exception
            );

            throw new ExternalServiceException(
                    "Unable to retrieve resumes for candidate "
                            + candidateId
                            + ". Cause: "
                            + getRootCauseMessage(exception),
                    exception
            );
        }


        // ------------------------------------------------------------
        // STEP 5: SELECT LATEST RESUME
        // ------------------------------------------------------------

        Map<String, Object> latestResume =
                getLatestResume(resumes);

        if (latestResume == null
                || latestResume.isEmpty()) {

            log.warn(
                    "No resume found for candidate {}",
                    candidateId
            );

        } else {

            log.info(
                    "Latest resume selected for candidate {}: {}",
                    candidateId,
                    latestResume
            );
        }


        // ------------------------------------------------------------
        // STEP 6: SEND JOB + RESUME TO REAL OPENAI
        // ------------------------------------------------------------

        JobMatchAIResult aiResult;

        try {

            log.info(
                    "Sending job {} and candidate {} to OpenAI for matching",
                    jobId,
                    candidateId
            );

            /*
             * IMPORTANT:
             *
             * OpenAI receives:
             *
             * 1. The raw job returned by Job Service.
             * 2. The latest resume returned by Resume Service.
             *
             * OpenAIService is responsible for converting these
             * objects into the AI prompt and calling the OpenAI API.
             */
            aiResult =
                    geminiService.analyzeJobMatch(
                            rawJob,
                            latestResume
                    );

            log.info(
                    "OpenAI job matching completed for job {} and candidate {}",
                    jobId,
                    candidateId
            );

            log.debug(
                    "OpenAI result for job {} / candidate {}: {}",
                    jobId,
                    candidateId,
                    aiResult
            );

        } catch (Exception exception) {

            String rootCause =
                    getRootCauseMessage(
                            exception
                    );

            log.error(
                    "OpenAI job matching failed for job {} and candidate {}. Cause: {}",
                    jobId,
                    candidateId,
                    rootCause,
                    exception
            );

            /*
             * Preserve the real OpenAI error.
             *
             * For example:
             *
             * 429 Too Many Requests
             * credit_balance_exhausted
             *
             * This makes debugging much easier than returning
             * only "AI matching failed".
             */
            throw new ExternalServiceException(
                    "OpenAI job matching failed for job "
                            + jobId
                            + " and candidate "
                            + candidateId
                            + ". Cause: "
                            + rootCause,
                    exception
            );
        }


        // ------------------------------------------------------------
        // STEP 7: VALIDATE OPENAI RESULT
        // ------------------------------------------------------------

        if (aiResult == null) {

            log.error(
                    "OpenAI returned null result for job {} and candidate {}",
                    jobId,
                    candidateId
            );

            throw new ExternalServiceException(
                    "OpenAI returned an empty job matching result."
            );
        }


        // ------------------------------------------------------------
        // STEP 8: NORMALIZE SCORE
        // ------------------------------------------------------------

        double score =
                Math.max(
                        0.0,
                        Math.min(
                                100.0,
                                aiResult.getMatchScore()
                        )
                );


        // ------------------------------------------------------------
        // STEP 9: NORMALIZE MATCHING SKILLS
        // ------------------------------------------------------------

        List<String> matchingSkills =
                aiResult.getMatchingSkills() != null
                        ? aiResult.getMatchingSkills()
                        : Collections.emptyList();


        // ------------------------------------------------------------
        // STEP 10: NORMALIZE MISSING SKILLS
        // ------------------------------------------------------------

        List<String> missingSkills =
                aiResult.getMissingSkills() != null
                        ? aiResult.getMissingSkills()
                        : Collections.emptyList();


        // ------------------------------------------------------------
        // STEP 11: NORMALIZE EXPLANATION
        // ------------------------------------------------------------

        String explanation =
                aiResult.getExplanation() != null
                        && !aiResult
                        .getExplanation()
                        .isBlank()
                        ? aiResult.getExplanation()
                        : "AI analysis completed.";


        // ------------------------------------------------------------
        // STEP 12: CALCULATE LOCAL MATCH LEVEL
        // ------------------------------------------------------------

        String matchLevel =
                MatchingScoreCalculator
                        .getMatchLevel(score);


        // ------------------------------------------------------------
        // STEP 13: RETURN RESPONSE
        // ------------------------------------------------------------

        return JobMatchingResponse.builder()
                .jobId(jobId)
                .candidateId(candidateId)
                .matchScore(score)
                .matchLevel(matchLevel)
                .matchingSkills(matchingSkills)
                .missingSkills(missingSkills)
                .explanation(explanation)
                .build();
    }


    // ================================================================
    // SKILL MATCH
    // ================================================================

    @Override
    public SkillMatchResponse getSkillMatch(
            Long candidateId) {

        CandidateData candidate =
                getCandidate(candidateId);

        if (candidate == null) {
            return emptySkillMatch(candidateId);
        }

        List<Map<String, Object>> jobResponses =
                jobServiceClient.getJobs();

        if (jobResponses == null
                || jobResponses.isEmpty()) {

            return emptySkillMatch(candidateId);
        }

        JobData bestJob =
                jobResponses.stream()
                        .filter(Objects::nonNull)
                        .filter(this::isRecommendationEligibleJob)
                        .map(this::convertToJobData)
                        .filter(Objects::nonNull)
                        .max(
                                Comparator.comparingDouble(
                                        job ->
                                                skillMatchingService
                                                        .calculateMatchScore(
                                                                candidate,
                                                                job
                                                        )
                                )
                        )
                        .orElse(null);

        if (bestJob == null) {
            return emptySkillMatch(candidateId);
        }

        double score =
                skillMatchingService
                        .calculateMatchScore(
                                candidate,
                                bestJob
                        );

        return SkillMatchResponse.builder()
                .candidateId(candidateId)
                .matchingSkills(
                        skillMatchingService
                                .getMatchingSkills(
                                        candidate,
                                        bestJob
                                )
                )
                .missingSkills(
                        skillMatchingService
                                .getMissingSkills(
                                        candidate,
                                        bestJob
                                )
                )
                .matchScore(score)
                .build();
    }


    // ================================================================
    // CANDIDATE RANKING
    // ================================================================

    @Override
    public List<CandidateRankingResponse> rankCandidates(
            CandidateRankingRequest request) {

        if (request == null
                || request.getJobId() == null) {

            return Collections.emptyList();
        }

        JobData job =
                convertToJobData(
                        jobServiceClient.getJobById(
                                request.getJobId()
                        )
                );

        if (job == null) {
            return Collections.emptyList();
        }

        List<CandidateData> candidates =
                getCandidatesForJob(
                        request.getJobId()
                );

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<CandidateRankingResponse> rankings =
                candidates.stream()
                        .filter(Objects::nonNull)
                        .map(candidate -> {

                            double score =
                                    skillMatchingService
                                            .calculateMatchScore(
                                                    candidate,
                                                    job
                                            );

                            return CandidateRankingResponse.builder()
                                    .candidateId(
                                            candidate.getId()
                                    )
                                    .matchScore(score)
                                    .explanation(
                                            buildDetailedMatchExplanation(
                                                    score,
                                                    skillMatchingService
                                                            .getMatchingSkills(
                                                                    candidate,
                                                                    job
                                                            ),
                                                    skillMatchingService
                                                            .getMissingSkills(
                                                                    candidate,
                                                                    job
                                                            )
                                            )
                                    )
                                    .build();
                        })
                        .sorted(
                                Comparator.comparing(
                                        CandidateRankingResponse
                                                ::getMatchScore,
                                        Comparator.reverseOrder()
                                )
                        )
                        .collect(Collectors.toList());

        for (int index = 0;
             index < rankings.size();
             index++) {

            rankings.get(index)
                    .setRank(index + 1);
        }

        return rankings;
    }


    // ================================================================
    // CANDIDATE RECOMMENDATIONS
    // ================================================================

    @Override
    public List<CandidateRecommendationResponse>
    getCandidateRecommendations(
            Long jobId) {

        if (jobId == null) {
            return Collections.emptyList();
        }

        JobData job =
                convertToJobData(
                        jobServiceClient.getJobById(jobId)
                );

        if (job == null) {
            return Collections.emptyList();
        }

        return getCandidatesForJob(jobId)
                .stream()
                .filter(Objects::nonNull)
                .map(candidate -> {

                    double score =
                            skillMatchingService
                                    .calculateMatchScore(
                                            candidate,
                                            job
                                    );

                    return CandidateRecommendationResponse.builder()
                            .candidateId(
                                    candidate.getId()
                            )
                            .candidateName(
                                    candidate.getFullName()
                            )
                            .matchScore(score)
                            .explanation(
                                    buildDetailedMatchExplanation(
                                            score,
                                            skillMatchingService
                                                    .getMatchingSkills(
                                                            candidate,
                                                            job
                                                    ),
                                            skillMatchingService
                                                    .getMissingSkills(
                                                            candidate,
                                                            job
                                                    )
                                    )
                            )
                            .build();
                })
                .filter(response ->
                        response.getMatchScore() != null
                                && response.getMatchScore()
                                >= aiConfig
                                .getRecommendations()
                                .getMinimumScore()
                )
                .sorted(
                        Comparator.comparing(
                                CandidateRecommendationResponse
                                        ::getMatchScore,
                                Comparator.reverseOrder()
                        )
                )
                .limit(
                        Math.max(
                                0,
                                aiConfig
                                        .getRecommendations()
                                        .getMaxResults()
                        )
                )
                .collect(Collectors.toList());
    }


    // ================================================================
    // RESUME ANALYSIS
    // ================================================================

    @Override
    public ResumeAnalysisResponse analyzeResume(
            ResumeAnalysisRequest request) {

        /*
         * Resume analysis remains rule-based for now.
         *
         * This can be connected to OpenAI separately.
         */
        if (request == null
                || request.getResumeText() == null
                || request.getResumeText().isBlank()) {

            return ResumeAnalysisResponse.builder()
                    .overallScore(0.0)
                    .strengths(
                            Collections.emptyList()
                    )
                    .weaknesses(
                            List.of(
                                    "No resume content was provided."
                            )
                    )
                    .recommendedSkills(
                            Collections.emptyList()
                    )
                    .summary(
                            "No resume content was provided for analysis."
                    )
                    .build();
        }

        String resumeText =
                request.getResumeText().trim();

        String normalizedText =
                resumeText.toLowerCase(
                        Locale.ROOT
                );

        List<String> strengths =
                new ArrayList<>();

        List<String> weaknesses =
                new ArrayList<>();

        List<String> recommendedSkills =
                new ArrayList<>();


        detectStrength(
                normalizedText,
                "java",
                "Java experience detected",
                strengths
        );

        detectStrength(
                normalizedText,
                "spring",
                "Spring experience detected",
                strengths
        );

        detectStrength(
                normalizedText,
                "react",
                "React experience detected",
                strengths
        );

        detectStrength(
                normalizedText,
                "typescript",
                "TypeScript experience detected",
                strengths
        );

        detectStrength(
                normalizedText,
                "sql",
                "SQL experience detected",
                strengths
        );


        if (strengths.isEmpty()) {

            weaknesses.add(
                    "No commonly recognized technical skills were detected."
            );
        }


        if (!normalizedText.contains("git")) {
            recommendedSkills.add("Git");
        }

        if (!normalizedText.contains("docker")) {
            recommendedSkills.add("Docker");
        }


        double score =
                calculateResumeScore(
                        strengths,
                        weaknesses,
                        resumeText
                );


        return ResumeAnalysisResponse.builder()
                .overallScore(score)
                .strengths(strengths)
                .weaknesses(weaknesses)
                .recommendedSkills(
                        recommendedSkills
                )
                .summary(
                        buildResumeSummary(
                                score,
                                strengths,
                                weaknesses
                        )
                )
                .build();
    }


    // ================================================================
    // GET CANDIDATE
    // ================================================================

    private CandidateData getCandidate(
            Long candidateId) {

        if (candidateId == null) {
            return null;
        }


        /*
         * Get basic candidate information from User Service.
         */
        Map<String, Object> user =
                userServiceClient.getUserById(
                        candidateId
                );

        if (user == null || user.isEmpty()) {
            return null;
        }


        CandidateData candidate =
                convertToCandidateData(user);


        /*
         * Resume Service is the source of truth for:
         *
         * - skills
         * - experience
         * - education
         */
        List<Map<String, Object>> resumes =
                resumeServiceClient
                        .getResumesByCandidate(
                                candidateId
                        );

        Map<String, Object> resume =
                getLatestResume(resumes);


        if (resume != null
                && !resume.isEmpty()) {

            List<SkillData> resumeSkills =
                    extractSkills(
                            resume,
                            "skills",
                            "skillSet",
                            "technicalSkills"
                    );

            if (!resumeSkills.isEmpty()) {

                candidate.setSkills(
                        resumeSkills
                );
            }


            if (isBlank(candidate.getExperience())) {

                candidate.setExperience(
                        getStringValue(
                                resume.get("experience")
                        )
                );
            }


            if (isBlank(candidate.getEducation())) {

                candidate.setEducation(
                        getStringValue(
                                resume.get("education")
                        )
                );
            }
        }


        /*
         * User Service fallback fields.
         */
        if (isBlank(candidate.getLocation())) {

            candidate.setLocation(
                    firstString(
                            user,
                            "location",
                            "city",
                            "address"
                    )
            );
        }


        if (isBlank(
                candidate.getPreferredJobType()
        )) {

            candidate.setPreferredJobType(
                    firstString(
                            user,
                            "preferredJobType",
                            "preferredJobTypeName",
                            "jobType"
                    )
            );
        }


        /*
         * User Service skills fallback.
         */
        if (candidate.getSkills() == null
                || candidate.getSkills().isEmpty()) {

            candidate.setSkills(
                    extractSkills(
                            user,
                            "skills",
                            "skillSet",
                            "technicalSkills"
                    )
            );
        }

        return candidate;
    }


    // ================================================================
    // GET CANDIDATES FOR JOB
    // ================================================================

    private List<CandidateData> getCandidatesForJob(
            Long jobId) {

        if (jobId == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> applications;

        try {

            applications =
                    applicationServiceClient
                            .getApplicationsByJob(
                                    jobId
                            );

        } catch (ExternalServiceException exception) {

            /*
             * Fallback if Application Service does not expose
             * /api/applications/job/{jobId}.
             */
            applications =
                    applicationServiceClient
                            .getApplications()
                            .stream()
                            .filter(application ->
                                    jobId.equals(
                                            getLongValue(
                                                    application.get("jobId"),
                                                    application.get("job")
                                            )
                                    )
                            )
                            .collect(
                                    Collectors.toList()
                            );
        }


        if (applications == null
                || applications.isEmpty()) {

            return Collections.emptyList();
        }


        Set<Long> candidateIds =
                applications.stream()
                        .map(this::extractCandidateId)
                        .filter(Objects::nonNull)
                        .collect(
                                Collectors.toCollection(
                                        LinkedHashSet::new
                                )
                        );


        List<CandidateData> candidates =
                new ArrayList<>();


        for (Long candidateId : candidateIds) {

            CandidateData candidate =
                    getCandidate(candidateId);

            if (candidate != null) {
                candidates.add(candidate);
            }
        }

        return candidates;
    }


    // ================================================================
    // EXTRACT CANDIDATE ID
    // ================================================================

    private Long extractCandidateId(
            Map<String, Object> application) {

        if (application == null) {
            return null;
        }


        Long direct =
                getLongValue(
                        application.get("candidateId"),
                        application.get("applicantId"),
                        application.get("userId")
                );


        if (direct != null) {
            return direct;
        }


        return getLongValue(
                application.get("candidate"),
                application.get("applicant")
        );
    }


    // ================================================================
    // BUILD JOB RECOMMENDATION
    // ================================================================

    private JobRecommendationResponse buildJobRecommendation(
            CandidateData candidate,
            JobData job) {

        double score =
                skillMatchingService
                        .calculateMatchScore(
                                candidate,
                                job
                        );

        return JobRecommendationResponse.builder()
                .jobId(job.getId())
                .title(job.getTitle())
                .companyName(job.getCompanyName())
                .matchScore(score)
                .explanation(
                        buildDetailedMatchExplanation(
                                score,
                                skillMatchingService
                                        .getMatchingSkills(
                                                candidate,
                                                job
                                        ),
                                skillMatchingService
                                        .getMissingSkills(
                                                candidate,
                                                job
                                        )
                        )
                )
                .build();
    }


    // ================================================================
    // JOB ELIGIBILITY
    // ================================================================

    private boolean isRecommendationEligibleJob(
            Map<String, Object> data) {

        String status =
                getStringValue(
                        data.get("status")
                );

        if (status == null) {
            return true;
        }

        return "OPEN".equalsIgnoreCase(
                status
        );
    }


    // ================================================================
    // CONVERT JOB DATA
    // ================================================================

    private JobData convertToJobData(
            Map<String, Object> data) {

        if (data == null
                || data.isEmpty()) {

            return null;
        }


        return JobData.builder()
                .id(
                        getLongValue(
                                data.get("id"),
                                data.get("jobId")
                        )
                )
                .title(
                        getStringValue(
                                data.get("title")
                        )
                )
                .description(
                        getStringValue(
                                data.get("description")
                        )
                )
                .category(
                        getStringValue(
                                data.get("category")
                        )
                )
                .jobType(
                        getStringValue(
                                data.get("jobType")
                        )
                )
                .workMode(
                        getStringValue(
                                data.get("workMode")
                        )
                )
                .experienceLevel(
                        getStringValue(
                                data.get("experienceLevel"),
                                data.get("experience")
                        )
                )
                .educationRequirement(
                        getStringValue(
                                data.get("educationRequirement"),
                                data.get("education"),
                                data.get("minimumEducation")
                        )
                )
                .city(
                        getStringValue(
                                data.get("city")
                        )
                )
                .country(
                        getStringValue(
                                data.get("country")
                        )
                )
                .openings(
                        getIntegerValue(
                                data.get("openings")
                        )
                )
                .employerId(
                        getLongValue(
                                data.get("employerId")
                        )
                )
                .companyName(
                        getStringValue(
                                data.get("companyName")
                        )
                )
                .skills(
                        extractSkills(
                                data,
                                "skills",
                                "jobSkills",
                                "requiredSkills",
                                "skillSet"
                        )
                )
                .build();
    }


    // ================================================================
    // CONVERT CANDIDATE DATA
    // ================================================================

    private CandidateData convertToCandidateData(
            Map<String, Object> data) {

        if (data == null
                || data.isEmpty()) {

            return null;
        }


        return CandidateData.builder()
                .id(
                        getLongValue(
                                data.get("id"),
                                data.get("userId")
                        )
                )
                .fullName(
                        getStringValue(
                                data.get("fullName"),
                                data.get("name")
                        )
                )
                .email(
                        getStringValue(
                                data.get("email")
                        )
                )
                .phone(
                        getStringValue(
                                data.get("phone")
                        )
                )
                .experience(
                        getStringValue(
                                data.get("experience")
                        )
                )
                .education(
                        getStringValue(
                                data.get("education")
                        )
                )
                .location(
                        firstString(
                                data,
                                "location",
                                "city",
                                "address"
                        )
                )
                .preferredJobType(
                        firstString(
                                data,
                                "preferredJobType",
                                "preferredJobTypeName",
                                "jobType"
                        )
                )
                .skills(
                        extractSkills(
                                data,
                                "skills",
                                "skillSet",
                                "technicalSkills"
                        )
                )
                .build();
    }


    // ================================================================
    // GET LATEST RESUME
    // ================================================================

    private Map<String, Object> getLatestResume(
            List<Map<String, Object>> resumes) {

        if (resumes == null
                || resumes.isEmpty()) {

            return null;
        }


        return resumes.stream()
                .filter(Objects::nonNull)
                .max(
                        Comparator.comparing(
                                resume ->
                                        parseInstant(
                                                getStringValue(
                                                        resume.get("updatedAt"),
                                                        resume.get("createdAt")
                                                )
                                        ),
                                Comparator.nullsFirst(
                                        Comparator.naturalOrder()
                                )
                        )
                )
                .orElse(null);
    }


    // ================================================================
    // PARSE INSTANT
    // ================================================================

    private Instant parseInstant(
            String value) {

        if (value == null
                || value.isBlank()) {

            return null;
        }


        try {

            return Instant.parse(value);

        } catch (Exception ignored) {

            return null;
        }
    }


    // ================================================================
    // EXTRACT SKILLS
    // ================================================================

    private List<SkillData> extractSkills(
            Map<String, Object> data,
            String... keys) {

        Set<String> names =
                new LinkedHashSet<>();


        if (data == null) {
            return Collections.emptyList();
        }


        for (String key : keys) {

            Object value =
                    data.get(key);

            collectSkillNames(
                    value,
                    names
            );
        }


        return names.stream()
                .map(
                        name ->
                                SkillData.builder()
                                        .name(name)
                                        .build()
                )
                .collect(
                        Collectors.toList()
                );
    }


    // ================================================================
    // COLLECT SKILL NAMES
    // ================================================================

    private void collectSkillNames(
            Object value,
            Set<String> names) {

        if (value == null) {
            return;
        }


        if (value instanceof Collection<?> collection) {

            for (Object item : collection) {

                collectSkillNames(
                        item,
                        names
                );
            }

            return;
        }


        if (value instanceof Map<?, ?> map) {

            Object name =
                    map.get("name");


            if (name == null) {
                name =
                        map.get("skillName");
            }


            if (name == null) {
                name =
                        map.get("title");
            }


            if (name != null
                    && !name.toString().isBlank()) {

                names.add(
                        name.toString().trim()
                );
            }

            return;
        }


        String text =
                value.toString().trim();


        if (!text.isBlank()) {

            names.add(text);
        }
    }


    // ================================================================
    // GET LONG VALUE
    // ================================================================

    private Long getLongValue(
            Object... values) {

        for (Object value : values) {

            if (value == null) {
                continue;
            }


            if (value instanceof Map<?, ?> map) {

                Long nested =
                        getLongValue(
                                map.get("id"),
                                map.get("candidateId"),
                                map.get("jobId")
                        );


                if (nested != null) {
                    return nested;
                }


                continue;
            }


            if (value instanceof Number number) {

                return number.longValue();
            }


            try {

                return Long.parseLong(
                        value.toString()
                );

            } catch (NumberFormatException ignored) {
                // Try next value.
            }
        }


        return null;
    }


    // ================================================================
    // GET INTEGER VALUE
    // ================================================================

    private Integer getIntegerValue(
            Object value) {

        if (value == null) {
            return null;
        }


        if (value instanceof Number number) {

            return number.intValue();
        }


        try {

            return Integer.parseInt(
                    value.toString()
            );

        } catch (NumberFormatException ignored) {

            return null;
        }
    }


    // ================================================================
    // FIRST STRING
    // ================================================================

    private String firstString(
            Map<String, Object> data,
            String... keys) {

        if (data == null) {
            return null;
        }


        for (String key : keys) {

            String value =
                    getStringValue(
                            data.get(key)
                    );


            if (!isBlank(value)) {
                return value;
            }
        }


        return null;
    }


    // ================================================================
    // GET STRING VALUE
    // ================================================================

    private String getStringValue(
            Object... values) {

        for (Object value : values) {

            if (value == null) {
                continue;
            }


            String result =
                    value.toString().trim();


            if (!result.isEmpty()) {
                return result;
            }
        }


        return null;
    }


    // ================================================================
    // BLANK CHECK
    // ================================================================

    private boolean isBlank(
            String value) {

        return value == null
                || value.isBlank();
    }


    // ================================================================
    // ROOT CAUSE MESSAGE
    // ================================================================

    /**
     * Gets the deepest useful error message.
     *
     * Example:
     *
     * RestClientException
     *     -> HttpClientErrorException
     *         -> OpenAI API error
     *
     * The deepest message is returned so Postman can show
     * the actual reason for failure.
     */
    private String getRootCauseMessage(
            Throwable throwable) {

        if (throwable == null) {
            return "Unknown error";
        }


        Throwable root =
                throwable;


        while (root.getCause() != null
                && root.getCause() != root) {

            root =
                    root.getCause();
        }


        String message =
                root.getMessage();


        if (message != null
                && !message.isBlank()) {

            return message;
        }


        return root.getClass()
                .getSimpleName();
    }


    // ================================================================
    // EMPTY SKILL MATCH
    // ================================================================

    private SkillMatchResponse emptySkillMatch(
            Long candidateId) {

        return SkillMatchResponse.builder()
                .candidateId(candidateId)
                .matchingSkills(
                        Collections.emptyList()
                )
                .missingSkills(
                        Collections.emptyList()
                )
                .matchScore(0.0)
                .build();
    }


    // ================================================================
    // RESUME STRENGTH DETECTION
    // ================================================================

    private void detectStrength(
            String text,
            String keyword,
            String message,
            List<String> strengths) {

        if (text.contains(keyword)) {

            strengths.add(message);
        }
    }


    // ================================================================
    // RESUME SCORE
    // ================================================================

    private double calculateResumeScore(
            List<String> strengths,
            List<String> weaknesses,
            String resumeText) {

        double score =
                50.0;


        score +=
                strengths.size() * 10.0;


        score -=
                weaknesses.size() * 5.0;


        if (resumeText.length() >= 500) {

            score += 10.0;
        }


        return Math.max(
                0.0,
                Math.min(
                        100.0,
                        score
                )
        );
    }


    // ================================================================
    // MATCH EXPLANATION
    // ================================================================

    private String buildDetailedMatchExplanation(
            double score,
            List<String> matchingSkills,
            List<String> missingSkills) {

        String level =
                MatchingScoreCalculator
                        .getMatchLevel(score);


        StringBuilder explanation =
                new StringBuilder(level)
                        .append(
                                " match based on skills and available profile/job information."
                        );


        if (matchingSkills != null
                && !matchingSkills.isEmpty()) {

            explanation
                    .append(
                            " Matching skills: "
                    )
                    .append(
                            String.join(
                                    ", ",
                                    matchingSkills
                            )
                    )
                    .append(".");
        }


        if (missingSkills != null
                && !missingSkills.isEmpty()) {

            explanation
                    .append(
                            " Missing skills: "
                    )
                    .append(
                            String.join(
                                    ", ",
                                    missingSkills
                            )
                    )
                    .append(".");
        }


        return explanation.toString();
    }


    // ================================================================
    // RESUME SUMMARY
    // ================================================================

    private String buildResumeSummary(
            double score,
            List<String> strengths,
            List<String> weaknesses) {

        if (!strengths.isEmpty()
                && weaknesses.isEmpty()) {

            return "The resume contains several recognizable strengths.";
        }


        if (!weaknesses.isEmpty()) {

            return "The resume has potential areas that could be improved.";
        }


        return "Resume analysis completed with an overall score of "
                + score
                + ".";
    }
}
