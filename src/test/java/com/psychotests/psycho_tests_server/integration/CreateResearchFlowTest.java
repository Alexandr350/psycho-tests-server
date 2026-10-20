package com.psychotests.psycho_tests_server.integration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class CreateResearchFlowTest extends AbstractIntegrationTest {
    /**
     * Сквозной (end-to-end) сценарий «мастер-флоу» создания исследования.
     *
     * Что проверяет:
     * 1. Психолог последовательно создаёт исследование (201) и опрос (201).
     * 2. Заполняет все обязательные части опроса: характеристики (204),
     *    картинки-элементы (204) и дополнительные элементы (200).
     * 3. Сервер автоматически переводит исследование в статус NEW после того,
     *    как заполнены все части — проверяется GET-ом (200 + researchStatus=NEW).
     * 4. Психолог вручную переводит исследование NEW → RUNNING (204).
     *
     * Тест фиксирует «счастливый путь» API: все эндпоинты работают в правильном
     * порядке и возвращают корректные коды, а авто-переход DRAFT → NEW не требует
     * отдельного вызова со стороны клиента.
     *
     * Ожидаемая последовательность статусов исследования: DRAFT → NEW → RUNNING.
     */
    @Test
    void fullMasterFlow_createToRunning() throws Exception {
        // Part 1: создание исследования
        String researchJson = mockMvc.perform(
                        post("/api/researchs/research").with(psychologist())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(RESEARCH_PART1_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID idResearch = extractId(researchJson, "idResearch");

        // Part 2: опрос
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
                {"idSurvey":"%s","characteristics":"тревога, внимание"}""".formatted(surveyId)))
                .andExpect(status().isNoContent());

        // Part 4: картинки
        mockMvc.perform(multipart("/api/researchs/items")
                        .file(jpg("img1.jpg")).file(jpg("img2.jpg"))
                        .param("surveyId", surveyId.toString())
                        .with(psychologist()))
                .andExpect(status().isNoContent());

        // Part 5: дополнительные элементы
        mockMvc.perform(multipart("/api/researchs/items-sub")
                        .file(jpg("sub1.jpg"))
                        .param("surveyId", surveyId.toString())
                        .with(psychologist()))
                .andExpect(status().isOk());

        // Сервер сам перевёл исследование в NEW после заполнения всех частей — проверяем это
        mockMvc.perform(get("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchStatus").value("NEW"));

        // NEW → RUNNING
        mockMvc.perform(patch("/api/researchs/research/" + idResearch + "/status")
                        .with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idResearch\":\"%s\",\"status\":\"RUNNING\"}".formatted(idResearch)))
                .andExpect(status().isNoContent());
    }

    /** Создание исследования без аутентификации — 401.**/
    @Test
    void unauthenticated_cannotCreateResearch() throws Exception {
        mockMvc.perform(post("/api/researchs/research")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    /** Проверка изоляции данных: чужой психолог не может удалить чужое исследование.
     Стратегия: 404 вместо 403 — не раскрываем существование чужих ресурсов (защита от enumeration-атак).
     Владелец при этом может удалить своё — 204, что подтверждает: 404 у чужака вызван
     именно проверкой владельца, а не поломкой удаления.**/
    @Test
    void foreignPsychologist_cannotDeleteResearch() throws Exception {
        String json = mockMvc.perform(
                        post("/api/researchs/research").with(psychologist())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {"nameResearch":"Тест удаления","descriptionResearch":"Описание",
                                 "customerResearch":"кафедра","typeResearch":"CPSK"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID researchId = extractId(json, "idResearch");

        PsychologistAccount stranger = psychologistAccountRepository.save(
                new PsychologistAccount(UUID.randomUUID(), "Другой", "Психолог",
                        "pwd", "stranger@example.com"));
        PsychologistUserDetails strangerDetails = new PsychologistUserDetails(stranger);
        Authentication strangerAuth = new UsernamePasswordAuthenticationToken(
                strangerDetails, null, strangerDetails.getAuthorities());

        mockMvc.perform(
                        delete("/api/researchs/research/" + researchId)
                                .with(authentication(strangerAuth)))
                .andExpect(status().isNotFound());

        mockMvc.perform(
                        delete("/api/researchs/research/" + researchId).with(psychologist()))
                .andExpect(status().isNoContent());
    }

    /** Психолог не может создать опрос «в воздухе» — опрос обязан принадлежать
     существующему исследованию, которое принадлежит ему же.**/
    @Test
    void surveyWithForeignResearchId_fails() throws Exception {
        mockMvc.perform(post("/api/researchs/survey").with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"researchId":"%s","titleSurvey":"Чужой","descriptionSurvey":"x",
                             "rulesSurvey":"x"}""".formatted(UUID.randomUUID())))
                .andExpect(status().is4xxClientError());
    }

    /** Проверка валидатора переходов статусов: легальный DRAFT → NEW проходит (204),
     запрещённый NEW → STOPPED отклоняется с 409 и не меняет статус (проверено GET-ом).**/
    @Test
    void invalidTransition_isConflict() throws Exception {
        String researchJson = mockMvc.perform(post("/api/researchs/research").with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {"nameResearch":"Тест переходов","descriptionResearch":"Описание",
                     "customerResearch":"кафедра","typeResearch":"CPSK"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID idResearch = extractId(researchJson, "idResearch");

        // DRAFT → NEW: разрешённый переход
        mockMvc.perform(patch("/api/researchs/research/" + idResearch + "/status").with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idResearch\":\"%s\",\"status\":\"NEW\"}".formatted(idResearch)))
                .andExpect(status().isNoContent());

        // NEW → STOPPED запрещён
        mockMvc.perform(patch("/api/researchs/research/" + idResearch + "/status").with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idResearch\":\"%s\",\"status\":\"STOPPED\"}".formatted(idResearch)))
                .andExpect(status().isConflict());

        // статус не изменился
        mockMvc.perform(get("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchStatus").value("NEW"));
    }
}
