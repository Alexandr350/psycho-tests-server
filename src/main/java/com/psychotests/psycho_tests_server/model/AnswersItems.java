package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

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

    @Column(name = "number_sector")
    private int numberSector;

    public AnswersItems(UUID idAnswersItems, int numberSector){
        this.idAnswersItems = idAnswersItems;
        this.numberSector = numberSector;
    }


}
