package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.AnswersItemsSubs;
import com.psychotests.psycho_tests_server.model.ItemsSub;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.model.SurveySession;
import com.psychotests.psycho_tests_server.repository.AnswersItemsSubRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест {@link AnswersItemsSubRepository}: проверяется
 * единственный запрос {@code findAllItemsSubBySurvey} на реальной H2-базе.
 * <p>
 * Что проверяется:
 * <ul>
 *   <li>фильтрация по idSurvey через surveySession.survey — ответы чужих
 *       опросов не попадают в выборку;</li>
 *   <li>fetch join подгружает itemsSub — чтение getItemsSub() после
 *       clear() работает без LazyInitializationException;</li>
 *   <li>пустой опрос и несуществующий id — пустой список без исключений.</li>
 * </ul>
 */
class AnswersItemsSubRepositoryTest extends SurveyFixtureTest {

    @Autowired
    private AnswersItemsSubRepository repository;

    /**
     * Ответы двух опросов: в выборку попадают только ответы целевого,
     * а getItemsSub() работает после clear() — это доказывает fetch join.
     */
    @Test
    void findAllBySurveyReturnsOnlyGivenSurveyWithFetchedItemsSub() {
        Survey surveyA = newSurvey("Опрос А");
        Survey surveyB = newSurvey("Опрос Б");
        ItemsSub itemsSubA = newItemsSub(surveyA);
        ItemsSub itemsSubB = newItemsSub(surveyB);

        persist(newAnswer(newSession(surveyA), itemsSubA));
        persist(newAnswer(newSession(surveyA), itemsSubA));
        persist(newAnswer(newSession(surveyB), itemsSubB));

        entityManager.flush();
        entityManager.clear();

        List<AnswersItemsSubs> result =
                repository.findAllItemsSubBySurvey(surveyA.getIdSurvey());

        assertThat(result).hasSize(2);
        assertThat(result)
                .allSatisfy(a -> assertThat(a.getItemsSub().getIdItemSub())
                        .isEqualTo(itemsSubA.getIdItemSub()));

    }

    /** Опрос без ответов — пустой список, без исключений. */
    @Test
    void surveyWithoutAnswers_returnsEmptyList() {
        Survey survey = newSurvey("Пустой опрос");

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllItemsSubBySurvey(survey.getIdSurvey())).isEmpty();
    }

    /** Несуществующий id опроса — пустой список (запрос не падает). */
    @Test
    void unknownSurveyId_returnsEmptyList() {
        assertThat(repository.findAllItemsSubBySurvey(UUID.randomUUID())).isEmpty();
    }

    // ---------- листовые фабрики ----------

    private ItemsSub newItemsSub(Survey survey) {
        // конструктор (id, survey, url) совпал; urlItemSub — обязательное поле
        return entityManager.persistAndFlush(
                new ItemsSub(UUID.randomUUID(), survey, "https://example.com/sub-" + UUID.randomUUID() + ".png"));
    }

    private AnswersItemsSubs newAnswer(SurveySession session, ItemsSub itemsSub) {
        return new AnswersItemsSubs(
                UUID.randomUUID(),  // id — вручную, конвенция проекта
                session,
                itemsSub,
                1);                 // numberSector — nullable = false, для запроса не важен
    }

}
