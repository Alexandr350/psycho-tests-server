package com.psychotests.psycho_tests_server.dto.response.research_dto;

import java.util.UUID;

/** Прогресс исследования: сколько респондентов завершили опрос. */
public record ResearchProgressResponseDto(
        UUID idResearch,
        long completedSessions
) {}