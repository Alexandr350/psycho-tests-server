package com.psychotests.psycho_tests_server.unit.model;

import java.time.Instant;
import java.util.UUID;

import com.psychotests.psycho_tests_server.model.Respondent;
import com.psychotests.psycho_tests_server.model.Research;
import com.psychotests.psycho_tests_server.model.Run;
import com.psychotests.psycho_tests_server.model.UniqueSurveyCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты UniqueSurveyCode — «уникальный код доступа»: токен для
 * входа респондента в опрос. Рождается с датой выдачи (issuedAt
 * инициализируется Instant.now() в объявлении поля, сеттера нет),
 * привязывается к запуску (Run), исследованию и респонденту сеттерами;
 * получает дату использования usedAt в момент первого входа.
 *
 * <p>Что проверяем: контракт конструктора — id, code и research
 * обязательны (requireNonNull с англоязычными сообщениями в стиле
 * семьи); respondent опционален — код выдаётся до того, как его
 * заберёт респондент (двухшаговый жизненный цикл: выдан → привязан →
 * использован); run в конструктор не входит и привязывается сеттером;
 * issuedAt всегда установлен и не переприсваивается; usedAt null,
 * пока код не использован; равенство только по id; NoArgsConstructor —
 * все поля null, кроме issuedAt.</p>
 *
 * <p>Зафиксированные решения:
 * 1) issuedAt задаётся один раз при рождении (инициализатор поля) —
 * сеттер закрыт ({@code @Setter(AccessLevel.NONE)}), updatable = false;
 * время выдачи не подделывается сервисом, как startTimeRun у запуска.
 * 2) Уникальность code живёт в схеме (unique = true на колонке) и в БД —
 * конструктором не проверяется; аналог uk_psychologist_email.
 * 3) research обязательна в конструкторе, хотя колонка id_research
 * в схеме nullable: код выпускается в рамках запуска существующего
 * исследования, «осиротевший» код — ошибка вызова. Сущность строже
 * схемы — сознательное решение, выравнивание при желании делается
 * через nullable = false в миграции.
 * 4) Формат и непустота code не проверяются сущностью — забота
 * сервиса/DTO; сущность хранит код как есть.</p>
 *
 * <p>Граница с другими тестами: уникальность code и целостность
 * join-колонок (id_run, id_research, id_respondent) покрываются
 * {@code @DataJpaTest} вместе с UniqueSurveyCodeRepository. Research,
 * Respondent и Run подменяются моками.</p>
 */
class UniqueSurveyCodeTest {

