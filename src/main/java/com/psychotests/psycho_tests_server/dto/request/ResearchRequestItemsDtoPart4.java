package com.psychotests.psycho_tests_server.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record ResearchRequestItemsDtoPart4 (
        @NotBlank
        String surveyId,
        List<String> itemPaths){

}
