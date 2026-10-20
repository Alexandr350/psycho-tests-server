package com.psychotests.psycho_tests_server.dto.response.research_dto;

import java.util.UUID;

public record SurveyCreatedResponseDto(UUID surveyId) {
    public static  SurveyCreatedResponseDto from(
            UUID idSurvey

    ){
        return new SurveyCreatedResponseDto(idSurvey);
    }
}
