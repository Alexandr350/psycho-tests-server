package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.AnswersCharacteristics;
import com.psychotests.psycho_tests_server.model.Characteristics;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.model.SurveySession;
import com.psychotests.psycho_tests_server.projection.SectorCountProjection;
import com.psychotests.psycho_tests_server.repository.AnswersCharacteristicsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Интеграционный тест {@link AnswersCharacteristicsRepository}: проверяются
 * оба запроса на реальной H2-базе.
 * <p>
 * Что проверяется:
 * <ul>
 *   <li>{@code countSectorsByCharacteristic} — группировка по numberSector со
 *       счётом УНИКАЛЬНЫХ сессий (два ответа в одной сессии = 1), фильтр по
 *       характеристике И опросу одновременно, сортировка по сектору;</li>
 *   <li>{@code findAllCharacteristicsBySurvey} — выборка ответов опроса,
 *       fetch join подгружает характеристику (после clear() доступ к
 *       getCharacteristic() работает без LazyInitializationException).</li>
 * </ul>
 */
class AnswersCharacteristicsRepositoryTest extends SurveyFixtureTest {

    @Autowired
    private AnswersCharacteristicsRepository repository;

    // ---------- countSectorsByCharacteristic ----------

    /**
     * Основной сценарий: три сессии одного опроса, ответы на одну характеристику
     * в секторах 1, 1, 2. Ожидание: сектор 1 → 2 сессии, сектор 2 → 1 сессия,
     * порядок — по возрастанию номера сектора. Ответы той же характеристики
     * из другого опроса в выборку не попадают.
     */
    @Test
    void countsDistinctSessionsPerSectorFilteredBySurvey() {
        Survey surveyA = newSurvey("Опрос А");
        Survey surveyB = newSurvey("Опрос Б");
        Characteristics characteristicA = newCharacteristic(surveyA);
        Characteristics characteristicB = newCharacteristic(surveyB);

        SurveySession s1 = newSession(surveyA);
        SurveySession s2 = newSession(surveyA);
        SurveySession s3 = newSession(surveyA);
        SurveySession foreignSession = newSession(surveyB);

        persist(newAnswer(s1, characteristicA, 1));
        persist(newAnswer(s2, characteristicA, 1));
        persist(newAnswer(s3, characteristicA, 2));
        // чужой опрос: тот же сектор 1, но не должен увеличить счёт
        persist(newAnswer(foreignSession, characteristicB, 1));

        entityManager.flush();
        entityManager.clear();

        List<SectorCountProjection> result =
                repository.countSectorsByCharacteristic(characteristicA.getIdCharacteristic(), surveyA.getIdSurvey());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getNumberSector()).isEqualTo(1);
        assertThat(result.get(0).getCount()).isEqualTo(2L);
        assertThat(result.get(1).getNumberSector()).isEqualTo(2);
        assertThat(result.get(1).getCount()).isEqualTo(1L);
    }

    /**
     * Два ответа на одну характеристику в одной сессии и в одном секторе
     * считаются один раз — проверка count(distinct surveySession.id).
     */
    @Test
    void twoAnswersInSameSessionCountedOnce() {
        Survey survey = newSurvey("Опрос");
        Characteristics characteristic = newCharacteristic(survey);
        SurveySession session = newSession(survey);

        persist(newAnswer(session, characteristic, 1));
        persist(newAnswer(session, characteristic, 1));

        entityManager.flush();
        entityManager.clear();

        List<SectorCountProjection> result =
                repository.countSectorsByCharacteristic(characteristic.getIdCharacteristic(), survey.getIdSurvey());

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getCount()).isEqualTo(1L);
    }

    /** Характеристика без ответов — пустой список, без исключений. */
    @Test
    void characteristicWithoutAnswers_returnsEmptyList() {
        Survey survey = newSurvey("Опрос");
        Characteristics characteristic = newCharacteristic(survey);

        entityManager.flush();
        entityManager.clear();

        assertThat(repository
                .countSectorsByCharacteristic(characteristic.getIdCharacteristic(), survey.getIdSurvey()))
                .isEmpty();
    }

    // ---------- findAllCharacteristicsBySurvey ----------

    /**
     * Выборка по опросу: чужие опросы отфильтрованы, характеристику можно
     * прочитать после clear() — это и доказывает работу fetch join.
     */
    @Test
    void findAllBySurveyReturnsOnlyGivenSurveyWithFetchedCharacteristic() {
        Survey surveyA = newSurvey("Опрос А");
        Survey surveyB = newSurvey("Опрос Б");
        Characteristics characteristicA = newCharacteristic(surveyA);
        Characteristics characteristicB = newCharacteristic(surveyB);

        persist(newAnswer(newSession(surveyA), characteristicA, 1));
        persist(newAnswer(newSession(surveyA), characteristicA, 2));
        persist(newAnswer(newSession(surveyB), characteristicB, 1));

        entityManager.flush();
        entityManager.clear();

        List<AnswersCharacteristics> result =
                repository.findAllCharacteristicsBySurvey(surveyA.getIdSurvey());

        assertThat(result).hasSize(2);
        // fetch join: характеристика загружена в том же запросе,
        // доступ к ней не бросает LazyInitializationException после clear()
        assertThat(result)
                .allSatisfy(a -> assertThat(a.getCharacteristic().getIdCharacteristic())
                        .isEqualTo(characteristicA.getIdCharacteristic()));
        assertThat(result)
                .extracting(AnswersCharacteristics::getNumberSector)
                .containsExactlyInAnyOrder(1, 2);
    }

    /** Опрос без ответов — пустой список. */
    @Test
    void surveyWithoutCharacteristicAnswers_returnsEmptyList() {
        Survey survey = newSurvey("Пустой опрос");

        entityManager.flush();
        entityManager.clear();

        assertThat(repository.findAllCharacteristicsBySurvey(survey.getIdSurvey())).isEmpty();
    }

    // ---------- листовые фабрики ----------

    private Characteristics newCharacteristic(Survey survey) {
        var characteristic = new Characteristics(UUID.randomUUID(), survey, "Характеристика");
        return entityManager.persistAndFlush(characteristic);
    }


    private AnswersCharacteristics newAnswer(SurveySession session,
                                             Characteristics characteristic,
                                             int sector) {
        return new AnswersCharacteristics(
                UUID.randomUUID(),  // id — вручную, конвенция проекта
                session,
                characteristic,
                sector);
    }

}
