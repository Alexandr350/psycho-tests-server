package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//Класс - личный кабинет психолога
@Entity
@Table(name = "psychologist_account")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PsychologistAccount {

    @Id
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    @Column(name = "id_psychologist_account")
    private UUID idPsychologistAccount;

    @Column(name = "name_psychologist", length = 20, nullable = false)
    private String namePsychologist;

    @Column(name = "lastname_psychologist", length = 50, nullable = false)
    private String lastnamePsychologist;

    @Column(name = "password_psychologist", nullable = false)
    private String passwordPsychologist;

    @Column(name = "email_psychologist", length = 100, nullable = false)
    private String emailPsychologist;

    @OneToMany(mappedBy = "psychologistAccount",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Research> researchList = new ArrayList<>();

    public PsychologistAccount(
            UUID idPsychologistAccount,
            String namePsychologist,
            String lastnamePsychologist,
            String passwordPsychologist,
            String emailPsychologist
    ){
        this.idPsychologistAccount = idPsychologistAccount;
        this.namePsychologist = namePsychologist;
        this.lastnamePsychologist = lastnamePsychologist;
        this.passwordPsychologist = passwordPsychologist;
        this.emailPsychologist = emailPsychologist;
    }
}
