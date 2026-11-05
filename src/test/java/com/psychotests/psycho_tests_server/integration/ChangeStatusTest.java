package com.psychotests.psycho_tests_server.integration;

import com.psychotests.psycho_tests_server.model.Run;
import com.psychotests.psycho_tests_server.repository.RunRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты смены статуса исследования и публичного токена.
 *
 * <p>Фикстуру (психолог, research, survey с характеристиками и items,
 * легальный перевод DRAFT → NEW → RUNNING через API) поднимает
 * AbstractCPSKFlowTest: к моменту каждого теста research уже в RUNNING
 * и у него есть открытый Run с publicToken.</p>
 */
class ChangeStatusTest extends AbstractCPSKFlowTest {

    @Autowired
    private RunRepository runRepository;

    /** Единственный открытый запуск фикстуры — ищется тем же запросом,
     *  каким сервис резолвит сессию (finishTimeRun is null). */
    private Run openRun() {
        return runRepository.findOpenRun(research.getIDResearch())
                .orElseThrow(() -> new AssertionError("Открытый Run не найден"));
    }



    // ---------- смена статуса: PAUSE / STOPPED / RUNNING ----------

    /** PAUSE сохраняет Run открытым, но токен перестаёт резолвиться;
     *  после возобновления запуск и токен восстанавливаются — те же. */
    @Test
    void pauseKeepsRunOpenAndResumeKeepsToken() throws Exception {
        Run runBefore = openRun();

        patchStatus(research.getIDResearch(), "PAUSE");

        Run afterPause = runRepository.findById(runBefore.getIdRun()).orElseThrow();
        assertThat(afterPause.isOpen()).isTrue();          // запуск не закрывается
        assertThat(afterPause.getPublicToken())
                .isEqualTo(runBefore.getPublicToken());    // токен не пересоздаётся

        patchStatus(research.getIDResearch(), "RUNNING");

        Run afterResume = openRun();
        assertThat(afterResume.getIdRun()).isEqualTo(runBefore.getIdRun());
        assertThat(afterResume.getPublicToken()).isEqualTo(runBefore.getPublicToken());
    }

    /** STOPPED закрывает Run и отзывает токен research. */
    @Test
    void stopClosesRunAndRevokesToken() throws Exception {
        Run run = openRun();

        patchStatus(research.getIDResearch(), "STOPPED");

        assertThat(runRepository.findById(run.getIdRun()).orElseThrow().isOpen()).isFalse();
        assertThat(researchRepository.findById(research.getIDResearch()).orElseThrow()
                .getPublicToken()).isNull();
    }

    /** Недопустимый прыжок статусов (DRAFT → RUNNING) отвергается — 409. */
    @Test
    void illegalTransition_rejected() throws Exception {
        // фикстура уже прогнала NEW → RUNNING; создаём новое research в DRAFT
        UUID freshId = createResearch();
        mockMvc.perform(patch("/api/researchs/research/" + freshId + "/status")
                        .with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idResearch\":\"" + freshId + "\",\"status\":\"RUNNING\"}"))
                .andExpect(status().isConflict());
    }

    // ---------- публичный резолв по токену ----------

    /** Валидный токен при RUNNING: отдаётся idSurvey и тексты опроса. */
    @Test
    void resolvePublicToken_returnsSurveyInfo_whenRunning() throws Exception {
        Run run = openRun();

        mockMvc.perform(get("/api/public/research/" + run.getPublicToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idSurvey").value(survey.getIdSurvey().toString()))
                .andExpect(jsonPath("$.titleSurvey").value("Опрос CPSK"))
                .andExpect(jsonPath("$.descriptionSurvey").value("Описание опроса"))
                .andExpect(jsonPath("$.rulesSurvey").value("Правила прохождения"));
    }

    /** PAUSE по токену — 404, не раскрываем существование опроса. */
    @Test
    void resolvePublicToken_blockedOnPause() throws Exception {
        patchStatus(research.getIDResearch(), "PAUSE");
        Run run = openRun();   // PAUSE не закрывает Run — токен есть, но гейт на RUNNING

        mockMvc.perform(get("/api/public/research/" + run.getPublicToken()))
                .andExpect(status().isNotFound());
    }

    /** STOPPED по токену — 404 (запуск закрыт). */
    @Test
    void resolvePublicToken_blockedOnStopped() throws Exception {
        Run run = openRun();   // фиксируем запуск до закрытия

        patchStatus(research.getIDResearch(), "STOPPED");

        // запуск закрыт — именно поэтому токен больше не резолвится
        assertThat(runRepository.findById(run.getIdRun()).orElseThrow().isOpen()).isFalse();

        mockMvc.perform(get("/api/public/research/" + run.getPublicToken()))
                .andExpect(status().isNotFound());
    }


    /** Неизвестный токен — тот же 404, что и для закрытых опросов. */
    @Test
    void resolvePublicToken_unknownToken_returns404() throws Exception {
        mockMvc.perform(get("/api/public/research/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
