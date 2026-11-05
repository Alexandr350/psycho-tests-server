package com.psychotests.psycho_tests_server.dto.response.authorization_dto;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;

public record LoginPsychologicalAccountResponseDto(
        String name,
        String lastname,
        String email
) {
    public static LoginPsychologicalAccountResponseDto from(PsychologistAccount psychologistAccount){
        return new LoginPsychologicalAccountResponseDto(
                psychologistAccount.getNamePsychologist(),
                psychologistAccount.getLastnamePsychologist(),
                psychologistAccount.getEmailPsychologist()
        );
    }
}
