package com.psychotests.psycho_tests_server.dto.response.research_dto;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.Characteristics;
import com.psychotests.psycho_tests_server.model.Items;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Survey;

import java.util.List;
import java.util.UUID;

public record ResearchResponseInfoDto(
        UUID idResearch,
        String nameResearch,
        String descriptionResearch,
        String customerResearch,
        TypeResearch typeResearch,
        ResearchStatus researchStatus,
        String publicToken,
        UUID idSurvey,
        String titleSurvey,
        String descriptionSurvey,
        String rulesSurvey,
        List<CharacteristicRefDto> characteristics,
        List<ItemRefDto> items
) {
    /** Краткая ссылка на характеристику: id для отчётов + текст для списков. */
    public record CharacteristicRefDto(UUID idCharacteristic, String text) {}

    /** Краткая ссылка на изображение: id для отчётов + путь для превью. */
    public record ItemRefDto(UUID idItem, String url) {}

    public static ResearchResponseInfoDto from(Research research) {
        return from(research, research.getSurveyResearch());
    }

    public static ResearchResponseInfoDto from(Research research, Survey survey) {
        return new ResearchResponseInfoDto(
                research.getIDResearch(),
                research.getNameResearch(),
                research.getDescriptionResearch(),
                research.getCustomerResearch(),
                research.getTypeResearch(),
                research.getStatusResearch(),
                research.getPublicToken(),
                survey != null ? survey.getIdSurvey() : null,
                survey != null ? survey.getTitleSurvey() : null,
                survey != null ? survey.getDescriptionSurvey() : null,
                survey != null ? survey.getRulesSurvey() : null,
                survey != null
                        ? survey.getCharacteristicsSurvey().stream()
                        .map(c -> new CharacteristicRefDto(
                                c.getIdCharacteristic(), c.getTextCharacteristic()))
                        .toList()
                        : List.of(),
                survey != null
                        ? survey.getItemsSurvey().stream()
                        .map(i -> new ItemRefDto(i.getIdItem(), i.getUrlItem()))
                        .toList()
                        : List.of()
        );
    }
}


