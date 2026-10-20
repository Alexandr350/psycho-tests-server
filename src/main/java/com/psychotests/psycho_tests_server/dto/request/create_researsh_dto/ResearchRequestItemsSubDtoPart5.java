package com.psychotests.psycho_tests_server.dto.request.create_researsh_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ResearchRequestItemsSubDtoPart5(
        @NotNull
        UUID idSurvey,
        List<String> itemSubPath) {
}
