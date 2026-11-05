package com.psychotests.psycho_tests_server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.*;

//Класс респондентов
@Entity
@Table(name = "respondents")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Respondent {

    @Id
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    @Column(name = "id_respondent", updatable = false)
    private UUID idRespondent;

    @OneToMany(
            mappedBy = "respondent",
            fetch = FetchType.LAZY
    )
    private List<SurveySession> surveySessions = new ArrayList<>();

    @OneToMany(mappedBy = "respondent", fetch = FetchType.LAZY)
    private List<UniqueSurveyCode> surveyCodes = new ArrayList<>();

    @Column(name = "last_name_respondent", length = 50)
    private String lastNameRespondent;

    @Column(name = "name_respondent", length = 50)
    private String nameRespondent;

    @Column(name = "age_respondent")
    private Integer ageRespondent;

    @Column(name = "gender_respondent", length = 1)
    private String genderRespondent;

    public Respondent(UUID idRespondent){
        this.idRespondent = Objects.requireNonNull(idRespondent, "idRespondent must be set explicitly");
    }

    
}
