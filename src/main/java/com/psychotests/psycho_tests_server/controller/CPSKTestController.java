package com.psychotests.psycho_tests_server.controller;

import com.psychotests.psycho_tests_server.dto.request.result_cpsk_dto.ResultOneCharacteristicRequestDto;
import com.psychotests.psycho_tests_server.dto.request.result_cpsk_dto.ResultSectorRequestDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowQuestionnaireRequestDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowSubmitRequestDto;
import com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto.ResultOneCharacteristicResponseDto;
import com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto.ResultSectorResponseDto;
import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.SurveyFlowResponseDto;
import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.SurveyFlowStartResponseDto;
import com.psychotests.psycho_tests_server.service.CPSKResultService;
import com.psychotests.psycho_tests_server.service.CPSKTestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/CPSKTest")
@RequiredArgsConstructor
public class CPSKTestController {
    private final CPSKTestService cpskTestService;
    private final CPSKResultService cpskResultService;

    @GetMapping("/flow/{idSurvey}")
    public ResponseEntity<SurveyFlowStartResponseDto> createSession(
            @PathVariable UUID idSurvey,
            @RequestParam(required = false) UUID codeId
            ){
        return ResponseEntity.ok(cpskTestService.createSession(idSurvey, codeId));
    }

    @PostMapping("/flow/{idSurvey}")
    public ResponseEntity<SurveyFlowResponseDto> startFlow(
            @PathVariable UUID idSurvey,
            @RequestBody @Valid SurveyFlowQuestionnaireRequestDto requestDto
            ){
        return ResponseEntity.ok(cpskTestService.startFlow(idSurvey, requestDto));
    }

    @PostMapping("/submit/{idSurvey}")
    public ResponseEntity<Void> submit(
            @PathVariable UUID idSurvey,
            @RequestBody @Valid SurveyFlowSubmitRequestDto requestDto
            ){
        cpskTestService.submit(idSurvey, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/results/one-characteristic")
    public ResponseEntity<ResultOneCharacteristicResponseDto> getResult(
            @RequestParam UUID idSurvey,
            @RequestParam(required = false) UUID idRun,
            @RequestParam(required = false) UUID idCharacteristic,
            @RequestParam(required = false) UUID idItem) {
        return ResponseEntity.ok(cpskResultService.getResult(
                new ResultOneCharacteristicRequestDto(idSurvey, idRun, idCharacteristic, idItem)));
    }


    /**
     * Отчёт по сектору (цвету): все объекты, попавшие в сектор,
     * по всем или выбранной волне.
     */
    @GetMapping("/results/by-sector")
    public ResponseEntity<ResultSectorResponseDto> getSectorResult(
            @RequestParam UUID idSurvey,
            @RequestParam(required = false) UUID idRun,
            @RequestParam int sector) {
        return ResponseEntity.ok(cpskResultService.getSectorResult(
                new ResultSectorRequestDto(idSurvey, idRun, sector)));
    }


}

/**
 * Обратите внимание, что GET /flow создаёт сессию при каждом вызове. После правки фронт будет звать его один раз за прохождение —
 * нормально. Но любой обновивший страницу до заполнения анкеты оставит в базе осиротевшую сессию без анкеты. Для MVP это приемлемо;
 * если захотите чистить — вопрос отдельной политики (TTL-джоба или «сессия пишется только при POST»). Сейчас не трогаем, но знайте,
 * что это уже не баг, а выбор дизайна.
 * */
