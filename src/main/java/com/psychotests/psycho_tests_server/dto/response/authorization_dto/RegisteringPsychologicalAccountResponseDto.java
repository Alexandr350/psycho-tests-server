package com.psychotests.psycho_tests_server.dto.response.authorization_dto;

import com.psychotests.psycho_tests_server.model.PsychologistAccount;

public record RegisteringPsychologicalAccountResponseDto(
        String namePsychologist,
        String lastnamePsychologist,
        String emailPsychologist
) {
    public static RegisteringPsychologicalAccountResponseDto from(PsychologistAccount psychologistAccount){
        return new RegisteringPsychologicalAccountResponseDto(
                psychologistAccount.getNamePsychologist(),
                psychologistAccount.getLastnamePsychologist(),
                psychologistAccount.getEmailPsychologist()
        );
    }
}
