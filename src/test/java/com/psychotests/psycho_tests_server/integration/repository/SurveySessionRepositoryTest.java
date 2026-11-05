package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;
import com.psychotests.psycho_tests_server.model.Respondent;
import com.psychotests.psycho_tests_server.model.Run;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.model.SurveySession;
import com.psychotests.psycho_tests_server.repository.SurveySessionRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Слойный тест SurveySessionRepository: ручной JPQL-запрос с составным
 * условием (status <> :status or status is null). Запрос построен явно,
 * потому что производное имя метода не совпадает с полем сущности
 * (statusSurveySession), а NULL-сессии считаются незавершёнными и
 * блокируют удаление опроса.
 *
 * <p>Семантика параметра: status — «безопасный» статус, при котором опрос
 * можно удалять. Сессия в IN_PROGRESS, NOT_STARTED или без статуса —
 * блокирует.</p>
 */
class SurveySessionRepositoryTest extends SurveyFixtureTest {

    @Autowired
    private SurveySessionRepository repository;

    /**
     * Fire-and-forget фикстура: создаёт и сохраняет сессию со случайным респондентом.
     */
    private void newSession(Survey survey, SurveySessionStatus status) {
        persistSession(buildSession(survey, newRespondent(), status));
    }

    /**
     * Сборщик без сохранения — для тестов, которым нужен сам объект.
     * Run обязателен: survey_session.id_run NOT NULL, сессия в проде
     * всегда живёт внутри запуска.
     */
    private SurveySession buildSession(Survey survey, Respondent respondent,
                                       SurveySessionStatus status) {
        var session = new SurveySession(UUID.randomUUID());
        session.setSurvey(survey);
        session.setRun(newRun(survey.getResearch()));
        session.setRespondent(respondent);
        if (status != null) {
            session.setStatusSurveySession(status);
        }
        return session;
    }


    private void persistSession(SurveySession session) {
        entityManager.persistAndFlush(session);
    }


    @Test
    void returnsTrue_whenSessionInProgress() {
        Survey survey = newSurvey("Тревожность");
        newSession(survey, SurveySessionStatus.IN_PROGRESS);

        assertThat(repository.existsBySurveyIdSurveyAndStatusNot(
                survey.getIdSurvey(), SurveySessionStatus.COMPLETED)).isTrue();
    }

    @Test
    void returnsTrue_whenSessionNotStarted() {
        Survey survey = newSurvey("Тревожность");
        newSession(survey, SurveySessionStatus.NOT_STARTED);

        assertThat(repository.existsBySurveyIdSurveyAndStatusNot(
                survey.getIdSurvey(), SurveySessionStatus.COMPLETED)).isTrue();
    }

    /**
     * Ключевой тест NULL-ловушки: строка без статуса — незавершённая.
     */
    @Test
    void returnsTrue_whenSessionStatusIsNull() {
        Survey survey = newSurvey("Тревожность");
        var session = buildSession(survey, newRespondent(), SurveySessionStatus.NOT_STARTED);
        session.setStatusSurveySession(null);   // проверяем именно NULL в колонке
        entityManager.persistAndFlush(session);

        assertThat(repository.existsBySurveyIdSurveyAndStatusNot(
                survey.getIdSurvey(), SurveySessionStatus.COMPLETED)).isTrue();
    }


    @Test
    void returnsFalse_whenAllSessionsCompleted() {
        Survey survey = newSurvey("Тревожность");
        newSession(survey, SurveySessionStatus.COMPLETED);
        newSession(survey, SurveySessionStatus.COMPLETED);

        assertThat(repository.existsBySurveyIdSurveyAndStatusNot(
                survey.getIdSurvey(), SurveySessionStatus.COMPLETED)).isFalse();
    }

    @Test
    void returnsFalse_whenSurveyHasNoSessions() {
        Survey survey = newSurvey("Пустой опросник");

        assertThat(repository.existsBySurveyIdSurveyAndStatusNot(
                survey.getIdSurvey(), SurveySessionStatus.COMPLETED)).isFalse();
    }


    /**
     * Повторная сессия одного респондента в ОДНОМ запуске отклоняется БД
     * (uk_session_respondent_run: unique (id_respondent, id_run)). Защита
     * от задвоения ответов внутри прохождения: даже если проверка в сервисе
     * пропустит (гонка), дубликат не создастся. Повторное прохождение
     * в новом запуске — легально, см. newSessionInNewRunForSameRespondentIsAllowed.
     */
    @Test
    void duplicateRespondentRunPairRejectedByUniqueConstraint() {
        Survey survey = newSurvey("Тревожность");
        Respondent respondent = newRespondent();
        Run run = newRun(survey.getResearch());

        var first = buildSession(survey, respondent, SurveySessionStatus.COMPLETED);
        first.setRun(run);
        entityManager.persistAndFlush(first);

        var second = buildSession(survey, respondent, SurveySessionStatus.COMPLETED);
        second.setRun(run);                                   // тот же Run — вот дубликат

        assertThatThrownBy(() -> repository.saveAndFlush(second))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Документирует обратную сторону правила: новый Run — новая сессия, повторное прохождение разрешено.
     */
    @Test
    void newSessionInNewRunForSameRespondentIsAllowed() {
        Survey survey = newSurvey("Тревожность");
        Respondent respondent = newRespondent();

        entityManager.persistAndFlush(
                buildSession(survey, respondent, SurveySessionStatus.COMPLETED));
        entityManager.persistAndFlush(
                buildSession(survey, respondent, SurveySessionStatus.COMPLETED));   // каждый со своим Run

        assertThat(repository.findAll()).hasSize(2);
    }

}