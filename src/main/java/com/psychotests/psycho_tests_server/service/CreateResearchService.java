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
                UUID.randomUUID(),
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

        Survey survey = surveyRepository
                .findByResearchIdResearchAndPsychologistId(research.getIDResearch(),
                        currentPsychologistProvider.getPsychologistAccount().getIdPsychologistAccount())
                .orElse(null);

        if (survey != null) {
            if (research.getStatusResearch() != ResearchStatus.DRAFT) {
                throw new IllegalStateException(
                        "Опрос уже создан; редактирование — через PATCH /survey/{id}");
            }
            survey.setTitleSurvey(dtoPart2.titleSurvey());
            survey.setDescriptionSurvey(dtoPart2.descriptionSurvey());
            survey.setRulesSurvey(dtoPart2.rulesSurvey());
            return survey.getIdSurvey();
        }

        Survey created = new Survey(UUID.randomUUID(), dtoPart2.titleSurvey(),
                dtoPart2.descriptionSurvey(), dtoPart2.rulesSurvey());
        created.setResearch(research);
        return surveyRepository.save(created).getIdSurvey();
    }


    @Transactional
    public void addCharacteristics(ResearchRequestCharacteristicsDtoPart3 dtoPart3) {
        Survey survey = researchAccessService.getOwnedSurvey(dtoPart3.idSurvey());

        List<Characteristics> tokens = splitTokens(dtoPart3.characteristics()).stream()
                .map(txt -> new Characteristics(UUID.randomUUID(), survey, txt))
                .toList();

        // save с назначенным UUID выполняет merge: возвращает управляемую копию,
        // а оригинал остаётся транзиентным — в коллекцию кладём копии
        characteristicsRepository.saveAll(tokens)
                .forEach(managed -> survey.getCharacteristicsSurvey().add(managed));
    }



    @Transactional
    public void addItems(UUID idSurvey, List<String> itemPaths) {
        Survey survey = researchAccessService.getOwnedSurvey(idSurvey);

        List<Items> items = itemPaths.stream()
                .map(path -> {
                    Items item = new Items(UUID.randomUUID(), path);
                    item.setSurvey(survey);
                    return item;
                })
                .toList();

        itemsRepository.saveAll(items)
                .forEach(managed -> survey.getItemsSurvey().add(managed));
    }



    /**
     * Обновляет опрос (шаг 2) при резюме черновика из мастера.
     * Опрос перезаписывается, а не создаётся заново: POST /survey при
     * повторном прохождении шага 2 дублировал бы запись.
     */
    @Transactional
    public ResearchResponseInfoDto updateSurvey(UUID idSurvey, ResearchRequestSurveyDtoPart2 dtoPart2) {
        Survey survey = researchAccessService.getOwnedSurvey(idSurvey);

        Research research = survey.getResearch();
        if (research.getStatusResearch() != ResearchStatus.DRAFT) {
            throw new IllegalStateException(
                    "Недопустимый переход статуса: " + research.getStatusResearch() + " → PATCH survey");
        }

        survey.setTitleSurvey(dtoPart2.titleSurvey());
        survey.setDescriptionSurvey(dtoPart2.descriptionSurvey());
        survey.setRulesSurvey(dtoPart2.rulesSurvey());

        // save не нужен: управляемая сущность, flush сохранит изменения;
        // собираем ответ после изменений — поля опроса попадут в DTO
        return ResearchResponseInfoDto.from(research, survey);
    }



    @Transactional
    public ResearchResponseInfoDto addItemSub(UUID idSurvey, List<String> itemPaths) {
        Survey survey = researchAccessService.getOwnedSurvey(idSurvey);

        Research research = survey.getResearch();
        if (research.getStatusResearch() != ResearchStatus.DRAFT) {
            throw new IllegalStateException(
                    "Недопустимый переход статуса: " + research.getStatusResearch() + " → NEW");
        }

        if (itemPaths != null && !itemPaths.isEmpty()) {
            List<ItemsSub> itemsSub = itemPaths.stream()
                    .map(path -> new ItemsSub(UUID.randomUUID(), survey, path))
                    .toList();
            itemsSubRepository.saveAll(itemsSub)
                    .forEach(managed -> survey.getItemsSubsSurvey().add(managed));
        }


        research.changeStatus(ResearchStatus.NEW);
        return ResearchResponseInfoDto.from(research, survey);

    }

}
