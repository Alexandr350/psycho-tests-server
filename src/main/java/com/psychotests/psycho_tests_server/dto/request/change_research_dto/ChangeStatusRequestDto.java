package com.psychotests.psycho_tests_server.dto.request.change_research_dto;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangeStatusRequestDto(
        @NotNull(message = "Нужен id исследования чей статус меняется")
        UUID idResearch,

        @NotNull(message = "Статус не передан")
        ResearchStatus status
) {
}
