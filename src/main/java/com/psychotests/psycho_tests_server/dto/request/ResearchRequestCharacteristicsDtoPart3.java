package com.psychotests.psycho_tests_server.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ResearchRequestCharacteristicsDtoPart3(
        @NotBlank
        String surveyId,

        @Pattern(regexp = "[\\p{L}\\p{P}\\s]+")
        String characteristics){}
