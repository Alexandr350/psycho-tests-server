package com.psychotests.psycho_tests_server.model;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import java.util.Objects;
import java.util.UUID;

//Класс исследования (личный кабинет)
@Entity
@Table(name = "research")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Research{

    @Id
    @Column(name = "id_research")
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID IDResearch;

    @Column(name = "name_research", length = 100,  nullable = false)
    private String nameResearch;

    @Column(name = "description_research", length = 1000)
    private String descriptionResearch;

    @Column(name = "customer_research", length = 100)
    private String customerResearch;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_research" , length = 15, nullable = false)
    private ResearchStatus statusResearch = ResearchStatus.DRAFT;

    @Column(name = "created_at_research", updatable = false, nullable = false)
    @CreationTimestamp
    private LocalDateTime createdAtResearch;

    @Column(name = "start_time_research")
    private LocalDateTime startTimeResearch;

    @Column(name = "finish_time_research")
    private LocalDateTime finishTimeResearch;

    @OneToOne(mappedBy = "research", cascade = CascadeType.ALL, orphanRemoval = true)
    private Survey surveyResearch;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_research", length = 10, nullable = false)
    private TypeResearch typeResearch;

    @Column(name = "public_token", unique = true)
    private String publicToken;

    @OneToMany(mappedBy = "research", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UniqueSurveyCode> uniqueSurveyCodes = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "psychologist_account", nullable = false)
    private PsychologistAccount psychologistAccount;

    public Research(
            UUID IDResearch,
            String nameResearch,
            String descriptionResearch,
            String customerResearch,
            TypeResearch typeResearch){
        this.IDResearch = Objects.requireNonNull(IDResearch, "idResearch must be set explicitly");
        this.nameResearch = Objects.requireNonNull(nameResearch, "nameResearch must be set explicitly");
        this.typeResearch = Objects.requireNonNull(typeResearch, "typeResearch must be set explicitly");
        this.descriptionResearch = descriptionResearch;
        this.customerResearch = customerResearch;
    }


    public void changeStatus(ResearchStatus newStatus) {
        if (!statusResearch.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    "Недопустимый переход статуса: " + statusResearch + " → " + newStatus);
        }
        this.statusResearch = newStatus;

        if (newStatus == ResearchStatus.RUNNING) {
            if (startTimeResearch == null) {
                this.startTimeResearch = LocalDateTime.now();
            }
            // токен создаётся при первом запуске и живёт до STOPPED:
            // пауза/возобновление сохраняют ту же ссылку
            if (this.publicToken == null) {
                this.publicToken = UUID.randomUUID().toString().replace("-", "");
            }
        }

        if (newStatus == ResearchStatus.STOPPED) {
            // ссылка отзывается насовсем: старый URL больше не отвечает,
            // при перезапуске (если появится) сгенерируется новый токен
            this.publicToken = null;
            // момент завершения запуска фиксируется один раз и живёт в ARCHIVE:
            // STOPPED → RUNNING запрещён, время не перезаписывается
            if (this.finishTimeResearch == null) {
                this.finishTimeResearch = LocalDateTime.now();
            }
        }
    }

    /**
     * «Запустить снова» на остановленном исследовании: STOPPED → RUNNING
     * с НОВЫМ токеном — рестарт открывает новую волну сбора, ссылки на
     * старую уже отозваны (см. changeStatus/STOPPED). Из любого другого
     * статуса — ошибка: прямой PATCH из STOPPED запрещён картой переходов.
     */
    public void restart() {
        if (statusResearch != ResearchStatus.STOPPED) {
            throw new IllegalStateException(
                    "Перезапуск возможен только из STOPPED, текущий статус: " + statusResearch);
        }
        this.statusResearch = ResearchStatus.RUNNING;
        // время первого запуска сохраняется: research «перезапускается»,
        // а не запускается заново с нуля
        if (this.startTimeResearch == null) { // теоретически недостижимо после RUNNING, защита на всякий случай
            this.startTimeResearch = LocalDateTime.now();
        }
        // research снова жив: момент завершения сбрасывается, чтобы не
        // противоречить статусу RUNNING (finish в будущем проставит STOPPED)
        this.finishTimeResearch = null;
        this.publicToken = UUID.randomUUID().toString().replace("-", "");
    }

}
