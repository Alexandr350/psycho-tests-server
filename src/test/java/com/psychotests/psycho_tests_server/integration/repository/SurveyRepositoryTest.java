package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.repository.SurveyRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Слойный тест SurveyRepository: ручной JPQL-запрос с фильтром по владельцу
 * через цепочку навигации s.research.psychologistAccount.id — опрос виден
 * только психологу-владельцу исследования. Авторизация по принадлежности,
 * поэтому тест на «чужое не возвращается» обязателен.
 */
class SurveyRepositoryTest extends SurveyFixtureTest {

    @Autowired
    private SurveyRepository repository;

    @Test
    void findByIdReturnsSurvey_whenOwnedByPsychologist() {
        Survey survey = newSurvey("Тревожность");

        var found = repository.findByIdAndResearchPsychologistAccountIdPsychologistAccount(
                survey.getIdSurvey(),
                survey.getResearch().getPsychologistAccount().getIdPsychologistAccount());

        assertThat(found).isPresent();
        assertThat(found.get().getIdSurvey()).isEqualTo(survey.getIdSurvey());
    }

    /** Ключевой тест на авторизацию: чужой опрос не найден. */
    @Test
    void findByIdReturnsEmpty_whenSurveyBelongsToAnotherPsychologist() {
        Survey foreignSurvey = newSurvey("Выгорание");
        UUID someoneElseId = newPsychologistAccount().getIdPsychologistAccount();

        var found = repository.findByIdAndResearchPsychologistAccountIdPsychologistAccount(
                foreignSurvey.getIdSurvey(), someoneElseId);

        assertThat(found).isEmpty();
    }

    @Test
    void findByIdReturnsEmpty_whenSurveyDoesNotExist() {
        UUID ownerId = newPsychologistAccount().getIdPsychologistAccount();

        assertThat(repository.findByIdAndResearchPsychologistAccountIdPsychologistAccount(
                UUID.randomUUID(), ownerId)).isEmpty();
    }
}
