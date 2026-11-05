package com.psychotests.psycho_tests_server.integration;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.security.PsychologistUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UpdateSurveyFlowTest extends AbstractIntegrationTest {

    private static final String UPDATED_SURVEY = """
            {"idResearch":"%s","titleSurvey":"Новое название","descriptionSurvey":"Новое описание",
             "rulesSurvey":"Новые правила"}""";

    /**
     * Ключевой тест фичи резюма: PATCH существующего опроса обновляет его
     * поля (200 с обновлённым survey в теле) и НЕ создаёт новый. До этой
     * фичи повторное прохождение шага 2 через POST /survey дублировало
     * запись — здесь count() фиксирует, что после двух PATCH опросов
     * в базе по-прежнему один.
     */
    @Test
    void patchSurveyOfDraft_updatesFields_noDuplicate() throws Exception {
        UUID idResearch = createResearch();
        UUID idSurvey = createSurvey(idResearch);

        mockMvc.perform(patch("/api/researchs/survey/" + idSurvey).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_SURVEY.formatted(idResearch)))
                .andExpect(status().isOk());

        // второй PATCH — свободная навигация «Назад/Далее» из мастера не должна плодить записи
        mockMvc.perform(patch("/api/researchs/survey/" + idSurvey).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_SURVEY.formatted(idResearch)))
                .andExpect(status().isOk());

        assertThat(surveyRepository.count()).isEqualTo(1);
        Survey survey = surveyRepository.findAll().getFirst();
        assertThat(survey.getIdSurvey()).isEqualTo(idSurvey);
        assertThat(survey.getTitleSurvey()).isEqualTo("Новое название");
        assertThat(survey.getDescriptionSurvey()).isEqualTo("Новое описание");
        assertThat(survey.getRulesSurvey()).isEqualTo("Новые правила");
    }

    /** Guard статуса: опрос правится только у черновика, в NEW — 409 и поля не тронуты. */
    @Test
    void patchSurveyOfNew_isConflict_fieldsUnchanged() throws Exception {
        UUID idResearch = createResearch();
        UUID idSurvey = createSurvey(idResearch);
        completeToNew(idSurvey, idResearch);

        mockMvc.perform(patch("/api/researchs/survey/" + idSurvey).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_SURVEY.formatted(idResearch)))
                .andExpect(status().isConflict());

        Survey survey = surveyRepository.findAll().getFirst();
        assertThat(survey.getTitleSurvey()).isEqualTo("Опрос");
    }

    /** Без аутентификации — 401. */
    @Test
    void unauthenticated_cannotPatchSurvey() throws Exception {
        UUID idResearch = createResearch();
        UUID idSurvey = createSurvey(idResearch);

        mockMvc.perform(patch("/api/researchs/survey/" + idSurvey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_SURVEY.formatted(idResearch)))
                .andExpect(status().isUnauthorized());
    }

    /** Чужой психолог: 404 (не раскрываем существование), владелец PATCH-ит штатно. */
    @Test
    void foreignPsychologist_patchSurvey_isNotFound() throws Exception {
        UUID idResearch = createResearch();
        UUID idSurvey = createSurvey(idResearch);

        PsychologistAccount stranger = psychologistAccountRepository.save(
                new PsychologistAccount(UUID.randomUUID(), "Другой", "Психолог",
                        "pwd", "stranger-survey@example.com"));
        PsychologistUserDetails strangerDetails = new PsychologistUserDetails(stranger);
        Authentication strangerAuth = new UsernamePasswordAuthenticationToken(
                strangerDetails, null, strangerDetails.getAuthorities());

        mockMvc.perform(patch("/api/researchs/survey/" + idSurvey)
                        .with(authentication(strangerAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_SURVEY.formatted(idResearch)))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/researchs/survey/" + idSurvey).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(UPDATED_SURVEY.formatted(idResearch)))
                .andExpect(status().isOk());
    }

    /** Пробельное название: 400 по @NotBlank, запись в базе не изменилась. */
    @Test
    void blankTitle_isRejected_surveyUnchanged() throws Exception {
        UUID idResearch = createResearch();
        UUID idSurvey = createSurvey(idResearch);

        mockMvc.perform(patch("/api/researchs/survey/" + idSurvey).with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {"idResearch":"%s","titleSurvey":"   ","descriptionSurvey":"d",
                 "rulesSurvey":"r"}""".formatted(idResearch)))
                .andExpect(status().isBadRequest());
        assertThat(surveyRepository.findById(idSurvey))
                .hasValueSatisfying(s ->
                        assertThat(s.getTitleSurvey()).isEqualTo("Опрос"));
        assertThat(surveyRepository.findAll().getFirst().getTitleSurvey()).isEqualTo("Опрос");
    }

    /** Доводит исследование до NEW: характеристики + объекты + sub-объекты. */
    private void completeToNew(UUID idSurvey, UUID idResearch) throws Exception {
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
    }
}
