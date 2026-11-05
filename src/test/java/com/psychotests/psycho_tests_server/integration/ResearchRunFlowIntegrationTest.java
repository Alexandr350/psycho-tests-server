package com.psychotests.psycho_tests_server.integration;

import com.jayway.jsonpath.JsonPath;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.CharacteristicAnswerDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowQuestionnaireRequestDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowSubmitRequestDto;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.SurveySessionStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.*;
import com.psychotests.psycho_tests_server.repository.CharacteristicsRepository;
import com.psychotests.psycho_tests_server.repository.RunRepository;
import com.psychotests.psycho_tests_server.repository.SurveySessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционный тест жизненного цикла запусков (Run) от статусов research
 * до состояния в базе: DTO статуса → контроллер → сервис (syncRuns) → репозиторий.
 * Проверяемая модель:
 *   - переход в RUNNING создаёт открытый запуск (startTimeRun != null, finishTimeRun == null);
 *   - уход с RUNNING закрывает текущий запуск (finishTimeRun проставлен);
 *   - возврат в RUNNING открывает НОВЫЙ запуск с новым токеном — старый остаётся закрытым;
 *   - сессии привязываются к тому запуску, который был открыт на момент старта сессии;
 *   - без открытого запуска старт сессии невозможен (гейт findOpenRun).
 */
class ResearchRunFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired private RunRepository runRepository;
    @Autowired private SurveySessionRepository sessionRepository;
    @Autowired private CharacteristicsRepository characteristicsRepository;

    private Research research;
    private Survey survey;

    @BeforeEach
    void setUpFixture() {
        research = new Research(
                UUID.randomUUID(), "Исследование запусков", "Описание", "кафедра", TypeResearch.CPSK);
        research.setPsychologistAccount(psychologistAccount);
        research = researchRepository.save(research);

        Survey surveyEntity = new Survey(
                UUID.randomUUID(), "Опрос запусков", "Описание", "Правила");
        surveyEntity.setResearch(research);
        survey = surveyRepository.save(surveyEntity);

        // одна характеристика — минимальный валидный пакет для сабмита
        characteristicsRepository.save(
                new Characteristics(UUID.randomUUID(), survey, "Характеристика 1"));

    }

    // ---------- Хелперы ----------

    private void patchStatus(UUID idResearch, String status) throws Exception {
        mockMvc.perform(patch("/api/researchs/research/" + idResearch + "/status")
                        .with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idResearch\":\"" + idResearch
                                + "\",\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    /** Все запуски research: база — источник истины, а не ответ контроллера. */
    private List<Run> runsOf(Research r) {
        return runRepository.findAll().stream()
                .filter(run -> run.getResearch().getIDResearch().equals(r.getIDResearch()))
                .toList();
    }

    private List<Run> openRuns(Research r) {
        return runsOf(r).stream().filter(Run::isOpen).toList();
    }

    // ---------- Сценарии ----------

    /**
     * Пауза — временная остановка: запуск остаётся ОТКРЫТЫМ, токен не меняется.
     * Возобновление продолжает тот же запуск: нового запуска не создаётся.
     * (В ответе API на PAUSE publicToken не меняется — семантика подтверждена.)
     */
    @Test
    void pauseKeepsRunOpen_resumeContinuesSameRun() throws Exception {
        patchStatus(research.getIDResearch(), "NEW");
        patchStatus(research.getIDResearch(), "RUNNING");

        Run run1 = runsOf(research).getFirst();
        String token1 = run1.getPublicToken();
        assertThat(run1.isOpen()).isTrue();

        patchStatus(research.getIDResearch(), "PAUSE");

        List<Run> afterPause = runsOf(research);
        assertThat(afterPause).hasSize(1);
        assertThat(afterPause.getFirst().isOpen()).isTrue();       // пауза не закрывает
        assertThat(afterPause.getFirst().getPublicToken()).isEqualTo(token1);

        patchStatus(research.getIDResearch(), "RUNNING");

        List<Run> afterResume = runsOf(research);
        assertThat(afterResume).hasSize(1);                        // новый запуск НЕ создан
        assertThat(afterResume.getFirst().getIdRun()).isEqualTo(run1.getIdRun());
        assertThat(afterResume.getFirst().getPublicToken()).isEqualTo(token1);
        assertThat(afterResume.getFirst().getStartTimeRun()).isNotNull();
    }

    /** STOPPED завершает волну: запуск закрывается, открытых не остаётся. */
    @Test
    void stopClosesRun_openRunsEmpty() throws Exception {
        patchStatus(research.getIDResearch(), "NEW");
        patchStatus(research.getIDResearch(), "RUNNING");
        assertThat(runsOf(research)).hasSize(1);

        patchStatus(research.getIDResearch(), "STOPPED");

        List<Run> atStop = runsOf(research);
        assertThat(atStop).hasSize(1);
        assertThat(atStop.getFirst().getFinishTimeRun()).isNotNull();
        assertThat(openRuns(research)).isEmpty();
    }

    /**
     * Рестарт — отдельная операция (POST /restart), не переход статусной
     * машины: STOPPED → RUNNING с новой волной. Прямой PATCH из STOPPED
     * по-прежнему запрещён — это проверяет карта переходов.
     */
    @Test
    void restartAfterStop_createsSecondRunWithNewToken() throws Exception {
        patchStatus(research.getIDResearch(), "NEW");
        patchStatus(research.getIDResearch(), "RUNNING");
        Run run1 = runsOf(research).getFirst();
        String token1 = run1.getPublicToken();

        patchStatus(research.getIDResearch(), "STOPPED");

        mockMvc.perform(post("/api/researchs/research/" + research.getIDResearch() + "/restart")
                        .with(psychologist()))
                .andExpect(status().isOk());

        List<Run> runs = runsOf(research);
        assertThat(runs).hasSize(2);
        Run run2 = runs.stream().filter(Run::isOpen).findFirst().orElseThrow();
        assertThat(run2.getIdRun()).isNotEqualTo(run1.getIdRun());
        assertThat(run2.getPublicToken()).isNotEqualTo(token1);
        assertThat(run1.getFinishTimeRun()).isNotNull();

        // research снова RUNNING — флоу доступен
        assertThat(researchRepository.findById(research.getIDResearch()).orElseThrow()
                .getStatusResearch()).isEqualTo(ResearchStatus.RUNNING);
    }

    /** Рестарт не из STOPPED — 409. */
    @Test
    void restart_rejectedWhenNotStopped() throws Exception {
        patchStatus(research.getIDResearch(), "NEW");

        mockMvc.perform(post("/api/researchs/research/" + research.getIDResearch() + "/restart")
                        .with(psychologist()))
                .andExpect(status().isConflict());
    }

    /** После рестарта: старый токен мёртв (404), новый жив (200). */
    @Test
    void publicToken_afterRestart_oldDeadNewAlive() throws Exception {
        patchStatus(research.getIDResearch(), "NEW");
        patchStatus(research.getIDResearch(), "RUNNING");
        Run run1 = runsOf(research).getFirst();

        patchStatus(research.getIDResearch(), "STOPPED");

        mockMvc.perform(get("/api/public/research/" + run1.getPublicToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/researchs/research/"
                        + research.getIDResearch() + "/restart")
                        .with(psychologist()))
                .andExpect(status().isOk());

        Run run2 = runsOf(research).stream()
                .filter(Run::isOpen).findFirst().orElseThrow();

        mockMvc.perform(get("/api/public/research/" + run1.getPublicToken()))
                .andExpect(status().isNotFound());          // старый не воскрес
        mockMvc.perform(get("/api/public/research/" + run2.getPublicToken()))
                .andExpect(status().isOk());                 // новый жив
    }



    /**
     * Сессии привязываются к открытому на момент старта запуску. В пределах
     * одного запуска (включая паузы — он же продолжается) респонденты
     * накапливаются; повторное прохождение того же респондента в том же
     * запуске запрещено констрейнтом (слайс-тест), в новом запуске — легально.
     */
    @Test
    void sessionsLandInOpenRun_acrossPauseCycle() throws Exception {
        patchStatus(research.getIDResearch(), "NEW");
        patchStatus(research.getIDResearch(), "RUNNING");

        UUID respondent1 = completePassage();

        patchStatus(research.getIDResearch(), "PAUSE");
        patchStatus(research.getIDResearch(), "RUNNING");   // тот же запуск продолжается

        UUID respondent2 = completePassage();

        Run currentRun = openRuns(research).getFirst();
        List<SurveySession> sessions = sessionRepository.findAll();
        assertThat(sessions).hasSize(2);
        assertThat(sessions).extracting(s -> s.getRun().getIdRun())
                .containsOnly(currentRun.getIdRun());
        assertThat(sessions).allSatisfy(s ->
                assertThat(s.getStatusSurveySession()).isEqualTo(SurveySessionStatus.COMPLETED));
        assertThat(respondent1).isNotEqualTo(respondent2);
    }

    /** Гейт findOpenRun: без открытого запуска старт сессии невозможен. */
    @Test
    void sessionStart_blockedWithoutOpenRun() throws Exception {
        patchStatus(research.getIDResearch(), "NEW");

        mockMvc.perform(get("/api/CPSKTest/flow/" + survey.getIdSurvey()))
                .andExpect(status().is4xxClientError());

        assertThat(runRepository.findAll()).isEmpty();
    }


    // ---------- Внутренние ----------

    /**
     * Полный проход респондента: старт сессии → анкета → сабмит → COMPLETED.
     * Возвращает id респондента из сессии в базе — надёжнее, чем парсинг ответа.
     */
    private UUID completePassage() throws Exception {
        String sessionBody = mockMvc
                .perform(get("/api/CPSKTest/flow/" + survey.getIdSurvey()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        UUID sessionId = UUID.fromString(JsonPath.parse(sessionBody).read("$.idSession"));

        mockMvc.perform(post("/api/CPSKTest/flow/" + survey.getIdSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SurveyFlowQuestionnaireRequestDto(sessionId, 30, "Ж"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIdSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new SurveyFlowSubmitRequestDto(
                                sessionId,
                                List.of(new CharacteristicAnswerDto(
                                        characteristicsRepository.findBySurvey(survey)
                                                .getFirst().getIdCharacteristic(), 3)),
                                List.of(),
                                List.of(),
                                List.of()))))
                .andExpect(status().isCreated());

        return sessionRepository.findById(sessionId).orElseThrow()
                .getRespondent().getIdRespondent();
    }
}
