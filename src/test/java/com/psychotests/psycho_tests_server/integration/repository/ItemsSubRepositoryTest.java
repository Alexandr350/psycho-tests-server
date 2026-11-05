package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.ItemsSub;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.repository.ItemsSubRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слойный тест ItemsSubRepository: производный query method findBySurvey(Survey).
 * Контракт идентичен CharacteristicsRepository и ItemsRepository: только свои
 * суб-объекты, чужие не подтекают; пустой список вместо null; связь с survey
 * не теряется.
 */
class ItemsSubRepositoryTest extends SurveyFixtureTest {

    @Autowired
    private ItemsSubRepository repository;

    /** Обязательны: id (конструктор), survey и url (NOT NULL). */
    private ItemsSub newItemsSub(Survey survey, String url) {
        return entityManager.persistAndFlush(new ItemsSub(UUID.randomUUID(), survey, url));
    }

    @Test
    void returnsOnlyItemsSubOfGivenSurvey() {
        Survey surveyA = newSurvey("Тревожность");
        Survey surveyB = newSurvey("Выгорание");
        ItemsSub foreign = newItemsSub(surveyB, "sub-b.png");
        newItemsSub(surveyA, "sub-a1.png");
        newItemsSub(surveyA, "sub-a2.png");

        List<ItemsSub> result = repository.findBySurvey(surveyA);

        assertThat(result)
                .extracting(ItemsSub::getUrlItemSub)
                .containsExactlyInAnyOrder("sub-a1.png", "sub-a2.png");
        assertThat(result).doesNotContain(foreign);
    }

    @Test
    void returnsEmptyList_whenNoItemsSub() {
        Survey survey = newSurvey("Пустой опросник");

        assertThat(repository.findBySurvey(survey)).isEmpty();
    }

    @Test
    void itemsSubReferenceTheSurvey() {
        Survey survey = newSurvey("Мотивация");
        newItemsSub(survey, "sub.png");

        List<ItemsSub> result = repository.findBySurvey(survey);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSurvey()).isSameAs(survey);
    }
}
