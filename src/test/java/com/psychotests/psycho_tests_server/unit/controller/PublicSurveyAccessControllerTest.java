package com.psychotests.psycho_tests_server.unit.controller;

import com.psychotests.psycho_tests_server.controller.PublicSurveyAccessController;
import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.PublicSurveyAccessResponseDto;
import com.psychotests.psycho_tests_server.model.Survey;
import com.psychotests.psycho_tests_server.service.ResearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Юнит-тесты PublicSurveyAccessController — публичный вход респондента
 * по токену запуска.
 *
 * <p>Контроллер тонкий: один метод, чистое делегирование в
 * ResearchService.resolvePublicToken. Поэтому проверяется только HTTP-клей,
 * а не бизнес-логика: 200 с idSurvey в JSON на валидный токен и прозрачный
 * проброс ResponseStatusException(404) от сервиса. Все содержательные ветви
 * (закрытый запуск, research не в RUNNING, неизвестный токен → 404) уже
 * зафиксированы в ResearchServiceTest.ResolvePublicToken — здесь их не
 * дублируем.</p>
 *
 * <p>Как работает: standalone MockMvc без Spring-контекста и без Security-
 * фильтров. ResponseStatusException преобразуется в 404 штатным
 * ResponseStatusExceptionResolver'ом, который входит в стандартный набор
 * standalone-конфигурации — никакого своего @ExceptionHandler не нужно.</p>
 */
@ExtendWith(MockitoExtension.class)
class PublicSurveyAccessControllerTest {

    private static final String TOKEN = "tok-123";

    @Mock
    private ResearchService researchService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PublicSurveyAccessController(researchService))
                .build();
    }

    @Nested
    class Resolve {

        /** Валидный токен: 200 и все поля PublicSurveyAccessResponseDto в JSON-теле. */
        @Test
        void validToken_returns200WithIdSurvey() throws Exception {
            var idSurvey = UUID.randomUUID();
            when(researchService.resolvePublicToken(TOKEN))
                    .thenReturn(new PublicSurveyAccessResponseDto(
                            idSurvey,
                            "Опрос CPSK",
                            "Описание опроса",
                            "Правила прохождения"));

            mockMvc.perform(get("/api/public/research/{token}", TOKEN))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.idSurvey").value(idSurvey.toString()))
                    .andExpect(jsonPath("$.titleSurvey").value("Опрос CPSK"))
                    .andExpect(jsonPath("$.descriptionSurvey").value("Описание опроса"))
                    .andExpect(jsonPath("$.rulesSurvey").value("Правила прохождения"));
        }


        /**
         * Сервис отклонил токен (закрытый запуск / не-RUNNING / неизвестный):
         * ResponseStatusException(404) пробрасывается как есть — контроллер
         * не оборачивает отказ, клиент видит ровно 404, а не 500.
         */
        @Test
        void serviceRejection_propagates404() throws Exception {
            when(researchService.resolvePublicToken(TOKEN))
                    .thenThrow(new ResponseStatusException(NOT_FOUND, "Опрос недоступен"));

            mockMvc.perform(get("/api/public/research/{token}", TOKEN))
                    .andExpect(status().isNotFound());
        }
    }
}
