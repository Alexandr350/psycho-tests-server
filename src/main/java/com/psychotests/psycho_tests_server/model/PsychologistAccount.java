package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

//Класс - личный кабинет психолога
@Entity
@Table(name = "psychologist_account", uniqueConstraints = @UniqueConstraint(
        name = "uk_psychologist_email",
        columnNames = "email_psychologist"
))
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PsychologistAccount {

    @Id
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    @Column(name = "id_psychologist_account", nullable = false, updatable = false)
    private UUID idPsychologistAccount;

    @Column(name = "name_psychologist", length = 20, nullable = false)
    private String namePsychologist;

    @Column(name = "lastname_psychologist", length = 50, nullable = false)
    private String lastnamePsychologist;

    @Column(name = "password_psychologist", nullable = false, length = 60)
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
        this.idPsychologistAccount = Objects.requireNonNull(idPsychologistAccount, "id не может быть null");
        this.namePsychologist = Objects.requireNonNull(namePsychologist, "имя не может быть null");
        this.lastnamePsychologist = Objects.requireNonNull(lastnamePsychologist, "фамилия не может быть null");
        this.passwordPsychologist = Objects.requireNonNull(passwordPsychologist, "пароль не может быть null");
        this.emailPsychologist = Objects.requireNonNull(emailPsychologist, "email не может быть null");
    }

}
