package com.emmzy.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class MatchingScoreCalculator {

    private MatchingScoreCalculator() {
    }

    public static double calculate(Collection<String> candidateSkills,
                                   Collection<String> requiredSkills) {
        return calculateRatio(candidateSkills, requiredSkills) * 100.0;
    }

    public static double calculateRatio(Collection<String> candidateSkills,
                                        Collection<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 0.0;
        }

        Set<String> candidateSkillSet = normalizeSkills(candidateSkills);
        Set<String> requiredSkillSet = normalizeSkills(requiredSkills);

        if (requiredSkillSet.isEmpty()) {
            return 0.0;
        }

        long matchingSkills = requiredSkillSet.stream()
                .filter(candidateSkillSet::contains)
                .count();

        return matchingSkills * 1.0 / requiredSkillSet.size();
    }

    private static Set<String> normalizeSkills(Collection<String> skills) {
        Set<String> normalized = new HashSet<>();

        if (skills == null) {
            return normalized;
        }

        for (String skill : skills) {
            String value = SkillNormalizer.normalize(skill);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }

        return normalized;
    }

    public static String getMatchLevel(double score) {
        if (score >= 80.0) return "EXCELLENT";
        if (score >= 60.0) return "GOOD";
        if (score >= 40.0) return "MODERATE";
        return "LOW";
    }
}
