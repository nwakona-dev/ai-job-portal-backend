package com.emmzy.service.impl;

import com.emmzy.client.ApplicationServiceClient;
import com.emmzy.client.JobServiceClient;
import com.emmzy.client.ResumeServiceClient;
import com.emmzy.client.UserServiceClient;
import com.emmzy.dto.request.CandidateRankingRequest;
import com.emmzy.dto.request.JobMatchRequest;
import com.emmzy.dto.request.OpenAIChatRequest;
import com.emmzy.dto.request.ResumeAnalysisRequest;
import com.emmzy.dto.response.CandidateRankingResponse;
import com.emmzy.dto.response.CandidateRecommendationResponse;
import com.emmzy.dto.response.JobMatchingResponse;
import com.emmzy.dto.response.JobRecommendationResponse;
import com.emmzy.dto.response.OpenAIChatResponse;
import com.emmzy.dto.response.ResumeAnalysisResponse;
import com.emmzy.dto.response.SkillMatchResponse;
import com.emmzy.model.CandidateData;
import com.emmzy.model.JobData;
import com.emmzy.model.SkillData;
import com.emmzy.service.AIRecommendationService;
import com.emmzy.service.OpenAIService;
import com.emmzy.service.SkillMatchingService;
import com.emmzy.util.MatchingScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AIRecommendationServiceImpl implements AIRecommendationService {

    private final JobServiceClient jobServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final ResumeServiceClient resumeServiceClient;
    private final UserServiceClient userServiceClient;
    private final SkillMatchingService skillMatchingService;
    private final OpenAIService openAIService;

    private static final List<String> RECOGNIZED_SKILLS = List.of(
            "Spring Boot", "Spring Framework", "React Native",
            "Node.js", "Express.js", "Tailwind CSS", "REST API",
            "Machine Learning", "Deep Learning", "Data Analysis",
            "Power BI", "UI/UX", "Drivers License", "JavaScript",
            "TypeScript", "Java", "Spring", "React", "Angular",
            "Vue", "Python", "Django", "Flask", "C++", "C#",
            ".NET", "PHP", "Laravel", "Go", "Rust", "Kotlin",
            "Swift", "Flutter", "SQL", "MySQL", "PostgreSQL",
            "MongoDB", "Redis", "Docker", "Kubernetes", "Git",
            "GitHub", "HTML", "CSS", "Bootstrap", "AWS", "Azure",
            "Google Cloud", "Linux", "Figma", "GraphQL", "Jenkins",
            "Maven", "Gradle", "JPA", "Hibernate", "Microservices",
            "Selenium", "Tableau", "Excel", "Driving"
    );

    private static final List<String> RESPONSE_WRAPPER_KEYS = List.of(
            "data", "result", "job", "user", "candidate", "resume"
    );

    /*
     * CHAT
     */

    @Override
    public OpenAIChatResponse chat(OpenAIChatRequest request) {
        if (request == null
                || request.getMessage() == null
                || request.getMessage().isBlank()) {
            throw new IllegalArgumentException("AI message is required.");
        }

        String response = openAIService.generateResponse(
                request.getMessage().trim()
        );

        return OpenAIChatResponse.builder()
                .success(true)
                .response(response)
                .error(null)
                .build();
    }

    /*
     * SKILL MATCH
     */

    @Override
    public @Nullable SkillMatchResponse matchSkills(Long candidateId) {
        return getSkillMatch(candidateId);
    }

    /*
     * JOB RECOMMENDATIONS
     */

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
                    double score = skillMatchingService.calculateMatchScore(
                            candidate, job
                    );

                    return JobRecommendationResponse.builder()
                            .jobId(job.getId())
                            .title(job.getTitle())
                            .companyName(job.getCompanyName())
                            .matchScore(score)
                            .explanation(buildMatchExplanation(score))
                            .build();
                })
                .sorted(
                        Comparator.comparing(
                                JobRecommendationResponse::getMatchScore,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .collect(Collectors.toList());
    }

    /*
     * MATCH A SPECIFIC JOB
     */

    @Override
    public JobMatchingResponse matchJob(JobMatchRequest request) {
        if (request == null
                || request.getJobId() == null
                || request.getCandidateId() == null) {
            return JobMatchingResponse.builder()
                    .matchScore(0.0)
                    .matchLevel("LOW")
                    .explanation("Job ID and candidate ID are required.")
                    .build();
        }

        JobData job = convertToJobData(
                jobServiceClient.getJobById(request.getJobId())
        );
        CandidateData candidate = getCandidate(request.getCandidateId());

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

        double score = skillMatchingService.calculateMatchScore(
                candidate, job
        );

        return JobMatchingResponse.builder()
                .jobId(job.getId())
                .candidateId(candidate.getId())
                .matchScore(score)
                .matchLevel(MatchingScoreCalculator.getMatchLevel(score))
                .explanation(buildMatchExplanation(score))
                .build();
    }

    /*
     * SKILL MATCH DETAILS
     */

    @Override
    public SkillMatchResponse getSkillMatch(Long candidateId) {
        CandidateData candidate = getCandidate(candidateId);

        if (candidate == null) {
            return emptySkillMatch(candidateId);
        }

        List<Map<String, Object>> jobResponses = jobServiceClient.getJobs();

        if (jobResponses == null || jobResponses.isEmpty()) {
            return emptySkillMatch(candidateId);
        }

        List<JobData> jobs = jobResponses.stream()
                .filter(Objects::nonNull)
                .map(this::convertToJobData)
                .filter(Objects::nonNull)
                .filter(job ->
                        job.getSkills() != null && !job.getSkills().isEmpty()
                )
                .collect(Collectors.toList());

        if (jobs.isEmpty()) {
            return emptySkillMatch(candidateId);
        }

        JobData bestJob = jobs.stream()
                .max(
                        Comparator.comparingDouble(
                                job -> skillMatchingService.calculateMatchScore(
                                        candidate, job
                                )
                        )
                )
                .orElse(null);

        if (bestJob == null) {
            return emptySkillMatch(candidateId);
        }

        double score = skillMatchingService.calculateMatchScore(
                candidate, bestJob
        );

        return SkillMatchResponse.builder()
                .candidateId(candidateId)
                .matchingSkills(
                        skillMatchingService.getMatchingSkills(candidate, bestJob)
                )
                .missingSkills(
                        skillMatchingService.getMissingSkills(candidate, bestJob)
                )
                .matchScore(score)
                .build();
    }

    /*
     * CANDIDATE RANKING
     */

    @Override
    public List<CandidateRankingResponse> rankCandidates(
            CandidateRankingRequest request
    ) {
        if (request == null || request.getJobId() == null) {
            return Collections.emptyList();
        }

        JobData job = convertToJobData(
                jobServiceClient.getJobById(request.getJobId())
        );

        if (job == null) {
            return Collections.emptyList();
        }

        List<CandidateData> candidates = getCandidatesForJob(
                request.getJobId()
        );

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        List<CandidateRankingResponse> rankings = new ArrayList<>();

        for (CandidateData candidate : candidates) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }

            double score = skillMatchingService.calculateMatchScore(
                    candidate, job
            );

            rankings.add(
                    CandidateRankingResponse.builder()
                            .candidateId(candidate.getId())
                            .matchScore(score)
                            .explanation(buildMatchExplanation(score))
                            .build()
            );
        }

        rankings.sort(
                Comparator.comparing(
                        CandidateRankingResponse::getMatchScore,
                        Comparator.nullsLast(Comparator.reverseOrder())
                )
        );

        for (int index = 0; index < rankings.size(); index++) {
            rankings.get(index).setRank(index + 1);
        }

        return rankings;
    }

    /*
     * LOAD CANDIDATES WHO APPLIED FOR A JOB
     */

    private List<CandidateData> getCandidatesForJob(Long jobId) {
        if (jobId == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> applications =
                applicationServiceClient.getApplicationsForJob(jobId);

        if (applications == null || applications.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, CandidateData> uniqueCandidates = new LinkedHashMap<>();

        for (Map<String, Object> application : applications) {
            if (application == null) {
                continue;
            }

            Long candidateId = extractCandidateId(application);

            if (candidateId == null || uniqueCandidates.containsKey(candidateId)) {
                continue;
            }

            CandidateData candidate = getCandidate(candidateId);

            if (candidate != null && candidate.getId() != null) {
                uniqueCandidates.put(candidate.getId(), candidate);
            }
        }

        return new ArrayList<>(uniqueCandidates.values());
    }

    private Long extractCandidateId(Map<String, Object> application) {
        if (application == null) {
            return null;
        }

        Long candidateId = getLongValue(
                application.get("candidateId"),
                application.get("candidate_id"),
                application.get("applicantId"),
                application.get("applicant_id"),
                application.get("jobSeekerId"),
                application.get("job_seeker_id"),
                application.get("userId"),
                application.get("user_id")
        );

        if (candidateId != null) {
            return candidateId;
        }

        for (String key : List.of(
                "candidate", "applicant", "jobSeeker", "job_seeker", "user"
        )) {
            candidateId = getNestedId(application.get(key));
            if (candidateId != null) {
                return candidateId;
            }
        }

        return null;
    }

    private Long getNestedId(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }

        return getLongValue(
                map.get("id"),
                map.get("candidateId"),
                map.get("candidate_id"),
                map.get("applicantId"),
                map.get("applicant_id"),
                map.get("jobSeekerId"),
                map.get("job_seeker_id"),
                map.get("userId"),
                map.get("user_id")
        );
    }

    /*
     * CANDIDATE RECOMMENDATIONS
     */

    @Override
    public List<CandidateRecommendationResponse> getCandidateRecommendations(
            Long jobId
    ) {
        if (jobId == null) {
            return Collections.emptyList();
        }

        JobData job = convertToJobData(jobServiceClient.getJobById(jobId));

        if (job == null) {
            return Collections.emptyList();
        }

        List<CandidateData> candidates = getCandidatesForJob(jobId);

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        return candidates.stream()
                .filter(Objects::nonNull)
                .map(candidate -> {
                    double score = skillMatchingService.calculateMatchScore(
                            candidate, job
                    );

                    return CandidateRecommendationResponse.builder()
                            .candidateId(candidate.getId())
                            .candidateName(candidate.getFullName())
                            .matchScore(score)
                            .explanation(buildMatchExplanation(score))
                            .build();
                })
                .sorted(
                        Comparator.comparing(
                                CandidateRecommendationResponse::getMatchScore,
                                Comparator.nullsLast(Comparator.reverseOrder())
                        )
                )
                .collect(Collectors.toList());
    }

    /*
     * RESUME ANALYSIS
     */

    @Override
    public ResumeAnalysisResponse analyzeResume(ResumeAnalysisRequest request) {
        if (request == null
                || request.getResumeText() == null
                || request.getResumeText().isBlank()) {
            return ResumeAnalysisResponse.builder()
                    .overallScore(0.0)
                    .strengths(Collections.emptyList())
                    .weaknesses(List.of("No resume content was provided."))
                    .recommendedSkills(Collections.emptyList())
                    .summary("No resume content was provided for analysis.")
                    .build();
        }

        String resumeText = request.getResumeText().trim();
        String normalizedText = resumeText.toLowerCase(Locale.ROOT);

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> recommendedSkills = new ArrayList<>();

        detectStrength(normalizedText, "java", "Java experience detected", strengths);
        detectStrength(normalizedText, "spring", "Spring experience detected", strengths);
        detectStrength(normalizedText, "react", "React experience detected", strengths);
        detectStrength(normalizedText, "typescript", "TypeScript experience detected", strengths);
        detectStrength(normalizedText, "sql", "SQL experience detected", strengths);

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

        double score = calculateResumeScore(
                strengths, weaknesses, resumeText
        );

        return ResumeAnalysisResponse.builder()
                .overallScore(score)
                .strengths(strengths)
                .weaknesses(weaknesses)
                .recommendedSkills(recommendedSkills)
                .summary(buildResumeSummary(score, strengths, weaknesses))
                .build();
    }

    /*
     * LOAD CANDIDATE AND RESUME DATA
     */

    private CandidateData getCandidate(Long candidateId) {
        if (candidateId == null) {
            return null;
        }

        Map<String, Object> candidateResponse =
                unwrapResponse(userServiceClient.getUserById(candidateId));

        if (candidateResponse == null) {
            return null;
        }

        CandidateData candidate = convertToCandidateData(candidateResponse);

        if (candidate == null) {
            return null;
        }

        List<Map<String, Object>> resumes =
                resumeServiceClient.getResumesByCandidate(candidateId);

        if (resumes != null && !resumes.isEmpty()) {
            Map<String, Object> selectedResume = selectBestResume(resumes);

            if (selectedResume != null) {
                mergeResumeIntoCandidate(
                        candidate,
                        unwrapResponse(selectedResume)
                );
            }
        }

        return candidate;
    }

    private Map<String, Object> selectBestResume(
            List<Map<String, Object>> resumes
    ) {
        if (resumes == null || resumes.isEmpty()) {
            return null;
        }

        for (Map<String, Object> resume : resumes) {
            if (resume == null) {
                continue;
            }

            Object isDefault = resume.get("isDefault");

            if (Boolean.TRUE.equals(isDefault)
                    || (isDefault != null
                    && "true".equalsIgnoreCase(isDefault.toString()))) {
                return resume;
            }
        }

        return resumes.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /*
     * MERGE RESUME SKILLS AND EXPERIENCE INTO CANDIDATE
     */

    private void mergeResumeIntoCandidate(
            CandidateData candidate,
            Map<String, Object> resume
    ) {
        if (candidate == null || resume == null) {
            return;
        }

        List<SkillData> resumeSkills = new ArrayList<>();

        addSkillsFromPossibleFields(
                resumeSkills,
                resume,
                "skills", "technicalSkills", "technical_skills",
                "skillSet", "skill_set", "keySkills", "key_skills"
        );

        String summary = firstNonBlank(
                getStringValue(resume.get("summary")),
                getStringValue(resume.get("professionalSummary")),
                getStringValue(resume.get("profileSummary")),
                getStringValue(resume.get("objective"))
        );

        String workExperienceText = joinTextFields(
                resume,
                "workExperiences", "work_experiences",
                "workExperience", "work_experience",
                "experience", "experiences", "employmentHistory",
                "employment_history"
        );

        String educationText = joinTextFields(
                resume,
                "educations", "educationHistory", "education_history",
                "education", "qualifications"
        );

        addRecognizedSkillsFromText(
                resumeSkills,
                summary,
                workExperienceText,
                educationText,
                getStringValue(resume.get("resumeText")),
                getStringValue(resume.get("resume_text")),
                getStringValue(resume.get("description"))
        );

        List<SkillData> existingSkills = candidate.getSkills();

        if (existingSkills == null) {
            existingSkills = new ArrayList<>();
            candidate.setSkills(existingSkills);
        }

        for (SkillData skill : resumeSkills) {
            if (skill != null && skill.getName() != null) {
                addSkillName(existingSkills, skill.getName());
            }
        }

        if ((candidate.getExperience() == null
                || candidate.getExperience().isBlank())
                && workExperienceText != null
                && !workExperienceText.isBlank()) {
            candidate.setExperience(workExperienceText);
        }

        if ((candidate.getEducation() == null
                || candidate.getEducation().isBlank())
                && educationText != null
                && !educationText.isBlank()) {
            candidate.setEducation(educationText);
        }

        addRecognizedSkillsFromText(
                existingSkills,
                summary,
                candidate.getExperience(),
                candidate.getEducation()
        );
    }

    private String joinTextFields(
            Map<String, Object> data,
            String... keys
    ) {
        if (data == null || keys == null) {
            return null;
        }

        List<String> parts = new ArrayList<>();

        for (String key : keys) {
            Object value = data.get(key);

            if (value == null) {
                continue;
            }

            if (value instanceof Collection<?> collection) {
                for (Object item : collection) {
                    collectTextFromObject(parts, item);
                }
            } else {
                collectTextFromObject(parts, value);
            }
        }

        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private void collectTextFromObject(List<String> parts, Object value) {
        if (value == null) {
            return;
        }

        if (value instanceof Map<?, ?> map) {
            for (String key : List.of(
                    "jobTitle", "job_title", "title", "position",
                    "company", "description", "responsibilities",
                    "degree", "fieldOfStudy", "field_of_study",
                    "institution", "school", "name", "details"
            )) {
                addTextValue(parts, map.get(key));
            }
            return;
        }

        addTextValue(parts, value);
    }

    /*
     * CANDIDATE CONVERSION
     */

    private CandidateData convertToCandidateData(
            Map<String, Object> originalData
    ) {
        Map<String, Object> data = unwrapResponse(originalData);

        if (data == null || data.isEmpty()) {
            return null;
        }

        Long id = getLongValue(
                data.get("id"),
                data.get("userId"),
                data.get("user_id"),
                data.get("candidateId"),
                data.get("candidate_id")
        );

        String fullName = firstNonBlank(
                getStringValue(data.get("fullName")),
                getStringValue(data.get("full_name")),
                getStringValue(data.get("name")),
                getStringValue(data.get("username"))
        );

        String email = firstNonBlank(
                getStringValue(data.get("email")),
                getStringValue(data.get("emailAddress")),
                getStringValue(data.get("email_address"))
        );

        String phone = firstNonBlank(
                getStringValue(data.get("phone")),
                getStringValue(data.get("phoneNumber")),
                getStringValue(data.get("phone_number"))
        );

        String experience = firstNonBlank(
                getStringValue(data.get("experience")),
                getStringValue(data.get("workExperience")),
                getStringValue(data.get("work_experience")),
                getStringValue(data.get("yearsOfExperience")),
                getStringValue(data.get("years_of_experience"))
        );

        String education = firstNonBlank(
                getStringValue(data.get("education")),
                getStringValue(data.get("highestEducation")),
                getStringValue(data.get("highest_education"))
        );

        List<SkillData> skills = extractCandidateSkills(data);

        return CandidateData.builder()
                .id(id)
                .fullName(fullName)
                .email(email)
                .phone(phone)
                .skills(skills)
                .experience(experience)
                .education(education)
                .build();
    }

    /*
     * CANDIDATE SKILL EXTRACTION
     */

    private List<SkillData> extractCandidateSkills(
            Map<String, Object> data
    ) {
        List<SkillData> skills = new ArrayList<>();

        if (data == null) {
            return skills;
        }

        addSkillsFromPossibleFields(
                skills,
                data,
                "skills", "technicalSkills", "technical_skills",
                "skillSet", "skill_set", "keySkills", "key_skills",
                "competencies", "technologies", "tools",
                "programmingLanguages", "programming_languages"
        );

        addRecognizedSkillsFromText(
                skills,
                getStringValue(data.get("experience")),
                getStringValue(data.get("workExperience")),
                getStringValue(data.get("work_experience")),
                getStringValue(data.get("education")),
                getStringValue(data.get("summary")),
                getStringValue(data.get("professionalSummary")),
                getStringValue(data.get("professional_summary")),
                getStringValue(data.get("bio")),
                getStringValue(data.get("about")),
                getStringValue(data.get("description")),
                getStringValue(data.get("resumeText")),
                getStringValue(data.get("resume_text"))
        );

        return skills;
    }

    /*
     * JOB CONVERSION
     */

    private JobData convertToJobData(Map<String, Object> originalData) {
        Map<String, Object> data = unwrapResponse(originalData);

        if (data == null || data.isEmpty()) {
            return null;
        }

        Long id = getLongValue(data.get("id"), data.get("jobId"), data.get("job_id"));

        String title = firstNonBlank(
                getStringValue(data.get("title")),
                getStringValue(data.get("jobTitle")),
                getStringValue(data.get("job_title")),
                getStringValue(data.get("position"))
        );

        String description = firstNonBlank(
                getStringValue(data.get("description")),
                getStringValue(data.get("jobDescription")),
                getStringValue(data.get("job_description"))
        );

        String category = firstNonBlank(
                getStringValue(data.get("category")),
                getNestedStringValue(data.get("category"), "name"),
                getStringValue(data.get("jobCategory")),
                getNestedStringValue(data.get("jobCategory"), "name")
        );

        String jobType = firstNonBlank(
                getStringValue(data.get("jobType")),
                getStringValue(data.get("job_type")),
                getStringValue(data.get("employmentType")),
                getStringValue(data.get("employment_type"))
        );

        String workMode = firstNonBlank(
                getStringValue(data.get("workMode")),
                getStringValue(data.get("work_mode")),
                getStringValue(data.get("workplaceType")),
                getStringValue(data.get("workplace_type"))
        );

        String experienceLevel = firstNonBlank(
                getStringValue(data.get("experienceLevel")),
                getStringValue(data.get("experience_level")),
                getStringValue(data.get("experienceRequired")),
                getStringValue(data.get("experience_required"))
        );

        String city = firstNonBlank(
                getStringValue(data.get("city")),
                getNestedStringValue(data.get("location"), "city")
        );

        String country = firstNonBlank(
                getStringValue(data.get("country")),
                getNestedStringValue(data.get("location"), "country")
        );

        Integer openings = getIntegerValue(
                firstNonNull(data.get("openings"), data.get("numberOfOpenings"))
        );

        Long employerId = getLongValue(
                data.get("employerId"),
                data.get("employer_id"),
                data.get("companyId"),
                data.get("company_id")
        );

        String companyName = firstNonBlank(
                getStringValue(data.get("companyName")),
                getStringValue(data.get("company_name")),
                getNestedStringValue(data.get("company"), "name"),
                getNestedStringValue(data.get("employer"), "name")
        );

        List<SkillData> skills = extractJobSkills(data);

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
                .skills(skills)
                .build();
    }

    /*
     * JOB SKILL EXTRACTION
     */

    private List<SkillData> extractJobSkills(Map<String, Object> data) {
        List<SkillData> skills = new ArrayList<>();

        if (data == null) {
            return skills;
        }

        addSkillsFromPossibleFields(
                skills,
                data,
                "skills", "requiredSkills", "required_skills",
                "technicalSkills", "technical_skills",
                "skillRequirements", "skill_requirements",
                "preferredSkills", "preferred_skills",
                "qualifications", "technologies", "tools",
                "programmingLanguages", "programming_languages"
        );

        addRecognizedSkillsFromText(
                skills,
                getStringValue(data.get("requirements")),
                getStringValue(data.get("responsibilities")),
                getStringValue(data.get("description")),
                getStringValue(data.get("jobDescription")),
                getStringValue(data.get("job_description")),
                getStringValue(data.get("benefits")),
                getStringValue(data.get("title")),
                getStringValue(data.get("jobTitle")),
                getStringValue(data.get("experienceLevel")),
                getStringValue(data.get("experience_level"))
        );

        return skills;
    }

    /*
     * READ SKILLS FROM MULTIPLE POSSIBLE FIELD NAMES
     */

    private void addSkillsFromPossibleFields(
            List<SkillData> skills,
            Map<String, Object> data,
            String... fieldNames
    ) {
        if (skills == null || data == null || fieldNames == null) {
            return;
        }

        for (String fieldName : fieldNames) {
            Object value = data.get(fieldName);

            if (value != null) {
                addStructuredSkills(skills, value);
            }
        }
    }

    /*
     * STRUCTURED SKILL PARSING
     */

    private void addStructuredSkills(
            List<SkillData> skills,
            Object rawSkills
    ) {
        if (rawSkills == null || skills == null) {
            return;
        }

        if (rawSkills instanceof Collection<?> collection) {
            for (Object rawSkill : collection) {
                addSingleStructuredSkill(skills, rawSkill);
            }
            return;
        }

        if (rawSkills instanceof Map<?, ?> map) {
            Object nestedSkills = firstNonNull(
                    map.get("skills"),
                    map.get("items"),
                    map.get("values"),
                    map.get("data")
            );

            if (nestedSkills != null) {
                addStructuredSkills(skills, nestedSkills);
            } else {
                addSingleStructuredSkill(skills, rawSkills);
            }
            return;
        }

        if (rawSkills instanceof String value) {
            String trimmed = value.trim();

            if (trimmed.isEmpty()) {
                return;
            }

            String[] splitSkills = trimmed.split("[,;|\\n\\r]+");

            if (splitSkills.length > 1) {
                for (String skill : splitSkills) {
                    addSkillName(skills, skill);
                }
            } else {
                addSkillName(skills, trimmed);
            }
        }
    }

    private void addSingleStructuredSkill(
            List<SkillData> skills,
            Object rawSkill
    ) {
        if (rawSkill == null) {
            return;
        }

        if (rawSkill instanceof String skill) {
            addStructuredSkills(skills, skill);
            return;
        }

        if (rawSkill instanceof Map<?, ?> skillMap) {
            String name = firstNonBlank(
                    getStringValue(skillMap.get("name")),
                    getStringValue(skillMap.get("skillName")),
                    getStringValue(skillMap.get("skill_name")),
                    getStringValue(skillMap.get("title")),
                    getStringValue(skillMap.get("skill")),
                    getStringValue(skillMap.get("value")),
                    getStringValue(skillMap.get("label"))
            );

            if (name != null) {
                addSkillName(skills, name);
            } else {
                Object nested = firstNonNull(
                        skillMap.get("skills"),
                        skillMap.get("items"),
                        skillMap.get("values")
                );

                if (nested != null) {
                    addStructuredSkills(skills, nested);
                }
            }
        }
    }

    /*
     * ADD SKILLS WITHOUT DUPLICATES
     */

    private void addSkillName(List<SkillData> skills, String name) {
        if (skills == null || name == null || name.isBlank()) {
            return;
        }

        String finalName = name.trim();

        boolean exists = skills.stream()
                .filter(Objects::nonNull)
                .map(SkillData::getName)
                .filter(Objects::nonNull)
                .anyMatch(existing ->
                        normalizeSkillName(existing)
                                .equals(normalizeSkillName(finalName))
                );

        if (!exists) {
            skills.add(
                    SkillData.builder()
                            .name(finalName)
                            .build()
            );
        }
    }

    private String normalizeSkillName(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9+#. ]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /*
     * EXTRACT RECOGNIZED SKILLS FROM TEXT
     */

    private void addRecognizedSkillsFromText(
            List<SkillData> skills,
            String... texts
    ) {
        if (skills == null || texts == null || texts.length == 0) {
            return;
        }

        for (String text : texts) {
            if (text == null || text.isBlank()) {
                continue;
            }

            String normalizedText = text.toLowerCase(Locale.ROOT);

            for (String recognizedSkill : RECOGNIZED_SKILLS) {
                if (containsSkill(normalizedText, recognizedSkill)) {
                    addSkillName(skills, recognizedSkill);
                }
            }
        }
    }

    private boolean containsSkill(String normalizedText, String skill) {
        if (normalizedText == null || skill == null || skill.isBlank()) {
            return false;
        }

        String normalizedSkill = skill.toLowerCase(Locale.ROOT);

        if (!normalizedSkill.matches("[a-z0-9 ]+")) {
            return normalizedText.contains(normalizedSkill);
        }

        Pattern pattern = Pattern.compile(
                "(?<![a-z0-9])"
                        + Pattern.quote(normalizedSkill)
                        + "(?![a-z0-9])"
        );

        return pattern.matcher(normalizedText).find();
    }

    /*
     * RESPONSE WRAPPER HANDLING
     */

    private Map<String, Object> unwrapResponse(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return response;
        }

        Map<String, Object> current = response;

        for (int depth = 0; depth < 3; depth++) {
            Map<String, Object> nested = null;

            for (String key : RESPONSE_WRAPPER_KEYS) {
                Object value = current.get(key);

                if (value instanceof Map<?, ?> map) {
                    nested = toStringKeyMap(map);
                    break;
                }
            }

            if (nested == null || nested.isEmpty()) {
                break;
            }

            current = nested;
        }

        return current;
    }

    private Map<String, Object> toStringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : source.entrySet()) {
            if (entry.getKey() != null) {
                result.put(entry.getKey().toString(), entry.getValue());
            }
        }

        return result;
    }

    /*
     * VALUE HELPERS
     */

    private Long getLongValue(Object... values) {
        if (values == null) {
            return null;
        }

        for (Object value : values) {
            if (value == null) {
                continue;
            }

            if (value instanceof Number number) {
                return number.longValue();
            }

            try {
                return Long.parseLong(value.toString().trim());
            } catch (NumberFormatException ignored) {
                // Try the next possible field.
            }
        }

        return null;
    }

    private Integer getIntegerValue(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String getStringValue(Object value) {
        if (value == null || value instanceof Map<?, ?>) {
            return null;
        }

        if (value instanceof Collection<?> collection) {
            if (collection.isEmpty()) {
                return null;
            }

            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.joining(" "))
                    .trim();
        }

        String result = value.toString().trim();
        return result.isEmpty() ? null : result;
    }

    private String getNestedStringValue(Object value, String key) {
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }

        return getStringValue(map.get(key));
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }

        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }

        return null;
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }

        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }

        return null;
    }

    private void addTextValue(List<String> values, Object value) {
        if (values == null || value == null) {
            return;
        }

        String text = getStringValue(value);

        if (text != null && !text.isBlank()) {
            values.add(text);
        }
    }

    /*
     * EMPTY SKILL MATCH
     */

    private SkillMatchResponse emptySkillMatch(Long candidateId) {
        return SkillMatchResponse.builder()
                .candidateId(candidateId)
                .matchingSkills(Collections.emptyList())
                .missingSkills(Collections.emptyList())
                .matchScore(0.0)
                .build();
    }

    /*
     * RESUME ANALYSIS HELPERS
     */

    private void detectStrength(
            String text,
            String keyword,
            String message,
            List<String> strengths
    ) {
        if (text != null && text.contains(keyword)) {
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

        return Math.max(0.0, Math.min(100.0, score));
    }

    /*
     * MATCH EXPLANATION
     */

    private String buildMatchExplanation(double score) {
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
                + score + ".";
    }
}