package com.psychotests.psycho_tests_server.dto.request.survey_answer_dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record AssociationAnswerDto(
        @NotNull @Min(1) @Max(8) Integer numberSector,
        @NotBlank @Size(min = 1, max = 200) String text
        ) {
}
