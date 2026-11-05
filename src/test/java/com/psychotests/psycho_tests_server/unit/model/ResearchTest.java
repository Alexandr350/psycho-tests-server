package com.psychotests.psycho_tests_server.unit.model;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.UUID;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import com.psychotests.psycho_tests_server.enums.TypeResearch;
import com.psychotests.psycho_tests_server.model.Research;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Юнит-тесты Research.changeStatus — автомат переходов статусов и его
 * побочные эффекты (таймстемпы старта/финиша).
 *
 * <p>Что проверяем: единственный источник правил — матрица EXPECTED_TRANSITIONS
 * ниже. Тест fullMatrix прогоняет все пары статусов и сверяет фактическое
 * поведение сущности с матрицей: разрешённый переход меняет статус,
 * запрещённый бросает IllegalStateException и НЕ меняет статус. Такой способ
 * гарантирует, что ни одна пара не забыта: новый статус enum'а автоматически
 * попадает в прогон и потребует явного решения в матрице.</p>
 *
 * <p>Побочные эффекты: переход в RUNNING ставит startTimeResearch (только
 * если он пуст), переход в STOPPED ставит finishTimeResearch. Прочие
 * переходы таймстемпы не трогают.</p>
 *
 * <p>Контракт canBeDeleted(): удаление разрешено в статусах без активного
 * прохождения (DRAFT, NEW, STOPPED, ARCHIVE) и запрещено в RUNNING и PAUSE —
 * пока опрос идёт или на паузе, сессии респондентов активны. Перебор всех
 * статусов enum'а не даст новому статусу войти в «разрешённые» без правки
 * теста.</p>
 *
 * <p>Граница с другими тестами: сам предикат canTransitionTo внутри enum —
 * тоже территория этого класса: матрица и есть его внешняя фиксация.
 * Отказы deleteResearch по статусу — в ResearchServiceTest; интеграционная
 * проверка «PATCH с запрещённым переходом → 409, статус не изменился» —
 * в CreateResearchFlowTest.invalidTransition_isConflict, и её выводы здесь
 * продублированы на уровне сущности.</p>
 */

class ResearchTest {


    /**
     * Эталонная таблица переходов — перенесена из ResearchStatus.ALLOWED
     * без изменений (2026-10-23, сверено с кодом enum). Правила:
     * DRAFT → NEW; NEW → RUNNING; RUNNING → PAUSE | STOPPED;
     * PAUSE → RUNNING | STOPPED; STOPPED → ARCHIVE; ARCHIVE — терминальный.
     * Диагонали пусты: переход «в себя» запрещён. Обратных переходов нет:
     * NEW → DRAFT, PAUSE → NEW, STOPPED → RUNNING запрещены.
     * При изменении ALLOWED в enum этот Map правится в ту же ревизию —
     * тест fullMatrix не даст разойтись одному без другого.
     */
    private static final Map<ResearchStatus, EnumSet<ResearchStatus>> EXPECTED_TRANSITIONS = Map.of(
            ResearchStatus.DRAFT, EnumSet.of(ResearchStatus.NEW),
            ResearchStatus.NEW, EnumSet.of(ResearchStatus.RUNNING),
            ResearchStatus.RUNNING, EnumSet.of(ResearchStatus.PAUSE, ResearchStatus.STOPPED),
            ResearchStatus.PAUSE, EnumSet.of(ResearchStatus.RUNNING, ResearchStatus.STOPPED),
            ResearchStatus.STOPPED, EnumSet.of(ResearchStatus.ARCHIVE),
            ResearchStatus.ARCHIVE, EnumSet.noneOf(ResearchStatus.class)
    );

    private Research research;

    @BeforeEach
    void setUp() {
        research = new Research(
                UUID.randomUUID(), "Исследование", "Описание", "Заказчик", TypeResearch.CPSK);
    }

    @Nested
    class Constructor {
        /** createdAtResearch выставляет только Hibernate при persist — конструктор не трогает. */
        @Test
        void constructor_doesNotSetCreatedAt() {
            assertThat(research.getCreatedAtResearch()).isNull();
        }

