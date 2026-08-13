package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

//Класс - ответы респондента - характеристики
@Entity
@Table(name = "answers_characteristics")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnswersCharacteristics {

    @Id
    @Column(name = "id_answers_characteristics")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idAnswersCharacteristic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey_session", nullable = false)
    private SurveySession surveySession;

    @ManyToOne(fetch =  FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_characteristic", nullable = false)
    private Characteristics characteristic;

    @Column(name = "number_sector")
    private int numberSector;

    public AnswersCharacteristics(UUID idAnswersCharacteristic, int numberSector){
        this.idAnswersCharacteristic = idAnswersCharacteristic;
        this.numberSector = numberSector;
    }

}
