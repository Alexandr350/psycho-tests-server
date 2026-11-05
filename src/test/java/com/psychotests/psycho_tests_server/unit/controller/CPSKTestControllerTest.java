package com.psychotests.psycho_tests_server.unit.controller;

import com.psychotests.psycho_tests_server.controller.CPSKTestController;
import com.psychotests.psycho_tests_server.dto.request.result_cpsk_dto.ResultOneCharacteristicRequestDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowQuestionnaireRequestDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowSubmitRequestDto;
import com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto.ResultOneCharacteristicResponseDto;
import com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto.SectorContentDto;
import com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto.SectorCountDto;
import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.SurveyFlowResponseDto;
import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.SurveyFlowStartResponseDto;
import com.psychotests.psycho_tests_server.service.CPSKResultService;
import com.psychotests.psycho_tests_server.service.CPSKTestService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * Юнит-тест CPSKTestController: контроллер тонкий, вся логика в сервисе.
 * Фиксируем делегирование аргументов (idSurvey, idRun, idCharacteristic/idItem),
 * статус-коды (200 на чтение, 201 на submit) и то, что ошибки сервиса
 * проходят насквозь к advice.
 */
@ExtendWith(MockitoExtension.class)
class CPSKTestControllerTest {
    @Mock
    private CPSKResultService resultService;
    @Mock
    private CPSKTestService service;

    @InjectMocks
    private CPSKTestController controller;

    private static final UUID SURVEY_ID = UUID.randomUUID();
    private static final UUID ID_CHARACTERISTIC = UUID.randomUUID();
    private static final UUID ID_ITEM = UUID.randomUUID();

    // ---------- GET /flow/{idSurvey} ----------

    /**
     * Проверяет: createSession делегирует (idSurvey, codeId) в сервис «как есть»
     * и отдаёт его результат телом ответа со статусом 200.
     */
    @Test
    void createSessionDelegatesIdsAndReturnsServiceResult() {
        var expected = mock(SurveyFlowStartResponseDto.class);
        var codeId = UUID.randomUUID();
        when(service.createSession(SURVEY_ID, codeId)).thenReturn(expected);

        var response = controller.createSession(SURVEY_ID, codeId);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(service).createSession(SURVEY_ID, codeId);
    }

