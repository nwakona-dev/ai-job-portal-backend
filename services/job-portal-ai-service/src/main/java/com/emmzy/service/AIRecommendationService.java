package com.emmzy.service;

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
import org.jspecify.annotations.Nullable;

import java.util.List;

public interface AIRecommendationService {

    List<JobRecommendationResponse> getJobRecommendations(
            Long candidateId
    );

    JobMatchingResponse matchJob(
            JobMatchRequest request
    );

    SkillMatchResponse getSkillMatch(
            Long candidateId
    );

    List<CandidateRankingResponse> rankCandidates(
            CandidateRankingRequest request
    );

    List<CandidateRecommendationResponse> getCandidateRecommendations(
            Long jobId
    );

    ResumeAnalysisResponse analyzeResume(
            ResumeAnalysisRequest request
    );

    OpenAIChatResponse chat(OpenAIChatRequest request);

    @Nullable SkillMatchResponse matchSkills(Long candidateId);
}

