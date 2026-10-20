package com.psychotests.psycho_tests_server.dto.request.survey_answer_dto;

import jakarta.validation.constraints.*;

import java.util.UUID;


public record SurveyFlowQuestionnaireRequestDto(
        @NotNull UUID idSession,
        @NotNull @Min(1) @Max(120) Integer age,
        @NotBlank @Size(max = 1) @Pattern(regexp = "[МЖмж]") String gender
) {}
