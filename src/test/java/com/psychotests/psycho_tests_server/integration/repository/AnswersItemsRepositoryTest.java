package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.AnswersItems;
import com.psychotests.psycho_tests_server.model.Items;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.model.SurveySession;
import com.psychotests.psycho_tests_server.projection.SectorCountProjection;
import com.psychotests.psycho_tests_server.repository.AnswersItemsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест {@link AnswersItemsRepository}: проверяются оба запроса
 * на реальной H2-базе.
 * <p>
 * Что проверяется:
 * <ul>
 *   <li>{@code countSectorsByItem} — группировка по numberSector со счётом
 *       УНИКАЛЬНЫХ сессий, двойной фильтр (пункт И опрос), сортировка по сектору;</li>
 *   <li>{@code findAllItemsBySurvey} — выборка ответов опроса с fetch join
 *       пункта (чтение getItem() после clear() без LazyInitializationException).</li>
 * </ul>
 */
class AnswersItemsRepositoryTest extends SurveyFixtureTest {

    @Autowired
    private AnswersItemsRepository repository;

    // ---------- countSectorsByItem ----------

    /**
     * Три сессии одного опроса, ответы на один пункт в секторах 1, 1, 2.
     * Ожидание: сектор 1 → 2 сессии, сектор 2 → 1 сессия, порядок по возрастанию.
     * Тот же пункт из чужого опроса (в тесте — отдельный «двойник») не влияет.
     */
    @Test
    void countsDistinctSessionsPerSectorFilteredBySurvey() {
        Survey surveyA = newSurvey("Опрос А");
        Survey surveyB = newSurvey("Опрос Б");
        Items itemA = newItem(surveyA);
        Items itemB = newItem(surveyB);

        SurveySession s1 = newSession(surveyA);
        SurveySession s2 = newSession(surveyA);
        SurveySession s3 = newSession(surveyA);
        SurveySession foreignSession = newSession(surveyB);

        persist(newAnswer(s1, itemA, 1));
        persist(newAnswer(s2, itemA, 1));
        persist(newAnswer(s3, itemA, 2));
        persist(newAnswer(foreignSession, itemB, 1));

        entityManager.flush();
        entityManager.clear();

        List<SectorCountProjection> result =
                repository.countSectorsByItem(itemA.getIdItem(), surveyA.getIdSurvey());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNumberSector()).isEqualTo(1);
        assertThat(result.get(0).getCount()).isEqualTo(2L);
        assertThat(result.get(1).getNumberSector()).isEqualTo(2);
        assertThat(result.get(1).getCount()).isEqualTo(1L);
    }

    /** Два ответа на пункт в одной сессии и одном секторе считаются один раз. */
    @Test
    void twoAnswersInSameSessionCountedOnce() {
        Survey survey = newSurvey("Опрос");
        Items item = newItem(survey);
        SurveySession session = newSession(survey);

        persist(newAnswer(session, item, 1));
        persist(newAnswer(session, item, 1));

        entityManager.flush();
        entityManager.clear();

        List<SectorCountProjection> result =
                repository.countSectorsByItem(item.getIdItem(), survey.getIdSurvey());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCount()).isEqualTo(1L);
    }

    /** Пункт без ответов — пустой список, без исключений. */
    @Test
    void itemWithoutAnswers_returnsEmptyList() {
        Survey survey = newSurvey("Опрос");
        Items item = newItem(survey);

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.countSectorsByItem(item.getIdItem(), survey.getIdSurvey()))
                .isEmpty();
    }

    // ---------- findAllItemsBySurvey ----------

    /**
     * Чужие опросы отфильтрованы; getItem() работает после clear() —
     * это и доказывает, что join fetch подгрузил пункт в том же запросе.
     */
    @Test
    void findAllBySurveyReturnsOnlyGivenSurveyWithFetchedItem() {
        Survey surveyA = newSurvey("Опрос А");
        Survey surveyB = newSurvey("Опрос Б");
        Items itemA = newItem(surveyA);
        Items itemB = newItem(surveyB);

        persist(newAnswer(newSession(surveyA), itemA, 1));
        persist(newAnswer(newSession(surveyA), itemA, 2));
        persist(newAnswer(newSession(surveyB), itemB, 1));

        entityManager.flush();
        entityManager.clear();

        List<AnswersItems> result =
                repository.findAllItemsBySurvey(surveyA.getIdSurvey());

        assertThat(result).hasSize(2);
        assertThat(result)
                .allSatisfy(a -> assertThat(a.getItem().getIdItem())
                        .isEqualTo(itemA.getIdItem()));
        assertThat(result)
                .extracting(AnswersItems::getNumberSector)
                .containsExactlyInAnyOrder(1, 2);
    }

    /** Опрос без ответов — пустой список. */
    @Test
    void surveyWithoutItemAnswers_returnsEmptyList() {
        Survey survey = newSurvey("Пустой опрос");

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllItemsBySurvey(survey.getIdSurvey())).isEmpty();
    }

    // ---------- листовые фабрики ----------

    private Items newItem(Survey survey) {
        // готовый конструктор с survey; urlItem — обязательное поле
        return entityManager.persistAndFlush(
                new Items(UUID.randomUUID(), survey, "https://example.com/image-" + UUID.randomUUID() + ".png"));
    }

    private AnswersItems newAnswer(SurveySession session, Items item, int sector) {
        return new AnswersItems(
                UUID.randomUUID(),  // id — вручную, конвенция проекта
                session,
                item,
                sector);
    }

}
