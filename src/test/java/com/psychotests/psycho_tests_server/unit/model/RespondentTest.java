package com.psychotests.psycho_tests_server.unit.model;

import java.util.UUID;

import com.psychotests.psycho_tests_server.model.Respondent;
import com.psychotests.psycho_tests_server.model.SurveySession;
import com.psychotests.psycho_tests_server.model.UniqueSurveyCode;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;

/**
 * Юнит-тесты Respondent — «респондент»: участник опросов, которого
 * идентифицирует UUID; профильные поля (ФИО, возраст, пол) опциональны.
 * Держит две обратные стороны OneToMany: сессии опросов (SurveySession)
 * и уникальные коды доступа (UniqueSurveyCode).
 *
 * <p>Что проверяем: контракт конструктора — id обязателен
 * (requireNonNull с англоязычным сообщением в стиле answer-сущностей);
 * id защищён Setter NONE — как во всей семье сущностей, переприсвоить
 * его нельзя; анонимность — все профильные поля по умолчанию null,
 * это зафиксированное решение; пол — однобуквенный код под колонку
 * length = 1, формат валидирует DTO; коллекции инициализированы пустыми
 * списками; равенство только по id; NoArgsConstructor — id остаётся null.</p>
 *
 * <p>Зафиксированные решения:
 * 1) Профиль опционален по дизайну: опросы бывают анонимными (профиль
 * не заполняется) и не-анонимными (заполняется сеттерами после создания).
 * Оба сценария легальны на уровне сущности — обязательность полей решает
 * слой использования, а не конструктор.
 * 2) Пол — работа DTO: сущность хранит однобуквенный код как есть.
 * 3) Каскада на коллекциях нет: сессии и коды сохраняются своими
 * репозиториями; orphanRemoval не задан — дефолт false, удаление из
 * коллекции не удаляет строку из БД.
 * 4) Две коллекции независимы: сессии и коды не связаны между собой.</p>
 *
 * <p>Граница с другими тестами: целостность join-колонок покрывается
 * @DataJpaTest вместе с SurveySessionRepository; SurveySession и
 * UniqueSurveyCode подменяются моками.</p>
 */
class RespondentTest {

    private UUID id;

    @BeforeEach
    void setUp() {
        id = UUID.randomUUID();
    }

    @Nested
    class ConstructorContracts {

        /** Конструктор задаёт только id — профиль остаётся пустым. */
        @Test
        void constructor_setsIdProfileStaysNull() {
            var respondent = new Respondent(id);

            assertThat(respondent.getIdRespondent()).isEqualTo(id);
            assertThat(respondent.getNameRespondent()).isNull();
            assertThat(respondent.getLastNameRespondent()).isNull();
            assertThat(respondent.getAgeRespondent()).isNull();
            assertThat(respondent.getGenderRespondent()).isNull();
        }

