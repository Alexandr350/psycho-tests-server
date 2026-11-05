package com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto;


import java.util.List;

public record SectorContentDto(
        int numberSector,
        List<CharacteristicHitDto> characteristics, // заполняется в ветке item
        List<ItemHitDto> items,                     // заполняется в ветке characteristic
        List<ItemSubHitDto> itemsSub,
        List<AssociationHitDto> associations
) {
}