    private UUID id;
    private Research research;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
        research = mock(Research.class);
    }

    @Nested
    class ConstructorContracts {

        /** Конструктор задаёт id, код и исследование; issuedAt уже проставлен. */
        @Test
        void constructor_setsAllFields() {
            var respondent = mock(Respondent.class);
            var codeEntity = new UniqueSurveyCode(id, "AB12-CD34", research, respondent);

            assertThat(codeEntity.getIdUniqueSurveyCode()).isEqualTo(id);
            assertThat(codeEntity.getCode()).isEqualTo("AB12-CD34");
            assertThat(codeEntity.getResearch()).isSameAs(research);
            assertThat(codeEntity.getRespondent()).isSameAs(respondent);
            assertThat(codeEntity.getRun()).isNull();
            assertThat(codeEntity.getIssuedAt()).isNotNull();
            assertThat(codeEntity.getUsedAt()).isNull();
        }

        /** id обязателен — null отвергается с сообщением про idUniqueSurveyCode. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new UniqueSurveyCode(null, "AB12-CD34", research, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idUniqueSurveyCode");
        }

        /** code обязателен — код без значения не выдаётся. */
        @Test
        void code_nullRejected() {
            assertThatThrownBy(() -> new UniqueSurveyCode(id, null, research, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("code");
        }

        /** research обязательна — код не существует вне исследования. */
        @Test
        void research_nullRejected() {
            assertThatThrownBy(() -> new UniqueSurveyCode(id, "AB12-CD34", null, null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("research");
        }

        /** respondent опционален: код легален до того, как его забрал респондент. */
        @Test
        void respondent_nullAllowed() {
            var codeEntity = new UniqueSurveyCode(id, "AB12-CD34", research, null);

            assertThat(codeEntity.getRespondent()).isNull();
        }

        /**
         * Сущность хранит код как есть, без обрезки и без проверки длины:
         * формат кода — забота сервиса/DTO, схема держит unique = true.
         */
        @Test
        void code_keptAsIs_validationAtService() {
            var codeEntity = new UniqueSurveyCode(id, "AB12-CD34", research, null);
            var longCode = "X".repeat(100);

            codeEntity.setCode(longCode);

            assertThat(codeEntity.getCode()).hasSize(100);
        }
    }

    @Nested
    class IssuedAt {

        /**
         * Дата выдачи проставляется инициализатором поля в момент создания —
         * в том числе через NoArgsConstructor: строка без issued_at в БД
         * невозможна. Сеттера нет ({@code @Setter(AccessLevel.NONE)}) —
         * подделать нельзя.
         */
        @Test
        void issuedAt_setAtBirth_andImmutable() {
            var before = Instant.now().minusSeconds(5);

            var codeEntity = new UniqueSurveyCode(id, "AB12-CD34", research, null);
            var fromNoArgs = new UniqueSurveyCode();

            assertThat(codeEntity.getIssuedAt()).isAfterOrEqualTo(before);
            assertThat(fromNoArgs.getIssuedAt()).isNotNull();
        }
    }

    @Nested
    class Binding {

        /** Привязка к запуску — сеттером: код выдан в рамках волны. */
        @Test
        void run_boundAfterCreation() {
            var codeEntity = new UniqueSurveyCode(id, "AB12-CD34", research, null);
            var run = mock(Run.class);

            codeEntity.setRun(run);

            assertThat(codeEntity.getRun()).isSameAs(run);
        }

        /** Привязка к исследованию — сеттером (перепривязка не закрыта). */
        @Test
        void research_boundAfterCreation() {
            var codeEntity = new UniqueSurveyCode(id, "AB12-CD34", research, null);
            var otherResearch = mock(Research.class);

            codeEntity.setResearch(otherResearch);

            assertThat(codeEntity.getResearch()).isSameAs(otherResearch);
        }

        /** Привязка к респонденту — сеттером, независимо от research. */
        @Test
        void respondent_boundAfterCreation() {
            var codeEntity = new UniqueSurveyCode(id, "AB12-CD34", research, null);
            var respondent = mock(Respondent.class);

            codeEntity.setRespondent(respondent);

            assertThat(codeEntity.getRespondent()).isSameAs(respondent);
        }

        /**
         * Момент использования: usedAt заполняется сервисом при первом
         * входе по коду; до этого — null, «не использован».
         */
        @Test
        void usedAt_nullMeansUnused_thenSet() {
            var codeEntity = new UniqueSurveyCode(id, "AB12-CD34", research, null);

            assertThat(codeEntity.getUsedAt()).isNull();

            var usedAt = Instant.now();
            codeEntity.setUsedAt(usedAt);

            assertThat(codeEntity.getUsedAt()).isEqualTo(usedAt);
        }
    }

    @Nested
    class EqualityContract {

        /** Равенство только по id: одинаковый id, разные коды — одна строка. */
        @Test
        void equals_sameIdDifferentFields_true() {
            var a = new UniqueSurveyCode(id, "AB12-CD34", research, null);
            var b = new UniqueSurveyCode(id, "ZZ99-WX88", mock(Research.class),
                    mock(Respondent.class));

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные коды, даже с одинаковым значением code. */
        @Test
        void equals_differentIds_false() {
            var a = new UniqueSurveyCode(id, "AB12-CD34", research, null);
            var b = new UniqueSurveyCode(UUID.randomUUID(), "AB12-CD34", research, null);

            assertThat(a).isNotEqualTo(b);
        }
    }

    @Nested
    class JpaLifecycle {

        /**
         * NoArgsConstructor (для JPA) оставляет id, код, привязки и usedAt
         * null — провайдер и сервис дозаполнят. issuedAt уже проставлен
         * инициализатором: nullable = false держится даже на «пустом»
         * объекте.
         */
        @Test
        void noArgsConstructor_fieldsStayNullExceptIssuedAt() {
            var codeEntity = new UniqueSurveyCode();

            assertThat(codeEntity.getIdUniqueSurveyCode()).isNull();
            assertThat(codeEntity.getCode()).isNull();
            assertThat(codeEntity.getRun()).isNull();
            assertThat(codeEntity.getResearch()).isNull();
            assertThat(codeEntity.getRespondent()).isNull();
            assertThat(codeEntity.getUsedAt()).isNull();
            assertThat(codeEntity.getIssuedAt()).isNotNull();
        }
    }
}
