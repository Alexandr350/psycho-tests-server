package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "answers_items")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class AnswersItems {

    @Id
    @Column(name = "id_answers_items")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idAnswersItems;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey_session", nullable = false)
    private SurveySession surveySession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_item", nullable = false)
    private Items item;

    @Column(name = "number_sector", nullable = false)
    private int numberSector;

    public AnswersItems(UUID idAnswersItems,
                        SurveySession surveySession,
                        Items item,
                        int numberSector){
        this.idAnswersItems = Objects.requireNonNull(idAnswersItems, "idAnswersItems must be set explicitly");
        this.surveySession = Objects.requireNonNull(surveySession, "surveySession must be set explicitly");
        this.item = Objects.requireNonNull(item, "item must be set explicitly");
        this.numberSector = numberSector;
    }


}
