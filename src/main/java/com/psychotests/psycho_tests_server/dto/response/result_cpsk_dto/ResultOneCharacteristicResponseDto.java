package com.psychotests.psycho_tests_server.dto.response.result_cpsk_dto;

import java.util.List;
import java.util.UUID;

public record ResultOneCharacteristicResponseDto(
        UUID idCharacteristic,
        UUID idItem,
        List<SectorCountDto> counts,        // подсчёт по секторам главного объекта
        List<SectorContentDto> sectors      // объекты, разложенные по секторам
) {
}
