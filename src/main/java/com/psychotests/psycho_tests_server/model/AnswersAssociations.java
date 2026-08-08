package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

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
    private SurveySession surveySession;

    @Column(name = "answer")
    private String answer;

    @Column(name = "number_sector")
    private int numberSector;


}
