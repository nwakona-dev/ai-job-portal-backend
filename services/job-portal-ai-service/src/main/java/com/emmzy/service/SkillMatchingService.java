package com.emmzy.service;

import com.emmzy.model.CandidateData;
import com.emmzy.model.JobData;
import com.emmzy.model.SkillData;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SkillMatchingService {

    /*
     * CALCULATE MATCH SCORE
     *
     * Exact matches receive full credit.
     * Equivalent names receive normalized-match credit.
     * Known related skills receive partial credit.
     */

    public double calculateMatchScore(
            CandidateData candidate,
            JobData job
    ) {
        if (candidate == null || job == null) {
            return 0.0;
        }

        List<String> candidateSkills =
                extractSkillNames(candidate.getSkills());

        List<String> jobSkills =
                extractSkillNames(job.getSkills());

        if (candidateSkills.isEmpty() || jobSkills.isEmpty()) {
            return 0.0;
        }

        double totalScore = 0.0;

        for (String jobSkill : jobSkills) {
            totalScore += findBestSkillMatch(jobSkill, candidateSkills);
        }

        double score = (totalScore / jobSkills.size()) * 100.0;

        return Math.round(score * 100.0) / 100.0;
    }

    /*
     * GET MATCHING SKILLS
     */

    public List<String> getMatchingSkills(
            CandidateData candidate,
            JobData job
    ) {
        if (candidate == null || job == null) {
            return Collections.emptyList();
        }

        List<String> candidateSkills =
                extractSkillNames(candidate.getSkills());

        List<String> jobSkills =
                extractSkillNames(job.getSkills());

        if (candidateSkills.isEmpty() || jobSkills.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> matchingSkills = new ArrayList<>();

        for (String jobSkill : jobSkills) {
            double score = findBestSkillMatch(jobSkill, candidateSkills);

            if (score >= 0.75) {
                matchingSkills.add(jobSkill);
            }
        }

        return matchingSkills.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /*
     * GET MISSING SKILLS
     */

    public List<String> getMissingSkills(
            CandidateData candidate,
            JobData job
    ) {
        if (candidate == null || job == null) {
            return Collections.emptyList();
        }

        List<String> candidateSkills =
                extractSkillNames(candidate.getSkills());

        List<String> jobSkills =
                extractSkillNames(job.getSkills());

        if (jobSkills.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> missingSkills = new ArrayList<>();

        for (String jobSkill : jobSkills) {
            double score = findBestSkillMatch(jobSkill, candidateSkills);

            if (score < 0.75) {
                missingSkills.add(jobSkill);
            }
        }

        return missingSkills.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /*
     * FIND THE BEST CANDIDATE SKILL FOR A JOB REQUIREMENT
     */

    private double findBestSkillMatch(
            String jobSkill,
            List<String> candidateSkills
    ) {
        if (jobSkill == null
                || jobSkill.isBlank()
                || candidateSkills == null
                || candidateSkills.isEmpty()) {
            return 0.0;
        }

        double bestScore = 0.0;

        for (String candidateSkill : candidateSkills) {
            if (candidateSkill == null || candidateSkill.isBlank()) {
                continue;
            }

            double score = compareSkills(candidateSkill, jobSkill);

            if (score > bestScore) {
                bestScore = score;
            }

            if (bestScore >= 1.0) {
                return 1.0;
            }
        }

        return bestScore;
    }

    /*
     * COMPARE TWO SKILLS
     *
     * 1.00 = exact or equivalent normalized match
     * 0.90 = punctuation or formatting variation
     * 0.80 = known related skill
     * 0.00 = no meaningful relationship
     */

    private double compareSkills(
            String candidateSkill,
            String jobSkill
    ) {
        String candidate = normalizeSkill(candidateSkill);
        String required = normalizeSkill(jobSkill);

        if (candidate.isBlank() || required.isBlank()) {
            return 0.0;
        }

        if (candidate.equals(required)) {
            return 1.0;
        }

        String normalizedCandidate = removeFormatting(candidate);
        String normalizedRequired = removeFormatting(required);

        if (normalizedCandidate.equals(normalizedRequired)) {
            return 0.90;
        }

        if (areRelatedSkills(candidate, required)) {
            return 0.80;
        }

        return 0.0;
    }

    /*
     * NORMALIZE COMMON ALIASES
     */

    private String normalizeSkill(String skill) {
        if (skill == null) {
            return "";
        }

        String normalized = skill
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");

        return switch (normalized) {
            case "js", "ecmascript" -> "javascript";
            case "ts" -> "typescript";
            case "node", "node js" -> "node.js";
            case "express" -> "express.js";
            case "reactjs", "react js" -> "react";
            case "vuejs", "vue js" -> "vue";
            case "angularjs", "angular js" -> "angular";
            case "postgres", "postgres sql" -> "postgresql";
            case "mongo" -> "mongodb";
            case "c sharp", "c-sharp" -> "c#";
            case "c plus plus", "cpp" -> "c++";
            case "dotnet", "dot net" -> ".net";
            case "springboot", "spring-boot" -> "spring boot";
            case "springframework", "spring-framework" -> "spring framework";
            case "tailwind", "tailwindcss" -> "tailwind css";
            case "restful api", "rest apis", "restful apis" -> "rest api";
            case "amazon web services" -> "aws";
            case "google cloud platform", "gcp" -> "google cloud";
            case "ms sql", "mssql", "sql server" -> "sql server";
            case "powerbi" -> "power bi";
            case "ui ux", "user interface design" -> "ui/ux";
            case "driver license", "driver's license",
                 "driving license", "driving licence",
                 "drivers licence" -> "drivers license";
            default -> normalized;
        };
    }

    /*
     * REMOVE FORMATTING WITHOUT MAKING DIFFERENT LANGUAGES IDENTICAL.
     *
     * C++, C#, and .NET retain their meaningful symbols.
     */

    private String removeFormatting(String value) {
        if (value == null) {
            return "";
        }

        return value
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("-", "")
                .replace("_", "")
                .replace(".", "")
                .replace("/", "");
    }

    /*
     * KNOWN RELATED SKILLS
     *
     * These groups provide partial credit, not full equivalence.
     * Membership in the same group does not mean two skills are
     * interchangeable in a real job.
     */

    private boolean areRelatedSkills(
            String candidate,
            String required
    ) {
        Set<String> candidateGroup = getSkillGroup(candidate);
        Set<String> requiredGroup = getSkillGroup(required);

        if (candidateGroup.isEmpty() || requiredGroup.isEmpty()) {
            return false;
        }

        return !Collections.disjoint(candidateGroup, requiredGroup);
    }

    private Set<String> getSkillGroup(String skill) {
        Set<String> groups = new LinkedHashSet<>();

        if (skill == null || skill.isBlank()) {
            return groups;
        }

        switch (skill) {
            case "java",
                 "spring",
                 "spring boot",
                 "spring framework",
                 "jpa",
                 "hibernate",
                 "maven",
                 "gradle" ->
                    groups.add("java-ecosystem");

            case "javascript",
                 "typescript",
                 "react",
                 "react native",
                 "node.js",
                 "express.js" ->
                    groups.add("javascript-ecosystem");

            case "python",
                 "django",
                 "flask" ->
                    groups.add("python-ecosystem");

            case "sql",
                 "mysql",
                 "postgresql",
                 "sql server" ->
                    groups.add("relational-database");

            case "mongodb",
                 "redis" ->
                    groups.add("nosql-database");

            case "aws",
                 "azure",
                 "google cloud" ->
                    groups.add("cloud-platform");

            case "docker",
                 "kubernetes",
                 "jenkins" ->
                    groups.add("devops");

            case "html",
                 "css",
                 "bootstrap",
                 "tailwind css" ->
                    groups.add("frontend");

            case "git",
                 "github" ->
                    groups.add("version-control");

            case "selenium" ->
                    groups.add("testing");

            case "data analysis",
                 "power bi",
                 "tableau",
                 "excel" ->
                    groups.add("data-analytics");

            case "driving",
                 "drivers license" ->
                    groups.add("driving");

            default -> {
                // Unknown skills are compared by exact or normalized name.
            }
        }

        return groups;
    }

    /*
     * EXTRACT CLEAN SKILL NAMES
     */

    private List<String> extractSkillNames(List<SkillData> skills) {
        if (skills == null || skills.isEmpty()) {
            return Collections.emptyList();
        }

        Set<String> uniqueSkills = new LinkedHashSet<>();

        for (SkillData skill : skills) {
            if (skill == null || skill.getName() == null) {
                continue;
            }

            String name = skill.getName().trim();

            if (!name.isBlank()) {
                uniqueSkills.add(name);
            }
        }

        return new ArrayList<>(uniqueSkills);
    }
}