        /** Обязательные поля защищены requireNonNull — выровнено со схемой. */
        @Test
        void requiredFields_nullRejected() {
            assertThatThrownBy(() -> new Research(
                    null, "R", "d", "c", TypeResearch.CPSK))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idResearch");

            assertThatThrownBy(() -> new Research(
                    UUID.randomUUID(), null, "d", "c", TypeResearch.CPSK))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("nameResearch");

            assertThatThrownBy(() -> new Research(
                    UUID.randomUUID(), "R", "d", "c", null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("typeResearch");
        }

        /** Конструктор ставит стартовый статус DRAFT. */
        @Test
        void newResearch_startsInDraft() {
            assertThat(research.getStatusResearch()).isEqualTo(ResearchStatus.DRAFT);
        }
    }

    @Nested
    class ChangeStatusMatrix {

        /**
         * Полный прогон матрицы: для каждой пары «откуда → куда» свежая
         * сущность приводится к статусу-источнику прямым сеттером, затем
         * вызывается changeStatus. Разрешённая пара — статус меняется;
         * запрещённая — исключение с сообщением и статус на месте.
         * Сообщение проверяем один раз ниже (rejectionMessage), здесь —
         * только факт отказа.
         */
        @Test
        void fullMatrix_matchesExpectedTransitions() {
            for (var from : ResearchStatus.values()) {
                for (var to : ResearchStatus.values()) {
                    Research r = new Research(
                            UUID.randomUUID(), "R", "d", "c", TypeResearch.CPSK);
                    r.setStatusResearch(from);

                    if (EXPECTED_TRANSITIONS.get(from).contains(to)) {
                        r.changeStatus(to);
                        assertThat(r.getStatusResearch()).isEqualTo(to);
                    } else {
                        assertThatThrownBy(() -> r.changeStatus(to))
                                .isInstanceOf(IllegalStateException.class);
                        assertThat(r.getStatusResearch()).isEqualTo(from);
                    }
                }
            }
        }

        /** Сообщение отказа называет оба статуса — от и к. */
        @Test
        void rejectionMessage_namesBothStatuses() {
            research.setStatusResearch(ResearchStatus.NEW);

            assertThatThrownBy(() -> research.changeStatus(ResearchStatus.STOPPED))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("NEW")
                    .hasMessageContaining("STOPPED");
        }

        /** Тот же статус «в себя» — запрещён (из матрицы: пустые диагонали). */
        @Test
        void sameStatus_isForbidden() {
            research.setStatusResearch(ResearchStatus.DRAFT);

            assertThatThrownBy(() -> research.changeStatus(ResearchStatus.DRAFT))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Timestamps {

        /** Переход в RUNNING ставит startTimeResearch, если он пуст. */
        @Test
        void running_setsStartTime() {
            research.setStatusResearch(ResearchStatus.NEW);

            var before = LocalDateTime.now().minusSeconds(1);
            research.changeStatus(ResearchStatus.RUNNING);

            assertThat(research.getStartTimeResearch()).isNotNull();
            assertThat(research.getStartTimeResearch()).isAfter(before);
        }

        /** Переходы не в RUNNING и не в STOPPED таймстемпы не трогают. */
        @Test
        void draftToNew_setsNoTimestamps() {
            research.setStatusResearch(ResearchStatus.DRAFT);

            research.changeStatus(ResearchStatus.NEW);

            assertThat(research.getStartTimeResearch()).isNull();
            assertThat(research.getFinishTimeResearch()).isNull();
        }

        /** Переход в STOPPED ставит finishTimeResearch. */
        @Test
        void stopped_setsFinishTime() {
            research.setStatusResearch(ResearchStatus.RUNNING);

            var before = LocalDateTime.now().minusSeconds(1);
            research.changeStatus(ResearchStatus.STOPPED);

            assertThat(research.getFinishTimeResearch()).isNotNull();
            assertThat(research.getFinishTimeResearch()).isAfter(before);
        }

        /**
         * Зеркало защиты старта: finishTimeResearch, выставленный до
         * STOPPED, НЕ перезаписывается. Контракт: момент завершения
         * фиксируется один раз и живёт в ARCHIVE.
         */
        @Test
        void stopped_doesNotOverwriteExistingFinishTime() {
            var existing = LocalDateTime.now().minusDays(3);
            research.setStatusResearch(ResearchStatus.RUNNING);
            research.setFinishTimeResearch(existing);

            research.changeStatus(ResearchStatus.STOPPED);

            assertThat(research.getFinishTimeResearch()).isEqualTo(existing);
        }


        /**
         * Защита первого запуска: если startTimeResearch уже стоит (например,
         * выставлен вручную или сущность восстановлена), переход в RUNNING
         * его НЕ перезаписывает. Условие «if (newStatus == RUNNING &&
         * startTimeResearch == null)» прямо зашито в код — тест фиксирует
         * его как контракт.
         */
        @Test
        void running_doesNotOverwriteExistingStartTime() {
            var existing = LocalDateTime.now().minusDays(3);
            research.setStatusResearch(ResearchStatus.NEW);
            research.setStartTimeResearch(existing);

            research.changeStatus(ResearchStatus.RUNNING);

            assertThat(research.getStartTimeResearch()).isEqualTo(existing);
        }

        /** Переход в PAUSE таймстемпов не ставит, старт с RUNNING сохраняется. */
        @Test
        void pause_doesNotTouchTimestamps() {
            research.setStatusResearch(ResearchStatus.NEW);
            research.changeStatus(ResearchStatus.RUNNING);
            var startedAt = research.getStartTimeResearch();

            research.changeStatus(ResearchStatus.PAUSE);
            research.changeStatus(ResearchStatus.RUNNING);

            assertThat(research.getStartTimeResearch()).isEqualTo(startedAt);
            assertThat(research.getFinishTimeResearch()).isNull();
        }

    }

    @Nested
    class CanBeDeleted {

        /**
         * Контракт: удаление разрешено в DRAFT, NEW, STOPPED, ARCHIVE —
         * статусах без активного прохождения. Запрещено в RUNNING и PAUSE —
         * пока опрос идёт или стоит на паузе, сессии респондентов активны.
         * Тест перебирает все статусы enum'а: новый статус не сможет
         * протихоньку войти в «разрешённые» без правки этого перечня.
         */
        @Test
        void deletableStatuses_matchContract() {
            assertThat(ResearchStatus.values())
                    .extracting(ResearchStatus::canBeDeleted)
                    .containsExactly(
                            true,   // DRAFT
                            true,   // NEW
                            false,  // RUNNING
                            false,  // PAUSE
                            true,   // STOPPED
                            true    // ARCHIVE
                    );
        }
    }

    @Nested
    class PublicToken {

        /**
         * Контракт токена: создаётся при первом входе в RUNNING, живёт через
         * PAUSE (та же ссылка у респондентов), обнуляется в STOPPED навсегда.
         * Формат — 32 hex-символа (UUID без дефисов): проверяем, чтобы
         * случайно не подменить генератор на что-то с дефисами или другой длины.
         */
        @Test
        void firstRunning_createsToken32HexChars() {
            research.setStatusResearch(ResearchStatus.NEW);

            research.changeStatus(ResearchStatus.RUNNING);

            assertThat(research.getPublicToken())
                    .isNotNull()
                    .hasSize(32)
                    .matches("[0-9a-f]{32}");
        }

        /** Повторный вход в RUNNING (после PAUSE) НЕ перегенерирует токен. */
        @Test
        void pauseAndResume_keepsSameToken() {
            research.setStatusResearch(ResearchStatus.NEW);
            research.changeStatus(ResearchStatus.RUNNING);
            var token = research.getPublicToken();

            research.changeStatus(ResearchStatus.PAUSE);
            assertThat(research.getPublicToken()).isEqualTo(token); // пауза не трогает

            research.changeStatus(ResearchStatus.RUNNING);
            assertThat(research.getPublicToken()).isEqualTo(token); // возобновление — та же ссылка
        }

        /** STOPPED отзывает ссылку насовсем. */
        @Test
        void stopped_revokesToken() {
            research.setStatusResearch(ResearchStatus.NEW);
            research.changeStatus(ResearchStatus.RUNNING);

            research.changeStatus(ResearchStatus.STOPPED);

            assertThat(research.getPublicToken()).isNull();
        }

        /** STOPPED → ARCHIVE терминален, токен не возвращается. */
        @Test
        void afterStopped_tokenStaysNull() {
            research.setStatusResearch(ResearchStatus.RUNNING);
            research.changeStatus(ResearchStatus.STOPPED);

            research.changeStatus(ResearchStatus.ARCHIVE);

            assertThat(research.getPublicToken()).isNull();
        }

        /** Запрещённый переход не создаёт токен как побочный эффект. */
        @Test
        void invalidTransition_doesNotCreateToken() {
            research.setStatusResearch(ResearchStatus.DRAFT);

            assertThatThrownBy(() -> research.changeStatus(ResearchStatus.RUNNING))
                    .isInstanceOf(IllegalStateException.class);

            assertThat(research.getPublicToken()).isNull();
        }
    }

    @Nested
    class JpaLifecycle {

        /** NoArgsConstructor (для JPA): id остаётся null, генерации на месте нет. */
        @Test
        void noArgsConstructor_idStaysNull() {
            assertThat(new Research().getIDResearch()).isNull();
        }

        /**
         * Коллекция uniqueSurveyCodes инициализирована пустым списком:
         * null недопустим до подмены JPA-провайдером при загрузке.
         */
        @Test
        void noArgsConstructor_uniqueCodesEmptyList() {
            assertThat(new Research().getUniqueSurveyCodes())
                    .isNotNull()
                    .isEmpty();
        }
    }

    @Nested
    class EqualityContract {

        /**
         * Равенство только по id: два исследования с одинаковым id, но
         * разными полями — это одна и та же строка (onlyExplicitlyIncluded).
         */
        @Test
        void equals_sameIdDifferentFields_true() {
            var a = new Research(
                    UUID.randomUUID(), "Первое", "d1", "c1", TypeResearch.CPSK);
            var b = new Research(
                    a.getIDResearch(), "Второе", "d2", "c2", TypeResearch.CPSK);

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные исследования, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new Research(
                    UUID.randomUUID(), "R", "d", "c", TypeResearch.CPSK);
            var b = new Research(
                    UUID.randomUUID(), "R", "d", "c", TypeResearch.CPSK);

            assertThat(a).isNotEqualTo(b);
        }
    }



}
