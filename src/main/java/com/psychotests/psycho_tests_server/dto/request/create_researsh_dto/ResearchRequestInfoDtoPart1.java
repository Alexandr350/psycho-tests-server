package com.psychotests.psycho_tests_server.dto.request.create_researsh_dto;

import com.psychotests.psycho_tests_server.enums.TypeResearch;
import jakarta.validation.constraints.NotBlank;

public record ResearchRequestInfoDtoPart1(

        @NotBlank
        String nameResearch,
        String descriptionResearch,
        String customerResearch,
        TypeResearch typeResearch
) {
}
