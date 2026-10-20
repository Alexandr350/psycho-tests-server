package com.psychotests.psycho_tests_server.dto.response.survey_answer_dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SurveyFlowStartResponseDto(
        @NotNull UUID idSession
) {}
