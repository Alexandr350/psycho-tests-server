package com.psychotests.psycho_tests_server.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Запуск исследования (волна сбора данных).
 * Один research может иметь несколько запусков; ответы респондентов
 * привязываются к запуску через его токен — запуски изолированы друг от друга.
 */
@Entity
@Getter
@Table(name = "research_run")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Run {

    @Id
    @Column(name = "id_run", nullable = false)
    @Setter(AccessLevel.NONE)
    @EqualsAndHashCode.Include
    private UUID idRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_research", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Research research;

    /** Токен запуска — то, по чему делятся респонденты и их ответы.
     * Неизменяем после создания: смена токена рвёт связь с ответами волны. */
    @Column(name = "public_token", nullable = false, unique = true)
    @Setter(AccessLevel.NONE)
    private String publicToken;

    @Column(name = "start_time_run", nullable = false)
    @Setter(AccessLevel.NONE)
    private LocalDateTime startTimeRun;

    @Column(name = "finish_time_run")
    @Setter(AccessLevel.NONE)
    private LocalDateTime finishTimeRun;

    /** Только для Hibernate при загрузке из БД. */
    protected Run() {
    }

    /**
     * idRun передаётся вызывающим кодом — единое правило бизнес-логики
     * проекта: UUID генерируется не внутри сущности, а в точке создания.
     */
    public Run(UUID idRun, Research research, String publicToken) {
        this.idRun = Objects.requireNonNull(idRun, "idRun must be set explicitly");
        this.research = Objects.requireNonNull(research, "research must be set explicitly");
        this.publicToken = Objects.requireNonNull(publicToken, "publicToken must be set explicitly");
        this.startTimeRun = LocalDateTime.now();
    }

    /** Закрывает запуск и фиксирует момент завершения.
     * Повторное завершение — ошибка: перезапуск невозможен. */
    public void finish() {
        if (this.finishTimeRun != null) {
            throw new IllegalStateException("Запуск уже завершён: " + finishTimeRun);
        }
        this.finishTimeRun = LocalDateTime.now();
    }



    public boolean isOpen() {
        return finishTimeRun == null;
    }
}
