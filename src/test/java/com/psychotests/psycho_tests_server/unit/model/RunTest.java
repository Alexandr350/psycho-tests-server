package com.psychotests.psycho_tests_server.unit.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.UUID;

import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Run;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты Run — «запуск исследования» (волна сбора данных). Один research
 * может иметь несколько запусков; ответы респондентов привязываются к запуску
 * через его токен — запуски изолированы друг от друга.
 *
 * <p>Что проверяем: контракты конструктора — id, research и publicToken
 * обязательны (Objects.requireNonNull с англоязычными сообщениями, в стиле
 * answer-сущностей); startTimeRun НЕ передаётся параметром — конструктор
 * ставит LocalDateTime.now() сам (в отличие от UUID, который по правилу
 * проекта генерируется в точке создания); finish() закрывает запуск
 * переданным временем, повторное завершение — IllegalStateException
 * с русскоязычным сообщением; isOpen() — обратная сторона finishTimeRun;
 * мутаторов у id/publicToken/startTime/finishTime нет
 * (@Setter(AccessLevel.NONE)) — токен неизменяем после создания: его смена
 * рвала бы связь ответов с волной; равенство только по id;
 * protected-конструктор без аргументов существует только для Hibernate.</p>
 *
 * <p>Зафиксированные решения:
 * 1) Повторный finish() — ошибка, а не no-op: перезапуск запуска невозможен
 * (STOPPED → RUNNING запрещён и на уровне Research.changeStatus, а syncRuns
 * закрывает только открытый запуск). Если решите сделать finish идемпотентным
 * — тест перевернётся и напомнит.
 * 2) finish(null) допустим формально (проверок на null нет): по схеме
 * finish_time_run nullable, а syncRuns всегда передаёт research
 * getFinishTimeResearch(). Null-финиш делает запуск формально закрытым
 * с пустым временем — если это нежелательно, добавить requireNonNull,
 * тест перевернётся.
 * 3) Токен запуска денормализован в Research.publicToken (см.
 * ResearchService.syncRuns): согласованность двух копий — территория
 * сервисных тестов, здесь она не проверяется.</p>
 *
 * <p>Граница с другими тестами: связь с Research тестируется только как
 * ссылка (isSameAs/requireNonNull) — целостность FK id_research и каскад
 * {@code @OnDelete(action = OnDeleteAction.CASCADE)} (снос research убирает
 * его запуски) покрывается {@code @DataJpaTest}. Отношение к
 * ResearchService.syncRuns ...
 */
class RunTest {

