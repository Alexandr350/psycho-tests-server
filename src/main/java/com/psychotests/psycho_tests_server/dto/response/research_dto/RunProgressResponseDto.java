package com.psychotests.psycho_tests_server.dto.response.research_dto;

import java.util.UUID;

/** Прогресс запуска: сколько респондентов завершили опрос и сколько кодов выдано. */
public record RunProgressResponseDto(
        UUID idRun,
        long completedSessions,
        Long invitedCount   // null, если в запуске нет персональных кодов (анонимные прохождения)
) {}
