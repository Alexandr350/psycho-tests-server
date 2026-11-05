
package com.psychotests.psycho_tests_server.service;
import com.psychotests.psycho_tests_server.exception.ResearchNotFoundException;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.repository.ResearchRepository;
import com.psychotests.psycho_tests_server.repository.SurveyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ResearchAccessService {

    private final ResearchRepository researchRepository;
    private final SurveyRepository surveyRepository;
    private final CurrentPsychologistProvider psychologistProvider;

    public Research getOwnedResearch(UUID researchId) {
        UUID currentId = psychologistProvider.getPsychologistAccount().getIdPsychologistAccount();
        return researchRepository
                .findByIdAndPsychologistAccountIdPsychologistAccount(researchId, currentId)
                .orElseThrow(() -> new ResearchNotFoundException("Исследование не найдено"));
    }

    public Survey getOwnedSurvey(UUID idSurvey) {
        UUID currentId = psychologistProvider.getPsychologistAccount().getIdPsychologistAccount();
        return surveyRepository
                .findByIdAndResearchPsychologistAccountIdPsychologistAccount(idSurvey, currentId)
                .orElseThrow(() -> new ResearchNotFoundException("Опрос не найден"));
    }
}