    private Research research;
    private UUID id;
    private String token;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        token = UUID.randomUUID().toString().replace("-", "");
        research = mock(Research.class);
    }

    @Nested
    class ConstructorContracts {

        /** Полное построение: все передаваемые поля делегируются без изменений. */
        @Test
        void constructor_setsAllFields() {
            var run = new Run(id, research, token);

            assertThat(run.getIdRun()).isEqualTo(id);
            assertThat(run.getResearch()).isSameAs(research);
            assertThat(run.getPublicToken()).isEqualTo(token);
            assertThat(run.getFinishTimeRun()).isNull();
        }

        /** id обязателен — null отвергается с сообщением про idRun. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new Run(null, research, token))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idRun");
        }

        /** Research обязателен — запуск не существует вне исследования. */
        @Test
        void research_nullRejected() {
            assertThatThrownBy(() -> new Run(id, null, token))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("research");
        }

        /** Токен обязателен — без него запуск недоступен респондентам. */
        @Test
        void publicToken_nullRejected() {
            assertThatThrownBy(() -> new Run(id, research, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("publicToken");
        }

        /**
         * startTimeRun не передаётся параметром: конструктор ставит
         * LocalDateTime.now() сам. Проверяем попадание в окно — точное
         * время не фиксируем, оно постоянно «уплывает» между assertion'ами.
         */
        @Test
        void constructor_setsStartTimeToNow() {
            var before = LocalDateTime.now().minusSeconds(1);

            var run = new Run(id, research, token);

            var after = LocalDateTime.now().plusSeconds(1);
            assertThat(run.getStartTimeRun())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
        }
    }

    @Nested
    class FinishContract {

        /**
         * finish() фиксирует момент завершения сам — время снаружи
         * не передаётся и задним числом не выставляется. Проверяем
         * попадание в окно: точное значение now() «уплывает» между
         * операторами, контрактом является только сам факт фиксации.
         */
        @Test
        void finish_setsFinishTimeToNow() {
            var run = new Run(id, research, token);
            var before = LocalDateTime.now().minusSeconds(1);

            run.finish();

            var after = LocalDateTime.now().plusSeconds(1);
            assertThat(run.getFinishTimeRun())
                    .isAfterOrEqualTo(before)
                    .isBeforeOrEqualTo(after);
            assertThat(run.isOpen()).isFalse();
        }

        /**
         * Повторное завершение — ошибка, а не no-op: перезапуск невозможен.
         * syncRuns закрывает только незакрытый запуск, но защита живёт
         * в сущности: повторный вызов не затирает первое время закрытия
         * (исключение выбрасывается до присваивания).
         */
        @Test
        void finish_secondCall_throws() {
            var run = new Run(id, research, token);
            run.finish();
            var firstFinish = run.getFinishTimeRun();

            assertThatThrownBy(run::finish)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("завершён");

            assertThat(run.getFinishTimeRun()).isEqualTo(firstFinish);
        }

    }

    @Nested
    class IsOpenContract {

        /** Свежий запуск открыт. */
        @Test
        void freshRun_isOpen() {
            assertThat(new Run(id, research, token).isOpen()).isTrue();
        }

        /** Закрытый запуск не открыт; isOpen — прямая проекция finishTimeRun. */
        @Test
        void finishedRun_isNotOpen() {
            var run = new Run(id, research, token);

            run.finish();

            assertThat(run.isOpen()).isFalse();
        }
    }


    @Nested
    class ImmutabilityContract {

        /**
         * У полей idRun, publicToken, startTimeRun, finishTimeRun нет
         * публичных сеттеров (@Setter(AccessLevel.NONE)): токен неизменяем
         * после создания — его смена рвала бы связь ответов с волной;
         * время старта не двигается задним числом. Reflection-тест не даст
         * вернуть сеттер незаметно.
         */
        @Test
        void immutableFields_haveNoPublicSetters() {
            var setterNames = java.util.Arrays.stream(Run.class.getMethods())
                    .map(Method::getName)
                    .filter(name -> name.startsWith("set"))
                    .toList();

            assertThat(setterNames).isEmpty();
        }

    }

    @Nested
    class EqualityContract {

        /**
         * Равенство только по id: два запуска с одинаковым id, но разными
         * полями — это одна и та же строка (onlyExplicitlyIncluded).
         */
        @Test
        void equals_sameIdDifferentFields_true() {
            var a = new Run(id, research, token);
            var b = new Run(id, mock(Research.class), "other-token");

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные запуски, независимо от остальных полей. */
        @Test
        void equals_differentIds_false() {
            var a = new Run(id, research, token);
            var b = new Run(UUID.randomUUID(), research, token);

            assertThat(a).isNotEqualTo(b);
        }
    }

    @Nested
    class JpaLifecycle {

        /**
         * Protected-конструктор без аргументов существует только для
         * Hibernate: из прикладного кода недоступен (другой пакет), поэтому
         * проверяем через reflection — конструктор на месте, не публичный,
         * и создаёт объект с null-id (генерации на месте нет).
         */
        @Test
        void noArgsConstructor_existsProtectedAndLeavesIdNull() throws Exception {
            Constructor<Run> constructor = Run.class.getDeclaredConstructor();
            assertThat(constructor.getModifiers() & Modifier.PUBLIC)
                    .as("no-arg конструктор не должен быть публичным — только для Hibernate")
                    .isZero();

            constructor.setAccessible(true);
            Run run = constructor.newInstance();

            assertThat(run.getIdRun()).isNull();
            assertThat(run.getPublicToken()).isNull();
            assertThat(run.getStartTimeRun()).isNull();
        }

        /**
         * Финишное время через JPA-конструктор — null: закрытый запуск
         * загружается из БД с выставленным Hibernate значением поля.
         */
        @Test
        void noArgsConstructor_isOpen() throws Exception {
            Constructor<Run> constructor = Run.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Run run = constructor.newInstance();

            assertThat(run.isOpen()).isTrue();
        }
    }
}