        /** id обязателен — null отвергается с сообщением про idRespondent. */
        @Test
        void id_nullRejected() {
            assertThatThrownBy(() -> new Respondent(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("idRespondent");
        }
    }

    @Nested
    class AnonymityProfile {

        /**
         * Зафиксированное решение: профиль опционален — анонимные опросы
         * опираются на UUID, не-анонимные дозаполняют профиль сеттерами.
         */
        @Test
        void profileFields_nullByDefault() {
            var respondent = new Respondent(UUID.randomUUID());

            assertThat(respondent.getNameRespondent()).isNull();
            assertThat(respondent.getLastNameRespondent()).isNull();
            assertThat(respondent.getAgeRespondent()).isNull();
            assertThat(respondent.getGenderRespondent()).isNull();
        }

        /** Сеттеры заполняют профиль по частям — без обязательного минимума. */
        @Test
        void setters_fillProfilePartially() {
            var respondent = new Respondent(id);

            respondent.setNameRespondent("Иван");
            respondent.setAgeRespondent(30);

            assertThat(respondent.getNameRespondent()).isEqualTo("Иван");
            assertThat(respondent.getAgeRespondent()).isEqualTo(30);
            assertThat(respondent.getLastNameRespondent()).isNull();
            assertThat(respondent.getGenderRespondent()).isNull();
        }

        /**
         * Возраст — Integer, а не int: null легален и по типу, и по схеме.
         * Тест фиксирует, что обёртка выбрана не случайно.
         */
        @Test
        void age_nullCyclesWithInteger() {
            var respondent = new Respondent(id);

            respondent.setAgeRespondent(25);
            assertThat(respondent.getAgeRespondent()).isEqualTo(25);

            respondent.setAgeRespondent(null);
            assertThat(respondent.getAgeRespondent()).isNull();
        }
    }

    @Nested
    class Collections {

        /** Коллекция сессий инициализирована пустым списком, а не null. */
        @Test
        void surveySessions_defaultEmptyList() {
            assertThat(new Respondent(id).getSurveySessions())
                    .isNotNull()
                    .isEmpty();
        }

        /**
         * Живая ссылка: добавление сессии видно через геттер. Каскада нет —
         * сессия сохраняется своим репозиторием, не через респондента.
         */
        @Test
        void surveySessions_liveReference() {
            var respondent = new Respondent(id);
            var session = mock(SurveySession.class);

            respondent.getSurveySessions().add(session);

            assertThat(respondent.getSurveySessions())
                    .containsExactly(session);
        }

        /** Коллекция кодов инициализирована пустым списком, а не null. */
        @Test
        void surveyCodes_defaultEmptyList() {
            assertThat(new Respondent(id).getSurveyCodes())
                    .isNotNull()
                    .isEmpty();
        }

        /** Коды добавляются независимо от сессий — две коллекции не связаны. */
        @Test
        void surveyCodes_liveReference() {
            var respondent = new Respondent(id);
            var code = mock(UniqueSurveyCode.class);

            respondent.getSurveyCodes().add(code);

            assertThat(respondent.getSurveyCodes())
                    .containsExactly(code);
            assertThat(respondent.getSurveySessions())
                    .isEmpty();
        }
    }

    @Nested
    class EqualityContract {

        /**
         * Равенство только по id: два респондента с одинаковым id, но разными
         * профилями — это одна и та же строка. Аннотация EqualsAndHashCode
         * с onlyExplicitlyIncluded гарантирует, что профиль не протекёт
         * в equals — важно для анонимных респондентов, у которых «поля»
         * просто отсутствуют.
         */
        @Test
        void equals_sameIdDifferentFields_true() {
            var a = new Respondent(id);
            a.setNameRespondent("Иван");
            var b = new Respondent(id);
            b.setNameRespondent("Мария");

            assertThat(a).isEqualTo(b);
            assertThat(a).hasSameHashCodeAs(b);
        }

        /** Разные id — разные респонденты, даже с пустыми профилями у обоих. */
        @Test
        void equals_differentIds_false() {
            var a = new Respondent(id);
            var b = new Respondent(UUID.randomUUID());

            assertThat(a).isNotEqualTo(b);
        }
    }

    @Nested
    class JpaLifecycle {

        /**
         * NoArgsConstructor (для JPA) создаёт объект с null-id — провайдер
         * заполнит его сам при persist. Тест фиксирует, что конструктор
         * не содержит скрытой логики: id не генерируется на месте.
         */
        @Test
        void noArgsConstructor_idStaysNull() {
            assertThat(new Respondent().getIdRespondent())
                    .isNull();
        }

        /** NoArgsConstructor инициализирует обе коллекции — null недопустим. */
        @Test
        void noArgsConstructor_collectionsEmpty() {
            var respondent = new Respondent();

            assertThat(respondent.getSurveySessions()).isNotNull().isEmpty();
            assertThat(respondent.getSurveyCodes()).isNotNull().isEmpty();
        }
    }
}
