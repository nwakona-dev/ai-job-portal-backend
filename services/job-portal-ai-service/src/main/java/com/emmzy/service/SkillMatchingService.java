package com.emmzy.service;

import com.emmzy.model.CandidateData;
import com.emmzy.model.JobData;
import com.emmzy.model.SkillData;
import com.emmzy.util.MatchingScoreCalculator;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class SkillMatchingService {

    public double calculateMatchScore(
            CandidateData candidate,
            JobData job
    ) {
        if (candidate == null || job == null) {
            return 0.0;
        }

        List<String> candidateSkills = extractSkillNames(
                candidate.getSkills()
        );

        List<String> jobSkills = extractSkillNames(
                job.getSkills()
        );

        return MatchingScoreCalculator.calculate(
                candidateSkills,
                jobSkills
        );
    }

    public List<String> getMatchingSkills(
            CandidateData candidate,
            JobData job
    ) {
        if (candidate == null || job == null) {
            return Collections.emptyList();
        }

        List<String> candidateSkills = extractSkillNames(
                candidate.getSkills()
        );

        List<String> jobSkills = extractSkillNames(
                job.getSkills()
        );

        return jobSkills.stream()
                .filter(jobSkill ->
                        candidateSkills.stream()
                                .anyMatch(candidateSkill ->
                                        candidateSkill.equalsIgnoreCase(jobSkill)
                                )
                )
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getMissingSkills(
            CandidateData candidate,
            JobData job
    ) {
        if (candidate == null || job == null) {
            return Collections.emptyList();
        }

        List<String> candidateSkills = extractSkillNames(
                candidate.getSkills()
        );

        List<String> jobSkills = extractSkillNames(
                job.getSkills()
        );

        return jobSkills.stream()
                .filter(jobSkill ->
                        candidateSkills.stream()
                                .noneMatch(candidateSkill ->
                                        candidateSkill.equalsIgnoreCase(jobSkill)
                                )
                )
                .distinct()
                .collect(Collectors.toList());
    }

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

