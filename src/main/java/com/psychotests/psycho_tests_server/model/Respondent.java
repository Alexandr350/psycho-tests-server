package com.psychotests.psycho_tests_server.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "survey_session", unique = true, nullable = false)
    private SurveySession surveySession;

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
