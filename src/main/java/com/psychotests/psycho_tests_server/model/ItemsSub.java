package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

//Класс суб-объектов опроса
@Entity
@Table(name = "items_sub")
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
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Survey survey;

    @Column(name = "url_item_sub", nullable = false)
    private String urlItemSub;

    @OneToMany(
            mappedBy = "itemsSub",
            cascade = {CascadeType.MERGE, CascadeType.PERSIST},
            fetch = FetchType.LAZY
    )
    private List<AnswersItemsSubs> answersItemsSubList = new ArrayList<>();

    public ItemsSub(UUID idItemSub, Survey survey, String urlItemSub) {
        this.idItemSub = Objects.requireNonNull(idItemSub, "idItemSub must be set explicitly");
        this.survey = Objects.requireNonNull(survey, "survey must be set explicitly");
        this.urlItemSub = Objects.requireNonNull(urlItemSub, "urlItemSub must be set explicitly");
    }
}
