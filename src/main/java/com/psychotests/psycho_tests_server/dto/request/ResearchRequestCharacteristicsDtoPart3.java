package com.psychotests.psycho_tests_server.dto.request;

import jakarta.validation.constraints.Pattern;

public record ResearchRequestCharacteristicsDtoPart3(
        @Pattern(regexp = "[\\p{L}\\p{P}\\s]+")
        String characteristics){}
