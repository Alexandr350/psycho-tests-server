package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.Items;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.repository.ItemsRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слойный тест ItemsRepository: производный query method findBySurvey(Survey).
 * Контракт идентичен CharacteristicsRepository: только свои изображения,
 * чужие не подтекают; пустой список вместо null; связь с survey не теряется.
 */
class ItemsRepositoryTest extends SurveyFixtureTest {

    @Autowired
    private ItemsRepository repository;

    /** Обязательны: id (конструктор), survey и url (NOT NULL). */
    private Items newItems(Survey survey, String url) {
        return entityManager.persistAndFlush(new Items(UUID.randomUUID(), survey, url));
    }

    @Test
    void returnsOnlyItemsOfGivenSurvey() {
        Survey surveyA = newSurvey("Тревожность");
        Survey surveyB = newSurvey("Выгорание");
        Items foreign = newItems(surveyB, "img-b.png");
        newItems(surveyA, "img-a1.png");
        newItems(surveyA, "img-a2.png");

        List<Items> result = repository.findBySurvey(surveyA);

        assertThat(result)
                .extracting(Items::getUrlItem)
                .containsExactlyInAnyOrder("img-a1.png", "img-a2.png");
        assertThat(result).doesNotContain(foreign);
    }

    @Test
    void returnsEmptyList_whenNoItems() {
        Survey survey = newSurvey("Пустой опросник");

        assertThat(repository.findBySurvey(survey)).isEmpty();
    }

    @Test
    void itemsReferenceTheSurvey() {
        Survey survey = newSurvey("Мотивация");
        newItems(survey, "img.png");

        List<Items> result = repository.findBySurvey(survey);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getSurvey()).isSameAs(survey);
    }
}
