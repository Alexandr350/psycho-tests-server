package com.psychotests.psycho_tests_server.dto.response.survey_answer_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ImageResponseDto(
        @NotNull UUID idItem,
        @NotBlank String imageUrl
) {
}
