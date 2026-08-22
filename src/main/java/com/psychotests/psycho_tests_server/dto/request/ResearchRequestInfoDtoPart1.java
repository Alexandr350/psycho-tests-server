package com.psychotests.psycho_tests_server.dto.request;

import com.psychotests.psycho_tests_server.enums.TypeResearch;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;

public record ResearchRequestDtoPart1(
        @NotBlank
        @Max(100)
        String nameResearch,
        
        String descriptionResearch,
        String customerResearch,
        TypeResearch typeResearch
) {
}
