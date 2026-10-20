package com.psychotests.psycho_tests_server.integration;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class DeleteResearchFlowTest extends AbstractIntegrationTest {

    private static final String RESEARCH_PART1_JSON = """
            {"nameResearch":"Тест удаления","descriptionResearch":"Описание",
             "customerResearch":"кафедра","typeResearch":"CPSK"}""";

    @Autowired
    private MockMvc mockMvc;

    /** Проверяет, что удаление разрешено во всех «тихих» статусах.
     Параметризованный тест проходит по матрице DRAFT, NEW, STOPPED —
     для каждого статуса создаётся исследование (createResearchWithStatus
     доводит его до нужного состояния), DELETE возвращает 204.

     Дополняет deleteRunning_isConflict до полного покрытия статусной
     модели: RUNNING (и, вероятно, FINISHED) → 409 запрещено,
     DRAFT/NEW/STOPPED → 204 разрешено. Если завтра добавят новый статус
     (например, ARCHIVED), EnumSource заставит осознанно решить, куда он
     попадает — в разрешённые или запрещённые, — а не забудет о нём молча.**/
    @ParameterizedTest
    @EnumSource(value = ResearchStatus.class, names = {"DRAFT", "NEW", "STOPPED"})
    void deleteAllowedStatuses_isNoContent(ResearchStatus target) throws Exception {
        UUID idResearch = createResearchWithStatus(target);

        mockMvc.perform(delete("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isNoContent());
    }

    /** Проверяет запрет удаления активного исследования.
     Психолог создаёт исследование, переводит его в RUNNING и пытается удалить —
     сервер отвечает 409 Conflict.

     Бизнес-логика: RUNNING означает, что по опросу уже могут идти респонденты,
     и его удаление на ходу лишило бы их ответов и нарушило бы целостность
     данных. Удаление разрешено только в «тихих» статусах — DRAFT, NEW, STOPPED
     (проверяются в deleteAllowedStatuses_isNoContent).

     409 (а не 404/403) выбран потому, что ресурс существует и принадлежит
     пользователю, но находится в состоянии, несовместимом с операцией —
     классическое применение этого кода.**/
    @Test
    void deleteRunning_isConflict() throws Exception {
        UUID idResearch = createResearchWithStatus(ResearchStatus.RUNNING);

        mockMvc.perform(delete("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isConflict());
    }

    /** Проверяет, что после удаления исследование действительно недоступно.
     Психолог создаёт исследование (STOPPED), удаляет его (204),
     а затем запрашивает его же GET-ом — сервер отвечает 404.

     Отличие от deleteTwice_secondIsNotFound: там проверяется второй DELETE
     (путь удаления), здесь — чтение по обычному GET (путь доступа). Вместе
     они гарантируют, что удаление не «зачитывает» мягко скрытую запись
     (soft delete без проверки), а реально делает её невидимой для любых
     операций с проверкой существования.**/
    @Test
    void delete_thenGet_isNotFound() throws Exception {
        UUID idResearch = createResearchWithStatus(ResearchStatus.STOPPED);

        mockMvc.perform(delete("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isNotFound());
    }

    /** Проверяет идемпотентную защиту от повторного удаления.
     Первый DELETE своего исследования в статусе STOPPED проходит (204).
     Второй DELETE по тому же id возвращает 404 — сущность уже удалена,
     и повторный запрос неотличим от обращения к несуществующему ресурсу.

     Сценарий покрывает два риска сразу:
     1. Удаление действительно убирает запись, а не меняет флаг или статус.
     2. Двойной клик/повторный запрос пользователя не приводит к ошибке 500
     (например, из-за ConstraintViolation) — API отвечает штатным 404.**/
    @Test
    void deleteTwice_secondIsNotFound() throws Exception {
        UUID idResearch = createResearchWithStatus(ResearchStatus.STOPPED);

        mockMvc.perform(delete("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isNotFound());
    }

    /**
     * Создаёт исследование и доводит его до указанного статуса.
     * DRAFT — сразу после создания; NEW — после заполнения всех частей;
     * RUNNING — через PATCH; STOPPED — через PATCH после RUNNING.
     */
    private UUID createResearchWithStatus(ResearchStatus target) throws Exception {
        // Part 1: создание исследования (статус DRAFT)
        String researchJson = mockMvc.perform(
                        post("/api/researchs/research").with(psychologist())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(RESEARCH_PART1_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID idResearch = extractId(researchJson, "idResearch");

        if (target == ResearchStatus.DRAFT) {
            return idResearch;
        }

        // Part 2: опрос → сервер переведёт исследование в NEW
        String surveyRequestJson = """
                {"idResearch":"%s","titleSurvey":"Опрос","descriptionSurvey":"desc",
                 "rulesSurvey":"rules"}""".formatted(idResearch);

        String surveyJson = mockMvc.perform(
                        post("/api/researchs/survey").with(psychologist())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(surveyRequestJson))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID surveyId = extractId(surveyJson, "surveyId");

        // Part 3: характеристики
        mockMvc.perform(post("/api/researchs/characteristics").with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idSurvey":"%s","characteristics":"тревога, внимание"}"""
                                .formatted(surveyId)))
                .andExpect(status().isNoContent());

        // Part 4: картинки
        mockMvc.perform(multipart("/api/researchs/items")
                        .file(jpg("img1.jpg")).file(jpg("img2.jpg"))
                        .param("surveyId", surveyId.toString())
                        .with(psychologist()))
                .andExpect(status().isNoContent());

        // Part 5: дополнительные элементы → авто-переход в NEW
        mockMvc.perform(multipart("/api/researchs/items-sub")
                        .file(jpg("sub1.jpg"))
                        .param("surveyId", surveyId.toString())
                        .with(psychologist()))
                .andExpect(status().isOk());

        // Подтверждаем статус NEW
        mockMvc.perform(get("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchStatus").value("NEW"));

        if (target == ResearchStatus.RUNNING || target == ResearchStatus.STOPPED) {
            // NEW → RUNNING
            mockMvc.perform(patch("/api/researchs/research/" + idResearch + "/status")
                            .with(psychologist())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"idResearch\":\"%s\",\"status\":\"RUNNING\"}"
                                    .formatted(idResearch)))
                    .andExpect(status().isNoContent());

            if (target == ResearchStatus.STOPPED) {
                // RUNNING → STOPPED
                mockMvc.perform(patch("/api/researchs/research/" + idResearch + "/status")
                                .with(psychologist())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"idResearch\":\"%s\",\"status\":\"STOPPED\"}"
                                        .formatted(idResearch)))
                        .andExpect(status().isNoContent());
            }
        }
        return idResearch;
    }


}
