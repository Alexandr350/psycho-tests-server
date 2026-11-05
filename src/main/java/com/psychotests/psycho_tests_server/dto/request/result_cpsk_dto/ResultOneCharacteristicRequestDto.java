package com.psychotests.psycho_tests_server.dto.request.result_cpsk_dto;

import java.util.UUID;

public record ResultOneCharacteristicRequestDto(
        UUID idSurvey,
        UUID idRun,             // null — все волны
        UUID idCharacteristic,  // null вместе с idItem — режим «весь опрос»
        UUID idItem
) {
    /** Старый контракт (до волн): отчёт по объекту, все волны. */
    public ResultOneCharacteristicRequestDto(UUID idSurvey, UUID idCharacteristic, UUID idItem) {
        this(idSurvey, null, idCharacteristic, idItem);
    }
}

