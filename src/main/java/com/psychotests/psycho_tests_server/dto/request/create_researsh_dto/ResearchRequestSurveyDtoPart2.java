package com.psychotests.psycho_tests_server.dto.request.create_researsh_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResearchRequestSurveyDtoPart2(
        @NotNull
        UUID idResearch,
        String titleSurvey,
        String descriptionSurvey,
        String rulesSurvey
) {}
