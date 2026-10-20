package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

//Класс - ответы респондента - объекты
@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemsSub {

    @Id
    @Column(name = "id_item_sub")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idItemSub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey", nullable = false)
    private Survey survey;

    @Column(name = "url_item_sub")
    private String urlItemSub;

    @OneToMany(
            mappedBy = "itemsSub",
            cascade = {CascadeType.MERGE, CascadeType.PERSIST},
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    private List<AnswersItemsSub> answersItemsSubList = new ArrayList<>();

    public ItemsSub(UUID idItemSub, Survey survey, String urlItemSub) {
        this.idItemSub = Objects.requireNonNull(idItemSub, "idItemSub must be set explicitly");
        this.survey = Objects.requireNonNull(survey, "survey must be set explicitly");
        this.urlItemSub = Objects.requireNonNull(urlItemSub, "urlItemSub must be set explicitly");
    }

}
