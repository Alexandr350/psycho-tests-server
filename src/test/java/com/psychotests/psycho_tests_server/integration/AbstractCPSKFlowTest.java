package com.psychotests.psycho_tests_server.integration;

import com.jayway.jsonpath.JsonPath;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.*;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AbstractCPSKFlowTest extends AbstractIntegrationTest {
    @Autowired protected SurveyRepository surveyRepository;
    @Autowired protected SurveySessionRepository sessionRepository;
    @Autowired protected CharacteristicsRepository characteristicsRepository;
    @Autowired protected ItemsRepository itemsRepository;
    @Autowired protected ItemsSubRepository itemsSubRepository;

    protected Survey survey;
    protected ItemsSub itemsSub;

    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    protected void setUpCPSKFixture() {
        Research research = new Research(
                UUID.randomUUID(), "Тест", "Описание", "кафедра", TypeResearch.CPSK);
        research.setPsychologistAccount(psychologistAccount);          // ДО save
        research = researchRepository.save(research);                  // merge вернёт managed-копию

        Survey surveyEntity = new Survey(
                UUID.randomUUID(), "Опрос CPSK", "Описание опроса", "Правила прохождения");
        surveyEntity.setResearch(research);                            // ДО save
        survey = surveyRepository.save(surveyEntity);

        characteristicsRepository.save(new Characteristics(UUID.randomUUID(), survey, "Характеристика 1"));
        characteristicsRepository.save(new Characteristics(UUID.randomUUID(), survey, "Характеристика 2"));
        itemsRepository.save(new Items(UUID.randomUUID(), survey, "img_1.png"));
        itemsSub = itemsSubRepository.save(new ItemsSub(UUID.randomUUID(), survey, "sub_1.png"));

        entityManager.flush();
    }
    protected UUID startSession() throws Exception {
        String body = mockMvc.perform(get("/api/CPSKTest/flow/" + survey.getIDSurvey()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.parse(body).read("$.idSession"));
    }
    protected void fillQuestionnaire(UUID sessionId) throws Exception {
        mockMvc.perform(post("/api/CPSKTest/flow/" + survey.getIDSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SurveyFlowQuestionnaireRequestDto(sessionId, 25, "М"))))
                .andExpect(status().isOk());
    }
    protected List<AssociationAnswerDto> fullAssociations() {
        List<AssociationAnswerDto> assoc = new ArrayList<>();
        for (int sector = 1; sector <= 8; sector++) {
            for (int n = 0; n < 5; n++) {
                assoc.add(new AssociationAnswerDto(sector, "слово-" + sector + "-" + n));
            }
        }
        return assoc;
    }
    protected UUID characteristicId(int index) {
        return characteristicsRepository.findBySurvey(survey).get(index).getIdCharacteristic();
    }
    protected UUID itemId() {
        return itemsRepository.findBySurvey(survey).getFirst().getIdItem();
    }
    protected UUID subItemId() {
        return itemsSub.getIdItemSub();
    }
    protected SurveyFlowSubmitRequestDto validRequest(UUID sessionId) {
        return new SurveyFlowSubmitRequestDto(sessionId,
                List.of(new CharacteristicAnswerDto(characteristicId(0), 3),
                        new CharacteristicAnswerDto(characteristicId(1), 7)),
                List.of(new ImageAnswerDto(itemId(), 5)),
                List.of(new ImageAnswerDto(subItemId(), 2)),
                fullAssociations());
    }
    protected void submitValidAnswers(UUID sessionId) throws Exception {
        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIDSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(sessionId))))
                .andExpect(status().isCreated());
    }

}
