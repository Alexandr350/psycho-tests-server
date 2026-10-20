package com.psychotests.psycho_tests_server.integration;

import com.psychotests.psycho_tests_server.dto.request.create_researsh_dto.ResearchRequestInfoDtoPart1;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.Order;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NoDataLeakGuardTest extends AbstractIntegrationTest {

    /**
     * Страж изоляции тестов: проверяет, что данные, созданные внутри
     * тестовой транзакции, не сохраняются в базу после её отката.
     *
     * Тест 1 создаёт исследование через MockMvc внутри @Transactional,
     * тест 2 — без транзакции — убеждается, что в базе пусто.
     *
     * Если второй тест падает — значит, откат перестал работать
     * (например, кто-то добавил @Commit или убрал @Transactional),
     * и остальные интеграционные тесты могут видеть чужие данные.
     */

    @Test
    @Order(1)
    @Transactional
    void createResearch_insideTransaction() throws Exception {
        mockMvc.perform(post("/api/researchs/research")
                        .with(psychologist())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResearchRequestInfoDtoPart1(
                                "Страж", "desc", "заказчик", TypeResearch.CPSK))))
                .andExpect(status().isCreated());

        // внутри транзакции данные видны
        assertEquals(1, researchRepository.count());
    }

    @Test
    @Order(2)
    void databaseIsEmpty_afterRollback() {
        assertEquals(0, researchRepository.count());
    }
}

