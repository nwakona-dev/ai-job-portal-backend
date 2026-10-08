package com.emmzy.service;

import com.emmzy.config.GeminiConfig;
import com.emmzy.dto.ai.JobMatchAIResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final RestClient geminiRestClient;
    private final ObjectMapper objectMapper;


    /**
     * General Gemini chat response.
     *
     * Used by:
     * POST /api/ai/chat
     */
    public String generateResponse(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Message cannot be empty"
            );
        }

        Map<String, Object> request = new HashMap<>();

        request.put(
                "contents",
                List.of(
                        Map.of(
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                message
                                        )
                                )
                        )
                )
        );

        String response;

        try {

            response = geminiRestClient
                    .post()
                    .uri(uriBuilder ->
                            uriBuilder
                                    .path(
                                            "/v1beta/models/"
                                                    + geminiConfig.getModel()
                                                    + ":generateContent"
                                    )
                                    .build()
                    )
                    .header(
                            "x-goog-api-key",
                            geminiConfig.getApiKey()
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .body(request)
                    .retrieve()
                    .body(String.class);

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Gemini API request failed: "
                            + exception.getMessage(),
                    exception
            );
        }

        return extractGeminiText(response);
    }


    /**
     * Performs AI-powered job matching using Gemini.
     *
     * Gemini receives:
     *
     * 1. Actual job data
     * 2. Candidate's actual resume
     *
     * Gemini returns:
     *
     * - matchScore
     * - matchingSkills
     * - missingSkills
     * - explanation
     */
    public JobMatchAIResult analyzeJobMatch(
            Map<String, Object> job,
            Map<String, Object> resume
    ) {

        if (job == null || job.isEmpty()) {

            throw new IllegalArgumentException(
                    "Job data cannot be empty"
            );
        }

        if (resume == null) {
            resume = Map.of();
        }


        /*
         * ============================================================
         * CONVERT JOB AND RESUME TO JSON
         * ============================================================
         */

        String jobJson;
        String resumeJson;

        try {

            jobJson = objectMapper.writeValueAsString(job);

            resumeJson = objectMapper.writeValueAsString(resume);

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to convert job or resume data to JSON",
                    exception
            );
        }


        /*
         * ============================================================
         * SYSTEM PROMPT
         * ============================================================
         */

        String systemPrompt = """
                You are an AI recruitment matching engine.

                Your task is to evaluate how well a candidate's resume
                matches a job.

                Follow these rules carefully:

                1. Use ONLY the job and candidate information supplied
                   in the request.

                2. Never invent skills, work experience, education,
                   certifications, responsibilities, or achievements
                   that are not present in the candidate data.

                3. Compare the candidate's actual experience, skills,
                   education, location, job preferences and projects
                   against the job information.

                4. Prioritize the job title, description, requirements,
                   responsibilities, experience level, education
                   requirement, work mode and job type.

                5. Job tags may be useful signals, but DO NOT automatically
                   treat a tag as a mandatory requirement unless the actual
                   job description, requirements or responsibilities support
                   that conclusion.

                6. If the job provides very little information, lower your
                   confidence rather than inventing requirements.

                7. Matching skills must be skills that are actually present
                   in both the candidate information and relevant job
                   requirements.

                8. Missing skills should only contain skills that are
                   reasonably supported by the actual job requirements or
                   responsibilities and are absent from the candidate data.

                9. Do not assume that a technology is required merely because
                   it appears as an unrelated tag.

                10. The matchScore must be a number from 0 to 100.

                11. A score near 100 means the candidate is an excellent
                    match based on strong evidence.

                12. A score near 0 means there is very little evidence that
                    the candidate matches the position.

                13. The explanation must be concise, specific and based on
                    evidence from the supplied data.

                14. Return ONLY valid JSON matching the requested schema.
                """;


        /*
         * ============================================================
         * USER PROMPT
         * ============================================================
         */

        String userPrompt = """
                Evaluate this candidate against this job.

                JOB DATA:
                %s

                CANDIDATE RESUME DATA:
                %s

                Analyze the compatibility between the candidate and job.
                """.formatted(
                jobJson,
                resumeJson
        );


        /*
         * ============================================================
         * JSON SCHEMA
         * ============================================================
         */

        Map<String, Object> schema = new HashMap<>();

        schema.put(
                "type",
                "OBJECT"
        );


        Map<String, Object> properties = new HashMap<>();


        /*
         * matchScore
         */

        properties.put(
                "matchScore",
                Map.of(
                        "type",
                        "NUMBER"
                )
        );


        /*
         * matchingSkills
         */

        properties.put(
                "matchingSkills",
                Map.of(
                        "type",
                        "ARRAY",
                        "items",
                        Map.of(
                                "type",
                                "STRING"
                        )
                )
        );


        /*
         * missingSkills
         */

        properties.put(
                "missingSkills",
                Map.of(
                        "type",
                        "ARRAY",
                        "items",
                        Map.of(
                                "type",
                                "STRING"
                        )
                )
        );


        /*
         * explanation
         */

        properties.put(
                "explanation",
                Map.of(
                        "type",
                        "STRING"
                )
        );


        schema.put(
                "properties",
                properties
        );


        schema.put(
                "required",
                List.of(
                        "matchScore",
                        "matchingSkills",
                        "missingSkills",
                        "explanation"
                )
        );


        schema.put(
                "additionalProperties",
                false
        );


        /*
         * ============================================================
         * BUILD GEMINI REQUEST
         * ============================================================
         */

        Map<String, Object> request = new HashMap<>();


        /*
         * Contents
         */

        request.put(
                "contents",
                List.of(
                        Map.of(
                                "parts",
                                List.of(
                                        Map.of(
                                                "text",
                                                systemPrompt
                                                        + "\n\n"
                                                        + userPrompt
                                        )
                                )
                        )
                )
        );


        /*
         * ============================================================
         * GENERATION CONFIGURATION
         * ============================================================
         */

        Map<String, Object> generationConfig =
                new HashMap<>();


        /*
         * Ask Gemini to return JSON.
         */

        generationConfig.put(
                "responseMimeType",
                "application/json"
        );


        /*
         * Tell Gemini the expected JSON structure.
         *
         * Gemini supports structured JSON output for supported models.
         */

        generationConfig.put(
                "responseJsonSchema",
                schema
        );


        request.put(
                "generationConfig",
                generationConfig
        );


        /*
         * ============================================================
         * CALL GEMINI
         * ============================================================
         */

        String response;

        try {

            response = geminiRestClient
                    .post()
                    .uri(uriBuilder ->
                            uriBuilder
                                    .path(
                                            "/v1beta/models/"
                                                    + geminiConfig.getModel()
                                                    + ":generateContent"
                                    )
                                    .build()
                    )
                    .header(
                            "x-goog-api-key",
                            geminiConfig.getApiKey()
                    )
                    .header(
                            "Content-Type",
                            "application/json"
                    )
                    .body(request)
                    .retrieve()
                    .body(String.class);

        } catch (Exception exception) {

            /*
             * Give a clearer error to the AI service.
             */

            throw new RuntimeException(
                    "Gemini job matching request failed: "
                            + exception.getMessage(),
                    exception
            );
        }


        /*
         * ============================================================
         * EXTRACT GEMINI RESPONSE
         * ============================================================
         */

        String aiJson = extractGeminiText(response);


        /*
         * ============================================================
         * CONVERT GEMINI JSON TO OUR DTO
         * ============================================================
         */

        try {

            JobMatchAIResult result =
                    objectMapper.readValue(
                            aiJson,
                            JobMatchAIResult.class
                    );


            /*
             * Safety validation
             */

            if (result == null) {

                throw new IllegalStateException(
                        "Gemini returned a null job matching result"
                );
            }


            /*
             * ========================================================
             * KEEP SCORE BETWEEN 0 AND 100
             * ========================================================
             */

            double score = result.getMatchScore();


            if (score < 0) {

                result.setMatchScore(0);
            }


            if (score > 100) {

                result.setMatchScore(100);
            }


            /*
             * ========================================================
             * PREVENT NULL ARRAYS
             * ========================================================
             */

            if (result.getMatchingSkills() == null) {

                result.setMatchingSkills(
                        List.of()
                );
            }


            if (result.getMissingSkills() == null) {

                result.setMissingSkills(
                        List.of()
                );
            }


            /*
             * ========================================================
             * PREVENT NULL EXPLANATION
             * ========================================================
             */

            if (result.getExplanation() == null
                    || result.getExplanation().isBlank()) {

                result.setExplanation(
                        "No explanation was returned by Gemini."
                );
            }


            return result;

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to convert Gemini job matching response "
                            + "into JobMatchAIResult. Response: "
                            + aiJson,
                    exception
            );
        }
    }


    /**
     * Extracts generated text from Gemini's response.
     *
     * Normal structure:
     *
     * candidates[]
     *     -> content
     *         -> parts[]
     *             -> text
     */
    private String extractGeminiText(
            String response
    ) {

        if (response == null
                || response.isBlank()) {

            throw new IllegalStateException(
                    "Gemini returned an empty response"
            );
        }


        try {

            JsonNode root =
                    objectMapper.readTree(response);


            /*
             * ========================================================
             * CHECK FOR GEMINI ERROR RESPONSE
             * ========================================================
             */

            JsonNode errorNode =
                    root.path("error");


            if (!errorNode.isMissingNode()
                    && !errorNode.isNull()) {

                throw new IllegalStateException(
                        "Gemini API returned an error: "
                                + errorNode
                );
            }


            /*
             * ========================================================
             * FIND CANDIDATES
             * ========================================================
             */

            JsonNode candidates =
                    root.path("candidates");


            if (!candidates.isArray()
                    || candidates.isEmpty()) {

                JsonNode promptFeedback =
                        root.path("promptFeedback");


                throw new IllegalStateException(
                        "Gemini response did not contain candidates. "
                                + "Prompt feedback: "
                                + promptFeedback
                );
            }


            /*
             * ========================================================
             * EXTRACT TEXT
             * ========================================================
             */

            StringBuilder result =
                    new StringBuilder();


            for (JsonNode candidate : candidates) {

                JsonNode content =
                        candidate.path("content");


                JsonNode parts =
                        content.path("parts");


                if (!parts.isArray()) {
                    continue;
                }


                for (JsonNode part : parts) {

                    JsonNode textNode =
                            part.path("text");


                    if (textNode.isTextual()) {

                        String text =
                                textNode.asText();


                        if (!text.isBlank()) {

                            if (!result.isEmpty()) {

                                result.append("\n");
                            }


                            result.append(text);
                        }
                    }
                }
            }


            /*
             * ========================================================
             * NO TEXT RETURNED
             * ========================================================
             */

            if (result.isEmpty()) {

                throw new IllegalStateException(
                        "Gemini returned no text output. "
                                + "Full response: "
                                + response
                );
            }


            return result
                    .toString()
                    .trim();

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Failed to parse Gemini response: "
                            + exception.getMessage(),
                    exception
            );
        }
    }
}
