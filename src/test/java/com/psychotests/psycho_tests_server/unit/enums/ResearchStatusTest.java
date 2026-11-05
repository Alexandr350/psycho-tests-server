package com.psychotests.psycho_tests_server.unit.enums;

import com.psychotests.psycho_tests_server.enums.ResearchStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Юнит-тесты ResearchStatus — конечный автомат жизненного цикла исследования.
 *
 * <p>Что проверяем: две группы правил. canBeDeleted() — разрешённые статусы
 * удаления (DRAFT, NEW, STOPPED, ARCHIVE) и запрещённые (RUNNING, PAUSE:
 * активное или приостановленное исследование не удаляется, его нужно
 * сначала остановить). canTransitionTo() — полная таблица переходов:
 * DRAFT → NEW → RUNNING ⇄ PAUSE, RUNNING/PAUSE → STOPPED, STOPPED → ARCHIVE,
 * ARCHIVE — терминальный статус без выходов.</p>
 *
 * <p>Как работает: enum без зависимостей, чистые функции. Отдельно стоит
 * защитный тест «каждый статус имеет правила переходов»: карта ALLOWED
 * закрыта private, а canTransitionTo делает ALLOWED.get(this) без проверки
 * на null — если в enum добавят новый статус и забудут про карту, метод
 * начнёт бросать NPE. Тест ловит это на всех статусах сразу.</p>
 *
 * <p>Граница с другими тестами: реакция сервиса на canBeDeleted() == false —
 * в ResearchServiceTest; применение правил переходов на реальной сущности —
 * в тестах Research (метод changeStatus); интеграционные сценарии удаления
 * по статусам — в DeleteResearchFlowTest.</p>
 */
class ResearchStatusTest {

    @ParameterizedTest
    @EnumSource(names = {"DRAFT", "NEW", "STOPPED", "ARCHIVE"})
    void canBeDeleted_true(ResearchStatus status) {
        assertThat(status.canBeDeleted()).isTrue();
    }

    @ParameterizedTest
    @EnumSource(names = {"RUNNING", "PAUSE"})
    void canBeDeleted_false(ResearchStatus status) {
        assertThat(status.canBeDeleted()).isFalse();
    }

    /**
     * Эталонная таблица переходов. Тест проходит по всем парам (из, в) и
     * сверяет canTransitionTo с эталоном — так добавление перехода в код
     * без обновления эталона (или наоборот) честно краснеет.
     */
    @Test
    void transitions_matchStateMachine() {
        Map<ResearchStatus, Set<ResearchStatus>> expected = Map.of(
                ResearchStatus.DRAFT,   EnumSet.of(ResearchStatus.NEW),
                ResearchStatus.NEW,     EnumSet.of(ResearchStatus.RUNNING),
                ResearchStatus.RUNNING, EnumSet.of(ResearchStatus.PAUSE, ResearchStatus.STOPPED),
                ResearchStatus.PAUSE,   EnumSet.of(ResearchStatus.RUNNING, ResearchStatus.STOPPED),
                ResearchStatus.STOPPED, EnumSet.of(ResearchStatus.ARCHIVE),
                ResearchStatus.ARCHIVE, EnumSet.noneOf(ResearchStatus.class));

        for (var from : ResearchStatus.values()) {
            for (var to : ResearchStatus.values()) {
                assertThat(from.canTransitionTo(to))
                        .as("%s -> %s", from, to)
                        .isEqualTo(expected.get(from).contains(to));
            }
        }
    }

    /**
     * Защита от забытого статуса: для каждого значения enum canTransitionTo
     * не должен бросать NPE ни с одним аргументом. Ловит случай, когда в
     * enum добавили статус, но не вписали его в ALLOWED.
     */
    @ParameterizedTest
    @EnumSource(ResearchStatus.class)
    void canTransitionTo_neverThrows_forAnyStatusPair(ResearchStatus from) {
        for (var to : ResearchStatus.values()) {
            assertThatCode(() -> from.canTransitionTo(to)).doesNotThrowAnyException();
        }
    }
}
