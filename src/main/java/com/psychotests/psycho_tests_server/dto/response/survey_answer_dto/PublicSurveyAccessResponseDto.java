package com.psychotests.psycho_tests_server.dto.response.survey_answer_dto;

import java.util.UUID;

public record PublicSurveyAccessResponseDto(
        UUID idSurvey,
        String titleSurvey,
        String descriptionSurvey,
        String rulesSurvey
) {}

