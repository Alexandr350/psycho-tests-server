package com.psychotests.psycho_tests_server.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ResearchRequestItemsSubDtoPart5(
        @NotBlank
        String surveyId,
        List<String> itemSubPath) {
}
