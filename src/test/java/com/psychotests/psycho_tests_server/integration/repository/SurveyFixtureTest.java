package com.psychotests.psycho_tests_server.integration.repository;

import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Respondent;
import com.psychotests.psycho_tests_server.model.Run;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.model.SurveySession;
import com.psychotests.psycho_tests_server.repository.RunRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

/**
 * Общая фикстурная база для слайс-тестов репозиториев: строит полную цепочку
 * PsychologistAccount → Research → Run → SurveySession (Survey прикрепляется
 * к Research наследниками или в тестах). Каждый уровень обязательный
 * (nullable = false): сессия не может существовать без запуска —
 * survey_session.id_run NOT NULL, поэтому newSession строит Run сам.
 * Наследники добавляют хелперы для листовых сущностей
 * (характеристики, изображения, ответы).
 */
@DataJpaTest
@ActiveProfiles("test")
abstract class SurveyFixtureTest {

    @Autowired
    protected TestEntityManager entityManager;
    @Autowired
    protected RunRepository runRepository;

    /** Счётчик для уникальных токенов запусков в фикстурах. */
    private int tokenCounter = 0;

    /**
     * Короткий хелпер: persist без flush. Листовые ответы можно копить
     * в persistence context, а сбрасывать общим entityManager.flush() в тесте.
     */
    protected <T> T persist(T entity) {
        return entityManager.persist(entity);
    }

    /**
     * Респондент — корень отдельной ветки (не принадлежит психологу).
     * Единственное обязательное поле — id, остальные nullable.
     */
    protected Respondent newRespondent() {
        return entityManager.persistAndFlush(new Respondent(UUID.randomUUID()));
    }

    /**
     * Все пять полей обязательны; email уникален (uk_psychologist_email),
     * поэтому генерируем с UUID. Пароль — колонка длины 60, как bcrypt-хеш.
     */
    protected PsychologistAccount newPsychologistAccount() {
        return entityManager.persistAndFlush(new PsychologistAccount(
                UUID.randomUUID(),
                "Анна",
                "Тестовая",
                "$2a$10$" + "x".repeat(53),
                "psychologist-" + UUID.randomUUID() + "@test.local"));
    }

    /** Research обязателен для Survey и Run; сам требует аккаунт и тип. */
    protected Research newResearch(PsychologistAccount owner) {
        var research = new Research(
                UUID.randomUUID(),
                "Исследование " + UUID.randomUUID(),
                null, null,
                TypeResearch.CPSK);
        research.setPsychologistAccount(owner);
        return entityManager.persistAndFlush(research);
    }

    /**
     * Запуск для сессии: publicToken обязателен (guard конструктора),
     * уникальность в рамках теста обеспечивает счётчик + UUID.
     */
    protected Run newRun(Research research) {
        return runRepository.save(new Run(
                UUID.randomUUID(),
                research,
                "tok-fixture-" + tokenCounter++ + "-" + UUID.randomUUID()));
    }

    /** Survey требует id в конструкторе и research (nullable = false). */
    protected Survey newSurvey(String title) {
        var survey = new Survey(UUID.randomUUID(), title, null, null);
        survey.setResearch(newResearch(newPsychologistAccount()));
        return entityManager.persistAndFlush(survey);
    }

    /**
     * Сессия нужна всем четырём ответным репозиториям — поэтому в базе,
     * а не в наследниках. Собирает полную прод-цепочку: survey → run → session.
     * Run строится по research, к которому привязан survey.
     */
    protected SurveySession newSession(Survey survey) {
        var session = new SurveySession(UUID.randomUUID());
        session.setSurvey(survey);
        session.setRun(newRun(survey.getResearch()));
        session.setRespondent(newRespondent());
        return entityManager.persistAndFlush(session);
    }
}
