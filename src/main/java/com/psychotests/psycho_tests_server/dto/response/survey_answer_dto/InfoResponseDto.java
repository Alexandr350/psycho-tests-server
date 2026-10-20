package com.psychotests.psycho_tests_server.dto.response.survey_answer_dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InfoResponseDto(
        @NotNull @Min(1) @Max(8) int sectorCount,
        @NotNull @Max(5) int minAssociationsPerSector
) {
}
