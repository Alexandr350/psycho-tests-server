package com.psychotests.psycho_tests_server.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

//Класс респондентов
@Entity
@Table(name = "respondents")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Respondent {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "id_respondent")
    private UUID IdRespondent;

    @OneToMany(
            mappedBy = "respondent",
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    private List<SurveySession> surveySession = new ArrayList<>();

    @OneToMany(mappedBy = "respondent", fetch = FetchType.LAZY)
    private List<UniqueSurveyCode> surveyCodes = new ArrayList<>();

    @Column(name = "last_name_respondent", length = 50)
    private String lastNameRespondent;

    @Column(name = "name_respondent", length = 50)
    private String nameRespondent;

    @Column(name = "age_respondent", nullable = false)
    private int ageRespondent;

    @Column(name = "gender_respondent", nullable = false)
    private char genderRespondent;

    public Respondent(UUID IdRespondent, int ageRespondent, char genderRespondent){
        this.IdRespondent = IdRespondent;
        this.ageRespondent = ageRespondent;
        this.genderRespondent = genderRespondent;
    }

    
}
