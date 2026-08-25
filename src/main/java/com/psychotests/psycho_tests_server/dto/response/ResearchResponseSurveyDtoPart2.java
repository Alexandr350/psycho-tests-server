package com.psychotests.psycho_tests_server.dto.response;

import com.psychotests.psycho_tests_server.model.Survey;
import org.springframework.http.ResponseEntity;

public record ResearchResponseSurveyDtoPart2(
        String titleSurvey,
        String descriptionSurvey,
        String rulesSurvey
) {
    public static ResearchResponseSurveyDtoPart2 from(Survey survey){
        return new ResearchResponseSurveyDtoPart2(
                survey.getTitleSurvey(),
                survey.getDescriptionSurvey(),
                survey.getRulesSurvey()
        );
    }
}
