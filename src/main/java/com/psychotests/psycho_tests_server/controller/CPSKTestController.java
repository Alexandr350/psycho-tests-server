package com.psychotests.psycho_tests_server.controller;

import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowQuestionnaireRequestDto;
import com.psychotests.psycho_tests_server.dto.request.survey_answer_dto.SurveyFlowSubmitRequestDto;
import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.SurveyFlowResponseDto;
import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.SurveyFlowStartResponseDto;
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

    @GetMapping("/flow/{surveyId}")
    public ResponseEntity<SurveyFlowStartResponseDto> createSession(
            @PathVariable UUID surveyId,
            @RequestParam(required = false) UUID codeId
            ){
        return ResponseEntity.ok(cpskTestService.createSession(surveyId, codeId));
    }

    @PostMapping("/flow/{surveyId}")
    public ResponseEntity<SurveyFlowResponseDto> startFlow(
            @PathVariable UUID surveyId,
            @RequestBody @Valid SurveyFlowQuestionnaireRequestDto requestDto
            ){
        return ResponseEntity.ok(cpskTestService.startFlow(surveyId, requestDto));
    }

    @PostMapping("/submit/{surveyId}")
    public ResponseEntity<Void> submit(
            @PathVariable UUID surveyId,
            @RequestBody @Valid SurveyFlowSubmitRequestDto requestDto
            ){
        cpskTestService.submit(surveyId, requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
