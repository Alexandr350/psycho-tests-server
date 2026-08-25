package com.psychotests.psycho_tests_server.dto.response;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.Research;

public record ResearchResponseInfoDtoPart1(
        String nameResearch,
        String descriptionResearch,
        String customerResearch,
        TypeResearch typeResearch,
        ResearchStatus researchStatus
){
    public static ResearchResponseInfoDtoPart1 from(Research research){
        return new ResearchResponseInfoDtoPart1(
                research.getNameResearch(),
                research.getDescriptionResearch(),
                research.getCustomerResearch(),
                research.getTypeResearch(),
                research.getStatusResearch()
        );
    }

}
