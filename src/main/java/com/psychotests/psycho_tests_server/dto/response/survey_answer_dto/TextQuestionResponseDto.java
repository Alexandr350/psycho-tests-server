package com.psychotests.psycho_tests_server.dto.response.survey_answer_dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record TextQuestionResponseDto(
        @NotNull UUID idCharacteristic,
        @NotNull @Size(min = 1, max = 200) String question
) {}
