package com.psychotests.psycho_tests_server.dto.response.research_dto;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.model.Research;

import java.util.UUID;

public record ResearchStatusChangeResponseDto(
        UUID idResearch,
        ResearchStatus status
) {
    public static ResearchStatusChangeResponseDto from(Research research) {
        return new ResearchStatusChangeResponseDto(
                research.getIDResearch(),
                research.getStatusResearch()
        );
    }
}
