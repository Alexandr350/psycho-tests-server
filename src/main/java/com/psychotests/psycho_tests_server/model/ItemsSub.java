package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ItemsSub {

    @Id
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idItemSub;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_survey_item_sub", nullable = false)
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

    public ItemsSub(UUID idItemSub, String urlItemSub){
        this.idItemSub = idItemSub;
        this.urlItemSub = urlItemSub;
    }
}
