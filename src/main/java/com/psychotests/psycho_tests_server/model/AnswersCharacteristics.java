package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
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

    @Column(name = "number_sector", nullable = false)
    private int numberSector;

    public AnswersCharacteristics(UUID idAnswersCharacteristic,
                                  SurveySession surveySession,
                                  Characteristics characteristic,
                                  int numberSector){
        this.idAnswersCharacteristic = Objects.requireNonNull(idAnswersCharacteristic, "idAnswersCharacteristic must be set explicitly");
        this.surveySession = Objects.requireNonNull(surveySession, "surveySession must be set explicitly");
        this.characteristic = Objects.requireNonNull(characteristic, "characteristic must be set explicitly");
        this.numberSector = numberSector;
    }

}
