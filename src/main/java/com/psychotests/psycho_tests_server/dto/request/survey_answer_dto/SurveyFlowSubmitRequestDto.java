package com.psychotests.psycho_tests_server.dto.request.survey_answer_dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Тело POST /api/CPSKTest/submit/{surveyId} — все ответы респондента.
 */
public record SurveyFlowSubmitRequestDto(
        @NotNull UUID idSession,
        @NotNull @Valid List<CharacteristicAnswerDto> characteristicAnswer,
        @NotNull @Valid List<ImageAnswerDto> imageAnswer,
        @NotNull @Valid List<ImageAnswerDto> imageSubAnswer,
        @NotNull @Valid List<AssociationAnswerDto> associationsAnswer
) {}