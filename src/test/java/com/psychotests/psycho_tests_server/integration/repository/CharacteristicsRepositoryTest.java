package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.Characteristics;
import com.psychotests.psycho_tests_server.model.Survey;

import com.psychotests.psycho_tests_server.repository.CharacteristicsRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слойный тест CharacteristicsRepository (@DataJpaTest, H2 в памяти):
 * проверяется собственная логика репозитория — производный query method
 * findBySurvey(Survey). Базовые методы JpaRepository не тестируются: их
 * генерирует Spring Data, у авторов они уже покрыты.
 *
 * <p>Фикстура строит цепочку Characteristics → Survey → Research →
 * PsychologistAccount: каждый уровень обязательный (nullable = false),
 * порядок persist — снизу вверх. Обратные OneToMany-коллекции (sessions,
 * characteristicsSurvey и прочие) не трогаем — сохранение идёт через
 * владельца связи (survey — владелец FK characteristics.id_survey).</p>
 *
 * <p>findBySurvey возвращает сущности в неопределённом порядке — сравнение
 * по множеству. Порядок, если станет важен, фиксируется сортировкой в
 * сервисе — тогда тест добавит проверку порядка.</p>
 */
@DataJpaTest
class CharacteristicsRepositoryTest extends SurveyFixtureTest {
    @Autowired
    protected CharacteristicsRepository repository;
    /** Характеристика: единственное обязательное поле — связь с survey. */
    private Characteristics newCharacteristic(Survey survey, String text) {
        return entityManager.persistAndFlush(
                new Characteristics(UUID.randomUUID(), survey, text));
    }

    /** Выборка по survey: только свои характеристики, чужие не подтекают. */
    @Test
    void returnsOnlyCharacteristicsOfGivenSurvey() {
        Survey surveyA = newSurvey("Тревожность");
        Survey surveyB = newSurvey("Выгорание");
        Characteristics foreign  = newCharacteristic(surveyB, "стрессоустойчивость");
        newCharacteristic(surveyA, "внимательность");
        newCharacteristic(surveyA, "отзывчивость");

        List<Characteristics> result = repository.findBySurvey(surveyA);

        assertThat(result)
                .extracting(Characteristics::getTextCharacteristic)
                .containsExactlyInAnyOrder("внимательность", "отзывчивость");
        // equals у сущности — только по id, так что проверка надёжна:
        // чужая характеристика с другим id не попадает в выборку
        assertThat(result).doesNotContain(foreign);
    }


    /** У survey без характеристик — пустой список, не null. */
    @Test
    void returnsEmptyList_whenNoCharacteristics() {
        Survey survey = newSurvey("Пустой опросник");

        assertThat(repository.findBySurvey(survey)).isEmpty();
    }

    /**
     * Ссылка на survey не теряется: у найденной характеристики survey —
     * тот же экземпляр, что и запрошенный (identity в рамках persistence
     * context). Для сервиса это важно: он кладёт эти объекты в allowed
     * и потом ссылается на них при сборке AnswersCharacteristics.
     */
    @Test
    void characteristicsReferenceTheSurvey() {
        Survey survey = newSurvey("Мотивация");
        newCharacteristic(survey, "настойчивость");

        List<Characteristics> result = repository.findBySurvey(survey);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSurvey()).isSameAs(survey);
    }
}
