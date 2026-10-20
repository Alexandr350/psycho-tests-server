package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

//Класс - ответы респондента - дополнительные объекты
@Entity
@Table(name = "answers_items_sub")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnswersItemsSub {

    @Id
    @Column(name = "id_answers_items_sub")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idAnswersItemsSub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey_session", nullable = false)
    private SurveySession surveySession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_items_sub", nullable = false)
    private ItemsSub itemsSub;

    @Column(name = "number_sector", nullable = false)
    private int numberSector;

    public AnswersItemsSub(UUID idAnswersItemsSub,
                           SurveySession surveySession,
                           ItemsSub itemsSub,
                           int numberSector){
        this.idAnswersItemsSub = idAnswersItemsSub;
        this.surveySession = surveySession;
        this.itemsSub = itemsSub;
        this.numberSector = numberSector;
    }

}
