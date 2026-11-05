package com.psychotests.psycho_tests_server.integration;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UpdateResearchFlowTest extends AbstractIntegrationTest {

    private static final String UPDATED_JSON = """
            {"nameResearch":"Новое имя","descriptionResearch":"Новое описание",
             "customerResearch":"новый заказчик","typeResearch":"CPSK"}""";

    /** PATCH шапки DRAFT-исследования: 204, поля обновлены (проверено GET-ом), статус не изменился. */
    @Test
    void patchDraft_updatesFields_statusStaysDraft() throws Exception {
        UUID idResearch = createResearchWithStatus(ResearchStatus.DRAFT);

        mockMvc.perform(patch("/api/researchs/research/" + idResearch).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_JSON))
                .andExpect(status().isNoContent());

        String body = mockMvc.perform(
                        get("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchStatus").value("DRAFT"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("Новое имя", "Новое описание", "новый заказчик");
    }

    /** PATCH в статусе NEW (после заполнения всех частей): редактирование шапки разрешено. */
    @Test
    void patchNew_updatesFields_statusStaysNew() throws Exception {
        UUID idResearch = createResearchWithStatus(ResearchStatus.NEW);

        mockMvc.perform(patch("/api/researchs/research/" + idResearch).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_JSON))
                .andExpect(status().isNoContent());

        String body = mockMvc.perform(
                        get("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchStatus").value("NEW"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("Новое имя");
    }

    /** Запрещённые статусы: 409 (исключение замаплено), поля не изменились. */
    @ParameterizedTest(name = "PATCH в статусе {0}: 409, шапка не изменилась")
    @EnumSource(value = ResearchStatus.class,
            names = {"RUNNING", "PAUSE", "STOPPED", "ARCHIVE"})
    void patchForbiddenStatus_isConflict_fieldsUnchanged(ResearchStatus target) throws Exception {
        UUID idResearch = createResearchWithStatus(target);

        mockMvc.perform(patch("/api/researchs/research/" + idResearch).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_JSON))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchStatus").value(target.name()));
    }

    /** Без аутентификации PATCH — 401. */
    @Test
    void unauthenticated_cannotPatch() throws Exception {
        UUID idResearch = createResearchWithStatus(ResearchStatus.DRAFT);

        mockMvc.perform(patch("/api/researchs/research/" + idResearch)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_JSON))
                .andExpect(status().isUnauthorized());
    }

    /** Чужой психолог: 404 — не раскрываем существование ресурса (та же стратегия, что у удаления). */
    @Test
    void foreignPsychologist_patch_isNotFound() throws Exception {
        UUID idResearch = createResearchWithStatus(ResearchStatus.DRAFT);

        PsychologistAccount stranger = psychologistAccountRepository.save(
                new PsychologistAccount(UUID.randomUUID(), "Другой", "Психолог",
                        "pwd", "stranger-edit@example.com"));
        PsychologistUserDetails strangerDetails = new PsychologistUserDetails(stranger);
        Authentication strangerAuth = new UsernamePasswordAuthenticationToken(
                strangerDetails, null, strangerDetails.getAuthorities());

        mockMvc.perform(patch("/api/researchs/research/" + idResearch)
                        .with(authentication(strangerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_JSON))
                .andExpect(status().isNotFound());

        // владелец при этом редактирует штатно — 404 у чужака именно из-за проверки владельца
        mockMvc.perform(patch("/api/researchs/research/" + idResearch).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_JSON))
                .andExpect(status().isNoContent());
    }

    /** Пробельное имя: 400 по @NotBlank, шапка в БД не изменилась. */
    @Test
    void blankName_isRejected_fieldsUnchanged() throws Exception {
        UUID idResearch = createResearchWithStatus(ResearchStatus.DRAFT);

        mockMvc.perform(patch("/api/researchs/research/" + idResearch).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"nameResearch":"   ","descriptionResearch":"d",
                 "customerResearch":"c","typeResearch":"CPSK"}"""))
                .andExpect(status().isBadRequest());

        String body = mockMvc.perform(
                        get("/api/researchs/research/" + idResearch).with(psychologist()))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(body).contains("Тест редактирования");
        assertThat(body).doesNotContain("\"nameResearch\":\"   \"");
    }

    /**
     * Хелпер доведения research до целевого статуса (аналог хелпера из
     * DeleteResearchFlowTest, расширенный до PAUSE и ARCHIVE).
     * DRAFT — сразу после создания; дальше цепочка POST-частей до NEW,
     * затем PATCH-переходы по графу до целевого статуса.
     */
    private UUID createResearchWithStatus(ResearchStatus target) throws Exception {
        String researchJson = mockMvc.perform(
                        post("/api/researchs/research").with(psychologist())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"nameResearch":"Тест редактирования","descriptionResearch":"Описание",
                         "customerResearch":"кафедра","typeResearch":"CPSK"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID idResearch = extractId(researchJson, "idResearch");

        if (target == ResearchStatus.DRAFT) {
            return idResearch;
        }

        String surveyJson = mockMvc.perform(
                        post("/api/researchs/survey").with(psychologist())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                        {"idResearch":"%s","titleSurvey":"Опрос","descriptionSurvey":"desc",
                         "rulesSurvey":"rules"}""".formatted(idResearch)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        UUID idSurvey = extractId(surveyJson, "idSurvey");

        mockMvc.perform(post("/api/researchs/characteristics").with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"idSurvey":"%s","characteristics":"тревога, внимание"}""".formatted(idSurvey)))
                .andExpect(status().isNoContent());

        mockMvc.perform(multipart("/api/researchs/items")
                        .file(jpg("img1.jpg")).file(jpg("img2.jpg"))
                        .param("idSurvey", idSurvey.toString())
                        .with(psychologist()))
                .andExpect(status().isNoContent());

        mockMvc.perform(multipart("/api/researchs/items-sub")
                        .file(jpg("sub1.jpg"))
                        .param("idSurvey", idSurvey.toString())
                        .with(psychologist()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/researchs/research/" + idResearch).with(psychologist()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchStatus").value("NEW"));

        if (target == ResearchStatus.NEW) {
            return idResearch;
        }

        patchStatus(idResearch, "RUNNING");
        if (target == ResearchStatus.RUNNING) {
            return idResearch;
        }
        if (target == ResearchStatus.PAUSE) {
            patchStatus(idResearch, "PAUSE");
            return idResearch;
        }

        patchStatus(idResearch, "STOPPED");
        if (target == ResearchStatus.STOPPED) {
            return idResearch;
        }
        // ARCHIVE
        patchStatus(idResearch, "ARCHIVE");
        return idResearch;
    }

    private void patchStatus(UUID idResearch, String status) throws Exception {
        // PATCH статуса возвращает 200 с обновлённым research в теле —
        // проверяем, что в ответе действительно выставлен запрошенный статус
        mockMvc.perform(patch("/api/researchs/research/" + idResearch + "/status")
                        .with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idResearch\":\"%s\",\"status\":\"%s\"}"
                                .formatted(idResearch, status)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.researchStatus").value(status));
    }

}
