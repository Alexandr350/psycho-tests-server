package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

//Класс объектов
@Entity
@Table(name = "items")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Items {

    @Id
    @Column(name = "id_item")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idItem;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_item", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Survey survey;

    @OneToMany(
            mappedBy = "item",
            cascade = {CascadeType.MERGE, CascadeType.PERSIST},
            fetch = FetchType.LAZY
    )
    private List<AnswersItems> answersItems = new ArrayList<>();

    @Column(name = "url_item", nullable = false)
    private String urlItem;

    public Items(UUID idItem, String urlItem){
        this.idItem = Objects.requireNonNull(idItem, "idItem must be set explicitly");
        this.urlItem = Objects.requireNonNull(urlItem, "urlItem must be set explicitly");
    }

    public Items(UUID idItem, Survey survey, String urlItem) {
        this.idItem = Objects.requireNonNull(idItem, "idItem must be set explicitly");
        this.survey = Objects.requireNonNull(survey, "survey must be set explicitly");
        this.urlItem = Objects.requireNonNull(urlItem, "urlItem must be set explicitly");
    }
}