    /**
     * Проверяет: codeId опционален — без кода в сервис уходит null
     * (через isNull(), чтобы матчер не путался с сырым null в stub/verify).
     */
    @Test
    void createSessionWithoutCodePassesNullCodeId() {
        when(service.createSession(eq(SURVEY_ID), isNull()))
                .thenReturn(mock(SurveyFlowStartResponseDto.class));

        var response = controller.createSession(SURVEY_ID, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(service).createSession(eq(SURVEY_ID), isNull());
    }

    // ---------- POST /flow/{idSurvey} ----------

    /**
     * Проверяет: startFlow пробрасывает DTO запроса в сервис без изменений
     * и возвращает результат сервиса телом со статусом 200.
     */
    @Test
    void startFlowDelegatesRequestDto() {
        var dto = mock(SurveyFlowQuestionnaireRequestDto.class);
        var expected = mock(SurveyFlowResponseDto.class);
        when(service.startFlow(SURVEY_ID, dto)).thenReturn(expected);

        var response = controller.startFlow(SURVEY_ID, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(service).startFlow(SURVEY_ID, dto);
    }

    // ---------- POST /submit/{idSurvey} ----------

    /**
     * Проверяет: submit делегирует DTO в сервис, возвращает статус 201
     * с пустым телом — создание ресурса, тела клиенту не нужно.
     */
    @Test
    void submitReturnsCreatedWithEmptyBody() {
        var dto = mock(SurveyFlowSubmitRequestDto.class);

        var response = controller.submit(SURVEY_ID, dto);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNull();
        verify(service).submit(SURVEY_ID, dto);
    }

    // ---------- ошибки ----------

    /**
     * Проверяет: контроллер не перехватывает ошибки сервиса — исключение
     * проходит насквозь, его маппит глобальный advice на статус/текст.
     */
    @Test
    void serviceExceptionsPropagate() {
        var dto = mock(SurveyFlowSubmitRequestDto.class);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Сессия не найдена"))
                .when(service).submit(SURVEY_ID, dto);

        assertThatThrownBy(() -> controller.submit(SURVEY_ID, dto))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ---------- GET /results/one-characteristic ----------

    /**
     * Проверяет делегирование в ветке характеристики: контроллер собирает
     * ResultOneCharacteristicRequestDto (idSurvey, idRun=null — все волны,
     * idCharacteristic, idItem=null) из параметров строки запроса, ответ
     * сервиса возвращается в теле без изменений, статус 200.
     * Record сравнивается по значению всех компонентов — матчер надёжен.
     */
    @Test
    void getResultDelegatesRequestAndReturnsServiceResult() {
        var expectedRequest = new ResultOneCharacteristicRequestDto(SURVEY_ID, ID_CHARACTERISTIC, null);

        List<SectorCountDto> counts = List.of(new SectorCountDto(1, 7, 70));
        List<SectorContentDto> sectors = List.of(
                new SectorContentDto(1, List.of(), List.of(), List.of(), List.of()));

        var expected = new ResultOneCharacteristicResponseDto(ID_CHARACTERISTIC, null, counts, sectors);
        when(resultService.getResult(any(ResultOneCharacteristicRequestDto.class))).thenReturn(expected);

        var response = controller.getResult(SURVEY_ID, null, ID_CHARACTERISTIC, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(resultService).getResult(expectedRequest);
    }

    /**
     * Проверяет ветку item: контроллер собирает DTO с idItem вместо
     * idCharacteristic (idCharacteristic = null). Контент результата здесь
     * не строим — состав покрыт юнит-тестами CPSKResultServiceTest,
     * важен только факт проброса.
     */
    @Test
    void getResultByItemDelegatesRequest() {
        var expectedRequest = new ResultOneCharacteristicRequestDto(SURVEY_ID, null, null, ID_ITEM);
        var expected = new ResultOneCharacteristicResponseDto(null, ID_ITEM, List.of(), List.of());

        when(resultService.getResult(any(ResultOneCharacteristicRequestDto.class))).thenReturn(expected);

        var response = controller.getResult(SURVEY_ID, null, null, ID_ITEM);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(resultService).getResult(expectedRequest);
    }

    /**
     * Проверяет проброс idRun — главную новую ветку с появлением волн:
     * контроллер не фильтрует и не проверяет волну сам, а передаёт её
     * в DTO «как есть» (вторая позиция конструктора).
     */
    @Test
    void getResultWithRunDelegatesRunId() {
        var idRun = UUID.randomUUID();
        var expectedRequest = new ResultOneCharacteristicRequestDto(SURVEY_ID, idRun, ID_CHARACTERISTIC, null);
        var expected = new ResultOneCharacteristicResponseDto(ID_CHARACTERISTIC, null, List.of(), List.of());

        when(resultService.getResult(any(ResultOneCharacteristicRequestDto.class))).thenReturn(expected);

        var response = controller.getResult(SURVEY_ID, idRun, ID_CHARACTERISTIC, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(expected);
        verify(resultService).getResult(expectedRequest);
    }

    /**
     * Проверяет: ошибка сервиса (невалидный запрос — нет ни одного id объекта)
     * не перехватывается контроллером и проходит насквозь к advice.
     * Сигнатура контроллера допускает оба id = null, значит валидация
     * обязана жить в сервисе — тест фиксирует этот контракт.
     */
    @Test
    void getResultServiceExceptionsPropagate() {
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Должен быть передан idCharacteristic или idItem"))
                .when(resultService).getResult(any(ResultOneCharacteristicRequestDto.class));

        assertThatThrownBy(() -> controller.getResult(SURVEY_ID, null, ID_CHARACTERISTIC, null))
                .isInstanceOf(ResponseStatusException.class);
    }
}
