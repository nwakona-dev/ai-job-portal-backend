package com.emmzy.service;

import com.emmzy.model.CandidateData;
import com.emmzy.model.JobData;
import com.emmzy.model.SkillData;
import com.emmzy.util.MatchingScoreCalculator;
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
     * ============================================================
     * CALCULATE MATCH SCORE
     * ============================================================
     *
     * Calculates a percentage based on how many job skills are
     * covered by the candidate.
     *
     * Exact matches receive full credit.
     *
     * Related skills receive partial credit where a clear
     * relationship exists.
     *
     * ============================================================
     */

    public double calculateMatchScore(
            CandidateData candidate,
            JobData job
    ) {

        if (candidate == null || job == null) {
            return 0.0;
        }

        List<String> candidateSkills =
                extractSkillNames(
                        candidate.getSkills()
                );

        List<String> jobSkills =
                extractSkillNames(
                        job.getSkills()
                );

        if (candidateSkills.isEmpty()
                || jobSkills.isEmpty()) {

            return 0.0;
        }

        double totalScore = 0.0;

        for (String jobSkill : jobSkills) {

            double bestMatch =
                    findBestSkillMatch(
                            jobSkill,
                            candidateSkills
                    );

            totalScore += bestMatch;
        }

        double score =
                (totalScore / jobSkills.size())
                        * 100.0;

        return Math.round(score * 100.0)
                / 100.0;
    }

    /*
     * ============================================================
     * GET MATCHING SKILLS
     * ============================================================
     *
     * Returns both exact and strongly related skills.
     * ============================================================
     */

    public List<String> getMatchingSkills(
            CandidateData candidate,
            JobData job
    ) {

        if (candidate == null || job == null) {
            return Collections.emptyList();
        }

        List<String> candidateSkills =
                extractSkillNames(
                        candidate.getSkills()
                );

        List<String> jobSkills =
                extractSkillNames(
                        job.getSkills()
                );

        if (candidateSkills.isEmpty()
                || jobSkills.isEmpty()) {

            return Collections.emptyList();
        }

        List<String> matchingSkills =
                new ArrayList<>();

        for (String jobSkill : jobSkills) {

            double matchScore =
                    findBestSkillMatch(
                            jobSkill,
                            candidateSkills
                    );

            /*
             * 0.75+ means the relationship is strong enough
             * to display as a matching skill.
             */
            if (matchScore >= 0.75) {
                matchingSkills.add(jobSkill);
            }
        }

        return matchingSkills.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /*
     * ============================================================
     * GET MISSING SKILLS
     * ============================================================
     *
     * A skill is considered missing when there is no exact or
     * strong related candidate skill.
     * ============================================================
     */

    public List<String> getMissingSkills(
            CandidateData candidate,
            JobData job
    ) {

        if (candidate == null || job == null) {
            return Collections.emptyList();
        }

        List<String> candidateSkills =
                extractSkillNames(
                        candidate.getSkills()
                );

        List<String> jobSkills =
                extractSkillNames(
                        job.getSkills()
                );

        if (jobSkills.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> missingSkills =
                new ArrayList<>();

        for (String jobSkill : jobSkills) {

            double matchScore =
                    findBestSkillMatch(
                            jobSkill,
                            candidateSkills
                    );

            if (matchScore < 0.75) {
                missingSkills.add(jobSkill);
            }
        }

        return missingSkills.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /*
     * ============================================================
     * BEST SKILL MATCH
     * ============================================================
     *
     * Score:
     *
     * 1.00 = exact match
     * 0.90 = normalized match
     * 0.80 = known equivalent
     * 0.75 = strong related skill
     * 0.00 = no meaningful relationship
     * ============================================================
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

        for (String candidateSkill :
                candidateSkills) {

            if (candidateSkill == null
                    || candidateSkill.isBlank()) {

                continue;
            }

            double score =
                    compareSkills(
                            candidateSkill,
                            jobSkill
                    );

            if (score > bestScore) {
                bestScore = score;
            }

            /*
             * Nothing can beat an exact match.
             */
            if (bestScore >= 1.0) {
                return 1.0;
            }
        }

        return bestScore;
    }

    /*
     * ============================================================
     * COMPARE SKILLS
     * ============================================================
     */

    private double compareSkills(
            String candidateSkill,
            String jobSkill
    ) {

        String candidate =
                normalizeSkill(
                        candidateSkill
                );

        String required =
                normalizeSkill(
                        jobSkill
                );

        if (candidate.isBlank()
                || required.isBlank()) {

            return 0.0;
        }

        /*
         * --------------------------------------------------------
         * Exact match
         * --------------------------------------------------------
         */

        if (candidate.equals(required)) {
            return 1.0;
        }

        /*
         * --------------------------------------------------------
         * Normalized variations.
         *
         * Example:
         *
         * "Driver's License"
         * "Drivers License"
         * --------------------------------------------------------
         */

        String normalizedCandidate =
                removePunctuation(
                        candidate
                );

        String normalizedRequired =
                removePunctuation(
                        required
                );

        if (normalizedCandidate.equals(
                normalizedRequired
        )) {

            return 0.90;
        }

        /*
         * --------------------------------------------------------
         * Known skill relationships.
         * --------------------------------------------------------
         */

        if (areRelatedSkills(
                candidate,
                required
        )) {

            return 0.80;
        }

        return 0.0;
    }

    /*
     * ============================================================
     * RELATED SKILLS
     * ============================================================
     *
     * This intentionally uses conservative relationships.
     *
     * We don't want:
     *
     * Java → Python
     *
     * to become a match simply because both are programming
     * languages.
     *
     * Instead we define genuinely useful equivalents.
     * ============================================================
     */

    private boolean areRelatedSkills(
            String candidate,
            String required
    ) {

        Set<String> candidateGroup =
                getSkillGroup(candidate);

        Set<String> requiredGroup =
                getSkillGroup(required);

        if (candidateGroup.isEmpty()
                || requiredGroup.isEmpty()) {

            return false;
        }

        return !Collections.disjoint(
                candidateGroup,
                requiredGroup
        );
    }

    /*
     * ============================================================
     * SKILL GROUPS
     * ============================================================
     */

    private Set<String> getSkillGroup(
            String skill
    ) {

        Set<String> group =
                new LinkedHashSet<>();

        if (skill == null
                || skill.isBlank()) {

            return group;
        }

        switch (skill) {

            /*
             * ----------------------------------------------------
             * Java ecosystem
             * ----------------------------------------------------
             */

            case "java",
                 "spring",
                 "spring boot",
                 "spring framework",
                 "jpa",
                 "hibernate",
                 "maven",
                 "gradle":

                group.add("java");
                break;

            /*
             * ----------------------------------------------------
             * JavaScript ecosystem
             * ----------------------------------------------------
             */

            case "javascript",
                 "typescript",
                 "react",
                 "react native",
                 "node.js",
                 "node",
                 "express.js",
                 "express":

                group.add("javascript");
                break;

            /*
             * ----------------------------------------------------
             * Python ecosystem
             * ----------------------------------------------------
             */

            case "python",
                 "django",
                 "flask":

                group.add("python");
                break;

            /*
             * ----------------------------------------------------
             * Database ecosystem
             * ----------------------------------------------------
             */

            case "sql",
                 "mysql",
                 "postgresql",
                 "mongodb",
                 "redis":

                group.add("database");
                break;

            /*
             * ----------------------------------------------------
             * Cloud
             * ----------------------------------------------------
             */

            case "aws",
                 "azure",
                 "google cloud":

                group.add("cloud");
                break;

            /*
             * ----------------------------------------------------
             * DevOps
             * ----------------------------------------------------
             */

            case "docker",
                 "kubernetes",
                 "jenkins":

                group.add("devops");
                break;

            /*
             * ----------------------------------------------------
             * Frontend
             * ----------------------------------------------------
             */

            case "html",
                 "css",
                 "bootstrap",
                 "tailwind css":

                group.add("frontend");
                break;

            /*
             * ----------------------------------------------------
             * Version control
             * ----------------------------------------------------
             */

            case "git",
                 "github":

                group.add("version-control");
                break;

            /*
             * ----------------------------------------------------
             * Testing
             * ----------------------------------------------------
             */

            case "selenium":

                group.add("testing");
                break;

            /*
             * ----------------------------------------------------
             * Data / analytics
             * ----------------------------------------------------
             */

            case "data analysis",
                 "power bi",
                 "tableau",
                 "excel":

                group.add("data-analytics");
                break;

            /*
             * ----------------------------------------------------
             * Driving
             * ----------------------------------------------------
             */

            case "driving",
                 "drivers license",
                 "driver license",
                 "driver's license":

                group.add("driving");
                break;

            default:
                break;
        }

        return group;
    }

    /*
     * ============================================================
     * NORMALIZE SKILL
     * ============================================================
     */

    private String normalizeSkill(
            String skill
    ) {

        if (skill == null) {
            return "";
        }

        return skill
                .trim()
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ");
    }

    /*
     * ============================================================
     * REMOVE PUNCTUATION
     * ============================================================
     */

    private String removePunctuation(
            String value
    ) {

        if (value == null) {
            return "";
        }

        return value
                .replaceAll(
                        "[^a-z0-9 ]",
                        ""
                )
                .replaceAll(
                        "\\s+",
                        " "
                )
                .trim();
    }

    /*
     * ============================================================
     * EXTRACT SKILL NAMES
     * ============================================================
     */

    private List<String> extractSkillNames(
            List<SkillData> skills
    ) {

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