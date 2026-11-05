package com.psychotests.psycho_tests_server.dto.request.result_cpsk_dto;

import java.util.UUID;

/**
 * Запрос отчёта по сектору (цвету): что попало в сектор sector,
 * среди волн idRun (null — все волны).
 */
public record ResultSectorRequestDto(
        UUID idSurvey,
        UUID idRun,     // null — все волны
        int sector      // 1..8, обязателен
) {
}
