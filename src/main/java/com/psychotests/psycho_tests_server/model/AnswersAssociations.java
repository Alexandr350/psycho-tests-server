package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

//Класс - ответы респондента - ассоциации
@Entity
@Table(name = "answers_associations")
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnswersAssociations {

    @Id
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    @Column(name = "id_answers_associations")
    private UUID idAnswersAssociations;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey_session", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SurveySession surveySession;

    @Column(name = "answer", nullable = false, length = 500)
    private String answer;

    @Column(name = "number_sector", nullable = false)
    private int numberSector;

    @Column(name = "association_order", nullable = false)
    private int associationOrder;

    public AnswersAssociations(UUID idAnswersAssociations,
                               SurveySession surveySession,
                               String answer,
                               int numberSector,
                               int associationOrder){
        this.idAnswersAssociations = Objects.requireNonNull(idAnswersAssociations, "idAnswersAssociations must be set explicitly");
        this.surveySession = Objects.requireNonNull(surveySession, "surveySession must be set explicitly");
        this.answer = Objects.requireNonNull(answer, "answer must be set explicitly");
        this.numberSector = numberSector;
        this.associationOrder = associationOrder;
    }
}
