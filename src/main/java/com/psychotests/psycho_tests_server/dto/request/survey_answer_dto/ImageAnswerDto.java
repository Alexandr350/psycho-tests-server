package com.psychotests.psycho_tests_server.dto.request.survey_answer_dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ImageAnswerDto(
        @NotNull UUID idImage,
        @NotNull @Min(1) @Max(8) Integer numberSector
        ) {}
