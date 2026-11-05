package com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto;

import java.util.UUID;

public record ItemSubHitDto(
        UUID id,
        String url,
        long hits
) {
}
