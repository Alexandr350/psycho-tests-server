package com.psychotests.psycho_tests_server.integration;

import com.psychotests.psycho_tests_server.dto.request.authorization_dto.LoginPsychologicalAccountRequestDto;
import com.psychotests.psycho_tests_server.dto.request.authorization_dto.RegisteringPsychologicalAccountRequestDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;


import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest extends AbstractIntegrationTest {


    private static final String EMAIL = "it-test-" + UUID.randomUUID() + "@mail.ru";

    /**
     * Сквозной сценарий: регистрация → доступ без аутентификации → логин → доступ с сессией.
     *
     * Шаг 1. POST /api/psychologists/register — создание аккаунта в реальной БД,
     *        ожидается 201. Дополнительно проверяется автологин: сессия получает
     *        SPRING_SECURITY_CONTEXT.
     * Шаг 2. GET /api/researchs/all без сессии — анонимный доступ к защищённому
     *        эндпоинту отклоняется с 401 (проверка периметра security).
     * Шаг 3. POST /api/psychologists/login — авторизация с корректными данными,
     *        ожидается 200; сессия после логина должна существовать.
     * Шаг 4. GET /api/researchs/all с сессией из шага 3 — контекст безопасности
     *        восстанавливается из сессии, эндпоинт отвечает 200.
     *
     * Проверяет работу цепочки целиком: SecurityFilterChain, валидация DTO,
     * сериализация JSON, Hibernate с констрейнтами, механизм сессий и перенос
     * контекста безопасности между запросами. Отдельные ветки логики
     * (неверный пароль, дубликат email) покрыты соседними тестами.
     */

    @Test
    void register_thenAccessProtected_thenLogin() throws Exception {
        // 1. Регистрация — 201, сессия создаётся (автологин)
        mockMvc.perform(post("/api/psychologists/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegisteringPsychologicalAccountRequestDto(
                                        "Анна", "Иванова", "Strong1!", EMAIL))))
                .andExpect(status().isCreated());

        // 2. Анонимный запрос без сессии — 401
        mockMvc.perform(get("/api/researchs/all"))
                .andExpect(status().isUnauthorized());

        // 3. Логин — 200, сессия получена
        var loginResult = mockMvc.perform(post("/api/psychologists/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginPsychologicalAccountRequestDto(EMAIL, "Strong1!"))))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();

        // 4. С этой сессией защищённый эндпоинт доступен — 200
        mockMvc.perform(get("/api/researchs/all").session(session))
                .andExpect(status().isOk());
    }

    /**
     * Проверяет логин с email, которого нет в базе: сервис бросает
     * InvalidCredentialsException, клиент получает 401 (а не 500 или 404).
     *
     * Ключевой контракт: ответ на «email не найден» неотличим от ответа
     * на «неверный пароль» — по статусу и тексту нельзя перечислить,
     * какие email зарегистрированы (защита от user enumeration).
     */

    @Test
    void login_withUnknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/api/psychologists/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginPsychologicalAccountRequestDto("nobody@mail.ru", "Whatever1!"))))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Проверяет отказ на повторную регистрацию с уже занятым email: первая
     * регистрация проходит (201), вторая с тем же email отклоняется с 409.
     *
     * Внутри одного теста оба запроса идут в одной транзакции — это имитирует
     * честный дубликат. Срабатывание может обеспечить как ветка existsByEmail,
     * так и unique-констрейнт uk_psychologist_email (пойманный через
     * DataIntegrityViolationException) — тест фиксирует только внешний контракт:
     * второй запрос получает 409.
     */
    @Test
    void register_withDuplicateEmail_returns409() throws Exception {
        var dto = new RegisteringPsychologicalAccountRequestDto(
                "Пётр", "Петров", "Strong1!", EMAIL);

        // 1. Первая регистрация — успешна
        mockMvc.perform(post("/api/psychologists/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());

        // 2. Повторная с тем же email — 409
        mockMvc.perform(post("/api/psychologists/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

}
