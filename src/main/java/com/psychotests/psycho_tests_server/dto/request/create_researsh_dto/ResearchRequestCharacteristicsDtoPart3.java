package com.psychotests.psycho_tests_server.dto.request.create_researsh_dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public record ResearchRequestCharacteristicsDtoPart3(
        @NotNull
        UUID idSurvey,

        @Pattern(regexp = "[\\p{L}\\p{P}\\s]+")
        String characteristics){}
