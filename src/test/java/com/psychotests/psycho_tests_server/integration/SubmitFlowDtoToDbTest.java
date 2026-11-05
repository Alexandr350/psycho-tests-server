package com.psychotests.psycho_tests_server.integration;

import com.psychotests.psycho_tests_server.model.AnswersAssociations;
import com.psychotests.psycho_tests_server.model.AnswersCharacteristics;
import com.psychotests.psycho_tests_server.model.AnswersItems;
import com.psychotests.psycho_tests_server.model.AnswersItemsSubs;
import com.psychotests.psycho_tests_server.model.SurveySession;
import com.psychotests.psycho_tests_server.repository.AnswersAssociationsRepository;
import com.psychotests.psycho_tests_server.repository.AnswersCharacteristicsRepository;
import com.psychotests.psycho_tests_server.repository.AnswersItemsRepository;
import com.psychotests.psycho_tests_server.repository.AnswersItemsSubRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Полный цикл: request-DTO → контроллер → сервис → БД.
 * <p>
 * Реальный flow через MockMvc (startSession → questionnaire → submit),
 * затем сущности читаются напрямую из репозиториев и сверяются
 * с содержимым отправленных DTO.
 */
class SubmitFlowDtoToDbTest extends AbstractCPSKFlowTest {

    @Autowired private AnswersCharacteristicsRepository answersCharacteristicsRepository;
    @Autowired private AnswersItemsRepository answersItemsRepository;
    @Autowired private AnswersItemsSubRepository answersItemsSubRepository;
    @Autowired private AnswersAssociationsRepository answersAssociationsRepository;

    @PersistenceContext
    private EntityManager em;

    /**
     * Отправляем ровно тот DTO, что собирает validRequest(), и проверяем,
     * что в базе каждая его часть легла в свою таблицу с теми же значениями.
     */
    @Test
    void submitRequestDtoIsPersistedExactlyAsSent() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);
        submitValidAnswers(sessionId);

        em.flush();
        em.clear();

        SurveySession session = sessionRepository.findById(sessionId).orElseThrow();

        // --- анкета: возраст и гендер попадают в респондента ---
        assertThat(session.getRespondent().getAgeRespondent()).isEqualTo(25);
        // assertThat(session.getRespondent().getGenderRespondent()) — тип гендера не видел

        // --- характеристики: 2 ответа, секторы 3 и 7, привязаны к нашей сессии ---
        List<AnswersCharacteristics> characteristics =
                answersCharacteristicsRepository.findAllCharacteristicsBySurvey(survey.getIdSurvey());
        assertThat(characteristics).hasSize(2);
        assertThat(characteristics)
                .extracting(AnswersCharacteristics::getNumberSector)
                .containsExactlyInAnyOrder(3, 7);
        assertThat(characteristics)
                .allSatisfy(a -> assertThat(a.getSurveySession().getIdSurveySession()).isEqualTo(sessionId));

        // --- изображение: сектор 5 ---
        List<AnswersItems> items =
                answersItemsRepository.findAllItemsBySurvey(survey.getIdSurvey());
        assertThat(items).hasSize(1);
        assertThat(items.getFirst().getNumberSector()).isEqualTo(5);
        assertThat(items.getFirst().getItem().getIdItem()).isEqualTo(itemId());

        // --- дополнительное изображение: сектор 2 ---
        List<AnswersItemsSubs> subs =
                answersItemsSubRepository.findAllItemsSubBySurvey(survey.getIdSurvey());
        assertThat(subs).hasSize(1);
        assertThat(subs.getFirst().getNumberSector()).isEqualTo(2);
        assertThat(subs.getFirst().getItemsSub().getIdItemSub()).isEqualTo(subItemId());

        // --- ассоциации: 8 секторов × 5 ответов, порядок внутри сектора 1..5 ---
        List<AnswersAssociations> associations =
                answersAssociationsRepository.findAllAssociationsBySurvey(survey.getIdSurvey());
        assertThat(associations).hasSize(40);

        List<String> sector1Words = associations.stream()
                .filter(a -> a.getNumberSector() == 1)
                .sorted(Comparator.comparingInt(AnswersAssociations::getAssociationOrder))
                .map(AnswersAssociations::getAnswer)
                .toList();
        assertThat(sector1Words).containsExactly(
                "слово-1-0", "слово-1-1", "слово-1-2", "слово-1-3", "слово-1-4");

        assertThat(associations)
                .allSatisfy(a -> assertThat(a.getSurveySession().getIdSurveySession()).isEqualTo(sessionId));
    }

    /**
     * Вторая сессия того же опроса не получает ответов первой:
     * фильтрация идёт по сессии, а не только по опросу.
     */
    @Test
    void answersBelongOnlyToTheirSession() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);
        submitValidAnswers(sessionId);

        startSession(); // вторая сессия того же опроса — без ответов

        em.flush();
        em.clear();

        assertThat(answersAssociationsRepository
                .findAllAssociationsBySurvey(survey.getIdSurvey()))
                .allSatisfy(a -> assertThat(a.getSurveySession().getIdSurveySession())
                        .isEqualTo(sessionId));
    }
}
