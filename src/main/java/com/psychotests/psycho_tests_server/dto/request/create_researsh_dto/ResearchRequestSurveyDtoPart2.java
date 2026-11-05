package com.psychotests.psycho_tests_server.dto.request.create_researsh_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ResearchRequestSurveyDtoPart2(
        @NotNull(message = "Нужен id исследования, к которому относится опрос")
        UUID idResearch,

        @NotBlank(message = "Название опроса не может быть пустым")
        String titleSurvey,

        String descriptionSurvey,   // опционально: на фронте поле не required

        @NotBlank(message = "Правила опроса не могут быть пустыми")
        String rulesSurvey
) {}
