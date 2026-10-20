package com.psychotests.psycho_tests_server.dto.response.survey_answer_dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record SurveyFlowResponseDto(
        @NotNull List<TextQuestionResponseDto> characteristics,
        @NotNull List<ImageResponseDto> items,
        @NotNull List<ImageResponseDto> itemsSub,
        @NotNull InfoResponseDto info
) {}


