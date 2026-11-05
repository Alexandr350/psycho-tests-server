package com.psychotests.psycho_tests_server.integration;

import com.jayway.jsonpath.JsonPath;
import com.psychotests.psycho_tests_server.dto.request.result_cpsk_dto.ResultOneCharacteristicRequestDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.CharacteristicAnswerDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.ImageAnswerDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowSubmitRequestDto;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Полный цикл: БД → сервис → response-DTO (JSON).
 * Пишем ответы реальным flow, читаем результат через
 * GET /api/CPSKTest/results/one-characteristic и сверяем JSON
 * с тем, что внесли.
 * <p>
 * Зафиксированные контракты {@link com.psychotests.psycho_tests_server.service.CPSKResultService}:
 * <ul>
 *   <li>ответ всегда содержит все 8 секторов (пустые — нулями);</li>
 *   <li>проценты — round(count * 100 / total), балансировка до суммы 100
 *       (разница уходит сектору с максимальным count);</li>
 *   <li>асимметрия: запрашиваемый объект скрыт из sectors
 *       (по пункту — скрыты items, по характеристике — characteristics);</li>
 *   <li>ассоциации группируются по тексту со счётом повторов;</li>
 *   <li>без idCharacteristic и idItem — легитимный режим «весь опрос»,
 *       а не ошибка.</li>
 * </ul>
 */
class SurveyResultsDbToDtoTest extends AbstractCPSKFlowTest {

    // ---------- счётчики и проценты ----------

