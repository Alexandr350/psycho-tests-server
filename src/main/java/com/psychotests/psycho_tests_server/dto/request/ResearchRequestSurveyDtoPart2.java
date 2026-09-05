package com.psychotests.psycho_tests_server.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ResearchRequestSurveyDtoPart2(
        @NotBlank
        String researchId,
        String titleSurvey,
        String descriptionSurvey,
        String rulesSurvey
) {}
