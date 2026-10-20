package com.psychotests.psycho_tests_server.service;

import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestCharacteristicsDtoPart3;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestInfoDtoPart1;
import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestSurveyDtoPart2;
import com.psychotests.psycho_tests_server.dto.response.research_dto.ResearchResponseInfoDto;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

import static com.psychotests.psycho_tests_server.service.Utility.createUUID;
import static com.psychotests.psycho_tests_server.service.Utility.splitTokens;

@Service
@RequiredArgsConstructor
public class CreateResearchService {

    private final ResearchRepository researchRepository;
    private final SurveyRepository surveyRepository;
    private final CharacteristicsRepository characteristicsRepository;
    private final ItemsRepository itemsRepository;
    private final ItemsSubRepository itemsSubRepository;
    private final ResearchAccessService researchAccessService;
    private final CurrentPsychologistProvider currentPsychologistProvider;

    @Transactional
    public UUID addInfoResearch(ResearchRequestInfoDtoPart1 dtoPart1) {
        Research research = new Research(
                createUUID(),
                dtoPart1.nameResearch(),
                dtoPart1.descriptionResearch(),
                dtoPart1.customerResearch(),
                dtoPart1.typeResearch()
        );
        research.setPsychologistAccount(currentPsychologistProvider.getPsychologistAccount());
        return researchRepository.save(research).getIDResearch();
    }

    @Transactional
    public UUID addInfoSurvey(ResearchRequestSurveyDtoPart2 dtoPart2) {
        Research research = researchAccessService.getOwnedResearch(dtoPart2.idResearch());

        Survey survey = new Survey(
                createUUID(),
                dtoPart2.titleSurvey(),
                dtoPart2.descriptionSurvey(),
                dtoPart2.rulesSurvey()
        );
        survey.setResearch(research);
        return surveyRepository.save(survey).getIDSurvey();
    }

    @Transactional
    public void addCharacteristics(ResearchRequestCharacteristicsDtoPart3 dtoPart3) {
        Survey survey = researchAccessService.getOwnedSurvey(dtoPart3.idSurvey());

        List<Characteristics> tokens = splitTokens(dtoPart3.characteristics())
                .stream()
                .map(txt -> {
                    Characteristics characteristics = new Characteristics(UUID.randomUUID(), txt);
                    characteristics.setSurvey(survey);
                    return characteristics;
                })
                .toList();
        characteristicsRepository.saveAll(tokens);
    }

    @Transactional
    public void addItems(UUID surveyId, List<String> itemPaths) {
        Survey survey = researchAccessService.getOwnedSurvey(surveyId);

        List<Items> items = itemPaths.stream()
                .map(path -> {
                    Items item = new Items(UUID.randomUUID(), path);
                    item.setSurvey(survey);
                    return item;
                })
                .toList();
        itemsRepository.saveAll(items);
    }

    @Transactional
    public ResearchResponseInfoDto addItemSub(UUID surveyId, List<String> itemPaths) {
        Survey survey = researchAccessService.getOwnedSurvey(surveyId);

        List<ItemsSub> itemsSub = itemPaths.stream()
                .map(path -> {
                    ItemsSub item = new ItemsSub(UUID.randomUUID(),survey, path);
                    item.setSurvey(survey);
                    return item;
                })
                .toList();
        itemsSubRepository.saveAll(itemsSub);

        Research research = survey.getResearch();
        research.changeStatus(ResearchStatus.NEW);
        return ResearchResponseInfoDto.from(research);
    }
}
