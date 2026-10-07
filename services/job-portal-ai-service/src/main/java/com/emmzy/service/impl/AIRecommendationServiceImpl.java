package com.emmzy.service.impl;

import com.emmzy.client.ApplicationServiceClient;
import com.emmzy.client.JobServiceClient;
import com.emmzy.client.UserServiceClient;
import com.emmzy.dto.request.CandidateRankingRequest;
import com.emmzy.dto.request.OpenAIChatRequest;
import com.emmzy.dto.request.JobMatchRequest;
import com.emmzy.dto.request.ResumeAnalysisRequest;
import com.emmzy.dto.response.CandidateRankingResponse;
import com.emmzy.dto.response.CandidateRecommendationResponse;
import com.emmzy.dto.response.OpenAIChatResponse;
import com.emmzy.dto.response.JobMatchingResponse;
import com.emmzy.dto.response.JobRecommendationResponse;
import com.emmzy.dto.response.ResumeAnalysisResponse;
import com.emmzy.dto.response.SkillMatchResponse;
import com.emmzy.model.CandidateData;
import com.emmzy.model.JobData;
import com.emmzy.service.AIRecommendationService;
import com.emmzy.service.OpenAIService;
import com.emmzy.service.SkillMatchingService;
import com.emmzy.util.MatchingScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIRecommendationServiceImpl implements AIRecommendationService {

    private final JobServiceClient jobServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final UserServiceClient userServiceClient;
    private final SkillMatchingService skillMatchingService;
    private final OpenAIService openAIService;

    /**
     * Global AI assistant chat.
     */
    @Override
    public OpenAIChatResponse chat(OpenAIChatRequest request) {

        if (request == null
                || request.getMessage() == null
                || request.getMessage().isBlank()) {

            throw new IllegalArgumentException(
                    "AI message is required."
            );
        }

        String message = request.getMessage().trim();

        String response = openAIService.generateResponse(message);

        return OpenAIChatResponse.builder()
                .success(true)
                .response(response)
                .error(null)
                .build();
    }

    @Override
    public @Nullable SkillMatchResponse matchSkills(Long candidateId) {
        return null;
    }

    @Override
    public List<JobRecommendationResponse> getJobRecommendations(
            Long candidateId
    ) {
        CandidateData candidate = getCandidate(candidateId);

        if (candidate == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> jobs = jobServiceClient.getJobs();

        if (jobs == null || jobs.isEmpty()) {
            return Collections.emptyList();
        }

        return jobs.stream()
                .filter(Objects::nonNull)
                .map(this::convertToJobData)
                .filter(Objects::nonNull)
                .map(job -> {

                    double score =
                            skillMatchingService.calculateMatchScore(
                                    candidate,
                                    job
                            );

                    return JobRecommendationResponse.builder()
                            .jobId(job.getId())
                            .title(job.getTitle())
                            .companyName(job.getCompanyName())
                            .matchScore(score)
                            .explanation(
                                    buildMatchExplanation(score)
                            )
                            .build();
                })
                .sorted(
                        Comparator.comparing(
                                JobRecommendationResponse::getMatchScore,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .collect(Collectors.toList());
    }

    @Override
    public JobMatchingResponse matchJob(
            JobMatchRequest request
    ) {
        if (request == null
                || request.getJobId() == null
                || request.getCandidateId() == null) {

            return JobMatchingResponse.builder()
                    .matchScore(0.0)
                    .matchLevel("LOW")
                    .explanation(
                            "Job ID and candidate ID are required."
                    )
                    .build();
        }

        Map<String, Object> jobResponse =
                jobServiceClient.getJobById(
                        request.getJobId()
                );

        JobData job = convertToJobData(jobResponse);

        CandidateData candidate =
                getCandidate(request.getCandidateId());

        if (job == null || candidate == null) {
            return JobMatchingResponse.builder()
                    .jobId(request.getJobId())
                    .candidateId(request.getCandidateId())
                    .matchScore(0.0)
                    .matchLevel("LOW")
                    .explanation(
                            "Job or candidate information could not be found."
                    )
                    .build();
        }

        double score =
                skillMatchingService.calculateMatchScore(
                        candidate,
                        job
                );

        return JobMatchingResponse.builder()
                .jobId(job.getId())
                .candidateId(candidate.getId())
                .matchScore(score)
                .matchLevel(
                        MatchingScoreCalculator.getMatchLevel(score)
                )
                .explanation(
                        buildMatchExplanation(score)
                )
                .build();
    }

    @Override
    public SkillMatchResponse getSkillMatch(
            Long candidateId
    ) {
        CandidateData candidate = getCandidate(candidateId);

        if (candidate == null) {
            return emptySkillMatch(candidateId);
        }

        List<Map<String, Object>> jobResponses =
                jobServiceClient.getJobs();

        if (jobResponses == null || jobResponses.isEmpty()) {
            return emptySkillMatch(candidateId);
        }

        JobData bestJob = jobResponses.stream()
                .filter(Objects::nonNull)
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
                skillMatchingService.calculateMatchScore(
                        candidate,
                        bestJob
                );

        return SkillMatchResponse.builder()
                .candidateId(candidateId)
                .matchingSkills(
                        skillMatchingService.getMatchingSkills(
                                candidate,
                                bestJob
                        )
                )
                .missingSkills(
                        skillMatchingService.getMissingSkills(
                                candidate,
                                bestJob
                        )
                )
                .matchScore(score)
                .build();
    }

    @Override
    public List<CandidateRankingResponse> rankCandidates(
            CandidateRankingRequest request
    ) {
        if (request == null || request.getJobId() == null) {
            return Collections.emptyList();
        }

        Map<String, Object> jobResponse =
                jobServiceClient.getJobById(
                        request.getJobId()
                );

        JobData job = convertToJobData(jobResponse);

        if (job == null) {
            return Collections.emptyList();
        }

        List<CandidateData> candidates =
                getCandidatesForJob(request.getJobId());

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<CandidateRankingResponse> rankings =
                new ArrayList<>();

        for (CandidateData candidate : candidates) {

            if (candidate == null) {
                continue;
            }

            double score =
                    skillMatchingService.calculateMatchScore(
                            candidate,
                            job
                    );

            rankings.add(
                    CandidateRankingResponse.builder()
                            .candidateId(candidate.getId())
                            .matchScore(score)
                            .explanation(
                                    buildMatchExplanation(score)
                            )
                            .build()
            );
        }

        rankings.sort(
                Comparator.comparing(
                        CandidateRankingResponse::getMatchScore,
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        for (int index = 0;
             index < rankings.size();
             index++) {

            rankings.get(index).setRank(index + 1);
        }

        return rankings;
    }

    @Override
    public List<CandidateRecommendationResponse>
    getCandidateRecommendations(Long jobId) {

        if (jobId == null) {
            return Collections.emptyList();
        }

        Map<String, Object> jobResponse =
                jobServiceClient.getJobById(jobId);

        JobData job = convertToJobData(jobResponse);

        if (job == null) {
            return Collections.emptyList();
        }

        List<CandidateData> candidates =
                getCandidatesForJob(jobId);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        return candidates.stream()
                .filter(Objects::nonNull)
                .map(candidate -> {

                    double score =
                            skillMatchingService.calculateMatchScore(
                                    candidate,
                                    job
                            );

                    return CandidateRecommendationResponse.builder()
                            .candidateId(candidate.getId())
                            .candidateName(candidate.getFullName())
                            .matchScore(score)
                            .explanation(
                                    buildMatchExplanation(score)
                            )
                            .build();
                })
                .sorted(
                        Comparator.comparing(
                                CandidateRecommendationResponse
                                        ::getMatchScore,
                                Comparator.nullsLast(
                                        Comparator.reverseOrder()
                                )
                        )
                )
                .collect(Collectors.toList());
    }

    @Override
    public ResumeAnalysisResponse analyzeResume(
            ResumeAnalysisRequest request
    ) {
        if (request == null
                || request.getResumeText() == null
                || request.getResumeText().isBlank()) {

            return ResumeAnalysisResponse.builder()
                    .overallScore(0.0)
                    .strengths(Collections.emptyList())
                    .weaknesses(
                            List.of(
                                    "No resume content was provided."
                            )
                    )
                    .recommendedSkills(Collections.emptyList())
                    .summary(
                            "No resume content was provided for analysis."
                    )
                    .build();
        }

        String resumeText =
                request.getResumeText().trim();

        String normalizedText =
                resumeText.toLowerCase(Locale.ROOT);

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
                .recommendedSkills(recommendedSkills)
                .summary(
                        buildResumeSummary(
                                score,
                                strengths,
                                weaknesses
                        )
                )
                .build();
    }

    private CandidateData getCandidate(
            Long candidateId
    ) {
        if (candidateId == null) {
            return null;
        }

        Map<String, Object> candidateResponse =
                userServiceClient.getUserById(candidateId);

        if (candidateResponse == null) {
            return null;
        }

        return convertToCandidateData(candidateResponse);
    }

    private List<CandidateData> getCandidatesForJob(
            Long jobId
    ) {
        if (jobId == null) {
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private JobData convertToJobData(
            Map<String, Object> data
    ) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        Long id = getLongValue(
                data.get("id"),
                data.get("jobId")
        );

        String title =
                getStringValue(data.get("title"));

        String description =
                getStringValue(data.get("description"));

        String category =
                getStringValue(data.get("category"));

        String jobType =
                getStringValue(data.get("jobType"));

        String workMode =
                getStringValue(data.get("workMode"));

        String experienceLevel =
                getStringValue(data.get("experienceLevel"));

        String city =
                getStringValue(data.get("city"));

        String country =
                getStringValue(data.get("country"));

        Integer openings =
                getIntegerValue(data.get("openings"));

        Long employerId =
                getLongValue(data.get("employerId"));

        String companyName =
                getStringValue(data.get("companyName"));

        return JobData.builder()
                .id(id)
                .title(title)
                .description(description)
                .category(category)
                .jobType(jobType)
                .workMode(workMode)
                .experienceLevel(experienceLevel)
                .city(city)
                .country(country)
                .openings(openings)
                .employerId(employerId)
                .companyName(companyName)
                .build();
    }

    private CandidateData convertToCandidateData(
            Map<String, Object> data
    ) {
        if (data == null || data.isEmpty()) {
            return null;
        }

        Long id =
                getLongValue(data.get("id"));

        String fullName =
                getStringValue(data.get("fullName"));

        String email =
                getStringValue(data.get("email"));

        String phone =
                getStringValue(data.get("phone"));

        String experience =
                getStringValue(data.get("experience"));

        String education =
                getStringValue(data.get("education"));

        return CandidateData.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .experience(experience)
                .education(education)
                .build();
    }

    private Long getLongValue(
            Object... values
    ) {
        for (Object value : values) {

            if (value == null) {
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
                // Try the next value.
            }
        }

        return null;
    }

    private Integer getIntegerValue(
            Object value
    ) {
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

    private String getStringValue(
            Object value
    ) {
        if (value == null) {
            return null;
        }

        String result =
                value.toString().trim();

        return result.isEmpty()
                ? null
                : result;
    }

    private SkillMatchResponse emptySkillMatch(
            Long candidateId
    ) {
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

    private void detectStrength(
            String text,
            String keyword,
            String message,
            List<String> strengths
    ) {
        if (text.contains(keyword)) {
            strengths.add(message);
        }
    }

    private double calculateResumeScore(
            List<String> strengths,
            List<String> weaknesses,
            String resumeText
    ) {
        double score = 50.0;

        score += strengths.size() * 10.0;
        score -= weaknesses.size() * 5.0;

        if (resumeText.length() >= 500) {
            score += 10.0;
        }

        return Math.max(
                0.0,
                Math.min(100.0, score)
        );
    }

    private String buildMatchExplanation(
            double score
    ) {
        if (score >= 80.0) {
            return "Strong match based on the available skills and job requirements.";
        }

        if (score >= 60.0) {
            return "Good match with several relevant skills.";
        }

        if (score >= 40.0) {
            return "Moderate match with some relevant skills.";
        }

        return "Limited match based on the available information.";
    }

    private String buildResumeSummary(
            double score,
            List<String> strengths,
            List<String> weaknesses
    ) {
        if (!strengths.isEmpty() && weaknesses.isEmpty()) {
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

