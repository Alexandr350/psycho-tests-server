package com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto;

import java.util.UUID;

public record CharacteristicHitDto(
        UUID id,
        String text,
        long hits
) {
}
