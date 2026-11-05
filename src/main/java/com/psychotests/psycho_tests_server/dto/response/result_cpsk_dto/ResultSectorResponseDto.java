package com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto;

/**
 * Отчёт по сектору: сектор, его цвет, число респондентов,
 * поместивших в него хотя бы один объект, и содержание.
 * В отличие от отчёта по объекту, скрытия запрошенного объекта нет —
 * объект не запрашивался, показывается всё.
 */
public record ResultSectorResponseDto(
        int numberSector,
        String color,
        long totalRespondents,
        SectorContentDto content
) {
}
