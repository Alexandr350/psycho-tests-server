package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.AnswersAssociations;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.model.SurveySession;
import com.psychotests.psycho_tests_server.repository.AnswersAssociationsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест репозитория {@link AnswersAssociationsRepository}
 * на поднятой in-memory базе (H2) — проверяется сам JPQL-запрос, а не моки.
 * <p>
 * Что проверяется:
 * <ul>
 *   <li>фильтрация по idSurvey через связь surveySession.survey:
 *       ответы чужих опросов не попадают в выборку;</li>
 *   <li>сортировка по associationOrder — порядок строк в выборке соответствует
 *       порядку заполнения анкеты, а не порядку вставки в базу;</li>
 *   <li>пустой опрос и несуществующий id — пустой список без исключений.</li>
 * </ul>
 */
@DataJpaTest
class AnswersAssociationsRepositoryTest extends SurveyFixtureTest {

    @Autowired
    private AnswersAssociationsRepository repository;

    // ---------- фильтрация и сортировка ----------

    /**
     * Основной сценарий: два опроса, в каждом по сессии и по паре ответов.
     * В выборку попадают только ответы запрошенного опроса, причём
     * отсортированные по associationOrder — а не в том порядке, в котором
     * ответы были вставлены в базу (здесь вставлены «задом наперёд»).
     */
    @Test
    void returnsOnlyGivenSurveyOrderedByAssociationOrder() {
        // ---- опрос A (целевой) ----
        Survey surveyA = newSurvey("Опрос А");
        SurveySession sessionA = newSession(surveyA);

        // вставляем в порядке 2, 1 — чтобы поймать отсутствие сортировки
        persist(newAssociation(sessionA, "тёплый", 2));
        persist(newAssociation(sessionA, "уютный", 1));

        // ---- опрос B (не должен попасть в выборку) ----
        Survey surveyB = newSurvey("Опрос Б");
        SurveySession sessionB = newSession(surveyB);
        persist(newAssociation(sessionB, "чужой ответ", 1));

        entityManager.flush();
        entityManager.clear();

        List<AnswersAssociations> result =
                repository.findAllAssociationsBySurvey(surveyA.getIdSurvey());

        assertThat(result)
                .extracting(AnswersAssociations::getAnswer)
                .containsExactly("уютный", "тёплый"); // по associationOrder, не по вставке
        assertThat(result)
                .allSatisfy(a -> assertThat(a.getSurveySession().getSurvey().getIdSurvey())
                        .isEqualTo(surveyA.getIdSurvey()));
    }

    /**
     * Одинаковый associationOrder у разных респондентов не ломает выборку:
     * обе строки возвращаются (порядок между ними не гарантируется).
     */
    @Test
    void sameOrderFromDifferentRespondentsAreAllReturned() {
        Survey survey = newSurvey("Опрос");
        SurveySession session1 = newSession(survey);
        SurveySession session2 = newSession(survey);

        persist(newAssociation(session1, "уютный", 1));
        persist(newAssociation(session2, "уютный", 1));

        entityManager.flush();
        entityManager.clear();

        List<AnswersAssociations> result =
                repository.findAllAssociationsBySurvey(survey.getIdSurvey());

        assertThat(result).hasSize(2);
    }

    // ---------- крайние случаи ----------

    /**
     * Опрос без ответов: запрос возвращает пустой список, не бросает исключений.
     */
    @Test
    void surveyWithoutAnswers_returnsEmptyList() {
        Survey survey = newSurvey("Пустой опрос");
        newSession(survey);

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllAssociationsBySurvey(survey.getIdSurvey())).isEmpty();
    }

    /**
     * Несуществующий id опроса — тоже пустой список (запрос не падает).
     */
    @Test
    void unknownSurveyId_returnsEmptyList() {
        assertThat(repository.findAllAssociationsBySurvey(UUID.randomUUID())).isEmpty();
    }

    private AnswersAssociations newAssociation(SurveySession session, String answer, int order) {
        return new AnswersAssociations(
                UUID.randomUUID(),  // id — вручную, как требует конвенция проекта
                session,
                answer,
                1,                  // numberSector: для запроса не важен, но nullable = false
                order);
    }

}
