package com.emmzy.service;

import com.emmzy.config.AIConfig;
import com.emmzy.model.CandidateData;
import com.emmzy.model.JobData;
import com.emmzy.model.SkillData;
import com.emmzy.util.MatchingScoreCalculator;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;


@Service
public class SkillMatchingService {

    private final AIConfig aiConfig;

    public SkillMatchingService(AIConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    public double calculateMatchScore(CandidateData candidate, JobData job) {
        if (candidate == null || job == null) {
            return 0.0;
        }

        if (!aiConfig.getMatching().isEnabled()) {
            return MatchingScoreCalculator.calculate(
                    extractSkillNames(candidate.getSkills()),
                    extractSkillNames(job.getSkills())
            );
        }

        double skills = skillScore(candidate, job);
        double experience = experienceScore(candidate.getExperience(), job.getExperienceLevel());
        double education = educationScore(candidate.getEducation(), job.getEducationRequirement());
        double location = locationScore(candidate.getLocation(), job.getCity(), job.getCountry(), job.getWorkMode());
        double jobType = textMatchScore(candidate.getPreferredJobType(), job.getJobType());

        AIConfig.Weights weights = aiConfig.getMatching().getWeights();

        double total =
                skills * weights.getSkills()
                        + experience * weights.getExperience()
                        + education * weights.getEducation()
                        + location * weights.getLocation()
                        + jobType * weights.getJobType();

        return round(total * 100.0);
    }

    private double skillScore(CandidateData candidate, JobData job) {
        return MatchingScoreCalculator.calculateRatio(
                extractSkillNames(candidate == null ? null : candidate.getSkills()),
                extractSkillNames(job == null ? null : job.getSkills())
        );
    }

    public List<String> getMatchingSkills(CandidateData candidate, JobData job) {
        if (candidate == null || job == null) {
            return Collections.emptyList();
        }

        List<String> candidateSkills = extractSkillNames(candidate.getSkills());
        List<String> jobSkills = extractSkillNames(job.getSkills());

        return jobSkills.stream()
                .filter(jobSkill -> candidateSkills.stream()
                        .anyMatch(candidateSkill ->
                                candidateSkill.equalsIgnoreCase(jobSkill)))
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getMissingSkills(CandidateData candidate, JobData job) {
        if (candidate == null || job == null) {
            return Collections.emptyList();
        }

        List<String> candidateSkills = extractSkillNames(candidate.getSkills());
        List<String> jobSkills = extractSkillNames(job.getSkills());

        return jobSkills.stream()
                .filter(jobSkill -> candidateSkills.stream()
                        .noneMatch(candidateSkill ->
                                candidateSkill.equalsIgnoreCase(jobSkill)))
                .distinct()
                .collect(Collectors.toList());
    }

    private double experienceScore(String candidateExperience, String requiredExperience) {
        if (isBlank(requiredExperience)) {
            return 0.5;
        }
        if (isBlank(candidateExperience)) {
            return 0.0;
        }

        String candidate = candidateExperience.toLowerCase(Locale.ROOT);
        String required = requiredExperience.toLowerCase(Locale.ROOT);

        Integer candidateYears = extractYears(candidate);
        Integer requiredYears = extractYears(required);

        if (candidateYears != null && requiredYears != null) {
            if (candidateYears >= requiredYears) {
                return 1.0;
            }
            return Math.max(0.0, (double) candidateYears / requiredYears);
        }

        if (containsAny(candidate, required, "senior", "lead", "principal")) {
            return 1.0;
        }
        if (containsAny(candidate, required, "mid", "intermediate")) {
            return 0.75;
        }
        if (containsAny(candidate, required, "junior", "entry", "graduate")) {
            return 0.75;
        }

        return candidate.contains(required) || required.contains(candidate)
                ? 1.0
                : 0.5;
    }

    private double educationScore(String candidateEducation, String requiredEducation) {
        if (isBlank(requiredEducation)) {
            return 0.5;
        }
        if (isBlank(candidateEducation)) {
            return 0.0;
        }

        return textMatchScore(candidateEducation, requiredEducation);
    }

    private double locationScore(
            String candidateLocation,
            String city,
            String country,
            String workMode
    ) {
        if ("REMOTE".equalsIgnoreCase(workMode)) {
            return 1.0;
        }

        if (isBlank(candidateLocation)) {
            return 0.5;
        }

        String location = candidateLocation.toLowerCase(Locale.ROOT);
        String jobCity = city == null ? "" : city.toLowerCase(Locale.ROOT);
        String jobCountry = country == null ? "" : country.toLowerCase(Locale.ROOT);

        if ((!jobCity.isBlank() && location.contains(jobCity))
                || (!jobCountry.isBlank() && location.contains(jobCountry))) {
            return 1.0;
        }

        return 0.0;
    }

    private double textMatchScore(String first, String second) {
        if (isBlank(first) || isBlank(second)) {
            return isBlank(first) && isBlank(second) ? 0.5 : 0.0;
        }

        String left = first.trim().toLowerCase(Locale.ROOT);
        String right = second.trim().toLowerCase(Locale.ROOT);

        if (left.equals(right) || left.contains(right) || right.contains(left)) {
            return 1.0;
        }

        return 0.0;
    }

    private Integer extractYears(String value) {
        if (value == null) {
            return null;
        }

        java.util.regex.Matcher matcher =
                java.util.regex.Pattern.compile("(\\d+)\\s*(?:\\+)?\\s*(?:years?|yrs?)")
                        .matcher(value);

        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private boolean containsAny(String candidate, String required, String... words) {
        for (String word : words) {
            if (candidate.contains(word) && required.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private List<String> extractSkillNames(List<SkillData> skills) {
        if (skills == null) {
            return Collections.emptyList();
        }

        return skills.stream()
                .filter(Objects::nonNull)
                .map(SkillData::getName)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(skill -> !skill.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
