package com.psychotests.psycho_tests_server.dto.response.research_dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record RunListItemDto(
        UUID idRun,
        LocalDateTime startTimeRun,
        LocalDateTime finishTimeRun,
        boolean open
) {
}
