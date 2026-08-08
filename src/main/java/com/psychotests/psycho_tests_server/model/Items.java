package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

//Класс объектов
@Entity
@Table(name =  "items")
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
    private Survey survey;

    @OneToMany(
            mappedBy = "item",
            cascade = {CascadeType.MERGE, CascadeType.PERSIST},
            orphanRemoval = false,
            fetch = FetchType.LAZY
    )
    private List<AnswersItems> items = new ArrayList<>();

    @Column(name = "url_item")
    private String urlItem;

    public Items(UUID idItem, String urlItem){
        this.idItem = idItem;
        this.urlItem = urlItem;
    }

}
