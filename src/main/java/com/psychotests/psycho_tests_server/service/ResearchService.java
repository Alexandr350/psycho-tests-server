package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.response.research_dto.ResearchResponseInfoDto;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.repository.ResearchRepository;
import com.psychotests.psycho_tests_server.repository.SurveyRepository;
import com.psychotests.psycho_tests_server.repository.SurveySessionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ResearchService {

    @PersistenceContext
    private EntityManager entityManager;
    private final ResearchRepository researchRepository;
    private final ResearchAccessService researchAccessService;
    private final CurrentPsychologistProvider currentPsychologistProvider;
    private final FileStorageService fileStorageService;
    private final SurveyRepository surveyRepository;
    private final SurveySessionRepository surveySessionRepository;

    @Transactional(readOnly = true)
    public List<ResearchResponseInfoDto> findAllResearches() {
        UUID currentId = currentPsychologistProvider.getPsychologistAccount().getIdPsychologistAccount();
        return researchRepository.findAllByPsychologistAccountIdPsychologistAccount(currentId).stream()
                .map(ResearchResponseInfoDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResearchResponseInfoDto getResearch(UUID researchId) {
        Research research = researchAccessService.getOwnedResearch(researchId);
        return ResearchResponseInfoDto.from(research);
    }

    @Transactional
    public void changeStatus(UUID researchId, ResearchStatus newStatus) {
        Research research = researchAccessService.getOwnedResearch(researchId);
        research.changeStatus(newStatus);
    }

    @Transactional
    public void deleteResearch(UUID researchId) {
        entityManager.flush();
        entityManager.clear();
        Research research = researchAccessService.getOwnedResearch(researchId);

        if(!research.getStatusResearch().canBeDeleted()){
            throw new IllegalStateException("Нельзя удалить исследование в статусе " + research.getStatusResearch());
        }

        Survey survey = research.getSurveyResearch();

        if(survey != null && surveySessionRepository.existsBySurveyIDSurvey(survey.getIDSurvey())){
            throw new IllegalStateException("Нельзя удалить исследование с сессиями прохождения");
        }
        if(survey != null){
            fileStorageService.deleteSurveyImages(survey.getIDSurvey());
            surveyRepository.delete(survey);
        }
        researchRepository.delete(research);

        entityManager.flush();
        entityManager.clear();
    }
}
