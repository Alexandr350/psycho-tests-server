package com.psychotests.psycho_tests_server.dto.response.research_dto;

import java.util.UUID;

public record ResearchCreatedResponseDto(UUID idResearch) {
    public static ResearchCreatedResponseDto from(UUID researchId){
        return new ResearchCreatedResponseDto(researchId);
    }
}
