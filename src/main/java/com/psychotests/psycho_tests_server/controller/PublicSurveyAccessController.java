package com.psychotests.psycho_tests_server.controller;

import com.psychotests.psycho_tests_server.dto.response.survey_answer_dto.PublicSurveyAccessResponseDto;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.repository.ResearchRepository;
import com.psychotests.psycho_tests_server.service.ResearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Публичные (без авторизации) эндпоинты для респондентов по токену. */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicSurveyAccessController {

    private final ResearchService researchService;

    /**
     * Резолвит токен в idSurvey. Отдаёт данные только для RUNNING:
     * PAUSE/STOPPED/ARCHIVE и несуществующий токен отвечают одинаково — 404,
     * чтобы не раскрывать существование исследования.
     */
    @GetMapping("/research/{token}")
    public ResponseEntity<PublicSurveyAccessResponseDto> resolve(@PathVariable String token) {
        return ResponseEntity.ok(researchService.resolvePublicToken(token));
    }

}
