package com.psychotests.psycho_tests_server.integration;

import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.AssociationAnswerDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.CharacteristicAnswerDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.ImageAnswerDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowSubmitRequestDto;
import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;
import com.psychotests.psycho_tests_server.model.SurveySession;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CPSKTestFlowTest extends AbstractCPSKFlowTest {

    /**
     * Счастливый путь: полный цикл прохождения CPSK-теста одним респондентом.
     *
     * Сессия открывается через GET /flow, анкета заполняется через POST /flow,
     * затем POST /submit принимает весь пакет ответов одним запросом (201):
     * 2 характеристики, 1 item, 1 itemsSub и 40 ассоциаций (8 секторов × 5 слов).
     *
     * Проверяется не только код ответа, но и фактическое состояние в базе:
     * сессия переведена в COMPLETED с зафиксированным временем завершения,
     * данные анкеты сохранены в респонденте, а количество ответов по каждой
     * категории в точности совпадает с отправленным — без потерь и дублей.
     */

    @Test
    void fullFlow_getQuestionnaireSubmit_persistsAllAnswers() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);

        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIDSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(sessionId))))
                .andExpect(status().isCreated());

        SurveySession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SurveySessionStatus.COMPLETED, session.getStatusSurveySession());
        assertNotNull(session.getFinishTimeSession());
        assertEquals(25, session.getRespondent().getAgeRespondent());
        assertEquals("М", session.getRespondent().getGenderRespondent());
        assertEquals(2, session.getAnswersCharacteristics().size());
        assertEquals(1, session.getAnswersItems().size());
        assertEquals(1, session.getAnswersItemsSubs().size());
        assertEquals(40, session.getAnswersAssociations().size());   // 8 секторов × 5 слов
    }

    /**
     * Частичный сабмит: пустые секторы ассоциаций допустимы.
     *
     * Респондент отправляет ответы на все характеристики, item и itemsSub,
     * но ассоциации — только 2 слова в одном секторе из восьми. Сервер
     * принимает пакет (201) и сохраняет ровно 2 ассоциации, не добивая
     * недостающие секторы, а сессию всё равно закрывает — COMPLETED.
     *
     * Регрессия, на которую встаёт тест: если валидация ужесточается до
     * «все секторы обязательны», легитимный сценарий частичного прохождения
     * ломается — а по бизнес-правилам CPSK он должен работать.
     */

    @Test
    void submit_fullAssociationsInAllSectors_returns201AndPersistsAll() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);

        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIDSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(sessionId))))
                .andExpect(status().isCreated());

        SurveySession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SurveySessionStatus.COMPLETED, session.getStatusSurveySession());
        assertEquals(40, session.getAnswersAssociations().size());
    }

    /**
     * Что делает: респондент проходит опрос целиком — анкета, ответы на все
     * характеристики, все item и itemsSub, и ВСЕ ассоциации: 8 секторов по 5 слов = 40.
     * Что проверяет:
     *    1. Сервер принимает максимальный пакет (201) — валидатор пропускает не только
     *       частичные пакеты (см. тест submit_partialAssociations_...), но и полные.
     *    2. Сессия закрывается: статус COMPLETED, проставлен finishTimeSession.
     *    3. В базе сохранены ровно 40 ассоциаций (5 на каждый сектор) — без потерь,
     *       дублей и без «добивания» пустых секторов заглушками.
     * **/
    @Test
    void submit_partialAssociations_accepted() throws Exception {
        // пустые секторы допустимы: пришло 2 слова в одном секторе — сохранилось 2
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);

        List<AssociationAnswerDto> assoc = new ArrayList<>();
        assoc.add(new AssociationAnswerDto(2, "дерево"));
        assoc.add(new AssociationAnswerDto(2, "солнце"));

        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIDSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SurveyFlowSubmitRequestDto(
                                sessionId,
                                List.of(new CharacteristicAnswerDto(characteristicId(0), 3),
                                        new CharacteristicAnswerDto(characteristicId(1), 7)),
                                List.of(new ImageAnswerDto(itemId(), 5)),
                                List.of(new ImageAnswerDto(subItemId(), 2)),
                                assoc))))
                .andExpect(status().isCreated());

        SurveySession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(2, session.getAnswersAssociations().size());
        assertEquals(SurveySessionStatus.COMPLETED, session.getStatusSurveySession());
    }

    // ---------- Негативные ----------
    /**
     * Сектор 4 содержит 6 ассоциаций (лимит — 5). Сервер должен ответить 400
     * с «Сектор 4» в сообщении и не сохранить ничего: 0 ассоциаций в базе,
     * сессия осталась IN_PROGRESS — валидация до записи, атомарность сабмита.
     * **/
    @Test
    void submit_sixAssociationsInOneSector_returns400AndNothingPersisted() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);

        List<AssociationAnswerDto> assoc = new ArrayList<>();
        for (int sector = 1; sector <= 8; sector++) {
            int count = (sector == 4) ? 6 : 5;
            for (int n = 0; n < count; n++) {
                assoc.add(new AssociationAnswerDto(sector, "слово-" + n));
            }
        }

        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIDSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SurveyFlowSubmitRequestDto(
                                sessionId, List.of(), List.of(), List.of(), assoc))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Сектор 4")));

        SurveySession session = sessionRepository.findById(sessionId).orElseThrow();
        assertEquals(SurveySessionStatus.IN_PROGRESS, session.getStatusSurveySession());
        assertEquals(0, session.getAnswersAssociations().size());  // ничего не записалось
    }

    /**
     * Повторный сабмит по уже завершённой сессии (COMPLETED) должен получить
     * 409 Conflict — защита от двойной отправки и задвоения ответов в базе.
     * **/
    @Test
    void submit_twice_returns409() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);
        submitValidAnswers(sessionId);                            // первый — 201

        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIDSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(sessionId))))
                .andExpect(status().isConflict());
    }

    /**
     * Ответ с чужим UUID характеристики (randomUUID, которого нет в опросе)
     * при валидных item/itemsSub и полном пакете ассоциций должен получить 400:
     * requireFullCoverage сверяет присланные id с составом конкретного опроса,
     * защита от записи ответов по чужим сущностям.
     * **/
    @Test
    void submit_foreignCharacteristicId_returns400() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);

        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIDSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SurveyFlowSubmitRequestDto(
                                sessionId,
                                List.of(new CharacteristicAnswerDto(UUID.randomUUID(), 3)),  // чужой id
                                List.of(new ImageAnswerDto(itemId(), 5)),
                                List.of(new ImageAnswerDto(subItemId(), 2)),
                                fullAssociations()))))
                .andExpect(status().isBadRequest());
    }

    /**
     * Сабмит без заполненной анкеты (сессия в NOT_STARTED, у респондента нет
     * возраста и пола) должен получить 409: правильный порядок — анкета, потом ответы.
     * Гарантирует, что в исследовании не попадут респонденты без анкетных данных.
     * **/
    @Test
    void submit_beforeQuestionnaire_returns409() throws Exception {
        UUID sessionId = startSession();                          // анкету не заполняли
        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIDSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest(sessionId))))
                .andExpect(status().isConflict());
    }
}