    /** Одна сессия: сектор 3 → 1 сессия, 100%; остальные секторы — нули. */
    @Test
    void resultsByCharacteristicCountSessionsPerSector() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);
        submitValidAnswers(sessionId);

        String json = results(byCharacteristic0());

        assertThat(JsonPath.<Integer>read(json, "$.counts.length()")).isEqualTo(8);
        assertThat(JsonPath.<Integer>read(json, "$.counts[2].numberSector")).isEqualTo(3);
        assertThat(JsonPath.<Integer>read(json, "$.counts[2].count")).isEqualTo(1);
        assertThat(JsonPath.<Integer>read(json, "$.counts[2].percent")).isEqualTo(100);
        for (int i = 0; i < 8; i++) {
            if (i != 2) {
                assertThat(JsonPath.<Integer>read(json, "$.counts[" + i + "].count")).isZero();
                assertThat(JsonPath.<Integer>read(json, "$.counts[" + i + "].percent")).isZero();
            }
        }
    }

    /**
     * Две сессии считаются как две (distinct по сессиям), а не схлопываются.
     * Заодно проверяем счёт повторов ассоциаций: каждая из 5 фраз сектора 1
     * встретилась в двух сессиях → hits 2.
     */
    @Test
    void twoSessionsAreCountedTwiceAndAssociationRepeatsAggregate() throws Exception {
        for (int i = 0; i < 2; i++) {
            UUID sessionId = startSession();
            fillQuestionnaire(sessionId);
            submitValidAnswers(sessionId);
        }

        String json = results(byCharacteristic0());

        assertThat(JsonPath.<Integer>read(json, "$.counts[2].count")).isEqualTo(2);
        List<Map<String, Object>> assoc = JsonPath.read(json, "$.sectors[0].associations");
        assertThat(assoc).hasSize(5);
        assertThat(assoc)
                .extracting(a -> a.get("answer"))
                .containsExactlyInAnyOrder(
                        "слово-1-0", "слово-1-1", "слово-1-2", "слово-1-3", "слово-1-4");
        assertThat(assoc)
                .allSatisfy(a -> assertThat(a.get("hits")).isEqualTo(2));
    }

    /**
     * Ребалансировка процентов: 7 сессий, секторы 3/3/3/7/7/1/1 →
     * 43/29/29, сумма 101. Разница (-1) уходит сектору с максимальным
     * count (сектор 3) → 42. Итоговая сумма всегда 100.
     */
    @Test
    void percentsAreRoundedAndRebalancedToSum100() throws Exception {
        int[] sectors = {3, 3, 3, 7, 7, 1, 1};
        for (int s : sectors) {
            UUID sessionId = startSession();
            fillQuestionnaire(sessionId);
            submit(requestWithCharSector(sessionId, s));
        }

        String json = results(byCharacteristic0());

        assertThat(JsonPath.<Integer>read(json, "$.counts[2].percent")).isEqualTo(42);
        assertThat(JsonPath.<Integer>read(json, "$.counts[6].percent")).isEqualTo(29);
        assertThat(JsonPath.<Integer>read(json, "$.counts[0].percent")).isEqualTo(29);
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += JsonPath.<Integer>read(json, "$.counts[" + i + "].percent");
        }
        assertThat(sum).isEqualTo(100);
    }

    // ---------- содержание секторов ----------

    /**
     * Асимметрия контракта: при запросе по пункту его хиты скрыты
     * (itemAnswers = List.of()), но попадания характеристик — видны.
     * Если асимметрия не задумана — тест укажет, где менять.
     */
    @Test
    void resultsByItemShowCharacteristicsButHideItemHits() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);
        submitValidAnswers(sessionId);

        String json = results(byItem());

        assertThat(JsonPath.<Integer>read(json, "$.counts[4].count")).isEqualTo(1);
        assertThat(JsonPath.<List<Object>>read(json, "$.sectors[4].items")).isEmpty();
        assertThat(JsonPath.<List<Object>>read(json, "$.sectors[2].characteristics")).hasSize(1);
        assertThat(JsonPath.<List<Object>>read(json, "$.sectors[6].characteristics")).hasSize(1);
        assertThat(JsonPath.<List<Object>>read(json, "$.sectors[1].itemsSub")).hasSize(1);
    }

    /** У опроса без ответов — нули и пустые листы во всех секторах. */
    @Test
    void emptySurveyReturnsZerosAndEmptyLists() throws Exception {
        String json = results(byCharacteristic0());

        for (int i = 0; i < 8; i++) {
            assertThat(JsonPath.<Integer>read(json, "$.counts[" + i + "].count")).isZero();
            assertThat(JsonPath.<Integer>read(json, "$.counts[" + i + "].percent")).isZero();
        }
        assertThat(JsonPath.<List<Object>>read(json, "$.sectors[*].characteristics[*]")).isEmpty();
        assertThat(JsonPath.<List<Object>>read(json, "$.sectors[*].items[*]")).isEmpty();
        assertThat(JsonPath.<List<Object>>read(json, "$.sectors[*].itemsSub[*]")).isEmpty();
        assertThat(JsonPath.<List<Object>>read(json, "$.sectors[*].associations[*]")).isEmpty();
    }

    // ---------- режим «весь опрос» ----------

    /**
     * Режим «весь опрос» (после добавления волн оба id = null — легитимный
     * запрос, а не ошибка): счётчик по секторам сливается со всех объектов.
     * <p>
     * Тест идёт через MockMvc, а не напрямую в сервис: getResult проверяет
     * владение опросом, и без аутентифицированного психолога вызов падает
     * UnauthorizedException раньше бизнес-логики.
     * <p>
     * Ожидание для валидной сессии: попадания характеристик (секторы 3 и 7)
     * и изображений (сектор 5) дают по 1 в трёх секторах; суб-объекты и
     * ассоциации в счётчик «весь опрос» не входят (они без уникальных
     * респондентов в своём query). Сумма процентов — ровно 100.
     */
    @Test
    void resultsWithoutCharacteristicAndItemReturnWholeSurvey() throws Exception {
        UUID sessionId = startSession();
        fillQuestionnaire(sessionId);
        submitValidAnswers(sessionId);

        String json = resultsSurveyMode();

        assertThat(JsonPath.<Integer>read(json, "$.counts.length()")).isEqualTo(8);
        assertThat(JsonPath.<Integer>read(json, "$.counts[2].count")).isEqualTo(1); // сектор 3: характеристика 0
        assertThat(JsonPath.<Integer>read(json, "$.counts[4].count")).isEqualTo(1); // сектор 5: item
        assertThat(JsonPath.<Integer>read(json, "$.counts[6].count")).isEqualTo(1); // сектор 7: характеристика 1

        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum += JsonPath.<Integer>read(json, "$.counts[" + i + "].percent");
        }
        assertThat(sum).isEqualTo(100);
    }

    // ---------- хелперы ----------

    private ResultOneCharacteristicRequestDto byCharacteristic0() {
        return new ResultOneCharacteristicRequestDto(survey.getIdSurvey(), characteristicId(0), null);
    }

    private ResultOneCharacteristicRequestDto byItem() {
        return new ResultOneCharacteristicRequestDto(survey.getIdSurvey(), null, itemId());
    }

    private SurveyFlowSubmitRequestDto requestWithCharSector(UUID sessionId, int sector) {
        return new SurveyFlowSubmitRequestDto(sessionId,
                List.of(new CharacteristicAnswerDto(characteristicId(0), sector),
                        new CharacteristicAnswerDto(characteristicId(1), 7)),
                List.of(new ImageAnswerDto(itemId(), 5)),
                List.of(new ImageAnswerDto(subItemId(), 2)),
                fullAssociations());
    }

    private void submit(SurveyFlowSubmitRequestDto dto) throws Exception {
        mockMvc.perform(post("/api/CPSKTest/submit/" + survey.getIdSurvey())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated());
    }

    /**
     * GET с конвертацией DTO в параметры строки запроса.
     * idRun, idCharacteristic и idItem опциональны: их null передаётся
     * как «все волны» / «не указан режим объекта» — параметр просто не ставится
     * (param(name, null) в MockMvc не передаёт null явно).
     */
    private String results(ResultOneCharacteristicRequestDto dto) throws Exception {
        var request = get("/api/CPSKTest/results/one-characteristic")
                .with(psychologist())
                .param("idSurvey", dto.idSurvey().toString());
        if (dto.idRun() != null) {
            request = request.param("idRun", dto.idRun().toString());
        }
        if (dto.idCharacteristic() != null) {
            request = request.param("idCharacteristic", dto.idCharacteristic().toString());
        }
        if (dto.idItem() != null) {
            request = request.param("idItem", dto.idItem().toString());
        }
        return mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Режим «весь опрос»: только idSurvey, остальные id не передаются. */
    private String resultsSurveyMode() throws Exception {
        return mockMvc.perform(get("/api/CPSKTest/results/one-characteristic")
                        .with(psychologist())
                        .param("idSurvey", survey.getIdSurvey().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

}
