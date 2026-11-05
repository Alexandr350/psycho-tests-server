package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

//Класс - ответы респондента - дополнительные объекты
@Entity
@Table(name = "answers_items_sub")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnswersItemsSubs {

    @Id
    @Column(name = "id_answers_items_sub")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idAnswersItemsSub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey_session", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private SurveySession surveySession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_items_sub", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ItemsSub itemsSub;

    @Column(name = "number_sector", nullable = false)
    private int numberSector;

    public AnswersItemsSubs(UUID idAnswersItemsSub,
                            SurveySession surveySession,
                            ItemsSub itemsSub,
                            int numberSector){
        this.idAnswersItemsSub = Objects.requireNonNull(idAnswersItemsSub, "idAnswersItemsSub must be set explicitly");
        this.surveySession = Objects.requireNonNull(surveySession, "surveySession must be set explicitly");
        this.itemsSub = Objects.requireNonNull(itemsSub, "itemsSub must be set explicitly");
        this.numberSector = numberSector;
    }
}
