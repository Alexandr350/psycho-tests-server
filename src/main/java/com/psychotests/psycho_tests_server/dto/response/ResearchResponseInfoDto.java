package com.psychotests.psycho_tests_server.dto.response;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.Research;

public record ResearchResponseInfoDto(
        String idResearch,
        String nameResearch,
        String descriptionResearch,
        String customerResearch,
        TypeResearch typeResearch,
        ResearchStatus researchStatus
){
    public static ResearchResponseInfoDto from(Research research){
        return new ResearchResponseInfoDto(
                String.valueOf(research.getIDResearch()),
                research.getNameResearch(),
                research.getDescriptionResearch(),
                research.getCustomerResearch(),
                research.getTypeResearch(),
                research.getStatusResearch()
        );
    }

}
